package com.example.model

import androidx.annotation.Keep
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldSignal
import com.example.ui.theme.VioletPulse
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import kotlin.random.Random

enum class EsimPlanType(val displayName: String) {
    TRAVEL("Travel Data"),
    BUSINESS("Business Dual"),
    IOT("IoT / M2M"),
    CUSTOM("Custom Lab");

    companion object {
        fun fromRaw(raw: String): EsimPlanType =
            entries.firstOrNull { it.name == raw } ?: CUSTOM
    }
}

enum class EsimStatus(val displayName: String) {
    ACTIVE("Active"),
    STANDBY("Standby"),
    EXPIRED("Expired");

    companion object {
        fun fromRaw(raw: String): EsimStatus =
            entries.firstOrNull { it.name == raw } ?: STANDBY
    }
}

enum class EsimAccent(val displayName: String, val primaryColor: Color, val secondaryColor: Color) {
    CYAN("Electric Cyan", CyanElectric, Color(0xFF006780)),
    EMERALD("Signal Emerald", EmeraldSignal, Color(0xFF065F46)),
    VIOLET("Orbital Violet", VioletPulse, Color(0xFF4C1D95)),
    AMBER("Solar Amber", AmberGold, Color(0xFF92400E)),
    CORAL("Crimson Pulse", CoralAlert, Color(0xFF881337));

    companion object {
        fun fromRaw(raw: String): EsimAccent =
            entries.firstOrNull { it.name == raw } ?: CYAN
    }
}

