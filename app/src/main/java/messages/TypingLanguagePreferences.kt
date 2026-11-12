package com.chatforia.android.messages

import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import com.chatforia.android.chats.ConversationDto
import com.chatforia.android.auth.LANGUAGES
import com.chatforia.android.auth.Language
import com.chatforia.android.R

/** Device-local keyboard hints. These do not change the app or translation language. */
object TypingLanguagePreferences {
    private const val PREFS = "chatforia_typing_language_v1"
    private val supportedCodes = LANGUAGES.associate { it.code.lowercase(java.util.Locale.ROOT) to it.code }

    private val languageNameIds = mapOf(
        "af" to R.string.languages_af,
        "ak" to R.string.languages_ak,
        "sq" to R.string.languages_sq,
        "am" to R.string.languages_am,
        "ar" to R.string.languages_ar,
        "hy" to R.string.languages_hy,
        "as" to R.string.languages_as,
        "ay" to R.string.languages_ay,
        "az" to R.string.languages_az,
        "bm" to R.string.languages_bm,
        "eu" to R.string.languages_eu,
        "be" to R.string.languages_be,
        "bn" to R.string.languages_bn,
        "bho" to R.string.languages_bho,
        "bs" to R.string.languages_bs,
        "br" to R.string.languages_br,
        "bg" to R.string.languages_bg,
        "my" to R.string.languages_my,
        "ca" to R.string.languages_ca,
        "yue" to R.string.languages_yue,
        "ceb" to R.string.languages_ceb,
        "zh-CN" to R.string.languages_zh_cn,
        "zh-TW" to R.string.languages_zh_tw,
        "co" to R.string.languages_co,
        "hr" to R.string.languages_hr,
        "cs" to R.string.languages_cs,
        "da" to R.string.languages_da,
        "prs" to R.string.languages_prs,
        "dv" to R.string.languages_dv,
        "nl" to R.string.languages_nl,
        "dz" to R.string.languages_dz,
        "en" to R.string.languages_en,
        "eo" to R.string.languages_eo,
        "et" to R.string.languages_et,
        "fi" to R.string.languages_fi,
        "fr" to R.string.languages_fr,
        "fy" to R.string.languages_fy,
        "ff" to R.string.languages_ff,
        "gl" to R.string.languages_gl,
        "ka" to R.string.languages_ka,
        "de" to R.string.languages_de,
        "el" to R.string.languages_el,
        "gn" to R.string.languages_gn,
        "gu" to R.string.languages_gu,
        "ht" to R.string.languages_ht,
        "ha" to R.string.languages_ha,
        "haw" to R.string.languages_haw,
        "he" to R.string.languages_he,
        "hi" to R.string.languages_hi,
        "hmn" to R.string.languages_hmn,
        "hu" to R.string.languages_hu,
        "is" to R.string.languages_is,
        "ig" to R.string.languages_ig,
        "ilo" to R.string.languages_ilo,
        "id" to R.string.languages_id,
        "ga" to R.string.languages_ga,
        "it" to R.string.languages_it,
        "ja" to R.string.languages_ja,
        "jv" to R.string.languages_jv,
        "kn" to R.string.languages_kn,
        "kk" to R.string.languages_kk,
        "km" to R.string.languages_km,
        "rw" to R.string.languages_rw,
        "gom" to R.string.languages_gom,
        "kri" to R.string.languages_kri,
        "ko" to R.string.languages_ko,
        "ku" to R.string.languages_ku,
        "ckb" to R.string.languages_ckb,
        "ky" to R.string.languages_ky,
        "lo" to R.string.languages_lo,
        "la" to R.string.languages_la,
        "lv" to R.string.languages_lv,
        "ln" to R.string.languages_ln,
        "lg" to R.string.languages_lg,
        "lt" to R.string.languages_lt,
        "lb" to R.string.languages_lb,
        "mk" to R.string.languages_mk,
        "mai" to R.string.languages_mai,
        "mg" to R.string.languages_mg,
        "ms" to R.string.languages_ms,
        "ml" to R.string.languages_ml,
        "mt" to R.string.languages_mt,
        "mi" to R.string.languages_mi,
        "mr" to R.string.languages_mr,
        "mni-Mtei" to R.string.languages_mni_mtei,
        "mn" to R.string.languages_mn,
        "zgh" to R.string.languages_zgh,
        "ne" to R.string.languages_ne,
        "nso" to R.string.languages_nso,
        "no" to R.string.languages_no,
        "oc" to R.string.languages_oc,
        "or" to R.string.languages_or,
        "om" to R.string.languages_om,
        "pam" to R.string.languages_pam,
        "pap" to R.string.languages_pap,
        "ps" to R.string.languages_ps,
        "fa" to R.string.languages_fa,
        "pl" to R.string.languages_pl,
        "pt" to R.string.languages_pt,
        "pa" to R.string.languages_pa,
        "qu" to R.string.languages_qu,
        "ro" to R.string.languages_ro,
        "ru" to R.string.languages_ru,
        "sm" to R.string.languages_sm,
        "sa" to R.string.languages_sa,
        "gd" to R.string.languages_gd,
        "sr" to R.string.languages_sr,
        "scn" to R.string.languages_scn,
        "szl" to R.string.languages_szl,
        "sn" to R.string.languages_sn,
        "sd" to R.string.languages_sd,
        "si" to R.string.languages_si,
        "sk" to R.string.languages_sk,
        "sl" to R.string.languages_sl,
        "so" to R.string.languages_so,
        "st" to R.string.languages_st,
        "ss" to R.string.languages_ss,
        "es" to R.string.languages_es,
        "su" to R.string.languages_su,
        "sw" to R.string.languages_sw,
        "sv" to R.string.languages_sv,
        "tl" to R.string.languages_tl,
        "tg" to R.string.languages_tg,
        "ta" to R.string.languages_ta,
        "tt" to R.string.languages_tt,
        "te" to R.string.languages_te,
        "tet" to R.string.languages_tet,
        "th" to R.string.languages_th,
        "bo" to R.string.languages_bo,
        "ti" to R.string.languages_ti,
        "tpi" to R.string.languages_tpi,
        "to" to R.string.languages_to,
        "tr" to R.string.languages_tr,
        "tk" to R.string.languages_tk,
        "ts" to R.string.languages_ts,
        "tn" to R.string.languages_tn,
        "ug" to R.string.languages_ug,
        "uk" to R.string.languages_uk,
        "ur" to R.string.languages_ur,
        "uz" to R.string.languages_uz,
        "vi" to R.string.languages_vi,
        "cy" to R.string.languages_cy,
        "wo" to R.string.languages_wo,
        "xh" to R.string.languages_xh,
        "yi" to R.string.languages_yi,
        "yo" to R.string.languages_yo,
        "zu" to R.string.languages_zu
    )

