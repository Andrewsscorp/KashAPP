package com.example.features.settings.security

import android.net.Uri
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * Implementación criptográfica real de RFC 6238 (TOTP: Time-Based One-Time Password)
 * y RFC 4226 (HOTP: HMAC-Based One-Time Password).
 *
 * Compatible al 100% con:
 * - Google Authenticator
 * - Microsoft Authenticator
 * - Authy / 1Password / Bitwarden
 */
object TotpTwoFactorAuthenticator {

    private const val SECRET_BYTES_LENGTH = 20 // 160 bits (Estándar RFC 6238 para HMAC-SHA1)
    private const val TIME_STEP_SECONDS = 30L
    private const val CODE_DIGITS = 6
    private const val HMAC_ALGO = "HmacSHA1"
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    private val secureRandom = SecureRandom()

    /**
     * Genera una semilla secreta criptográficamente aleatoria de 160 bits codificada en Base32 (32 caracteres).
     */
    fun generateSecretBase32(): String {
        val buffer = ByteArray(SECRET_BYTES_LENGTH)
        secureRandom.nextBytes(buffer)
        return encodeBase32(buffer)
    }

    /**
     * Construye el URI estándar `otpauth://totp/...` para abrir directamente en Google Authenticator / Authy
     * o renderizar en un código QR.
     */
    fun buildOtpAuthUri(
        accountEmail: String,
        secretBase32: String,
        issuer: String = "KashApp"
    ): String {
        val cleanAccount = accountEmail.trim().ifBlank { "usuario@kashapp.co" }
        val encodedIssuer = encodeUriComponent(issuer)
        val encodedAccount = encodeUriComponent(cleanAccount)
        return "otpauth://totp/$encodedIssuer:$encodedAccount?secret=$secretBase32&issuer=$encodedIssuer&algorithm=SHA1&digits=$CODE_DIGITS&period=$TIME_STEP_SECONDS"
    }

    private fun encodeUriComponent(value: String): String {
        return try {
            Uri.encode(value) ?: URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        } catch (_: Throwable) {
            URLEncoder.encode(value, "UTF-8").replace("+", "%20")
        }
    }

    /**
     * Genera el código TOTP de 6 dígitos para el instante dado (en milisegundos).
     */
    fun generateCurrentCode(
        secretBase32: String,
        timestampMs: Long = System.currentTimeMillis()
    ): String {
        val timeCounter = (timestampMs / 1000L) / TIME_STEP_SECONDS
        return generateCodeForCounter(secretBase32, timeCounter)
    }

    /**
     * Verifica si el código ingresado por el usuario de 6 dígitos es válido.
     * Permite una ventana de tolerancia de +/- [windowSteps] intervalos de 30s para compensar desfase de reloj.
     */
    fun verifyCode(
        secretBase32: String,
        userCode: String,
        timestampMs: Long = System.currentTimeMillis(),
        windowSteps: Int = 1
    ): Boolean {
        val sanitizedCode = userCode.trim().replace(" ", "")
        if (sanitizedCode.length != CODE_DIGITS || !sanitizedCode.all { it.isDigit() }) {
            return false
        }

        val currentCounter = (timestampMs / 1000L) / TIME_STEP_SECONDS
        for (offset in -windowSteps..windowSteps) {
            val candidate = generateCodeForCounter(secretBase32, currentCounter + offset)
            if (constantTimeEquals(candidate, sanitizedCode)) {
                return true
            }
        }
        return false
    }

    /**
     * Genera 8 códigos de respaldo de emergencia de un solo uso (formato XXXX-XXXX).
     */
    fun generateRecoveryCodes(count: Int = 8): List<String> {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Sin O, 0, I, 1 para evitar confusión visual
        return List(count) {
            val part1 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
            val part2 = (1..4).map { chars[secureRandom.nextInt(chars.length)] }.joinToString("")
            "$part1-$part2"
        }
    }

    /**
     * Segundos restantes de validez del código actual (1..30).
     */
    fun getRemainingSecondsInWindow(timestampMs: Long = System.currentTimeMillis()): Int {
        val elapsed = (timestampMs / 1000L) % TIME_STEP_SECONDS
        return (TIME_STEP_SECONDS - elapsed).toInt()
    }

    private fun generateCodeForCounter(secretBase32: String, counter: Long): String {
        val keyBytes = decodeBase32(secretBase32)
        if (keyBytes.isEmpty()) return "000000"

        val counterBytes = ByteBuffer.allocate(8).putLong(counter).array()
        val mac = Mac.getInstance(HMAC_ALGO)
        mac.init(SecretKeySpec(keyBytes, HMAC_ALGO))
        val hash = mac.doFinal(counterBytes)

        // Truncamiento dinámico según RFC 4226 Sección 5.4
        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        val otp = binary % 10.0.pow(CODE_DIGITS.toDouble()).toInt()
        return otp.toString().padStart(CODE_DIGITS, '0')
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diff = 0
        for (i in a.indices) {
            diff = diff or (a[i].code xor b[i].code)
        }
        return diff == 0
    }

    /**
     * Codificador RFC 4648 Base32 sin padding '='.
     */
    fun encodeBase32(data: ByteArray): String {
        if (data.isEmpty()) return ""
        val output = StringBuilder((data.size * 8 + 4) / 5)
        var buffer = data[0].toInt() and 0xFF
        var next = 1
        var bitsLeft = 8

        while (bitsLeft > 0 || next < data.size) {
            if (bitsLeft < 5) {
                if (next < data.size) {
                    buffer = (buffer shl 8) or (data[next++].toInt() and 0xFF)
                    bitsLeft += 8
                } else {
                    val pad = 5 - bitsLeft
                    buffer = buffer shl pad
                    bitsLeft += pad
                }
            }
            val index = (buffer shr (bitsLeft - 5)) and 0x1F
            bitsLeft -= 5
            output.append(BASE32_ALPHABET[index])
        }
        return output.toString()
    }

    /**
     * Decodificador RFC 4648 Base32.
     */
    fun decodeBase32(base32: String): ByteArray {
        val clean = base32.uppercase().replace(" ", "").replace("-", "").trimEnd('=')
        if (clean.isEmpty()) return ByteArray(0)

        val outLength = clean.length * 5 / 8
        val result = ByteArray(outLength)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (char in clean) {
            val value = BASE32_ALPHABET.indexOf(char)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                result[index++] = ((buffer shr (bitsLeft - 8)) and 0xFF).toByte()
                bitsLeft -= 8
            }
        }
        return result
    }
}
