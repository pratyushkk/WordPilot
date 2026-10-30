package com.wordpilot.assistant.data.repository

import com.wordpilot.assistant.domain.models.ModelState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ModelRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var modelDir: File
    private lateinit var repository: ModelRepositoryImpl

    @Before
    fun setup() {
        modelDir = tempFolder.newFolder("models")
        repository = ModelRepositoryImpl(modelDir)
    }

    @Test
    fun testInitialStateNotInstalled() {
        assertFalse(repository.isModelInstalled())
        assertEquals(ModelState.NOT_INSTALLED, repository.modelState.value)
        assertEquals(0f, repository.downloadProgress.value, 0.001f)
    }

    @Test
    fun testModelInfoDefaults() {
        val info = repository.getModelInfo()
        assertEquals("Meta Llama 3.2 (1B Instruct)", info.name)
        assertFalse(info.isInstalled)
        assertEquals(770L, info.downloadSizeMb)
    }

    @Test
    fun testDeleteModelResetsState() {
        val dummyFile = File(modelDir, ModelRepositoryImpl.MODEL_FILENAME)
        dummyFile.writeBytes(ByteArray(1024))
        assertTrue(dummyFile.exists())

        repository.deleteModel()
        assertFalse(dummyFile.exists())
        assertEquals(ModelState.NOT_INSTALLED, repository.modelState.value)
    }
}
