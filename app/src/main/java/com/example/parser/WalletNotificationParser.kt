package com.example.parser

import java.util.Locale
import java.util.regex.Pattern

enum class WalletAppSource(
    val walletName: String,
    val officialPackages: List<String>,
    val packageHints: List<String>
) {
    NEQUI(
        walletName = "Nequi",
        officialPackages = listOf(
            "com.nequi.MobileApp",
            "co.com.nequi.MobileApp",
            "com.nequi.co",
            "co.com.nequi"
        ),
        packageHints = listOf("nequi")
    ),
    DAVIPLATA(
        walletName = "Daviplata",
        officialPackages = listOf(
            "com.davivienda.daviplataapp",
            "com.davivienda.daviplata",
            "co.com.davivienda.daviplata"
        ),
        packageHints = listOf("daviplata")
    ),
    BANCOLOMBIA(
        walletName = "Bancolombia",
        officialPackages = listOf(
            "com.todo1.mobile",
            "co.com.bancolombia.personas.superapp",
            "com.bancolombia.alamanoconsumer",
            "com.todo1.alm",
            "co.com.bancolombia"
        ),
        packageHints = listOf("bancolombia", "todo1", "alamano")
    ),
    SMS(
        walletName = "SMS Bancario",
        officialPackages = listOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.mms",
            "com.android.messaging",
            "com.miui.smsextra",
            "com.motorola.messaging"
        ),
        packageHints = listOf("messaging", "mms", "sms")
    ),
    SELF_TEST(
        walletName = "Test",
        officialPackages = listOf("com.example"),
        packageHints = listOf("cantapagos", "aistudio")
    )
}

data class ParsedPayment(
    val wallet: String, // "Nequi", "Daviplata", "Bancolombia", "SMS Bancario"
    val amount: Long,
    val spokenAmount: String,
    val formattedAmount: String,
    val sender: String? = null,
    val rawText: String,
    val packageName: String = ""
)

object WalletNotificationParser {

    // Minimum valid payment in Colombian Pesos (filters out $0.22 interest / cent notifications)
    private const val MIN_VALID_COP_AMOUNT = 100L

    // Explicitly blocked apps/packages that must NEVER trigger announcements (e.g. Ualá)
    private val BLOCKED_APP_TOKENS = listOf(
        "uala", "ualá", "ar.com.uala", "co.com.uala", "com.uala",
        "nu.production", "nubank", "mercadopago", "littio", "lulo"
    )

    // Words indicating interest yields, advertising, marketing, or credit/loan offers to ignore
    private val NEGATIVE_KEYWORDS = listOf(
        "uala", "ualá",
        "abono de interés", "abono de interes", "interés", "interes", "intereses",
        "rendimiento", "rendimientos", "cashback", "devolución", "devolucion",
        "préstamo", "prestamo", "tarjeta de crédito", "tarjeta de credito",
        "crédito", "credito", "preaprobado", "pre-aprobado", "aprovecha",
        "paga tu", "descarga", "beneficio", "conoce nuestra", "adquiere",
        "seguro", "ahorra", "invierte", "solicita", "descuento", "promoción",
        "promocion", "sorteo", "puntos colombia", "clave dinámica", "clave dinamica",
        "código de seguridad", "codigo de seguridad", "inicia sesión", "inicia sesion",
        "batería", "bateria", "cargue el teléfono", "cargue el telefono"
    )

    // Outbound keywords (the merchant paid or withdrew money, NOT received)
    private val OUTBOUND_KEYWORDS = listOf(
        "enviaste", "pagaste", "retiraste", "compra en", "compraste",
        "transferiste", "transfiriendo a", "retiro exitoso", "pago exitoso de factura",
        "envío exitoso", "envio exitoso", "pago con qr exitoso", "comprobante de pago",
        "le enviaste", "hiciste un pago"
    )

