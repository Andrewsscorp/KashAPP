package com.example

import com.example.domain.usecase.DocumentUploader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class DocumentUploaderTest {

    private lateinit var uploader: DocumentUploader

    @Before
    fun setUp() {
        uploader = DocumentUploader()
    }

    @Test
    fun `file upload rejects over 2mb`() {
        try {
            // Simulate 2.1 MB file (2.1 * 1024 * 1024 bytes)
            val overLimitSize = (2.1 * 1024 * 1024).toLong()
            uploader.upload(
                userId = "user-123",
                fileName = "extracto.pdf",
                fileType = "application/pdf",
                fileSize = overLimitSize
            )
            fail("Should have thrown IllegalArgumentException for file exceeding 2MB")
        } catch (e: IllegalArgumentException) {
            assertEquals("El archivo excede el límite máximo de 2MB.", e.message)
        }
    }

    @Test
    fun `file upload rejects invalid formats`() {
        try {
            // Simulate malicious or unsupported binary format
            uploader.upload(
                userId = "user-123",
                fileName = "malicious.exe",
                fileType = "application/x-msdownload",
                fileSize = (1 * 1024 * 1024).toLong()
            )
            fail("Should have thrown IllegalArgumentException for unsupported format")
        } catch (e: IllegalArgumentException) {
            assertEquals("Formato no soportado. Solo JPG, PNG o PDF.", e.message)
        }
    }

    @Test
    fun `file upload success returns secure paths`() {
        val result = uploader.upload(
            userId = "user-123",
            fileName = "logo.png",
            fileType = "image/png",
            fileSize = (1.5 * 1024 * 1024).toLong()
        )

        assertTrue(result["local"]!!.contains("/data/data/com.kashapp/files/hidden_docs/logo.png"))
        assertTrue(result["remote"]!!.contains("gs://kashapp/users/user-123/logo.png"))
    }
}
