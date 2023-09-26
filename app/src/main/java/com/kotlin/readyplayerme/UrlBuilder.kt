package com.kotlin.readyplayerme

data class UrlConfig(
    var subdomain: String = "demo",
    var clearCache: Boolean = false,
    var quickStart: Boolean = false,
    var gender: Gender = Gender.NONE,
    var bodyType: BodyType = BodyType.SELECTABLE,
    var loginToken: String = "",
    var language: Language = Language.DEFAULT
)

class UrlBuilder(
    private val configParams: UrlConfig = UrlConfig()
) {
    companion object {
        private const val CLEAR_CACHE_PARAM = "clearCache"
        private const val FRAME_API_PARAM = "frameApi"
        private const val QUICK_START_PARAM = "quickStart"
        private const val SELECT_BODY_PARAM = "selectBodyType"
        private const val LOGIN_TOKEN_PARAM = "token"
    }

    fun buildUrl(loginToken: String = ""): String {
        val baseUrl = "https://${configParams.subdomain}.readyplayer.me/"
        val builder = StringBuilder(baseUrl)

        if (configParams.language != Language.DEFAULT) {
            builder.append("${configParams.language}/")
        }

        builder.append("avatar?$FRAME_API_PARAM")

        if (configParams.clearCache) {
            builder.append("&$CLEAR_CACHE_PARAM")
        }

        if (loginToken.isNotEmpty()) {
            builder.append("&$LOGIN_TOKEN_PARAM=$loginToken")
        }

        if (configParams.quickStart) {
            builder.append("&$QUICK_START_PARAM")
        } else {
            if (configParams.gender != Gender.NONE) {
                builder.append("&gender=${configParams.gender}")
            }
            builder.append(if (configParams.bodyType == BodyType.SELECTABLE) "&$SELECT_BODY_PARAM" else "&bodyType=${configParams.bodyType}")
        }
        return builder.toString()
    }
}

enum class Language(val stringValue: String) {
    DEFAULT(""),
    CHINESE("ch"),
    GERMAN("de"),
    ENGLISH_IRELAND("en-IE"),
    ENGLISH("en"),
    SPANISH_MEXICO("es-MX"),
    SPANISH("es"),
    FRENCH("fr"),
    ITALIAN("it"),
    JAPANESE("jp"),
    KOREAN("kr"),
    PORTUGUESE_BRAZIL("pt-BR"),
    PORTUGUESE("pt"),
    TURKISH("tr")
}

enum class BodyType(val stringValue: String) {
    SELECTABLE(""),
    FULLBODY("fullbody"),
    HALFBODY("halfbody")
}

enum class Gender(val stringValue: String) {
    NONE(""),
    MALE("male"),
    FEMALE("female")
}