    fun localizedName(context: Context, language: Language): String {
        val translated = languageNameIds[language.code]?.let(context::getString) ?: language.name
        if (translated != language.name) return translated

        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]
            ?: java.util.Locale.getDefault()
        if (locale.language == "en") return translated
        val name = java.util.Locale.forLanguageTag(language.code).getDisplayName(locale)
        return name.takeUnless { it.isBlank() || it.equals(language.code, ignoreCase = true) }
            ?: translated
    }

    private fun canonicalCode(value: String?): String? = value
        ?.trim()
        ?.replace('_', '-')
        ?.lowercase(java.util.Locale.ROOT)
        ?.let(supportedCodes::get)

    private fun key(userId: Int?, conversation: ConversationDto): String? {
        val id = conversation.id ?: return null
        if (userId == null || userId <= 0 || id <= 0) return null
        return "$userId:${conversation.kind}:$id"
    }

    fun read(context: Context, userId: Int?, conversation: ConversationDto): String? {
        val key = key(userId, conversation) ?: return null
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, null)
            .let(::canonicalCode)
    }

    fun save(context: Context, userId: Int?, conversation: ConversationDto, languageTag: String?) {
        val key = key(userId, conversation) ?: return
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        if (languageTag == null) editor.remove(key)
        else {
            val code = canonicalCode(languageTag) ?: return
            editor.putString(key, code)
        }
        editor.apply()
    }

    fun hintLocales(languageTag: String?): LocaleList? {
        val code = canonicalCode(languageTag) ?: return null
        return when (code) {
            // Preserve specified scripts/dialects and match the app's Bokmål
            // mapping. Include legacy region/generic tags for compatible IMEs.
            "zh-CN" -> LocaleList(Locale("zh-Hans-CN"), Locale("zh-CN"))
            "zh-TW" -> LocaleList(Locale("zh-Hant-TW"), Locale("zh-TW"))
            "ku" -> LocaleList(Locale("kmr"), Locale("ku"))
            "no" -> LocaleList(Locale("nb"), Locale("no"))
            else -> LocaleList(Locale(code))
        }
    }
}
