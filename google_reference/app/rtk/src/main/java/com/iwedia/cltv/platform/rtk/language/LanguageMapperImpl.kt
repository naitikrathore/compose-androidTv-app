package com.iwedia.cltv.platform.rtk.language

import com.iwedia.cltv.platform.base.language.LanguageMapperBaseImpl
import com.iwedia.cltv.platform.model.language.LanguageCode

internal class LanguageMapperImpl : LanguageMapperBaseImpl() {
    init {
        preferredLanguageCodes.clear()

        preferredLanguageCodes.add(LanguageCode("eng", "en", "English", "anglais", "Englisch"))
        preferredLanguageCodes.add(LanguageCode("wel (B)", "", "Welsh", "gallois", "Kymrisch"))
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
        preferredLanguageCodes.add(LanguageCode("deu", "de", "German", "allemand", "Deutsch"))
        preferredLanguageCodes.add(LanguageCode("fra", "fr", "French", "français", "Französisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "nld",
                "nl",
                "Dutch",
                "néerlandais; flamand",
                "Niederländisch"
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
                "por",
                "pt",
                "Portuguese",
                "portugais",
                "Portugiesisch"
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
        preferredLanguageCodes.add(LanguageCode("tur", "tr", "Turkish", "turc", "Türkisch"))
        preferredLanguageCodes.add(LanguageCode("dan", "da", "Danish", "danois", "Dänisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "swe",
                "sv",
                "Swedish",
                "suédois",
                "Schwedisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("fin", "fi", "Finnish", "finnois", "Finnisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "nno",
                "nn",
                "Norwegian",
                "norvégien nynorsk; nynorsk, norvégien",
                "Nynorsk"
            )
        )
        preferredLanguageCodes.add(LanguageCode("pol", "pl", "Polish", "polonais", "Polnisch"))
        preferredLanguageCodes.add(LanguageCode("baq (B)", "", "Basque", "basque", "Baskisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "cat",
                "ca",
                "Catalan",
                "catalan; valencien",
                "Katalanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("cos", "co", "Corsican", "corse", "Korsisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "glg",
                "gl",
                "Galician",
                "galicien",
                "Galicisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("cze", "", "Czech", "tchèque", "Tschechisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ell",
                "el",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
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
                "slk",
                "sk",
                "Slovak",
                "slovaque",
                "Slowakisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("hrv", "hr", "Croatian", "croate", "Kroatisch"))
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
                "est",
                "et",
                "Estonian",
                "estonien",
                "Estnisch"
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
        preferredLanguageCodes.add(
            LanguageCode(
                "hun",
                "hu",
                "Hungarian",
                "hongrois",
                "Ungarisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("lav", "lv", "Latvian", "letton", "Lettisch"))
        preferredLanguageCodes.add(LanguageCode("srp", "sr", "Serbian", "serbe", "Serbisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ron",
                "ro",
                "Romanian",
                "roumain; moldave",
                "Rumänisch"
            )
        )
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
                "mkd",
                "mk",
                "Macedonian",
                "macédonien",
                "Makedonisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("rus", "ru", "Russian", "russe", "Russisch"))
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
                "chi",
                "",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("ara", "ar", "Arabic", "arabe", "Arabisch"))
        preferredLanguageCodes.add(LanguageCode("arm", "", "Armenian", "arménien", "Armenisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "aze",
                "az",
                "Azerbaijani",
                "azéri",
                "Aserbeidschanisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("heb", "he", "Hebrew", "hébreu", "Hebräisch"))
        preferredLanguageCodes.add(LanguageCode("per", "", "Persian", "persan", "Persisch"))
        preferredLanguageCodes.add(LanguageCode("bos", "bs", "Bosnian", "bosniaque", "Bosnisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "bel",
                "be",
                "Belarusian",
                "biélorusse",
                "Weißrussisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("uzb", "uz", "Uzbek", "ouszbek", "Usbekisch"))
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
                "ice (B)",
                "",
                "Icelandic",
                "islandais",
                "Isländisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("hin", "hi", "Hindi", "hindi", "Hindi"))
        preferredLanguageCodes.add(LanguageCode("tha", "th", "Thai", "thaï", "Thailändisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "ukr",
                "uk",
                "Ukrainian",
                "ukrainien",
                "Ukrainisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("mlt", "mt", "Maltese", "maltais", "Maltesisch"))
        preferredLanguageCodes.add(LanguageCode("kor", "ko", "Korean", "coréen", "Koreanisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "jpn",
                "ja",
                "Japanese",
                "japonais",
                "Japanisch"
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
        preferredLanguageCodes.add(LanguageCode("mao", "", "Maori", "maori", "Maori-Sprache"))
        preferredLanguageCodes.add(
            LanguageCode(
                "msa",
                "ms",
                "Bahasa Melayu",
                "malais",
                "Malaiisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("tam", "ta", "Tamil", "tamoul", "Tamil"))
        preferredLanguageCodes.add(LanguageCode("ben", "bn", "Bengali", "bengali", "Bengali"))
        preferredLanguageCodes.add(LanguageCode("kir", "ky", "Kirghiz; Kyrgyz", "kirghiz", "Kirgisisch"))
        preferredLanguageCodes.add(LanguageCode("urd", "ur", "Urdu", "ourdou", "Urdu"))
        preferredLanguageCodes.add(LanguageCode("nep", "ne", "Nepali", "népalais", "Nepali"))
        preferredLanguageCodes.add(LanguageCode("mya", "my", "Burmese", "birman", "Birmanisch"))
        preferredLanguageCodes.add(LanguageCode("swa", "sw", "Swahili", "swahili", "Swahili"))
        preferredLanguageCodes.add(LanguageCode("kaz", "kk", "Kazakh", "kazakh", "Kasachisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "geo (B)",
                "",
                "Georgian",
                "géorgien",
                "Georgisch"
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
        preferredLanguageCodes.add(LanguageCode("cnr", "", "Montenegrin", "monténégrin", "Montenegrinisch"))
        preferredLanguageCodes.add(LanguageCode("qaa", "qaa", "Original", "original", "Original"))
    }
}