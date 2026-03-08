

package org.meshtastic.feature.settings.util

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.core.os.LocaleListCompat
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.fr_HT
import org.meshtastic.core.strings.preferences_system_default
import org.meshtastic.core.strings.pt_BR
import org.meshtastic.core.strings.zh_CN
import org.meshtastic.core.strings.zh_TW
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

object LanguageUtils {

    const val SYSTEM_DEFAULT = "zz"

    fun setAppLocale(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (languageTag == SYSTEM_DEFAULT) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(languageTag)
            },
        )
    }

    @Suppress("CyclomaticComplexMethod")
    @Composable
    fun languageMap(): Map<String, String> {
        val resources = LocalResources.current
        val languageTags =
            remember(resources) {
                buildList {
                    add(SYSTEM_DEFAULT)

                    try {
                        resources.getXml(org.meshtastic.feature.settings.R.xml.locales_config).use { parser ->
                            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                                    val languageTag =
                                        parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                                    languageTag?.let { add(it) }
                                }
                                parser.next()
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }

        return languageTags.associateWith { languageTag ->
            when (languageTag) {
                SYSTEM_DEFAULT -> stringResource(Res.string.preferences_system_default)
                "fr-HT" -> stringResource(Res.string.fr_HT)
                "pt-BR" -> stringResource(Res.string.pt_BR)
                "zh-CN" -> stringResource(Res.string.zh_CN)
                "zh-TW" -> stringResource(Res.string.zh_TW)
                else -> {
                    Locale.forLanguageTag(languageTag).let { locale ->
                        locale.getDisplayLanguage(locale).replaceFirstChar { char ->
                            if (char.isLowerCase()) char.titlecase(locale) else char.toString()
                        }
                    }
                }
            }
        }
    }
}