@Keep
@IgnoreExtraProperties
data class VirtualEsim(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val carrierName: String = "",
    val countryCode: String = "",
    val countryName: String = "",
    val planType: String = EsimPlanType.TRAVEL.name,
    val status: String = EsimStatus.STANDBY.name,
    val iccid: String = "",
    val smdpAddress: String = "",
    val activationCode: String = "",
    val apn: String = "internet",
    val mccMnc: String = "00101",
    val dataLimitMb: Long = 10240L,
    val dataUsedMb: Long = 0L,
    val validityDays: Long = 30L,
    val roamingEnabled: Boolean = true,
    val fiveGEnabled: Boolean = true,
    val unlimitedData: Boolean = false,
    val colorAccent: String = EsimAccent.CYAN.name,
    val notes: String = "",
    @com.google.firebase.firestore.ServerTimestamp val createdAt: Timestamp? = null,
    @com.google.firebase.firestore.ServerTimestamp val updatedAt: Timestamp? = null
) {
    val lpaString: String
        get() = "LPA:1\$$smdpAddress\$$activationCode"

    val formattedIccid: String
        get() = iccid.chunked(4).joinToString(" ")

    val planTypeEnum: EsimPlanType
        get() = EsimPlanType.fromRaw(planType)

    val statusEnum: EsimStatus
        get() = EsimStatus.fromRaw(status)

    val accentEnum: EsimAccent
        get() = EsimAccent.fromRaw(colorAccent)

    val remainingMb: Long
        get() = (dataLimitMb - dataUsedMb).coerceAtLeast(0L)

    val usageFraction: Float
        get() = when {
            unlimitedData -> 0.18f
            dataLimitMb <= 0L -> 1f
            else -> (dataUsedMb.toFloat() / dataLimitMb.toFloat()).coerceIn(0f, 1f)
        }

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "label" to label.trim().take(50),
        "carrierName" to carrierName.trim().take(40),
        "countryCode" to countryCode.trim().uppercase().take(10),
        "countryName" to countryName.trim().take(40),
        "planType" to EsimPlanType.fromRaw(planType).name,
        "status" to EsimStatus.fromRaw(status).name,
        "iccid" to iccid.filter { it.isDigit() }.take(22),
        "smdpAddress" to smdpAddress.trim().take(120),
        "activationCode" to activationCode.trim().take(120),
        "apn" to apn.trim().take(60),
        "mccMnc" to mccMnc.trim().take(10),
        "dataLimitMb" to dataLimitMb.coerceIn(0L, 10_485_760L),
        "dataUsedMb" to dataUsedMb.coerceIn(0L, 10_485_760L),
        "validityDays" to validityDays.coerceIn(1L, 3650L),
        "roamingEnabled" to roamingEnabled,
        "fiveGEnabled" to fiveGEnabled,
        "unlimitedData" to unlimitedData,
        "colorAccent" to EsimAccent.fromRaw(colorAccent).name,
        "notes" to notes.trim().take(250),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

data class CountryPreset(
    val code: String,
    val name: String,
    val defaultCarrier: String,
    val mccMnc: String,
    val defaultApn: String,
    val smdpHost: String,
    val accent: EsimAccent
)

object EsimCatalog {
    val presets = listOf(
        CountryPreset("GLOBAL", "Global 120+", "AeroLink Global", "90140", "global.esim", "smdp.aerolink.esim", EsimAccent.CYAN),
        CountryPreset("JP", "Japan", "Sakura 5G", "44010", "sakura.5g", "smdp.sakura-sim.jp", EsimAccent.CORAL),
        CountryPreset("US", "United States", "Liberty Wave", "310410", "wave.broadband", "smdp.libertywave.us", EsimAccent.VIOLET),
        CountryPreset("EU", "Europe 38", "EuroConnect", "20801", "euro.roam", "smdp.euroconnect.eu", EsimAccent.EMERALD),
        CountryPreset("GB", "United Kingdom", "Thames Mobile", "23415", "thames.data", "smdp.thames-esim.uk", EsimAccent.CYAN),
        CountryPreset("SG", "Singapore", "Merlion Telco", "52501", "merlion.5g", "smdp.merlion.sg", EsimAccent.AMBER),
        CountryPreset("KR", "South Korea", "Hanbit Telecom", "45005", "hanbit.5g", "smdp.hanbit.kr", EsimAccent.VIOLET),
        CountryPreset("AE", "UAE", "Oasis Wireless", "42402", "oasis.data", "smdp.oasis-sim.ae", EsimAccent.AMBER)
    )

    fun starterProfiles(uid: String): List<VirtualEsim> = listOf(
        VirtualEsim(
            userId = uid,
            label = "Global Roam 5G",
            carrierName = "AeroLink Global",
            countryCode = "GLOBAL",
            countryName = "Global 120+",
            planType = EsimPlanType.TRAVEL.name,
            status = EsimStatus.ACTIVE.name,
            iccid = EsimGenerator.generateLuhnIccid("901"),
            smdpAddress = "smdp.aerolink.esim",
            activationCode = EsimGenerator.generateActivationCode("GLB"),
            apn = "global.esim",
            mccMnc = "90140",
            dataLimitMb = 20480L,
            dataUsedMb = 4350L,
            validityDays = 30L,
            roamingEnabled = true,
            fiveGEnabled = true,
            unlimitedData = false,
            colorAccent = EsimAccent.CYAN.name,
            notes = "Primary virtual travel data eSIM across 120+ countries."
        ),
        VirtualEsim(
            userId = uid,
            label = "Tokyo 5G Pass",
            carrierName = "Sakura 5G",
            countryCode = "JP",
            countryName = "Japan",
            planType = EsimPlanType.TRAVEL.name,
            status = EsimStatus.STANDBY.name,
            iccid = EsimGenerator.generateLuhnIccid("81"),
            smdpAddress = "smdp.sakura-sim.jp",
            activationCode = EsimGenerator.generateActivationCode("JP"),
            apn = "sakura.5g",
            mccMnc = "44010",
            dataLimitMb = 10240L,
            dataUsedMb = 0L,
            validityDays = 15L,
            roamingEnabled = true,
            fiveGEnabled = true,
            unlimitedData = false,
            colorAccent = EsimAccent.CORAL.name,
            notes = "Ready for Tokyo & Osaka transit."
        ),
        VirtualEsim(
            userId = uid,
            label = "US Business Line",
            carrierName = "Liberty Wave",
            countryCode = "US",
            countryName = "United States",
            planType = EsimPlanType.BUSINESS.name,
            status = EsimStatus.STANDBY.name,
            iccid = EsimGenerator.generateLuhnIccid("1"),
            smdpAddress = "smdp.libertywave.us",
            activationCode = EsimGenerator.generateActivationCode("US"),
            apn = "wave.broadband",
            mccMnc = "310410",
            dataLimitMb = 51200L,
            dataUsedMb = 12288L,
            validityDays = 90L,
            roamingEnabled = false,
            fiveGEnabled = true,
            unlimitedData = false,
            colorAccent = EsimAccent.VIOLET.name,
            notes = "Secondary enterprise data line."
        )
    )
}

data class ParsedLpa(
    val smdpAddress: String,
    val activationCode: String
)

object EsimGenerator {
    private val codeAlphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generateLuhnIccid(countryPrefixDigits: String = "00"): String {
        val cleanCountry = countryPrefixDigits.filter { it.isDigit() }.padEnd(2, '0').take(3)
        val base = StringBuilder("89").append(cleanCountry)
        while (base.length < 18) {
            base.append(Random.nextInt(0, 10))
        }
        val digits18 = base.toString().take(18)
        val checkDigit = computeLuhnCheckDigit(digits18)
        return digits18 + checkDigit
    }

    private fun computeLuhnCheckDigit(partial: String): Int {
        var sum = 0
        var doubleIt = true
        for (i in partial.length - 1 downTo 0) {
            var d = partial[i] - '0'
            if (doubleIt) {
                d *= 2
                if (d > 9) d -= 9
            }
            sum += d
            doubleIt = !doubleIt
        }
        return (10 - (sum % 10)) % 10
    }

    fun generateActivationCode(prefix: String = "VSIM"): String {
        val cleanPrefix = prefix.filter { it.isLetterOrDigit() }.uppercase().ifBlank { "VSIM" }.take(4)
        val block1 = (1..4).map { codeAlphabet[Random.nextInt(codeAlphabet.length)] }.joinToString("")
        val block2 = (1..4).map { codeAlphabet[Random.nextInt(codeAlphabet.length)] }.joinToString("")
        return "LPA-$cleanPrefix-$block1-$block2"
    }

    fun parseLpaUri(raw: String): ParsedLpa? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("LPA:", ignoreCase = true)) return null
        val parts = trimmed.split("$")
        if (parts.size < 3) return null
        val smdp = parts[1].trim()
        val code = parts[2].trim()
        if (smdp.length < 3 || code.length < 3) return null
        return ParsedLpa(smdpAddress = smdp, activationCode = code)
    }

    fun formatDataAmount(mb: Long, unlimited: Boolean = false): String {
        if (unlimited) return "Unlimited"
        return if (mb >= 1024L) {
            val gb = mb / 1024.0
            if (gb % 1.0 == 0.0) "${gb.toInt()} GB" else String.format("%.1f GB", gb)
        } else {
            "$mb MB"
        }
    }
}
