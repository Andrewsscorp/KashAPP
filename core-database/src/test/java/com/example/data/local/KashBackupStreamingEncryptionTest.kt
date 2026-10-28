package com.example.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

/**
 * Suite de pruebas de cifrado y descifrado por streaming con compresión GZIP y AAD (OWASP MASVS-CRYPTO).
 */
class KashBackupStreamingEncryptionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var engine: KashBackupEncryptionEngine
    private val testPassphrase = "SuperSecurePassphrase2026!"

    @Before
    fun setUp() {
        engine = KashBackupEncryptionEngine()
    }

    @Test
    fun testStreamingEncryptAndDecryptRoundTripIntegrity() {
        val plainFile = tempFolder.newFile("sample_backup.json")
        val contentBuilder = StringBuilder()
        contentBuilder.append("{\"portfolio\":{\"accounts\":[")
        for (i in 1..200) {
            contentBuilder.append("{\"id\":\"acc_$i\",\"name\":\"Cuenta Bancaria $i\",\"balance\":${i * 15000.50}},")
        }
        contentBuilder.append("{\"id\":\"acc_final\",\"name\":\"Cierre\",\"balance\":1000000.0}]}}")
        val originalJson = contentBuilder.toString()
        plainFile.writeText(originalJson, Charsets.UTF_8)

        val encryptedFile = tempFolder.newFile("backup.kash_enc")
        val decryptedFile = tempFolder.newFile("restored_backup.json")

        // 1. Cifrar
        val encryptResult = engine.encryptBackupFile(plainFile, encryptedFile, testPassphrase)
        assertTrue("El cifrado debe ser exitoso", encryptResult.isSuccess)
        val metadata = encryptResult.getOrThrow()
        assertTrue("El archivo cifrado debe existir", metadata.outputFile.exists())
        assertTrue("La firma magica debe ser detectada", engine.isEncryptedKashBackup(encryptedFile))

        // 2. Descifrar
        val decryptResult = engine.decryptBackupFile(
            encryptedInputFile = encryptedFile,
            plainOutputFile = decryptedFile,
            passphrase = testPassphrase,
            expectedPlaintextSha256Hex = metadata.plaintextSha256Hex
        )
        assertTrue("El descifrado debe ser exitoso", decryptResult.isSuccess)

        val restoredJson = decryptedFile.readText(Charsets.UTF_8)
        assertEquals("El JSON restaurado debe ser idéntico al original", originalJson, restoredJson)
    }

    @Test
    fun testCompressionRatioReducesJsonSize() {
        val plainFile = tempFolder.newFile("large_portfolio.json")
        // Generar un JSON representativo de varios KB
        val sb = StringBuilder("{\"tx\":[")
        repeat(500) { i ->
            sb.append("{\"id\":\"tx_$i\",\"desc\":\"Compra supermercado Exito $i\",\"amount\":$i.99},")
        }
        sb.append("{\"id\":\"tx_end\"}]}")
        plainFile.writeText(sb.toString(), Charsets.UTF_8)

        val encryptedFile = tempFolder.newFile("compressed.kash_enc")
        val result = engine.encryptBackupFile(plainFile, encryptedFile, testPassphrase)
        assertTrue(result.isSuccess)

        val meta = result.getOrThrow()
        assertTrue(
            "El tamaño comprimido y cifrado (${meta.encryptedSizeBytes}) debe ser menor al plano (${meta.plainSizeBytes})",
            meta.encryptedSizeBytes < meta.plainSizeBytes
        )
    }

    @Test
    fun testWrongPassphraseFailsWithSecurityException() {
        val plainFile = tempFolder.newFile("secure_data.json")
        plainFile.writeText("{\"confidential\":true}", Charsets.UTF_8)

        val encryptedFile = tempFolder.newFile("secure.kash_enc")
        val decryptedFile = tempFolder.newFile("fail_restored.json")

        engine.encryptBackupFile(plainFile, encryptedFile, testPassphrase)

        val decryptResult = engine.decryptBackupFile(
            encryptedInputFile = encryptedFile,
            plainOutputFile = decryptedFile,
            passphrase = "WrongPassphrase1234!"
        )

        assertFalse("Descifrar con clave errónea debe fallar", decryptResult.isSuccess)
        assertTrue("La excepción debe ser de tipo SecurityException", decryptResult.exceptionOrNull() is SecurityException)
        assertFalse("El archivo de salida parcial no debe quedar en disco", decryptedFile.exists())
    }

    @Test
    fun testHeaderBitFlipDetectedByGcmAad() {
        val plainFile = tempFolder.newFile("aad_test.json")
        plainFile.writeText("{\"test\":123}", Charsets.UTF_8)

        val encryptedFile = tempFolder.newFile("aad.kash_enc")
        val decryptedFile = tempFolder.newFile("aad_fail.json")

        engine.encryptBackupFile(plainFile, encryptedFile, testPassphrase)

        // Alterar el byte 8 (versión de formato protegida por AAD)
        RandomAccessFile(encryptedFile, "rw").use { raf ->
            raf.seek(8)
            val currentByte = raf.readByte()
            raf.seek(8)
            raf.writeByte(currentByte.toInt() xor 0x01)
        }

        val decryptResult = engine.decryptBackupFile(
            encryptedInputFile = encryptedFile,
            plainOutputFile = decryptedFile,
            passphrase = testPassphrase
        )

        assertFalse("La alteración de la cabecera AAD debe ser detectada y rechazada", decryptResult.isSuccess)
    }

    @Test
    fun testIsEncryptedKashBackupDetection() {
        val notKashFile = tempFolder.newFile("random.bin")
        notKashFile.writeBytes(ByteArray(100) { it.toByte() })
        assertFalse(engine.isEncryptedKashBackup(notKashFile))

        val emptyFile = tempFolder.newFile("empty.bin")
        assertFalse(engine.isEncryptedKashBackup(emptyFile))
    }
}
