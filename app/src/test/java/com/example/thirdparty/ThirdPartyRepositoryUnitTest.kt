package com.example.thirdparty

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.ThirdPartyEntity
import com.example.data.repository.ThirdPartyRepositoryImpl
import com.example.domain.repository.DuplicateThirdPartyException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ThirdPartyRepositoryUnitTest {

    private lateinit var database: SecurityDatabase
    private lateinit var repository: ThirdPartyRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ThirdPartyRepositoryImpl(database.thirdPartyDao(), database.thirdPartyCategoryDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createCategory_persistsAndObservesCorrectly() = runBlocking {
        val result = repository.createCategory(
            userId = "user-1",
            name = "Distribuidor Mayorista",
            iconName = "Storefront",
            colorHex = "#0284C7"
        )
        assertTrue(result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("DISTRIBUIDOR_MAYORISTA", created?.code)

        val categories = repository.observeCategories("user-1").first()
        assertTrue(categories.any { it.code == "DISTRIBUIDOR_MAYORISTA" && it.name == "Distribuidor Mayorista" })
    }

    @Test
    fun saveThirdParty_success() = runBlocking {
        val tp = ThirdPartyEntity(
            id = "tp-1",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            category = "PROVEEDOR"
        )
        val result = repository.saveThirdParty(tp)
        assertTrue(result.isSuccess)

        val retrieved = repository.getThirdPartyById("tp-1")
        assertNotNull(retrieved)
        assertEquals("Juan Pérez", retrieved?.name)
        assertEquals("1234", retrieved?.idNumber)
    }

    @Test
    fun saveThirdParty_blocksDuplicateTuple_sameName_sameId() = runBlocking {
        // Juan Pérez con 1234
        val tp1 = ThirdPartyEntity(
            id = "tp-1",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            category = "PROVEEDOR"
        )
        val res1 = repository.saveThirdParty(tp1)
        assertTrue(res1.isSuccess)

        // Intento de crear Juan Pérez con 1234 nuevamente con distinto id de entidad
        val tp2 = ThirdPartyEntity(
            id = "tp-2",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            category = "CONCESIONARIO"
        )
        val res2 = repository.saveThirdParty(tp2)
        assertFalse("Debe bloquear duplicado exacto de nombre y cédula", res2.isSuccess)
        assertTrue(res2.exceptionOrNull() is DuplicateThirdPartyException)
        assertTrue(res2.exceptionOrNull()?.message?.contains("Ya existe un tercero") == true)
    }

    @Test
    fun saveThirdParty_blocksDuplicateTuple_caseInsensitive_and_spaces() = runBlocking {
        val tp1 = ThirdPartyEntity(
            id = "tp-1",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            category = "PROVEEDOR"
        )
        repository.saveThirdParty(tp1)

        // Intento con mayúsculas y espacios extra: "  JUAN   PÉREZ  " con documento " 1234 "
        val tp2 = ThirdPartyEntity(
            id = "tp-2",
            userId = "user-1",
            name = "  JUAN   PÉREZ  ",
            normalizedName = "juan perez",
            idNumber = " 1234 ",
            category = "PROVEEDOR"
        )
        val res2 = repository.saveThirdParty(tp2)
        assertFalse("Debe bloquear duplicado insensible a mayúsculas y espacios", res2.isSuccess)
        assertTrue(res2.exceptionOrNull() is DuplicateThirdPartyException)
    }

    @Test
    fun saveThirdParty_allowsSameName_differentId() = runBlocking {
        // Juan Pérez con 1234
        val tp1 = ThirdPartyEntity(
            id = "tp-1",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            category = "PROVEEDOR"
        )
        val res1 = repository.saveThirdParty(tp1)
        assertTrue(res1.isSuccess)

        // Juan Pérez con 1236 (Homónimo permitido)
        val tp2 = ThirdPartyEntity(
            id = "tp-2",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1236",
            category = "PROVEEDOR"
        )
        val res2 = repository.saveThirdParty(tp2)
        assertTrue("Debe permitir Juan Pérez con distinta identificación (1236)", res2.isSuccess)

        val all = repository.observeThirdParties("user-1").first()
        assertEquals(2, all.size)
    }

    @Test
    fun saveThirdParty_allowsUpdatingSameEntity() = runBlocking {
        val tp1 = ThirdPartyEntity(
            id = "tp-1",
            userId = "user-1",
            name = "Juan Pérez",
            normalizedName = "juan perez",
            idNumber = "1234",
            primaryPhone = "3001234567"
        )
        repository.saveThirdParty(tp1)

        // Actualizar el teléfono de la misma entidad tp-1
        val updated = tp1.copy(primaryPhone = "3109999999")
        val resUpdate = repository.saveThirdParty(updated)
        assertTrue(resUpdate.isSuccess)

        val retrieved = repository.getThirdPartyById("tp-1")
        assertEquals("3109999999", retrieved?.primaryPhone)
    }

    @Test
    fun observeByCategory_filtersCorrectly() = runBlocking {
        repository.saveThirdParty(ThirdPartyEntity(id = "tp-1", userId = "user-1", name = "Toyota", normalizedName = "toyota", idNumber = "1", category = "CONCESIONARIO"))
        repository.saveThirdParty(ThirdPartyEntity(id = "tp-2", userId = "user-1", name = "Éxito", normalizedName = "exito", idNumber = "2", category = "TIENDA_COMERCIO"))
        repository.saveThirdParty(ThirdPartyEntity(id = "tp-3", userId = "user-1", name = "Servientrega", normalizedName = "servientrega", idNumber = "3", category = "PROVEEDOR"))

        val concesionarios = repository.observeByCategory("user-1", "CONCESIONARIO").first()
        assertEquals(1, concesionarios.size)
        assertEquals("Toyota", concesionarios[0].name)

        val tiendas = repository.observeByCategory("user-1", "TIENDA_COMERCIO").first()
        assertEquals(1, tiendas.size)
        assertEquals("Éxito", tiendas[0].name)
    }

    @Test
    fun toggleFavorite_worksCorrectly() = runBlocking {
        repository.saveThirdParty(ThirdPartyEntity(id = "tp-1", userId = "user-1", name = "Toyota", normalizedName = "toyota", idNumber = "1", isFavorite = false))

        repository.setFavorite("tp-1", true)
        val favs = repository.observeFavorites("user-1").first()
        assertEquals(1, favs.size)
        assertEquals("Toyota", favs[0].name)
    }

    @Test
    fun seedDefaults_populatesOnlySingleDefaultTerceroComun() = runBlocking {
        repository.seedDefaultThirdPartiesIfNeeded("user-seed")
        val all = repository.observeThirdParties("user-seed").first()

        assertEquals("Solo debe existir el Tercero Común por defecto", 1, all.size)
        val defaultTp = all[0]
        assertEquals("Tercero Común", defaultTp.name)
        assertEquals("222222222", defaultTp.idNumber)
        assertEquals("CC", defaultTp.idType)
        assertFalse("No debe contener datos mockeados", all.any { it.name.contains("Éxito", ignoreCase = true) })
        assertFalse("No debe contener datos mockeados", all.any { it.name.contains("Toyota", ignoreCase = true) })
    }
}
