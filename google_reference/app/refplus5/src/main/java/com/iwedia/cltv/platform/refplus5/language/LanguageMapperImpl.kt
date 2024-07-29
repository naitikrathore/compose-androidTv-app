package com.iwedia.cltv.platform.refplus5.language

import com.iwedia.cltv.platform.base.language.LanguageMapperBaseImpl
import com.iwedia.cltv.platform.model.language.LanguageCode

internal class LanguageMapperImpl(private val countryCode : String) : LanguageMapperBaseImpl() {

    init {
        preferredLanguageCodes.clear()

        preferredLanguageCodes.add(LanguageCode("baq (B)", "", "Basque", "basque", "Baskisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "bul",
                "bg",
                "Bulgarian",
                "bulgare",
                "Bulgarisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "yue",
                "",
                "Cantonese",
                "",
                ""
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "cat",
                "ca",
                "Catalan",
                "catalan; valencien",
                "Katalanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("cze", "", "Czech", "tchèque", "Tschechisch"))
        preferredLanguageCodes.add(LanguageCode("dan", "da", "Danish", "danois", "Dänisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "nld",
                "nl",
                "Dutch",
                "néerlandais; flamand",
                "Niederländisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("eng", "en", "English", "anglais", "Englisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "est",
                "et",
                "Estonian",
                "estonien",
                "Estnisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("fin", "fi", "Finnish", "finnois", "Finnisch"))
        preferredLanguageCodes.add(LanguageCode("fra", "fr", "French", "français", "Französisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "gla",
                "gd",
                "Scottish Gaelic",
                "gaélique; gaélique écossais",
                "Gälisch-Schottisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("gle", "ga", "Irish", "irlandais", "Irisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "glg",
                "gl",
                "Galician",
                "galicien",
                "Galicisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("ger", "", "German", "allemand", "Deutsch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ell",
                "el",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("hin", "hi", "Hindi", "hindi", "Hindi"))
        preferredLanguageCodes.add(
            LanguageCode(
                "hun",
                "hu",
                "Hungarian",
                "hongrois",
                "Ungarisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "ita",
                "it",
                "Italian",
                "italien",
                "Italienisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "ice (B)",
                "",
                "Icelandic",
                "islandais",
                "Isländisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "jpn",
                "ja",
                "Japanese",
                "japonais",
                "Japanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("kor", "ko", "Korean", "coréen", "Koreanisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "nor",
                "no",
                "Norwegian",
                "norvégien nynorsk; nynorsk, norvégien",
                "Nynorsk"
            )
        )
        preferredLanguageCodes.add(LanguageCode("pol", "pl", "Polish", "polonais", "Polnisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "por",
                "pt",
                "Portuguese",
                "portugais",
                "Portugiesisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "ron",
                "ro",
                "Romanian",
                "roumain; moldave",
                "Rumänisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("rus", "ru", "Russian", "russe", "Russisch"))
        preferredLanguageCodes.add(LanguageCode("srp", "sr", "Serbian", "serbe", "Serbisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "slk",
                "sk",
                "Slovak",
                "slovaque",
                "Slowakisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "slv",
                "sl",
                "Slovenian",
                "slovène",
                "Slowenisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "spa",
                "es",
                "Spanish",
                "espagnol; castillan",
                "Spanisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "swe",
                "sv",
                "Swedish",
                "suédois",
                "Schwedisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("tur", "tr", "Turkish", "turc", "Türkisch"))
        preferredLanguageCodes.add(LanguageCode("wel (B)", "", "Welsh", "gallois", "Kymrisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ind",
                "id",
                "Indonesian",
                "indonésien",
                "Bahasa Indonesia"
            )
        )
        preferredLanguageCodes.add(LanguageCode("tha", "th", "Thai", "thaï", "Thailändisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "msa",
                "ms",
                "Bahasa Melayu",
                "malais",
                "Malaiisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "vie",
                "vi",
                "Vietnamese",
                "vietnamien",
                "Vietnamesisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "fij",
                "fj",
                "Fijian",
                "fidjien",
                "Fidschi-Sprache"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "khm",
                "km",
                "Central Khmer",
                "khmer central",
                "Kambodschanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("mya", "my", "Burmese", "birman", "Birmanisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "sin",
                "si",
                "Sinhala",
                "singhalais",
                "Singhalesisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("tam", "ta", "Tamil", "tamoul", "Tamil"))
        preferredLanguageCodes.add(LanguageCode("nep", "ne", "Nepali", "népalais", "Nepali"))
        preferredLanguageCodes.add(
            LanguageCode(
                "div",
                "dv",
                "Divehi",
                "maldivien",
                "Maledivisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("lao", "lo", "Lao", "lao", "Laotisch"))
        preferredLanguageCodes.add(LanguageCode("mao", "", "Maori", "maori", "Maori-Sprache"))
        preferredLanguageCodes.add(
            LanguageCode(
                "tpi",
                "",
                "Tok Pisin",
                "tok pisin",
                "Neumelanesisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "mon",
                "mn",
                "Mongolian",
                "mongol",
                "Mongolisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "tuk",
                "tk",
                "Turkmen",
                "turkmène",
                "Turkmenisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("arm", "", "Armenian", "arménien", "Armenisch"))
        preferredLanguageCodes.add(LanguageCode("kaz", "kk", "Kazakh", "kazakh", "Kasachisch"))
        preferredLanguageCodes.add(LanguageCode("ara", "ar", "Arabic", "arabe", "Arabisch"))
        preferredLanguageCodes.add(LanguageCode("kur", "ku", "Kurdish", "kurde", "Kurdisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "aze",
                "az",
                "Azerbaijani",
                "azéri",
                "Aserbeidschanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("uzb", "uz", "Uzbek", "ouszbek", "Usbekisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ukr",
                "uk",
                "Ukrainian",
                "ukrainien",
                "Ukrainisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("tgk", "tg", "Tajik", "tadjik", "Tadschikisch"))
        preferredLanguageCodes.add(LanguageCode("per", "", "Persian", "persan", "Persisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "geo (B)",
                "",
                "Georgian",
                "géorgien",
                "Georgisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("urd", "ur", "Urdu", "ourdou", "Urdu"))
        preferredLanguageCodes.add(
            LanguageCode(
                "mkd",
                "mk",
                "Macedonian",
                "macédonien",
                "Makedonisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("som", "so", "Somali", "somali", "Somali"))
        preferredLanguageCodes.add(
            LanguageCode(
                "amh",
                "am",
                "Amharic",
                "amharique",
                "Amharisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "kin",
                "rw",
                "Kinyarwanda",
                "rwanda",
                "Rwanda-Sprache"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "chi (B)",
                "",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "mal",
                "ml",
                "Malayalam",
                "malayalam",
                "Malayalam"
            )
        )
        preferredLanguageCodes.add(LanguageCode("gaa", "", "Ga", "ga", "Ga-Sprache"))
        preferredLanguageCodes.add(
            LanguageCode(
                "mul",
                "",
                "Multiple languages",
                "multilingue",
                "Mehrere Sprachen"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "zho (T)",
                "zh",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "sme",
                "se",
                "Northern Sami",
                "sami du Nord",
                "Nordsaamisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "ltz",
                "lb",
                "Luxembourgish",
                "luxembourgeois",
                "Luxemburgisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "sqi",
                "sq",
                "Albanian",
                "albanais",
                "Albanisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "lit",
                "lt",
                "Lithuanian",
                "lituanien",
                "Litauisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("lav", "lv", "Latvian", "letton", "Lettisch"))
        //To add Valencian
        //To add Bangla
        preferredLanguageCodes.add(LanguageCode("swa", "sw", "Swahili", "swahili", "Swahili"))
        preferredLanguageCodes.add(LanguageCode("hrv", "hr", "Croatian", "croate", "Kroatisch"))
        preferredLanguageCodes.add(LanguageCode("heb", "he", "Hebrew", "hébreu", "Hebräisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "afr",
                "af",
                "Afrikaans",
                "afrikaans",
                "Afrikaans"
            )
        )
        //To add Kasem
        preferredLanguageCodes.add(
            LanguageCode(
                "bel",
                "be",
                "Belarusian",
                "biélorusse",
                "Weißrussisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("qaa", "qaa", "Original Audio", "Audio original", "Original-Audio",
            hideInAudioPref = false,
            hideInSubtitlePref = true
        ))

        if(countryCode.uppercase() == "MY") {
            preferredLanguageCodes.clear()

            preferredLanguageCodes.add(LanguageCode("eng", "en", "English", "anglais", "Englisch"))
            preferredLanguageCodes.add(
                LanguageCode(
                    "msa",
                    "ms",
                    "Bahasa melayu",
                    "malais",
                    "Malaiisch"
                )
            )
            preferredLanguageCodes.add(
                LanguageCode(
                    "zho",
                    "zh",
                    "Chinese",
                    "chinois",
                    "Chinesisch"
                )
            )
            preferredLanguageCodes.add(LanguageCode("tam", "ta", "Tamil", "tamoul", "Tamil"))
            preferredLanguageCodes.add(LanguageCode("qaa", "qaa", "Original Audio", "Audio original", "Original-Audio",
                hideInAudioPref = false,
                hideInSubtitlePref = true
            ))

        }
    }

    override fun getLanguageName(languageCode: String): String? {

        for (languageCodeItem in preferredLanguageCodes) {
            if (languageCodeItem.languageCodeISO6391 == languageCode || languageCodeItem.languageCodeISO6392 == languageCode) {
                return languageCodeItem.englishName
            }
        }
        return super.getLanguageName(languageCode)
    }

    override fun getLanguageCode(trackLanguage: String): String {
        if (countryCode.uppercase() == "MY" || countryCode.uppercase() == "ID" || countryCode.uppercase() == "TH") {
            if (trackLanguage.length >= 3) {
                return trackLanguage.substring(0, 3)
            }
        }
        return super.getLanguageCode(trackLanguage)
    }
}