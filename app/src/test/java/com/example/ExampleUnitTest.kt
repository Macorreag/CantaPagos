package com.example

import com.example.parser.WalletNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testNequiPlainNumberAndAnonymization() {
        val parsed = WalletNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "Envío",
            text = "REMITENTE PRUEBA te envió 1000, ¡lo mejor!"
        )
        assertNotNull(parsed)
        assertEquals("Nequi", parsed?.wallet)
        assertEquals("com.nequi.MobileApp", parsed?.packageName)
        assertEquals(1000L, parsed?.amount)
        assertEquals("mil pesos", parsed?.spokenAmount)
        assertNull(parsed?.sender)
        assertTrue(parsed?.rawText?.contains("[Cliente] te envió 1000") == true)
    }

    @Test
    fun testNequiTransferWithDotsAndCurrencyParsing() {
        val parsed = WalletNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "¡Te enviaron plata!",
            text = "Recibiste $ 25.000 de USUARIO DEMO"
        )
        assertNotNull(parsed)
        assertEquals("Nequi", parsed?.wallet)
        assertEquals("com.nequi.MobileApp", parsed?.packageName)
        assertEquals(25000L, parsed?.amount)
        assertEquals("veinticinco mil pesos", parsed?.spokenAmount)
        assertNull(parsed?.sender)
        assertTrue(parsed?.rawText?.contains("de [Cliente]") == true)
    }

    @Test
    fun testUalaAndInterestYieldNotificationsBlocked() {
        // Exact Ualá notification from screenshot:
        // "Ualá: Recibiste $0.22 como abono de interés a tu cuenta 💰"
        val ualaStrict = WalletNotificationParser.parse(
            packageName = "ar.com.uala",
            title = "Ualá",
            text = "Recibiste $0.22 como abono de interés a tu cuenta 💰",
            strictPackageFilter = true
        )
        assertNull(ualaStrict)

        val ualaNonStrict = WalletNotificationParser.parse(
            packageName = "ar.com.uala",
            title = "Ualá",
            text = "Recibiste $0.22 como abono de interés a tu cuenta 💰",
            strictPackageFilter = false
        )
        assertNull(ualaNonStrict)

        // Interest notification even from a known bank package must be ignored
        val bankInterest = WalletNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "Nequi",
            text = "Recibiste $0.22 como abono de interés a tu cuenta"
        )
        assertNull(bankInterest)
    }

    @Test
    fun testStrictPackageFilterBlocksUnrelatedApps() {
        val weather = WalletNotificationParser.parse(
            packageName = "com.google.android.googlequicksearchbox",
            title = "28° en Villavicencio",
            text = "Recibiste 1000 puntos hoy",
            strictPackageFilter = true
        )
        assertNull(weather)

        val battery = WalletNotificationParser.parse(
            packageName = "com.android.systemui",
            title = "Nivel de batería al 8 %",
            text = "Cargue el teléfono.",
            strictPackageFilter = true
        )
        assertNull(battery)
    }

    @Test
    fun testDaviplataTransferParsing() {
        val parsed = WalletNotificationParser.parse(
            packageName = "com.davivienda.daviplataapp",
            title = "DaviPlata",
            text = "Pasaron $20.000 a tu DaviPlata"
        )
        assertNotNull(parsed)
        assertEquals("Daviplata", parsed?.wallet)
        assertEquals("com.davivienda.daviplataapp", parsed?.packageName)
        assertEquals(20000L, parsed?.amount)
        assertEquals("veinte mil pesos", parsed?.spokenAmount)
    }

    @Test
    fun testBancolombiaTransferParsing() {
        val parsed = WalletNotificationParser.parse(
            packageName = "com.todo1.mobile",
            title = "Bancolombia",
            text = "Bancolombia le informa recepción de transferencia por $50.000,00"
        )
        assertNotNull(parsed)
        assertEquals("Bancolombia", parsed?.wallet)
        assertEquals("com.todo1.mobile", parsed?.packageName)
        assertEquals(50000L, parsed?.amount)
        assertEquals("cincuenta mil pesos", parsed?.spokenAmount)
    }

    @Test
    fun testMarketingAndAdsFilteredOut() {
        val ad1 = WalletNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "¡Aprovecha!",
            text = "Tienes un préstamo preaprobado por $5.000.000 de pesos"
        )
        assertNull(ad1)

        val ad2 = WalletNotificationParser.parse(
            packageName = "com.davivienda.daviplataapp",
            title = "Tarjeta de crédito",
            text = "Solicita tu tarjeta y recibe beneficios increíbles"
        )
        assertNull(ad2)
    }

    @Test
    fun testOutboundPaymentsFilteredOut() {
        val outbound = WalletNotificationParser.parse(
            packageName = "com.nequi.MobileApp",
            title = "Envío exitoso",
            text = "Enviaste plata por $15.000"
        )
        assertNull(outbound)
    }

    @Test
    fun testSpanishNumberToWords() {
        assertEquals("mil pesos", WalletNotificationParser.numberToSpanishWords(1000L))
        assertEquals("diez mil pesos", WalletNotificationParser.numberToSpanishWords(10000L))
        assertEquals("quince mil pesos", WalletNotificationParser.numberToSpanishWords(15000L))
        assertEquals("cien mil pesos", WalletNotificationParser.numberToSpanishWords(100000L))
        assertEquals("un millón de pesos", WalletNotificationParser.numberToSpanishWords(1000000L))
    }
}