    // Positive indicators of an incoming credit transfer
    private val POSITIVE_KEYWORDS = listOf(
        "recibiste", "te envió", "te envio", "te transfirió", "te transfirio",
        "te llegó", "te llego", "abono", "transferencia", "recepción", "recepcion",
        "transferencia recibida", "transferencia exitosa",
        "pago recibido", "pasaron", "te pasaron", "le transfirieron", "consignación",
        "consignacion", "giro recibido", "te mandaron", "te mandó", "te mando",
        "te consignaron", "te pagó", "te pago", "enviaron plata"
    )

    // 1. Contextual amount pattern: matches numbers right after incoming payment phrases,
    // WITH OR WITHOUT '$' / 'COP' (e.g. "[Cliente] te envió 1000, ¡lo mejor!")
    private val CONTEXTUAL_AMOUNT_REGEX = Pattern.compile(
        "(?:te\\s+envi[oó]|te\\s+mand[oó]|te\\s+pas[oó]|te\\s+transfiri[oó]|te\\s+pag[oó]|" +
            "recibiste|te\\s+enviaron|pasaron|abono\\s+(?:de|por)|transferencia\\s+(?:de|por)|" +
            "pago\\s+(?:de|por)|por\\s+valor\\s+de|valor\\s+de)\\s*(?:\\$|COP)?\\s*" +
            "([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{2})?|[0-9]+(?:[.,][0-9]{2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // 2. Explicit currency symbol pattern: matches $ 15.000 or $1000 or COP 15.000
    private val CURRENCY_AMOUNT_REGEX = Pattern.compile(
        "(?:\\$|COP)\\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?|[0-9]+(?:[.,][0-9]{2})?)",
        Pattern.CASE_INSENSITIVE
    )

    // 3. Number followed by "pesos" or "cop"
    private val PESOS_AMOUNT_REGEX = Pattern.compile(
        "([0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]{3,})\\s*(?:pesos|cop)",
        Pattern.CASE_INSENSITIVE
    )

    // 4. Standalone amount pattern for verified wallet packages (e.g., Nequi notification with plain number)
    private val STANDALONE_WALLET_AMOUNT_REGEX = Pattern.compile(
        "\\b([0-9]{1,3}(?:[.,][0-9]{3})+(?:[.,][0-9]{2})?|[0-9]{3,9})\\b"
    )

    // Prefix sender pattern for anonymization: "<NOMBRE> te envió 1000"
    private val PREFIX_NAME_ANONYMIZE_REGEX = Pattern.compile(
        "(^|[.!?:]\\s+)([A-Za-zÁÉÍÓÚÜáéíóúüñÑ][A-Za-zÁÉÍÓÚÜáéíóúüñÑ .'-]{1,38}?)(\\s+" +
            "(?:te\\s+envi[oó]|te\\s+mand[oó]|te\\s+pas[oó]|te\\s+transfiri[oó]|te\\s+pag[oó]|te\\s+consign[oó]))(?=\\s|$)",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    // Suffix sender pattern for anonymization: "Recibiste $ 25.000 de <NOMBRE>"
    private val SUFFIX_NAME_ANONYMIZE_REGEX = Pattern.compile(
        "((?:de\\s+parte\\s+de|por\\s+parte\\s+de|remitente:?|\\bde)\\s+)" +
            "([A-Za-zÁÉÍÓÚÜáéíóúüñÑ][A-Za-zÁÉÍÓÚÜáéíóúüñÑ ]{1,35}?)" +
            "(?=\\.|,|!|\\s+en\\b|\\s+a\\b|\\s+por\\b|\\s+tu\\b|\\s+desde\\b|$)",
        Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
    )

    /**
     * Identifies the wallet/app source strictly from the Android package identifier (`sbn.packageName`).
     */
    fun identifyAppByPackage(packageName: String): WalletAppSource? {
        val cleanPkg = packageName.trim()
        if (cleanPkg.isEmpty()) return null
        val lowerPkg = cleanPkg.lowercase(Locale.ROOT)

        // Immediately reject blocked apps like Ualá
        if (BLOCKED_APP_TOKENS.any { lowerPkg.contains(it) }) {
            return null
        }

        for (source in WalletAppSource.entries) {
            if (source.officialPackages.any { it.equals(cleanPkg, ignoreCase = true) }) {
                return source
            }
            if (source.packageHints.any { lowerPkg.contains(it) }) {
                return source
            }
        }
        return null
    }

    /**
     * Anonymizes any person names in notification text so no personal names are stored or displayed.
     */
    fun anonymizeNotificationText(text: String): String {
        if (text.isBlank()) return text
        var result = PREFIX_NAME_ANONYMIZE_REGEX.matcher(text).replaceAll("$1[Cliente]$3")
        result = SUFFIX_NAME_ANONYMIZE_REGEX.matcher(result).replaceAll("$1[Cliente]")
        return result
    }

    fun parse(
        packageName: String,
        title: String?,
        text: String?,
        strictPackageFilter: Boolean = true
    ): ParsedPayment? {
        val cleanTitle = title?.trim() ?: ""
        val cleanText = text?.trim() ?: ""
        val fullText = "$cleanTitle $cleanText".trim()
        if (fullText.isEmpty()) return null

        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerFull = fullText.lowercase(Locale.ROOT)
        val lowerTitle = cleanTitle.lowercase(Locale.ROOT)
        val lowerText = cleanText.lowercase(Locale.ROOT)

        // 0. Hard block excluded apps (such as Ualá) by package name, title, or text
        if (BLOCKED_APP_TOKENS.any { lowerPkg.contains(it) || lowerTitle.contains(it) || lowerFull.contains(it) }) {
            return null
        }

        // 1. Identify App by Package Identifier (sbn.packageName) FIRST
        val appSource = identifyAppByPackage(packageName)
        if (strictPackageFilter && appSource == null) {
            return null
        }

        // 2. Check for negative keywords (interest yields, ads, loan offers, security codes)
        for (negative in NEGATIVE_KEYWORDS) {
            if (lowerFull.contains(negative)) {
                return null
            }
        }

        // 3. Check for outbound payments (the user sent or paid money)
        for (outbound in OUTBOUND_KEYWORDS) {
            if (lowerFull.contains(outbound)) {
                return null
            }
        }

        // 4. Resolve specific wallet name from Package ID first
        val wallet = resolveWalletName(appSource, lowerFull) ?: return null

        // 5. Verify positive incoming credit intent tailored to the identified app
        val hasPositiveKeyword = POSITIVE_KEYWORDS.any { lowerFull.contains(it) }
        val isNequiEnvioHeader = appSource == WalletAppSource.NEQUI &&
            (lowerTitle == "envío" || lowerTitle == "envio" || lowerTitle.contains("enviaron") || lowerText.contains("te envió") || lowerText.contains("te envio"))
        val isDedicatedWalletCredit = (appSource == WalletAppSource.NEQUI ||
            appSource == WalletAppSource.DAVIPLATA ||
            appSource == WalletAppSource.BANCOLOMBIA) &&
            (isNequiEnvioHeader || lowerFull.contains("plata") || lowerFull.contains("abono") ||
                lowerFull.contains("recibiste") || lowerFull.contains("pasaron") ||
                lowerFull.contains("transferencia") || lowerFull.contains("qr"))

        if (!hasPositiveKeyword && !isDedicatedWalletCredit) {
            return null
        }

        // 6. Extract amount and enforce minimum COP threshold (>= $100 COP)
        val isVerifiedWalletApp = appSource == WalletAppSource.NEQUI ||
            appSource == WalletAppSource.DAVIPLATA ||
            appSource == WalletAppSource.BANCOLOMBIA ||
            appSource == WalletAppSource.SELF_TEST

        val amount = extractAmount(
            bodyText = cleanText.ifEmpty { fullText },
            fullText = fullText,
            allowStandaloneAmount = isVerifiedWalletApp
        ) ?: return null

        if (amount < MIN_VALID_COP_AMOUNT) return null

        // 7. Anonymize text so no real person names are stored or exposed; keep official packageName
        val anonymizedText = anonymizeNotificationText(fullText)
        val officialPkgId = resolveDisplayPackageId(packageName, wallet)

        val spoken = numberToSpanishWords(amount)
        val formatted = formatCop(amount)

        return ParsedPayment(
            wallet = wallet,
            amount = amount,
            spokenAmount = spoken,
            formattedAmount = formatted,
            sender = null, // Anonymized: never store or expose real person names
            rawText = anonymizedText,
            packageName = officialPkgId
        )
    }

    fun resolveDisplayPackageId(packageName: String, wallet: String): String {
        val clean = packageName.trim()
        if (clean.isNotEmpty() && !clean.startsWith("com.example") && !clean.contains("aistudio")) {
            return clean
        }
        return when (wallet) {
            "Nequi" -> "com.nequi.MobileApp"
            "Daviplata" -> "com.davivienda.daviplataapp"
            "Bancolombia" -> "com.todo1.mobile"
            else -> "com.android.messaging"
        }
    }

    private fun resolveWalletName(appSource: WalletAppSource?, textLower: String): String? {
        return when (appSource) {
            WalletAppSource.NEQUI -> "Nequi"
            WalletAppSource.DAVIPLATA -> "Daviplata"
            WalletAppSource.BANCOLOMBIA -> "Bancolombia"
            WalletAppSource.SMS -> {
                when {
                    textLower.contains("nequi") -> "Nequi"
                    textLower.contains("daviplata") -> "Daviplata"
                    textLower.contains("bancolombia") || textLower.contains("a la mano") -> "Bancolombia"
                    textLower.contains("banco") || textLower.contains("cta") -> "SMS Bancario"
                    else -> null
                }
            }
            WalletAppSource.SELF_TEST -> {
                when {
                    textLower.contains("nequi") || textLower.contains("te envió") || textLower.contains("te envio") -> "Nequi"
                    textLower.contains("daviplata") -> "Daviplata"
                    textLower.contains("bancolombia") || textLower.contains("a la mano") -> "Bancolombia"
                    textLower.contains("banco") || textLower.contains("cta") -> "SMS Bancario"
                    else -> null
                }
            }
            null -> null
        }
    }

    private fun extractAmount(
        bodyText: String,
        fullText: String,
        allowStandaloneAmount: Boolean
    ): Long? {
        // Priority 1: Contextual incoming phrase + number (e.g., "te envió 1000" or "Recibiste $ 25.000")
        val contextualMatcher = CONTEXTUAL_AMOUNT_REGEX.matcher(fullText)
        if (contextualMatcher.find()) {
            val rawNumber = contextualMatcher.group(1)
            val parsed = rawNumber?.let { cleanNumberString(it) }
            if (parsed != null && parsed >= MIN_VALID_COP_AMOUNT) return parsed
        }

        // Priority 2: Explicit '$' or 'COP' currency symbol
        val currencyMatcher = CURRENCY_AMOUNT_REGEX.matcher(fullText)
        if (currencyMatcher.find()) {
            val rawNumber = currencyMatcher.group(1)
            val parsed = rawNumber?.let { cleanNumberString(it) }
            if (parsed != null && parsed >= MIN_VALID_COP_AMOUNT) return parsed
        }

        // Priority 3: Number followed by "pesos" or "cop"
        val pesosMatcher = PESOS_AMOUNT_REGEX.matcher(fullText)
        if (pesosMatcher.find()) {
            val rawNumber = pesosMatcher.group(1)
            val parsed = rawNumber?.let { cleanNumberString(it) }
            if (parsed != null && parsed >= MIN_VALID_COP_AMOUNT) return parsed
        }

        // Priority 4: If notification came from an official wallet package (e.g. com.nequi.MobileApp),
        // match standalone numbers >= 100 in the notification body
        if (allowStandaloneAmount) {
            val standaloneMatcher = STANDALONE_WALLET_AMOUNT_REGEX.matcher(bodyText)
            while (standaloneMatcher.find()) {
                val rawNumber = standaloneMatcher.group(1)
                val parsed = rawNumber?.let { cleanNumberString(it) }
                if (parsed != null && parsed >= MIN_VALID_COP_AMOUNT) {
                    return parsed
                }
            }
        }

        return null
    }

    private fun cleanNumberString(raw: String): Long? {
        var str = raw.trim().trimEnd('.', ',')
        if (str.isEmpty()) return null

        // Reject sub-peso decimal amounts like "0.22" or "0,50"
        if (str.startsWith("0.") || str.startsWith("0,")) {
            return 0L
        }

        // If string ends with ,XX or .XX decimal cents (2 digits after separator, e.g. 50.000,00 or 10.50),
        // strip the decimal cents part
        if (str.length >= 4 && (str[str.length - 3] == ',' || str[str.length - 3] == '.')) {
            str = str.substring(0, str.length - 3)
        }
        // Remove all thousands separators (dots and commas)
        val digitsOnly = str.replace(".", "").replace(",", "").trim()
        return digitsOnly.toLongOrNull()
    }

    private fun formatCop(amount: Long): String {
        return "$ " + String.format(Locale.forLanguageTag("es-CO"), "%,d", amount).replace(",", ".")
    }

    /**
     * Converts an amount into clear, natural Spanish words for Text-to-Speech pronunciation.
     * Example: 1000 -> "mil pesos", 15000 -> "quince mil pesos"
     */
    fun numberToSpanishWords(amount: Long): String {
        if (amount == 0L) return "cero pesos"
        if (amount < 0L) return "menos " + numberToSpanishWords(-amount)

        val words = convertNumber(amount)
        return if (amount >= 1000000L && amount % 1000000L == 0L) {
            "$words de pesos"
        } else {
            "$words pesos"
        }
    }

    private fun convertNumber(n: Long): String {
        return when {
            n == 0L -> ""
            n < 1000L -> convertUnderThousand(n.toInt())
            n == 1000L -> "mil"
            n < 2000L -> "mil " + convertUnderThousand((n % 1000).toInt())
            n < 1000000L -> {
                val thousands = n / 1000
                val remainder = n % 1000
                val thWords = convertUnderThousand(thousands.toInt()) + " mil"
                if (remainder > 0) "$thWords " + convertUnderThousand(remainder.toInt()) else thWords
            }
            n == 1000000L -> "un millón"
            n < 2000000L -> {
                val remainder = n % 1000000
                if (remainder > 0) "un millón " + convertNumber(remainder) else "un millón"
            }
            else -> {
                val millions = n / 1000000
                val remainder = n % 1000000
                val milWords = convertNumber(millions) + " millones"
                if (remainder > 0) "$milWords " + convertNumber(remainder) else milWords
            }
        }.trim()
    }

    private fun convertUnderThousand(n: Int): String {
        val units = arrayOf("", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve")
        val teens = arrayOf(
            "diez", "once", "doce", "trece", "catorce", "quince",
            "dieciséis", "diecisiete", "dieciocho", "diecinueve"
        )
        val twenties = arrayOf(
            "veinte", "veintiún", "veintidós", "veintitrés", "veinticuatro",
            "veinticinco", "veintiséis", "veintisiete", "veintiocho", "veintinueve"
        )
        val tens = arrayOf("", "diez", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa")
        val hundreds = arrayOf(
            "", "ciento", "doscientos", "trescientos", "cuatrocientos",
            "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"
        )

        return when {
            n == 0 -> ""
            n < 10 -> units[n]
            n in 10..19 -> teens[n - 10]
            n in 20..29 -> twenties[n - 20]
            n in 30..99 -> {
                val t = n / 10
                val u = n % 10
                if (u == 0) tens[t] else "${tens[t]} y ${units[u]}"
            }
            n == 100 -> "cien"
            n in 101..999 -> {
                val h = n / 100
                val rem = n % 100
                if (rem == 0) hundreds[h] else "${hundreds[h]} " + convertUnderThousand(rem)
            }
            else -> n.toString()
        }.trim()
    }
}
