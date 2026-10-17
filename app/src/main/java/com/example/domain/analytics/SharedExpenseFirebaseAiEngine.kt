package com.example.domain.analytics

import com.example.feature.gastoscompartidos.domain.ocr.SharedOcrReceiptBridge
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig

/**
 * Inicializador de puente entre el SDK oficial `com.google.firebase:firebase-ai` en `:app`
 * y los motores `SharedExpenseFirebaseAiEngine` y `SharedOcrReceiptBridge` de `:feature-gastos-compartidos`.
 */
object FirebaseAiCloudProviderBridge {

    fun installIfNeeded() {
        if (SharedExpenseFirebaseAiEngine.cloudContentGenerator == null) {
            SharedExpenseFirebaseAiEngine.cloudContentGenerator = { prompt, temperature, maxTokens ->
                val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                    modelName = "gemini-2.5-flash",
                    generationConfig = generationConfig {
                        this.temperature = temperature
                        this.maxOutputTokens = maxTokens
                    }
                )
                model.generateContent(prompt).text.orEmpty()
            }
        }
        if (SharedOcrReceiptBridge.cloudAiContentGenerator == null) {
            SharedOcrReceiptBridge.cloudAiContentGenerator = { prompt ->
                SharedExpenseFirebaseAiEngine.cloudContentGenerator?.invoke(prompt, 0.05f, 768).orEmpty()
            }
        }
    }
}
