package com.iwedia.cltv.platform.base.language

import android.util.Log
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.language.LanguageCode

open class LanguageMapperBaseImpl : LanguageMapperInterface {

    private var languageCodes = mutableListOf<LanguageCode>()
    override var preferredLanguageCodes = mutableListOf<LanguageCode>()
    override var countryCodeToLanguageCodeMap = mutableMapOf<String, String?>()
    private var countryCodeToTxtDigitalLanguageMap = mutableMapOf<String, Int>()
    private var countryCodeToTxtDecodeLanguageMap = mutableMapOf<String, Int>()
    private var ttxPositionToDigitalLanguageMap = mutableMapOf<Int, Int>()
    private var ttxPositionToDecodeLanguageMap = mutableMapOf<Int, Int>()
    private val defaultLanguageCode = LanguageCode("eng", "en", "English", "anglais", "Englisch")
    override fun getLanguageName(languageCode: String): String? {
        for (languageCodeItem in languageCodes) {
            if (languageCodeItem.languageCodeISO6391.lowercase() == languageCode.lowercase()
                || languageCodeItem.languageCodeISO6392.lowercase() == languageCode.lowercase()) {
                return languageCodeItem.englishName
            }
        }

        return null
    }

    override fun getPreferredLanguageName(languageCode: String): String? {
        for (languageCodeItem in preferredLanguageCodes) {
            if (languageCodeItem.languageCodeISO6391 == languageCode || languageCodeItem.languageCodeISO6392 == languageCode) {
                return languageCodeItem.englishName
            }
        }

        return null
    }

    override fun getLanguageCodeByCountryCode(countryCode: String?): String? {
        return countryCodeToLanguageCodeMap[countryCode]
    }

    override fun getTxtDigitalLanguageMapByCountryCode(countryCode: String?): Int? {
        return countryCodeToTxtDigitalLanguageMap[countryCode]
    }

    override fun getTxtDigitalLanguageMapByPosition(position: Int?): Int? {
        return ttxPositionToDigitalLanguageMap[position]
    }

    override fun getLanguageCodes(): MutableList<LanguageCode> {
        return languageCodes;
    }

    override fun getDefaultLanguageCode(): LanguageCode {
        return defaultLanguageCode
    }

    override fun getLanguageCode(trackLanguage: String): String {
        return if (trackLanguage.length > 2) {
            trackLanguage.substring(0, 2)
        } else {
            trackLanguage
        }
    }


    init {
        countryCodeToLanguageCodeMap.put("ALB", "sqi")
        countryCodeToLanguageCodeMap.put("AUT", "deu")
        countryCodeToLanguageCodeMap.put("BLR", "bel")
        countryCodeToLanguageCodeMap.put("BEL", "nld")
        countryCodeToLanguageCodeMap.put("BGR", "bul")
        countryCodeToLanguageCodeMap.put("HRV", "hrv")
        countryCodeToLanguageCodeMap.put("CYP", "ell")
        countryCodeToLanguageCodeMap.put("CZE", "ces")
        countryCodeToLanguageCodeMap.put("DNK", "dan")
        countryCodeToLanguageCodeMap.put("EST", "est")
        countryCodeToLanguageCodeMap.put("FIN", "fin")
        countryCodeToLanguageCodeMap.put("FRA", "fra")
        countryCodeToLanguageCodeMap.put("DEU", "deu")
        countryCodeToLanguageCodeMap.put("GRC", "ell")
        countryCodeToLanguageCodeMap.put("HUN", "hun")
        countryCodeToLanguageCodeMap.put("IRL", "eng")
        countryCodeToLanguageCodeMap.put("ITA", "ita")
        countryCodeToLanguageCodeMap.put("LVA", "lav")
        countryCodeToLanguageCodeMap.put("LTU", "lit")
        countryCodeToLanguageCodeMap.put("LUX", "deu")
        countryCodeToLanguageCodeMap.put("MKD", "mkd")
        countryCodeToLanguageCodeMap.put("NLD", "nld")
        countryCodeToLanguageCodeMap.put("NOR", "nno")
        countryCodeToLanguageCodeMap.put("POL", "pol")
        countryCodeToLanguageCodeMap.put("PRT", "por")
        countryCodeToLanguageCodeMap.put("ROU", "ron")
        countryCodeToLanguageCodeMap.put("RUS", "rus")
        countryCodeToLanguageCodeMap.put("SRB", "srp")
        countryCodeToLanguageCodeMap.put("SVK", "slk")
        countryCodeToLanguageCodeMap.put("SVN", "slv")
        countryCodeToLanguageCodeMap.put("ESP", "spa")
        countryCodeToLanguageCodeMap.put("SWE", "swe")
        countryCodeToLanguageCodeMap.put("CHE", "deu")
        countryCodeToLanguageCodeMap.put("TUR", "tur")
        countryCodeToLanguageCodeMap.put("GBR", "eng")

        //TTX DigitalLanguages by country
        countryCodeToTxtDigitalLanguageMap.put("AUT", 15)
        countryCodeToTxtDigitalLanguageMap.put("BEL", 15)
        countryCodeToTxtDigitalLanguageMap.put("BGR", 1)
        countryCodeToTxtDigitalLanguageMap.put("HRV", 4)
        countryCodeToTxtDigitalLanguageMap.put("CZE", 5)
        countryCodeToTxtDigitalLanguageMap.put("DNK", 6)
        countryCodeToTxtDigitalLanguageMap.put("EST", 9)
        countryCodeToTxtDigitalLanguageMap.put("FIN", 10)
        countryCodeToTxtDigitalLanguageMap.put("FRA", 11)
        countryCodeToTxtDigitalLanguageMap.put("DEU", 15)
        countryCodeToTxtDigitalLanguageMap.put("GRC", 16)
        countryCodeToTxtDigitalLanguageMap.put("HUN", 18)
        countryCodeToTxtDigitalLanguageMap.put("IRL", 13)
        countryCodeToTxtDigitalLanguageMap.put("ITA", 19)
        countryCodeToTxtDigitalLanguageMap.put("LUX", 72)
        countryCodeToTxtDigitalLanguageMap.put("MKD", 66)
        countryCodeToTxtDigitalLanguageMap.put("NLD", 7)
        countryCodeToTxtDigitalLanguageMap.put("NOR", 24)
        countryCodeToTxtDigitalLanguageMap.put("POL", 25)
        countryCodeToTxtDigitalLanguageMap.put("ROU", 27)
        countryCodeToTxtDigitalLanguageMap.put("RUS", 28)
        countryCodeToTxtDigitalLanguageMap.put("SRB", 30)
        countryCodeToTxtDigitalLanguageMap.put("SVK", 31)
        countryCodeToTxtDigitalLanguageMap.put("SVN", 31)
        countryCodeToTxtDigitalLanguageMap.put("ESP", 33)
        countryCodeToTxtDigitalLanguageMap.put("SWE", 34)
        countryCodeToTxtDigitalLanguageMap.put("CHE", 15)
        countryCodeToTxtDigitalLanguageMap.put("TUR", 35)
        countryCodeToTxtDigitalLanguageMap.put("GBR", 8)

        //TTX Decode Languages by country
        countryCodeToTxtDecodeLanguageMap.put("AUT", 0)
        countryCodeToTxtDecodeLanguageMap.put("BEL", 0)
        countryCodeToTxtDecodeLanguageMap.put("BGR", 0)
        countryCodeToTxtDecodeLanguageMap.put("HRV", 0)
        countryCodeToTxtDecodeLanguageMap.put("CZE", 0)
        countryCodeToTxtDecodeLanguageMap.put("DNK", 0)
        countryCodeToTxtDecodeLanguageMap.put("EST", 2)
        countryCodeToTxtDecodeLanguageMap.put("FIN", 0)
        countryCodeToTxtDecodeLanguageMap.put("FRA", 0)
        countryCodeToTxtDecodeLanguageMap.put("DEU", 0)
        countryCodeToTxtDecodeLanguageMap.put("GRC", 0)
        countryCodeToTxtDecodeLanguageMap.put("HUN", 0)
        countryCodeToTxtDecodeLanguageMap.put("IRL", 0)
        countryCodeToTxtDecodeLanguageMap.put("ITA", 0)
        countryCodeToTxtDecodeLanguageMap.put("LUX", 0)
        countryCodeToTxtDecodeLanguageMap.put("MKD", 0)
        countryCodeToTxtDecodeLanguageMap.put("NLD", 0)
        countryCodeToTxtDecodeLanguageMap.put("NOR", 0)
        countryCodeToTxtDecodeLanguageMap.put("POL", 0)
        countryCodeToTxtDecodeLanguageMap.put("ROU", 0)
        countryCodeToTxtDecodeLanguageMap.put("RUS", 2)
        countryCodeToTxtDecodeLanguageMap.put("SRB", 0)
        countryCodeToTxtDecodeLanguageMap.put("SVK", 0)
        countryCodeToTxtDecodeLanguageMap.put("SVN", 0)
        countryCodeToTxtDecodeLanguageMap.put("ESP", 0)
        countryCodeToTxtDecodeLanguageMap.put("SWE", 0)
        countryCodeToTxtDecodeLanguageMap.put("CHE", 0)
        countryCodeToTxtDecodeLanguageMap.put("TUR", 5)
        countryCodeToTxtDecodeLanguageMap.put("GBR", 0)

        //TTX Digital Values by Position
        ttxPositionToDigitalLanguageMap.put(1, 8)
        ttxPositionToDigitalLanguageMap.put(2, 24)
        ttxPositionToDigitalLanguageMap.put(3, 6)
        ttxPositionToDigitalLanguageMap.put(4, 34)
        ttxPositionToDigitalLanguageMap.put(5, 10)
        ttxPositionToDigitalLanguageMap.put(6, 15)
        ttxPositionToDigitalLanguageMap.put(7, 11)
        ttxPositionToDigitalLanguageMap.put(8, 19)
        ttxPositionToDigitalLanguageMap.put(9, 7)
        ttxPositionToDigitalLanguageMap.put(10, 28)
        ttxPositionToDigitalLanguageMap.put(11, 35)
        ttxPositionToDigitalLanguageMap.put(12, 52)
        ttxPositionToDigitalLanguageMap.put(12, 52)
        ttxPositionToDigitalLanguageMap.put(13, 57)


        //TTX Decode Page Language by Position
        ttxPositionToDecodeLanguageMap.put(10, 2)
        ttxPositionToDecodeLanguageMap.put(11, 5)
        ttxPositionToDecodeLanguageMap.put(12, 6)
        ttxPositionToDecodeLanguageMap.put(13, 8)



        preferredLanguageCodes.add(LanguageCode("sqi", "sq", "Albanian", "albanais", "Albanisch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "bel",
                "be",
                "Belarusian",
                "biélorusse",
                "Weißrussisch"
            )
        )
        preferredLanguageCodes.add(LanguageCode("bul", "bg", "Bulgarian", "bulgare", "Bulgarisch"))
        preferredLanguageCodes.add(LanguageCode("ces", "cs", "Czech", "tchèque", "Tschechisch"))
        preferredLanguageCodes.add(LanguageCode("dan", "da", "Danish", "danois", "Dänisch"))
        preferredLanguageCodes.add(LanguageCode("deu", "de", "German", "allemand", "Deutsch"))
        preferredLanguageCodes.add(
            LanguageCode(
                "nld", "nl", "Dutch", "néerlandais; flamand", "Niederländisch"
            )
        )
        preferredLanguageCodes.add(
            LanguageCode(
                "ell",
                "el",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        preferredLanguageCodes.add(defaultLanguageCode)
        preferredLanguageCodes.add(LanguageCode("fin", "fi", "Finnish", "finnois", "Finnisch"))
        preferredLanguageCodes.add(LanguageCode("fra", "fr", "French", "français", "Französisch"))
        preferredLanguageCodes.add(LanguageCode("hrv", "hr", "Croatian", "croate", "Kroatisch"))
        preferredLanguageCodes.add(LanguageCode("hun", "hu", "Hungarian", "hongrois", "Ungarisch"))
        preferredLanguageCodes.add(LanguageCode("ita", "it", "Italian", "italien", "Italienisch"))
        preferredLanguageCodes.add(LanguageCode("lav", "lv", "Latvian", "letton", "Lettisch"))
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
                "mkd",
                "mk",
                "Macedonian",
                "macédonien",
                "Makedonisch"
            )
        )
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
        preferredLanguageCodes.add(LanguageCode("slk", "sk", "Slovak", "slovaque", "Slowakisch"))
        preferredLanguageCodes.add(LanguageCode("slv", "sl", "Slovenian", "slovène", "Slowenisch"))
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
                "srp",
                "sr",
                "Serbian",
                "serbe",
                "Serbisch"
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
        preferredLanguageCodes.add(
            LanguageCode(
                "tur",
                "tr",
                "Turkish",
                "turc",
                "Türkisch"
            )
        )
//    constructor() {

        languageCodes.add(LanguageCode("aar", "aa", "Afar", "afar", "Danakil-Sprache"))
        languageCodes.add(LanguageCode("abk", "ab", "Abkhazian", "abkhaze", "Abchasisch"))
        languageCodes.add(LanguageCode("ace", "", "Achinese", "aceh", "Aceh-Sprache"))
        languageCodes.add(LanguageCode("ach", "", "Acoli", "acoli", "Acholi-Sprache"))
        languageCodes.add(LanguageCode("ada", "", "Adangme", "adangme", "Adangme-Sprache"))
        languageCodes.add(LanguageCode("ady", "", "Adyghe; Adygei", "adyghé", "Adygisch"))
        languageCodes.add(
            LanguageCode(
                "afa",
                "",
                "Afro-Asiatic languages",
                "afro-asiatique, langues",
                "Hamitosemitische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("afh", "", "Afrihili", "afrihili", "Afrihili"))
        languageCodes.add(LanguageCode("afr", "af", "Afrikaans", "afrikaans", "Afrikaans"))
        languageCodes.add(LanguageCode("ain", "", "Ainu", "aïnou", "Ainu-Sprache"))
        languageCodes.add(LanguageCode("aka", "ak", "Akan", "akan", "Akan-Sprache"))
        languageCodes.add(LanguageCode("akk", "", "Akkadian", "akkadien", "Akkadisch"))
        languageCodes.add(LanguageCode("alb (B)", "", "Albanian", "albanais", "Albanisch"))
        languageCodes.add(LanguageCode("sqi", "sq", "Albanian", "albanais", "Albanisch"))
        languageCodes.add(LanguageCode("ale", "", "Aleut", "aléoute", "Aleutisch"))
        languageCodes.add(
            LanguageCode(
                "alg",
                "",
                "Algonquian languages",
                "algonquines, langues",
                "Algonkin-Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("alt", "", "Southern Altai", "altai du Sud", "Altaisch"))
        languageCodes.add(LanguageCode("amh", "am", "Amharic", "amharique", "Amharisch"))
        languageCodes.add(
            LanguageCode(
                "ang",
                "",
                "English Old (ca.450-1100)",
                "anglo-saxon (ca.450-1100)",
                "Altenglisch"
            )
        )
        languageCodes.add(LanguageCode("anp", "", "Angika", "angika", "Anga-Sprache"))
        languageCodes.add(
            LanguageCode(
                "apa",
                "",
                "Apache languages",
                "apaches, langues",
                "Apachen-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("ara", "ar", "Arabic", "arabe", "Arabisch"))
        languageCodes.add(
            LanguageCode(
                "arc",
                "",
                "Official Aramaic (700-300 BCE); Imperial Aramaic (700-300 BCE)",
                "araméen d'empire (700-300 BCE)",
                "Aramäisch"
            )
        )
        languageCodes.add(LanguageCode("arg", "an", "Aragonese", "aragonais", "Aragonesisch"))
        languageCodes.add(LanguageCode("arm (B)", "", "Armenian", "arménien", "Armenisch"))
        languageCodes.add(LanguageCode("hye (T)", "hy", "Armenian", "arménien", "Armenisch"))
        languageCodes.add(
            LanguageCode(
                "arn",
                "",
                "Mapudungun; Mapuche",
                "mapudungun; mapuche; mapuce",
                "Arauka-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("arp", "", "Arapaho", "arapaho", "Arapaho-Sprache"))
        languageCodes.add(
            LanguageCode(
                "art",
                "",
                "Artificial languages",
                "artificielles, langues",
                "Kunstsprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("arw", "", "Arawak", "arawak", "Arawak-Sprachen"))
        languageCodes.add(LanguageCode("asm", "as", "Assamese", "assamais", "Assamesisch"))
        languageCodes.add(
            LanguageCode(
                "ast",
                "",
                "Asturian; Bable; Leonese; Asturleonese",
                "asturien; bable; léonais; asturoléonais",
                "Asturisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ath",
                "",
                "Athapascan languages",
                "athapascanes, langues",
                "Athapaskische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "aus",
                "",
                "Australian languages",
                "australiennes, langues",
                "Australische Sprachen"
            )
        )
        languageCodes.add(LanguageCode("ava", "av", "Avaric", "avar", "Awarisch"))
        languageCodes.add(LanguageCode("ave", "ae", "Avestan", "avestique", "Avestisch"))
        languageCodes.add(LanguageCode("awa", "", "Awadhi", "awadhi", "Awadhi"))
        languageCodes.add(LanguageCode("aym", "ay", "Aymara", "aymara", "Aymará-Sprache"))
        languageCodes.add(LanguageCode("aze", "az", "Azerbaijani", "azéri", "Aserbeidschanisch"))
        languageCodes.add(
            LanguageCode(
                "bad",
                "",
                "Banda languages",
                "banda, langues",
                "Banda-Sprachen (Ubangi-Sprachen)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "bai",
                "",
                "Bamileke languages",
                "bamiléké, langues",
                "Bamileke-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("bak", "ba", "Bashkir", "bachkir", "Baschkirisch"))
        languageCodes.add(LanguageCode("bal", "", "Baluchi", "baloutchi", "Belutschisch"))
        languageCodes.add(LanguageCode("bam", "bm", "Bambara", "bambara", "Bambara-Sprache"))
        languageCodes.add(LanguageCode("ban", "", "Balinese", "balinais", "Balinesisch"))
        languageCodes.add(LanguageCode("baq (B)", "", "Basque", "basque", "Baskisch"))
        languageCodes.add(LanguageCode("eus (T)", "eu", "Basque", "basque", "Baskisch"))
        languageCodes.add(LanguageCode("bas", "", "Basa", "basa", "Basaa-Sprache"))
        languageCodes.add(
            LanguageCode(
                "bat",
                "",
                "Baltic languages",
                "baltes, langues",
                "Baltische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("bej", "", "Beja; Bedawiyet", "bedja", "Bedauye"))
        languageCodes.add(LanguageCode("bel", "be", "Belarusian", "biélorusse", "Weißrussisch"))
        languageCodes.add(LanguageCode("bem", "", "Bemba", "bemba", "Bemba-Sprache"))
        languageCodes.add(LanguageCode("ben", "bn", "Bengali", "bengali", "Bengali"))
        languageCodes.add(
            LanguageCode(
                "ber",
                "",
                "Berber languages",
                "berbères, langues",
                "Berbersprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("bho", "", "Bhojpuri", "bhojpuri", "Bhojpuri"))
        languageCodes.add(
            LanguageCode(
                "bih",
                "bh",
                "Bihari languages",
                "langues biharis",
                "Bihari (Andere)"
            )
        )
        languageCodes.add(LanguageCode("bik", "", "Bikol", "bikol", "Bikol-Sprache"))
        languageCodes.add(LanguageCode("bin", "", "Bini; Edo", "bini; edo", "Edo-Sprache"))
        languageCodes.add(LanguageCode("bis", "bi", "Bislama", "bichlamar", "Beach-la-mar"))
        languageCodes.add(LanguageCode("bla", "", "Siksika", "blackfoot", "Blackfoot-Sprache"))
        languageCodes.add(
            LanguageCode(
                "bnt",
                "",
                "Bantu languages",
                "bantou, langues",
                "Bantusprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("tib (B)", "", "Tibetan", "tibétain", "Tibetisch"))
        languageCodes.add(LanguageCode("bod (T)", "bo", "Tibetan", "tibétain", "Tibetisch"))
        languageCodes.add(LanguageCode("bos", "bs", "Bosnian", "bosniaque", "Bosnisch"))
        languageCodes.add(LanguageCode("bra", "", "Braj", "braj", "Braj-Bhakha"))
        languageCodes.add(LanguageCode("bre", "br", "Breton", "breton", "Bretonisch"))
        languageCodes.add(
            LanguageCode(
                "btk",
                "",
                "Batak languages",
                "batak, langues",
                "Batak-Sprache"
            )
        )
        languageCodes.add(LanguageCode("bua", "", "Buriat", "bouriate", "Burjatisch"))
        languageCodes.add(LanguageCode("", "bug", "Buginese", "bugi", "Bugi-Sprache"))
        languageCodes.add(LanguageCode("bul", "bg", "Bulgarian", "bulgare", "Bulgarisch"))
        languageCodes.add(LanguageCode("bur (B)", "", "Burmese", "birman", "Birmanisch"))
        languageCodes.add(LanguageCode("mya (T)", "my", "Burmese", "birman", "Birmanisch"))
        languageCodes.add(LanguageCode("byn", "", "Blin; Bilin", "blin; bilen", "Bilin-Sprache"))
        languageCodes.add(LanguageCode("cad", "", "Caddo", "caddo", "Caddo-Sprachen"))
        languageCodes.add(
            LanguageCode(
                "cai",
                "",
                "Central American Indian languages",
                "amérindiennes de l'Amérique centrale, langues",
                "Indianersprachen, Zentralamerika (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "car",
                "",
                "Galibi Carib",
                "karib; galibi; carib",
                "Karibische Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "cat",
                "ca",
                "Catalan",
                "catalan; valencien",
                "Katalanisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "cau",
                "",
                "Caucasian languages",
                "caucasiennes, langues",
                "Kaukasische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("ceb", "", "Cebuano", "cebuano", "Cebuano"))
        languageCodes.add(
            LanguageCode(
                "cel",
                "",
                "Celtic languages",
                "celtiques, langues; celtes, langues",
                "Keltische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("cze", "", "Czech", "tchèque", "Tschechisch"))
        languageCodes.add(LanguageCode("ces", "cs", "Czech", "tchèque", "Tschechisch"))
        languageCodes.add(LanguageCode("cha", "ch", "Chamorro", "chamorro", "Chamorro-Sprache"))
        languageCodes.add(LanguageCode("chb", "", "Chibcha", "chibcha", "Chibcha-Sprachen"))
        languageCodes.add(LanguageCode("che", "ce", "Chechen", "tchétchène", "Tschetschenisch"))
        languageCodes.add(LanguageCode("chg", "", "Chagatai", "djaghataï", "Tschagataisch"))
        languageCodes.add(LanguageCode("chi (B)", "", "Chinese", "chinois", "Chinesisch"))
        languageCodes.add(LanguageCode("zho (T)", "zh", "Chinese", "chinois", "Chinesisch"))
        languageCodes.add(LanguageCode("chk", "", "Chuukese", "chuuk", "Trukesisch"))
        languageCodes.add(LanguageCode("chm", "", "Mari", "mari", "Tscheremissisch"))
        languageCodes.add(
            LanguageCode(
                "chn",
                "",
                "Chinook jargon",
                "chinook, jargon",
                "Chinook-Jargon"
            )
        )
        languageCodes.add(LanguageCode("cho", "", "Choctaw", "choctaw", "Choctaw-Sprache"))
        languageCodes.add(
            LanguageCode(
                "chp",
                "",
                "Chipewyan; Dene Suline",
                "chipewyan",
                "Chipewyan-Sprache"
            )
        )
        languageCodes.add(LanguageCode("chr", "", "Cherokee", "cherokee", "Cherokee-Sprache"))
        languageCodes.add(
            LanguageCode(
                "chu",
                "cu",
                "Church Slavic; Old Slavonic; Church Slavonic; Old Bulgarian; Old Church Slavonic",
                "slavon d'église; vieux slave; slavon liturgique; vieux bulgare",
                "Kirchenslawisch"
            )
        )
        languageCodes.add(LanguageCode("chv", "cv", "Chuvash", "tchouvache", "Tschuwaschisch"))
        languageCodes.add(LanguageCode("chy", "", "Cheyenne", "cheyenne", "Cheyenne-Sprache"))
        languageCodes.add(
            LanguageCode(
                "cmc",
                "",
                "Chamic languages",
                "chames, langues",
                "Cham-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("cnr", "", "Montenegrin", "monténégrin", "Montenegrinisch"))
        languageCodes.add(LanguageCode("cop", "", "Coptic", "copte", "Koptisch"))
        languageCodes.add(LanguageCode("cor", "kw", "Cornish", "cornique", "Kornisch"))
        languageCodes.add(LanguageCode("cos", "co", "Corsican", "corse", "Korsisch"))
        languageCodes.add(
            LanguageCode(
                "cpe",
                "",
                "Creoles and pidgins, English based",
                "créoles et pidgins basés sur l'anglais",
                "Kreolisch-Englisch (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "cpf",
                "",
                "Creoles and pidgins, French-based",
                "créoles et pidgins basés sur le français",
                "Kreolisch-Französisch (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "cpp",
                "",
                "Creoles and pidgins, Portuguese-based",
                "créoles et pidgins basés sur le portugais",
                "Kreolisch-Portugiesisch (Andere)"
            )
        )
        languageCodes.add(LanguageCode("cre", "cr", "Cree", "cree", "Cree-Sprache"))
        languageCodes.add(
            LanguageCode(
                "crh",
                "",
                "Crimean Tatar; Crimean Turkish",
                "tatar de Crimé",
                "Krimtatarisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "crp",
                "",
                "Creoles and pidgins",
                "créoles et pidgins",
                "Kreolische Sprachen; Pidginsprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("csb", "", "Kashubian", "kachoube", "Kaschubisch"))
        languageCodes.add(
            LanguageCode(
                "cus",
                "",
                "Cushitic languages",
                "couchitiques, langues",
                "Kuschitische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("wel (B)", "", "Welsh", "gallois", "Kymrisch"))
        languageCodes.add(LanguageCode("cym (T)", "cy", "Welsh", "gallois", "Kymrisch"))
        languageCodes.add(LanguageCode("cze (B)", "", "Czech", "tchèque", "Tschechisch"))
        languageCodes.add(LanguageCode("ces (T)", "cs", "Czech", "tchèque", "Tschechisch"))
        languageCodes.add(LanguageCode("dak", "", "Dakota", "dakota", "Dakota-Sprache"))
        languageCodes.add(LanguageCode("dan", "da", "Danish", "danois", "Dänisch"))
        languageCodes.add(LanguageCode("dar", "", "Dargwa", "dargwa", "Darginisch"))
        languageCodes.add(
            LanguageCode(
                "day",
                "",
                "Land Dayak languages",
                "dayak, langues",
                "Dajakisch"
            )
        )
        languageCodes.add(LanguageCode("del", "", "Delaware", "delaware", "Delaware-Sprache"))
        languageCodes.add(
            LanguageCode(
                "den",
                "",
                "Slave (Athapascan)",
                "esclave (athapascan)",
                "Slave-Sprache"
            )
        )
        languageCodes.add(LanguageCode("ger", "", "German", "allemand", "Deutsch"))
        languageCodes.add(LanguageCode("deu", "de", "German", "allemand", "Deutsch"))
        languageCodes.add(LanguageCode("dgr", "", "Dogrib", "dogrib", "Dogrib-Sprache"))
        languageCodes.add(LanguageCode("din", "", "Dinka", "dinka", "Dinka-Sprache"))
        languageCodes.add(
            LanguageCode(
                "div",
                "dv",
                "Divehi; Dhivehi; Maldivian",
                "maldivien",
                "Maledivisch"
            )
        )
        languageCodes.add(LanguageCode("doi", "", "Dogri", "dogri", "Dogri"))
        languageCodes.add(
            LanguageCode(
                "dra",
                "",
                "Dravidian languages",
                "dravidiennes, langues",
                "Drawidische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("dsb", "", "Lower Sorbian", "bas-sorabe", "Niedersorbisch"))
        languageCodes.add(LanguageCode("dua", "", "Duala", "douala", "Duala-Sprachen"))
        languageCodes.add(
            LanguageCode(
                "dum",
                "",
                "Dutch, Middle (ca.1050-1350)",
                "néerlandais moyen (ca. 1050-1350)",
                "Mittelniederländisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "dut",
                "",
                "Dutch; Flemish",
                "néerlandais; flamand",
                "Niederländisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nld", "nl", "Dutch", "néerlandais; flamand", "Niederländisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "dyu", "", "Dyula", "dioula", "Dyula-Sprache"
            )
        )
        languageCodes.add(LanguageCode("dzo", "dz", "Dzongkha", "dzongkha", "Dzongkha"))
        languageCodes.add(LanguageCode("efi", "", "Efik", "efik", "Efik"))
        languageCodes.add(LanguageCode("egy", "", "Egyptian (Ancient)", "égyptien", "Ägyptisch"))
        languageCodes.add(LanguageCode("eka", "", "Ekajuk", "ekajuk", "Ekajuk"))
        languageCodes.add(
            LanguageCode(
                "gre",
                "",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ell",
                "el",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        languageCodes.add(LanguageCode("elx", "", "Elamite", "élamite", "Elamisch"))
        languageCodes.add(defaultLanguageCode)
        languageCodes.add(
            LanguageCode(
                "enm",
                "",
                "English, Middle (1100-1500)",
                "anglais moyen (1100-1500)",
                "Mittelenglisch"
            )
        )
        languageCodes.add(LanguageCode("epo", "eo", "Esperanto", "espéranto", "Esperanto"))
        languageCodes.add(LanguageCode("est", "et", "Estonian", "estonien", "Estnisch"))
        languageCodes.add(LanguageCode("baq (B)", "", "Basque", "basque", "Baskisch"))
        languageCodes.add(LanguageCode("eus (T)", "eu", "Basque", "basque", "Baskisch"))
        languageCodes.add(LanguageCode("ewe", "ee", "Ewe", "éwé", "Ewe-Sprache"))
        languageCodes.add(LanguageCode("ewo", "", "Ewondo", "éwondo", "Ewondo"))
        languageCodes.add(LanguageCode("fan", "", "Fang", "fang", "Pangwe-Sprache"))
        languageCodes.add(LanguageCode("fao", "fo", "Faroese", "féroïen", "Färöisch"))
        languageCodes.add(LanguageCode("per (B)", "", "Persian", "persan", "Persisch"))
        languageCodes.add(LanguageCode("fas (T)", "fa", "Persian", "persan", "Persisch"))
        languageCodes.add(LanguageCode("fat", "", "Fanti", "fanti", "Fante-Sprache"))
        languageCodes.add(LanguageCode("fij", "fj", "Fijian", "fidjien", "Fidschi-Sprache"))
        languageCodes.add(
            LanguageCode(
                "fil",
                "",
                "Filipino; Pilipino",
                "filipino; pilipino",
                "Pilipino"
            )
        )
        languageCodes.add(LanguageCode("fin", "fi", "Finnish", "finnois", "Finnisch"))
        languageCodes.add(
            LanguageCode(
                "fiu",
                "",
                "Finno-Ugrian languages",
                "finno-ougriennes, langues",
                "Finnougrische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("fon", "", "Fon", "fon", "Fon-Sprache"))
        languageCodes.add(LanguageCode("fre (B)", "", "French", "français", "Französisch"))
        languageCodes.add(LanguageCode("fra", "fr", "French", "français", "Französisch"))
        languageCodes.add(LanguageCode("fre", "fr", "French", "français", "Französisch"))
        languageCodes.add(LanguageCode("fra", "fr", "French", "français", "Französisch"))
        languageCodes.add(LanguageCode("fra (T)", "fr", "French", "français", "Französisch"))
        languageCodes.add(
            LanguageCode(
                "frm",
                "",
                "French, Middle (ca.1400-1600)",
                "français moyen (1400-1600)",
                "Mittelfranzösisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "fro",
                "",
                "French, Old (842-ca.1400)",
                "français ancien (842-ca.1400)",
                "Altfranzösisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "frr",
                "",
                "Northern Frisian",
                "frison septentrional",
                "Nordfriesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "frs",
                "",
                "Eastern Frisian",
                "frison oriental",
                "Ostfriesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "fry",
                "fy",
                "Western Frisian",
                "frison occidental",
                "Friesisch"
            )
        )
        languageCodes.add(LanguageCode("ful", "ff", "Fulah", "peul", "Ful"))
        languageCodes.add(LanguageCode("fur", "", "Friulian", "frioulan", "Friulisch"))
        languageCodes.add(LanguageCode("gaa", "", "Ga", "ga", "Ga-Sprache"))
        languageCodes.add(LanguageCode("gay", "", "Gayo", "gayo", "Gayo-Sprache"))
        languageCodes.add(LanguageCode("gba", "", "Gbaya", "gbaya", "Gbaya-Sprache"))
        languageCodes.add(
            LanguageCode(
                "gem",
                "",
                "Germanic languages",
                "germaniques, langues",
                "Germanische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("geo (B)", "", "Georgian", "géorgien", "Georgisch"))
        languageCodes.add(LanguageCode("kat (T)", "ka", "Georgian", "géorgien", "Georgisch"))
        languageCodes.add(LanguageCode("ger", "", "German", "allemand", "Deutsch"))
        languageCodes.add(LanguageCode("deu", "de", "German", "allemand", "Deutsch"))
        languageCodes.add(LanguageCode("gez", "", "Geez", "guèze", "Altäthiopisch"))
        languageCodes.add(LanguageCode("gil", "", "Gilbertese", "kiribati", "Gilbertesisch"))
        languageCodes.add(
            LanguageCode(
                "gla",
                "gd",
                "Gaelic; Scottish Gaelic",
                "gaélique; gaélique écossais",
                "Gälisch-Schottisch"
            )
        )
        languageCodes.add(LanguageCode("gle", "ga", "Irish", "irlandais", "Irisch"))
        languageCodes.add(LanguageCode("glg", "gl", "Galician", "galicien", "Galicisch"))
        languageCodes.add(LanguageCode("glv", "gv", "Manx", "manx; mannois", "Manx"))
        languageCodes.add(
            LanguageCode(
                "gmh",
                "",
                "German, Middle High (ca.1050-1500)",
                "allemand, moyen haut (ca. 1050-1500)",
                "Mittelhochdeutsch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "goh",
                "",
                "German, Old High (ca.750-1050)",
                "allemand, vieux haut (ca. 750-1050)",
                "Althochdeutsch"
            )
        )
        languageCodes.add(LanguageCode("gon", "", "Gondi", "gond", "Gondi-Sprache"))
        languageCodes.add(LanguageCode("gor", "", "Gorontalo", "gorontalo", "Gorontalesisch"))
        languageCodes.add(LanguageCode("got", "", "Gothic", "gothique", "Gotisch"))
        languageCodes.add(LanguageCode("grb", "", "Grebo", "grebo", "Grebo-Sprache"))
        languageCodes.add(
            LanguageCode(
                "grc",
                "",
                "Greek, Ancient (to 1453)",
                "grec ancien (jusqu'à 1453)",
                "Griechisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "gre (B)",
                "",
                "Greek,Modern (1453-)",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ell",
                "el",
                "Greek",
                "grec moderne (après 1453)",
                "Neugriechisch"
            )
        )
        languageCodes.add(LanguageCode("grn", "gn", "Guarani", "guarani", "Guaraní-Sprache"))
        languageCodes.add(
            LanguageCode(
                "gsw",
                "",
                "Swiss German; Alemannic; Alsatian",
                "suisse alémanique; alémanique; alsacien",
                "Schweizerdeutsch"
            )
        )
        languageCodes.add(LanguageCode("guj", "gu", "Gujarati", "goudjrati", "Gujarati-Sprache"))
        languageCodes.add(LanguageCode("gwi", "", "Gwich'in", "gwich'in", "Kutchin-Sprache"))
        languageCodes.add(LanguageCode("hai", "", "Haida", "haida", "Haida-Sprache"))
        languageCodes.add(
            LanguageCode(
                "hat",
                "ht",
                "Haitian; Haitian Creole",
                "haïtien; créole haïtien",
                "Haïtien (Haiti-Kreolisch)"
            )
        )
        languageCodes.add(LanguageCode("hau", "ha", "Hausa", "haoussa", "Haussa-Sprache"))
        languageCodes.add(LanguageCode("haw", "", "Hawaiian", "hawaïen", "Hawaiisch"))
        languageCodes.add(LanguageCode("heb", "he", "Hebrew", "hébreu", "Hebräisch"))
        languageCodes.add(LanguageCode("her", "hz", "Herero", "herero", "Herero-Sprache"))
        languageCodes.add(LanguageCode("hil", "", "Hiligaynon", "hiligaynon", "Hiligaynon-Sprache"))
        languageCodes.add(
            LanguageCode(
                "him",
                "",
                "Himachali languages; Western Pahari languages",
                "langues himachalis; langues paharis occidentales",
                "Himachali"
            )
        )
        languageCodes.add(LanguageCode("hin", "hi", "Hindi", "hindi", "Hindi"))
        languageCodes.add(LanguageCode("hit", "", "Hittite", "hittite", "Hethitisch"))
        languageCodes.add(LanguageCode("hmn", "", "Hmong; Mong", "hmong", "Miao-Sprachen"))
        languageCodes.add(LanguageCode("hmo", "ho", "Hiri Motu", "hiri motu", "Hiri-Motu"))
        languageCodes.add(LanguageCode("hrv", "hr", "Croatian", "croate", "Kroatisch"))
        languageCodes.add(LanguageCode("hsb", "", "Upper Sorbian", "haut-sorabe", "Obersorbisch"))
        languageCodes.add(LanguageCode("hun", "hu", "Hungarian", "hongrois", "Ungarisch"))
        languageCodes.add(LanguageCode("hup", "", "Hupa", "hupa", "Hupa-Sprache"))
        languageCodes.add(LanguageCode("arm (B)", "", "Armenian", "arménien", "Armenisch"))
        languageCodes.add(LanguageCode("hye (T)", "hy", "Armenian", "arménien", "Armenisch"))
        languageCodes.add(LanguageCode("iba", "", "Iban", "iban", "Iban-Sprache"))
        languageCodes.add(LanguageCode("ibo", "ig", "Igbo", "igbo", "Ibo-Sprache"))
        languageCodes.add(LanguageCode("ice (B)", "", "Icelandic", "islandais", "Isländisch"))
        languageCodes.add(LanguageCode("isl (T)", "is", "Icelandic", "islandais", "Isländisch"))
        languageCodes.add(LanguageCode("ido", "io", "Ido", "ido", "Ido"))
        languageCodes.add(
            LanguageCode(
                "iii",
                "ii",
                "Sichuan Yi; Nuosu",
                "yi de Sichuan",
                "Lalo-Sprache"
            )
        )
        languageCodes.add(LanguageCode("ijo", "", "Ijo languages", "ijo, langues", "Ijo-Sprache"))
        languageCodes.add(LanguageCode("iku", "iu", "Inuktitut", "inuktitut", "Inuktitut"))
        languageCodes.add(
            LanguageCode(
                "ile",
                "ie",
                "Interlingue; Occidental",
                "interlingue",
                "Interlingue"
            )
        )
        languageCodes.add(LanguageCode("ilo", "", "Iloko", "ilocano", "Ilokano-Sprache"))
        languageCodes.add(LanguageCode("ina", "ia", "Interlingua", "interlingua", "Interlingua"))
        languageCodes.add(
            LanguageCode(
                "inc",
                "",
                "Indic languages",
                "indo-aryennes, langues",
                "Indoarische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("ind", "id", "Indonesian", "indonésien", "Bahasa Indonesia"))
        languageCodes.add(LanguageCode("in", "in", "Indonesian", "indonésien", "Bahasa Indonesia"))

        languageCodes.add(
            LanguageCode(
                "ine",
                "",
                "Indo-European languages",
                "indo-européennes, langues",
                "Indogermanische Sprachen (Andere)"
            )
        )

        languageCodes.add(LanguageCode("inh", "", "Ingush", "ingouche", "Inguschisch"))
        languageCodes.add(LanguageCode("ipk", "ik", "Inupiaq", "inupiaq", "Inupik"))
        languageCodes.add(
            LanguageCode(
                "ira",
                "",
                "Iranian languages",
                "iraniennes, langues",
                "Iranische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "iro",
                "",
                "Iroquoian languages",
                "iroquoises, langues",
                "Irokesische Sprachen"
            )
        )
        languageCodes.add(LanguageCode("ice (B)", "", "Icelandic", "islandais", "Isländisch"))
        languageCodes.add(LanguageCode("isl (T)", "is", "Icelandic", "islandais", "Isländisch"))
        languageCodes.add(LanguageCode("ita", "it", "Italian", "italien", "Italienisch"))
        languageCodes.add(LanguageCode("jav", "jv", "Javanese", "javanais", "Javanisch"))
        languageCodes.add(LanguageCode("jbo", "", "Lojban", "lojban", "Lojban"))
        languageCodes.add(LanguageCode("jpn", "ja", "Japanese", "japonais", "Japanisch"))
        languageCodes.add(
            LanguageCode(
                "jpr",
                "",
                "Judeo-Persian",
                "judéo-persan",
                "Jüdisch-Persisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "jrb",
                "",
                "Judeo-Arabic",
                "judéo-arabe",
                "Jüdisch-Arabisch"
            )
        )
        languageCodes.add(LanguageCode("kaa", "", "Kara-Kalpak", "karakalpak", "Karakalpakisch"))
        languageCodes.add(LanguageCode("kab", "", "Kabyle", "kabyle", "Kabylisch"))
        languageCodes.add(
            LanguageCode(
                "kac",
                "",
                "Kachin; Jingpho",
                "kachin; jingpho",
                "Kachin-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "kal",
                "kl",
                "Kalaallisut; Greenlandic",
                "groenlandais",
                "Grönländisch"
            )
        )
        languageCodes.add(LanguageCode("kam", "", "Kamba", "kamba", "Kamba-Sprache"))
        languageCodes.add(LanguageCode("kan", "kn", "Kannada", "kannada", "Kannada"))
        languageCodes.add(LanguageCode("kar", "", "Karen languages", "karen, langues", "Karenisch"))
        languageCodes.add(LanguageCode("kas", "ks", "Kashmiri", "kashmiri", "Kaschmiri"))
        languageCodes.add(LanguageCode("geo (B)", "", "Georgian", "géorgien", "Georgisch"))
        languageCodes.add(LanguageCode("kat (T)", "ka", "Georgian", "géorgien", "Georgisch"))
        languageCodes.add(LanguageCode("kau", "kr", "Kanuri", "kanouri", "Kanuri-Sprache"))
        languageCodes.add(LanguageCode("kaw", "", "Kawi", "kawi", "Kawi"))
        languageCodes.add(LanguageCode("kaz", "kk", "Kazakh", "kazakh", "Kasachisch"))
        languageCodes.add(LanguageCode("kbd", "", "Kabardian", "kabardien", "Kabardinisch"))
        languageCodes.add(LanguageCode("kha", "", "Khasi", "khasi", "Khasi-Sprache"))
        languageCodes.add(
            LanguageCode(
                "khi",
                "",
                "Khoisan languages",
                "khoïsan, langues",
                "Khoisan-Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "khm",
                "km",
                "Central Khmer",
                "khmer central",
                "Kambodschanisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "kho",
                "",
                "Khotanese; Sakan",
                "khotanais; sakan",
                "Sakisch"
            )
        )
        languageCodes.add(LanguageCode("kik", "ki", "Kikuyu; Gikuyu", "kikuyu", "Kikuyu-Sprache"))
        languageCodes.add(LanguageCode("kin", "rw", "Kinyarwanda", "rwanda", "Rwanda-Sprache"))
        languageCodes.add(LanguageCode("kir", "ky", "Kirghiz; Kyrgyz", "kirghiz", "Kirgisisch"))
        languageCodes.add(LanguageCode("kmb", "", "Kimbundu", "kimbundu", "Kimbundu-Sprache"))
        languageCodes.add(LanguageCode("kok", "", "Konkani", "konkani", "Konkani"))
        languageCodes.add(LanguageCode("kom", "kv", "Komi", "kom", "Komi-Sprache"))
        languageCodes.add(LanguageCode("kon", "kg", "Kongo", "kongo", "Kongo-Sprache"))
        languageCodes.add(LanguageCode("kor", "ko", "Korean", "coréen", "Koreanisch"))
        languageCodes.add(LanguageCode("kos", "", "Kosraean", "kosrae", "Kosraeanisch"))
        languageCodes.add(LanguageCode("kpe", "", "Kpelle", "kpellé", "Kpelle-Sprache"))
        languageCodes.add(
            LanguageCode(
                "krc",
                "",
                "Karachay-Balkar", "karatchai balkar", "Karatschaiisch-Balkarisch"
            )
        )
        languageCodes.add(LanguageCode("krl", "", "Karelian", "carélien", "Karelisch"))
        languageCodes.add(
            LanguageCode(
                "kro",
                "",
                "Kru languages",
                "krou, langues",
                "Kru-Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("kru", "", "Kurukh", "kurukh", "Oraon-Sprache"))
        languageCodes.add(
            LanguageCode(
                "kua",
                "kj",
                "Kuanyama; Kwanyama",
                "kuanyama; kwanyama",
                "Kwanyama-Sprache"
            )
        )
        languageCodes.add(LanguageCode("kum", "", "Kumyk", "koumyk", "Kumükisch"))
        languageCodes.add(LanguageCode("kur", "ku", "Kurdish", "kurde", "Kurdisch"))
        languageCodes.add(LanguageCode("kut", "", "Kutenai", "kutenai", "Kutenai-Sprache"))
        languageCodes.add(LanguageCode("lad", "", "Ladino", "judéo-espagnol", "Judenspanisch"))
        languageCodes.add(LanguageCode("lah", "", "Lahnda", "lahnda", "Lahnda"))
        languageCodes.add(LanguageCode("lam", "", "Lamba", "lamba", "Lamba-Sprache (Bantusprache)"))
        languageCodes.add(LanguageCode("lao", "lo", "Lao", "lao", "Laotisch"))
        languageCodes.add(LanguageCode("lat", "la", "Latin", "latin", "Latein"))
        languageCodes.add(LanguageCode("lav", "lv", "Latvian", "letton", "Lettisch"))
        languageCodes.add(LanguageCode("lez", "", "Lezghian", "lezghien", "Lesgisch"))
        languageCodes.add(
            LanguageCode(
                "lim",
                "li",
                "Limburgan; Limburger; Limburgish",
                "limbourgeois",
                "Limburgisch"
            )
        )
        languageCodes.add(LanguageCode("lin", "ln", "Lingala", "lingala", "Lingala"))
        languageCodes.add(LanguageCode("lit", "lt", "Lithuanian", "lituanien", "Litauisch"))
        languageCodes.add(LanguageCode("lol", "", "Mongo", "mongo", "Mongo-Sprache"))
        languageCodes.add(LanguageCode("loz", "", "Lozi", "lozi", "Rotse-Sprache"))
        languageCodes.add(
            LanguageCode(
                "ltz",
                "lb",
                "Luxembourgish; Letzeburgesch",
                "luxembourgeois",
                "Luxemburgisch"
            )
        )
        languageCodes.add(LanguageCode("lua", "", "Luba-Lulua", "luba-lulua", "Lulua-Sprache"))
        languageCodes.add(
            LanguageCode(
                "lub",
                "lu",
                "Luba-Katanga",
                "luba-katanga",
                "Luba-Katanga-Sprache"
            )
        )
        languageCodes.add(LanguageCode("lug", "lg", "Ganda", "ganda", "Ganda-Sprache"))
        languageCodes.add(LanguageCode("lui", "", "Luiseno", "luiseno", "Luiseño-Sprache"))
        languageCodes.add(LanguageCode("lun", "", "Lunda", "lunda", "Lunda-Sprache"))
        languageCodes.add(
            LanguageCode(
                "luo",
                "",
                "Luo (Kenya and Tanzania)",
                "luo (Kenya et Tanzanie)",
                "Luo-Sprache"
            )
        )
        languageCodes.add(LanguageCode("lus", "", "Lushai", "lushai", "Lushai-Sprache"))
        languageCodes.add(LanguageCode("mac (B)", "", "Macedonian", "macédonien", "Makedonisch"))
        languageCodes.add(LanguageCode("mkd", "mk", "Macedonian", "macédonien", "Makedonisch"))
        languageCodes.add(LanguageCode("mad", "", "Madurese", "madourais", "Maduresisch"))
        languageCodes.add(LanguageCode("mag", "", "Magahi", "magahi", "Khotta"))
        languageCodes.add(LanguageCode("mah", "mh", "Marshallese", "marshall", "Marschallesisch"))
        languageCodes.add(LanguageCode("mai", "", "Maithili", "maithili", "Maithili"))
        languageCodes.add(LanguageCode("mak", "", "Makasar", "makassar", "Makassarisch"))
        languageCodes.add(LanguageCode("mal", "ml", "Malayalam", "malayalam", "Malayalam"))
        languageCodes.add(LanguageCode("man", "", "Mandingo", "mandingue", "Malinke-Sprache"))
        languageCodes.add(LanguageCode("mao (B)", "", "Maori", "maori", "Maori-Sprache"))
        languageCodes.add(LanguageCode("mri (T)", "mi", "Maori", "maori", "Maori-Sprache"))
        languageCodes.add(
            LanguageCode(
                "map",
                "",
                "Austronesian languages",
                "austronésiennes, langues",
                "Austronesische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("mar", "mr", "Marathi", "marathe", "Marathi"))
        languageCodes.add(LanguageCode("mas", "", "Masai", "massaï", "Massai-Sprache"))
        languageCodes.add(LanguageCode("may (B)", "", "Malay", "malais", "Malaiisch"))
        languageCodes.add(LanguageCode("msa (T)", "ms", "Malay", "malais", "Malaiisch"))
        languageCodes.add(LanguageCode("msa", "ms", "Malay", "malais", "Malaiisch"))
        languageCodes.add(LanguageCode("mdf", "", "Moksha", "moksa", "Mokscha-Sprache"))
        languageCodes.add(LanguageCode("mdr", "", "Mandar", "mandar", "Mandaresisch"))
        languageCodes.add(LanguageCode("men", "", "Mende", "mendé", "Mende-Sprache"))
        languageCodes.add(
            LanguageCode(
                "mga",
                "",
                "Irish, Middle (900-1200)",
                "irlandais moyen (900-1200)",
                "Mittelirisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "mic",
                "",
                "Mi'kmaq; Micmac	mi'kmaq;",
                "micmac",
                "Micmac-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "min",
                "",
                "Minangkabau",
                "minangkabau",
                "Minangkabau-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "mis",
                "",
                "Uncoded languages",
                "langues non codées",
                "Einzelne andere Sprachen"
            )
        )
        languageCodes.add(LanguageCode("mac (B)", "", "Macedonian", "macédonien", "Makedonisch"))
        languageCodes.add(LanguageCode("mkd (T)", "mk", "Macedonian", "macédonien", "Makedonisch"))
        languageCodes.add(
            LanguageCode(
                "mkh",
                "",
                "Mon-Khmer, languages",
                "môn-khmer, langues",
                "Mon-Khmer-Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("mlg", "mg", "Malagasy", "malgache", "Malagassi-Sprache"))
        languageCodes.add(LanguageCode("mlt", "mt", "Maltese", "maltais", "Maltesisch"))
        languageCodes.add(LanguageCode("mnc", "", "Manchu", "mandchou", "Mandschurisch"))
        languageCodes.add(LanguageCode("mni", "", "Manipuri", "manipuri", "Meithei-Sprache"))
        languageCodes.add(
            LanguageCode(
                "mno",
                "",
                "Manobo languages",
                "manobo, langues",
                "Manobo-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("moh", "", "Mohawk", "mohawk", "Mohawk-Sprache"))
        languageCodes.add(LanguageCode("mon", "mn", "Mongolian", "mongol", "Mongolisch"))
        languageCodes.add(LanguageCode("mos", "", "Mossi", "moré", "Mossi-Sprache"))
        languageCodes.add(LanguageCode("mao (B)", "", "Maori", "maori", "Maori-Sprache"))
        languageCodes.add(LanguageCode("mri (T)", "mi", "Maori", "maori", "Maori-Sprache"))
        languageCodes.add(LanguageCode("may (B)", "", "Malay", "malais", "Malaiisch"))
        languageCodes.add(LanguageCode("msa (T)", "ms", "Malay", "malais", "Malaiisch"))
        languageCodes.add(
            LanguageCode(
                "mul",
                "",
                "Multiple languages",
                "multilingue",
                "Mehrere Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "mun",
                "",
                "Munda languages",
                "mounda, langues",
                "Mundasprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("mus", "", "Creek", "muskogee", "Muskogisch"))
        languageCodes.add(LanguageCode("mwl", "", "Mirandese", "mirandais", "Mirandesisch"))
        languageCodes.add(LanguageCode("mwr", "", "Marwari", "marvari", "Marwari"))
        languageCodes.add(LanguageCode("bur (B)", "", "Burmese", "birman", "Birmanisch"))
        languageCodes.add(LanguageCode("mya (T)", "my", "Burmese", "birman", "Birmanisch"))
        languageCodes.add(
            LanguageCode(
                "myn",
                "",
                "Mayan languages",
                "maya, langues",
                "Maya-Sprachen"
            )
        )
        languageCodes.add(LanguageCode("myv", "", "Erzya", "erza", "Erza-Mordwinisch"))
        languageCodes.add(
            LanguageCode(
                "nah",
                "",
                "Nahuatl languages",
                "nahuatl, langues",
                "Nahuatl"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nai",
                "",
                "North American Indian languages",
                "nord-amérindiennes, langues",
                "Indianersprachen, Nordamerika (Andere)"
            )
        )
        languageCodes.add(LanguageCode("nap", "", "Neapolitan", "napolitain", "Neapel / Mundart"))
        languageCodes.add(LanguageCode("nau", "na", "Nauru", "nauruan", "Nauruanisch"))
        languageCodes.add(LanguageCode("nav", "nv", "Navajo; Navaho", "navaho", "Navajo-Sprache"))
        languageCodes.add(
            LanguageCode(
                "nbl",
                "nr",
                "Ndebele, South; South Ndebele",
                "ndébélé du Sud",
                "Ndebele-Sprache (Transvaal)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nde",
                "nd",
                "Ndebele, North; North Ndebele",
                "ndébélé du Nord",
                "Ndebele-Sprache (Simbabwe)"
            )
        )
        languageCodes.add(LanguageCode("ndo", "ng", "Ndonga", "ndonga", "Ndonga"))
        languageCodes.add(
            LanguageCode(
                "nds",
                "",
                "Low German; Low Saxon; German, Low; Saxon, Low",
                "bas allemand; bas saxon; allemand, bas; saxon, bas",
                "Niederdeutsch"
            )
        )
        languageCodes.add(LanguageCode("nep", "ne", "Nepali", "népalais", "Nepali"))
        languageCodes.add(
            LanguageCode(
                "new",
                "",
                "Nepal Bhasa; Newari",
                "nepal bhasa; newari",
                "Newari"
            )
        )
        languageCodes.add(LanguageCode("nia", "", "Nias", "nias", "Nias-Sprache"))
        languageCodes.add(
            LanguageCode(
                "nic",
                "",
                "Niger-Kordofanian languages",
                "nigéro-kordofaniennes, langues",
                "Nigerkordofanische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("niu", "", "Niuean", "niué", "Niue-Sprache"))
        languageCodes.add(
            LanguageCode(
                "dut (B)",
                "",
                "Dutch; Flemish",
                "néerlandais; flamand",
                "Niederländisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nld (T)",
                "nl",
                "Dutch; Flemish",
                "néerlandais; flamand",
                "Niederländisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nno",
                "nn",
                "Norwegian",
                "norvégien nynorsk; nynorsk, norvégien",
                "Nynorsk"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nob",
                "nb",
                "Bokmål, Norwegian; Norwegian Bokmål",
                "norvégien bokmål",
                "Bokmål"
            )
        )
        languageCodes.add(LanguageCode("nog", "", "Nogai", "nogaï; nogay", "Nogaisch"))
        languageCodes.add(LanguageCode("non", "", "Norse, Old", "norrois, vieux", "Altnorwegisch"))
        languageCodes.add(LanguageCode("nor", "no", "Norwegian", "norvégien", "Norwegisch"))
        languageCodes.add(LanguageCode("nqo", "", "N'Ko", "n'ko", "N'Ko"))
        languageCodes.add(
            LanguageCode(
                "nso",
                "",
                "Pedi; Sepedi; Northern Sotho",
                "pedi; sepedi; sotho du Nord",
                "Pedi-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nub",
                "",
                "Nubian languages",
                "nubiennes, langues",
                "Nubische Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nwc",
                "",
                "Classical Newari; Old Newari; Classical Nepal Bhasa",
                "newari classique",
                "Alt-Newari"
            )
        )
        languageCodes.add(
            LanguageCode(
                "nya",
                "ny",
                "Chichewa; Chewa; Nyanja",
                "chichewa; chewa; nyanja",
                "Nyanja-Sprache"
            )
        )
        languageCodes.add(LanguageCode("nym", "", "Nyamwezi", "nyamwezi", "Nyamwezi-Sprache"))
        languageCodes.add(LanguageCode("nyn", "", "Nyankole", "nyankolé", "Nkole-Sprache"))
        languageCodes.add(LanguageCode("nyo", "", "Nyoro", "nyoro", "Nyoro-Sprache"))
        languageCodes.add(LanguageCode("nzi", "", "Nzima", "nzema", "Nzima-Sprache"))
        languageCodes.add(
            LanguageCode(
                "oci",
                "oc",
                "Occitan (post 1500)",
                "occitan (après 1500)",
                "Okzitanisch"
            )
        )
        languageCodes.add(LanguageCode("oji", "oj", "Ojibwa", "ojibwa", "Ojibwa-Sprache"))
        languageCodes.add(LanguageCode("ori", "or", "Oriya", "oriya", "Oriya-Sprache"))
        languageCodes.add(LanguageCode("orm", "om", "Oromo", "galla", "Galla-Sprache"))
        languageCodes.add(LanguageCode("osa", "", "Osage", "osage", "Osage-Sprache"))
        languageCodes.add(LanguageCode("oss", "os", "Ossetian; Ossetic", "ossète", "Ossetisch"))
        languageCodes.add(
            LanguageCode(
                "ota",
                "",
                "Turkish, Ottoman (1500-1928)",
                "turc ottoman (1500-1928)",
                "Osmanisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "oto",
                "",
                "Otomian languages",
                "otomi, langues",
                "Otomangue-Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "paa",
                "",
                "Papuan languages",
                "papoues, langues",
                "Papuasprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("pag", "", "Pangasinan", "pangasinan", "Pangasinan-Sprache"))
        languageCodes.add(LanguageCode("pal", "", "Pahlavi", "pahlavi", "Mittelpersisch"))
        languageCodes.add(
            LanguageCode(
                "pam",
                "",
                "Pampanga; Kapampangan",
                "pampangan",
                "Pampanggan-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "pan",
                "pa",
                "Panjabi; Punjabi",
                "pendjabi",
                "Pandschabi-Sprache"
            )
        )
        languageCodes.add(LanguageCode("pap", "", "Papiamento", "papiamento", "Papiamento"))
        languageCodes.add(LanguageCode("pau", "", "Palauan", "palau", "Palau-Sprache"))
        languageCodes.add(
            LanguageCode(
                "peo",
                "",
                "Persian, Old (ca.600-400 B.C.)",
                "perse, vieux (ca. 600-400 av. J.-C.)",
                "Altpersisch"
            )
        )
        languageCodes.add(LanguageCode("per (B)", "", "Persian", "persan", "Persisch"))
        languageCodes.add(LanguageCode("fas (T)", "fa", "Persian", "persan", "Persisch"))
        languageCodes.add(
            LanguageCode(
                "phi",
                "",
                "Philippine languages",
                "philippines, langues",
                "Philippinisch-Austronesisch (Andere)"
            )
        )
        languageCodes.add(LanguageCode("phn", "", "Phoenician", "phénicien", "Phönikisch"))
        languageCodes.add(LanguageCode("pli", "pi", "Pali", "pali", "Pali"))
        languageCodes.add(LanguageCode("pol", "pl", "Polish", "polonais", "Polnisch"))
        languageCodes.add(LanguageCode("pon", "", "Pohnpeian", "pohnpei", "Ponapeanisch"))
        languageCodes.add(LanguageCode("por", "pt", "Portuguese", "portugais", "Portugiesisch"))
        languageCodes.add(
            LanguageCode(
                "pra",
                "",
                "Prakrit languages",
                "prâkrit, langues",
                "Prakrit"
            )
        )
        languageCodes.add(
            LanguageCode(
                "pro",
                "",
                "Provençal, Old (to 1500);Occitan, Old (to 1500)",
                "provençal ancien (jusqu'à 1500); occitan ancien (jusqu'à 1500)",
                "Altokzitanisch"
            )
        )
        languageCodes.add(LanguageCode("pus", "ps", "Pushto; Pashto", "pachto", "Paschtu"))
        languageCodes.add(
            LanguageCode(
                "qaa-qtz",
                "",
                "Reserved for local use",
                "réservée à l'usage local",
                "Reserviert für lokale Verwendung"
            )
        )
        languageCodes.add(LanguageCode("qaa","","Original audio","réservée à l'usage local","Son original"))
        languageCodes.add(LanguageCode("que", "qu", "Quechua", "quechua", "Quechua-Sprache"))
        languageCodes.add(LanguageCode("raj", "", "Rajasthani", "rajasthani", "Rajasthani"))
        languageCodes.add(LanguageCode("rap", "", "Rapanui", "rapanui", "Osterinsel-Sprache"))
        languageCodes.add(
            LanguageCode(
                "rar",
                "",
                "Rarotongan; Cook Islands Maori",
                "rarotonga; maori des îles",
                "Cook	Rarotonganisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "roa",
                "",
                "Romance languages",
                "romanes, langues",
                "Romanische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("roh", "rm", "Romansh", "romanche", "Rätoromanisch"))
        languageCodes.add(LanguageCode("rom", "", "Romany", "tsigane", "Romani (Sprache)"))
        languageCodes.add(
            LanguageCode(
                "rum (B)",
                "",
                "Romanian; Moldavian; Moldovan",
                "roumain; moldave",
                "Rumänisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ron",
                "ro",
                "Romanian",
                "roumain; moldave",
                "Rumänisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "rum",
                "",
                "Romanian",
                "roumain",
                "Rumänisch"
            )
        )
        languageCodes.add(LanguageCode("run", "rn", "Rundi", "rundi", "Rundi-Sprache"))
        languageCodes.add(
            LanguageCode(
                "rup", "",
                "Aromanian; Arumanian; Macedo-Romanian",
                "aroumain; macédo-roumain",
                "Aromunisch"
            )
        )
        languageCodes.add(LanguageCode("rus", "ru", "Russian", "russe", "Russisch"))
        languageCodes.add(LanguageCode("sad", "", "Sandawe", "sandawe", "Sandawe-Sprache"))
        languageCodes.add(LanguageCode("sag", "sg", "Sango", "sango", "Sango-Sprache"))
        languageCodes.add(LanguageCode("sah", "", "Yakut", "iakoute", "Jakutisch"))
        languageCodes.add(
            LanguageCode(
                "sai",
                "",
                "South American Indian languages",
                "sud-amérindiennes, langues",
                "Indianersprachen, Südamerika (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sal",
                "",
                "Salishan languages",
                "salishennes, langues",
                "Salish-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sam",
                "",
                "Samaritan Aramaic",
                "samaritain",
                "Samaritanisch"
            )
        )
        languageCodes.add(LanguageCode("san", "sa", "Sanskrit", "sanskrit", "Sanskrit"))
        languageCodes.add(LanguageCode("sas", "", "Sasak", "sasak", "Sasak"))
        languageCodes.add(LanguageCode("sat", "", "Santali", "santal", "Santali"))
        languageCodes.add(LanguageCode("scn", "", "Sicilian", "sicilien", "Sizilianisch"))
        languageCodes.add(LanguageCode("sco", "", "Scots", "écossais", "Schottisch"))
        languageCodes.add(LanguageCode("sel", "", "Selkup", "selkoupe", "Selkupisch"))
        languageCodes.add(
            LanguageCode(
                "sem",
                "",
                "Semitic languages",
                "sémitiques, langues",
                "Semitische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sga",
                "",
                "Irish, Old (to 900)",
                "irlandais ancien (jusqu'à 900)",
                "Altirisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sgn",
                "",
                "Sign Languages",
                "langues des signes",
                "Zeichensprachen"
            )
        )
        languageCodes.add(LanguageCode("shn", "", "Shan", "chan", "Schan-Sprache"))
        languageCodes.add(LanguageCode("sid", "", "Sidamo", "sidamo", "Sidamo-Sprache"))
        languageCodes.add(
            LanguageCode(
                "sin",
                "si",
                "Sinhala; Sinhalese",
                "singhalais",
                "Singhalesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sio",
                "",
                "Siouan languages",
                "sioux, langues",
                "Sioux-Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sit",
                "",
                "Sino-Tibetan languages",
                "sino-tibétaines, langues",
                "Sinotibetische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sla",
                "",
                "Slavic languages",
                "slaves, langues",
                "Slawische Sprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("slo (B)", "", "Slovak", "slovaque", "Slowakisch"))
        languageCodes.add(LanguageCode("slk", "sk", "Slovak", "slovaque", "Slowakisch"))
        languageCodes.add(LanguageCode("slo (B)", "", "Slovak", "slovaque", "Slowakisch"))
        languageCodes.add(LanguageCode("slk (T)", "sk", "Slovak", "slovaque", "Slowakisch"))
        languageCodes.add(LanguageCode("slv", "sl", "Slovenian", "slovène", "Slowenisch"))
        languageCodes.add(LanguageCode("sma", "", "Southern Sami", "sami du Sud", "Südsaamisch"))
        languageCodes.add(
            LanguageCode(
                "sme",
                "se",
                "Northern Sami",
                "sami du Nord",
                "Nordsaamisch"
            )
        )
        languageCodes.add(LanguageCode("smi", "", "Sami languages", "sames, langues", "Saamisch"))
        languageCodes.add(LanguageCode("smj", "", "Lule Sami", "sami de Lule", "Lulesaamisch"))
        languageCodes.add(LanguageCode("smn", "", "Inari Sami", "sami d'Inari", "Inarisaamisch"))
        languageCodes.add(LanguageCode("smo", "sm", "Samoan", "samoan", "Samoanisch"))
        languageCodes.add(LanguageCode("sms", "", "Skolt Sami", "sami skolt", "Skoltsaamisch"))
        languageCodes.add(LanguageCode("sna", "sn", "Shona", "shona", "Schona-Sprache"))
        languageCodes.add(LanguageCode("snd", "sd", "Sindhi", "sindhi", "Sindhi-Sprache"))
        languageCodes.add(LanguageCode("snk", "", "Soninke", "soninké", "Soninke-Sprache"))
        languageCodes.add(LanguageCode("sog", "", "Sogdian", "sogdien", "Sogdisch"))
        languageCodes.add(LanguageCode("som", "so", "Somali", "somali", "Somali"))
        languageCodes.add(
            LanguageCode(
                "son",
                "",
                "Songhai languages",
                "songhai, langues",
                "Songhai-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sot",
                "st",
                "Sotho, Southern",
                "sotho du Sud",
                "Süd-Sotho-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "spa",
                "es",
                "Spanish",
                "espagnol; castillan",
                "Spanisch"
            )
        )
        languageCodes.add(LanguageCode("alb (B)", "", "Albanian", "albanais", "Albanisch"))
        languageCodes.add(LanguageCode("sqi (T)", "sq", "Albanian", "albanais", "Albanisch"))
        languageCodes.add(LanguageCode("srd", "sc", "Sardinian", "sarde", "Sardisch"))
        languageCodes.add(LanguageCode("srn", "", "Sranan Tongo", "sranan tongo", "Sranantongo"))
        languageCodes.add(
            LanguageCode(
                "srp",
                "sr",
                "Serbian",
                "serbe",
                "Serbisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "srr",
                "",
                "Serer",
                "sérère",
                "Serer-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ssa",
                "",
                "Nilo-Saharan languages",
                "nilo-sahariennes, langues",
                "Nilosaharanische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ssw",
                "ss",
                "Swati",
                "swati",
                "Swasi-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "suk",
                "",
                "Sukuma",
                "sukuma",
                "Sukuma-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "sun",
                "su",
                "Sundanese",
                "soundanais",
                "Sundanesisch"
            )
        )
        languageCodes.add(LanguageCode("sus", "", "Susu", "soussou", "Susu"))
        languageCodes.add(
            LanguageCode(
                "sux",
                "",
                "Sumerian",
                "sumérien",
                "Sumerisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "swa",
                "sw",
                "Swahili",
                "swahili",
                "Swahili"
            )
        )
        languageCodes.add(
            LanguageCode(
                "swe",
                "sv",
                "Swedish",
                "suédois",
                "Schwedisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "syc",
                "",
                "Classical Syriac",
                "syriaque classique",
                "Syrisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "syr",
                "",
                "Syriac",
                "syriaque",
                "Neuostaramäisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tah",
                "ty",
                "Tahitian",
                "tahitien",
                "Tahitisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tai",
                "",
                "Tai languages",
                "tai, langues",
                "Thaisprachen (Andere)"
            )
        )
        languageCodes.add(LanguageCode("tam", "ta", "Tamil", "tamoul", "Tamil"))
        languageCodes.add(
            LanguageCode(
                "tat",
                "tt",
                "Tatar",
                "tatar",
                "Tatarisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tel",
                "te",
                "Telugu",
                "télougou",
                "Telugu-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tem",
                "",
                "Timne",
                "temne",
                "Temne-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ter",
                "",
                "Tereno",
                "tereno",
                "Tereno-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tet",
                "",
                "Tetum",
                "tetum",
                "Tetum-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tgk",
                "tg",
                "Tajik",
                "tadjik",
                "Tadschikisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tgl",
                "tl",
                "Tagalog",
                "tagalog",
                "Tagalog"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tha",
                "th",
                "Thai",
                "thaï",
                "Thailändisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tib (B)",
                "",
                "Tibetan",
                "tibétain",
                "Tibetisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "bod (T)",
                "bo",
                "Tibetan",
                "tibétain",
                "Tibetisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tig",
                "",
                "Tigre",
                "tigré",
                "Tigre-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tir",
                "ti",
                "Tigrinya",
                "tigrigna",
                "Tigrinja-Sprache"
            )
        )
        languageCodes.add(LanguageCode("tiv", "", "Tiv", "tiv", "Tiv-Sprache"))
        languageCodes.add(
            LanguageCode(
                "tkl",
                "",
                "Tokelau",
                "tokelau",
                "Tokelauanisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tlh",
                "",
                "Klingon; tlhIngan-Hol",
                "klingon",
                "Klingonisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tli",
                "",
                "Tlingit",
                "tlingit",
                "Tlingit-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tmh",
                "",
                "Tamashek",
                "tamacheq",
                "Tamašeq"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tog",
                "",
                "Tonga (Nyasa)",
                "tonga (Nyasa)",
                "Tonga (Bantusprache, Sambia)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ton",
                "to",
                "Tonga (Tonga Islands)",
                "tongan (Îles Tonga)",
                "Tongaisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tpi",
                "",
                "Tok Pisin",
                "tok pisin",
                "Neumelanesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tsi",
                "",
                "Tsimshian",
                "tsimshian",
                "Tsimshian-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tsn",
                "tn",
                "Tswana",
                "tswana",
                "Tswana-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tso",
                "ts",
                "Tsonga",
                "tsonga",
                "Tsonga-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tuk",
                "tk",
                "Turkmen",
                "turkmène",
                "Turkmenisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tum",
                "",
                "Tumbuka",
                "tumbuka",
                "Tumbuka-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tup",
                "",
                "Tupi languages",
                "tupi, langues",
                "Tupi-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tur",
                "tr",
                "Turkish",
                "turc",
                "Türkisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tut",
                "",
                "Altaic languages",
                "altaïques, langues",
                "Altaische Sprachen (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "tvl",
                "",
                "Tuvalu",
                "tuvalu",
                "Elliceanisch"
            )
        )
        languageCodes.add(LanguageCode("twi", "tw", "Twi", "twi", "Twi-Sprache"))
        languageCodes.add(
            LanguageCode(
                "tyv",
                "",
                "Tuvinian",
                "touva",
                "Tuwinisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "udm",
                "",
                "Udmurt",
                "oudmourte",
                "Udmurtisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "uga",
                "",
                "Ugaritic",
                "ougaritique",
                "Ugaritisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "uig",
                "ug",
                "Uighur; Uyghur",
                "ouïgour",
                "Uigurisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ukr",
                "uk",
                "Ukrainian",
                "ukrainien",
                "Ukrainisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "umb",
                "",
                "Umbundu",
                "umbundu",
                "Mbundu-Sprache"
            )
        )
        languageCodes.add(LanguageCode("urd", "ur", "Urdu", "ourdou", "Urdu"))
        languageCodes.add(
            LanguageCode(
                "uzb",
                "uz",
                "Uzbek",
                "ouszbek",
                "Usbekisch"
            )
        )
        languageCodes.add(LanguageCode("vai", "", "Vai", "vaï", "Vai-Sprache"))
        languageCodes.add(
            LanguageCode(
                "ven",
                "ve",
                "Venda",
                "venda",
                "Venda-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "vie",
                "vi",
                "Vietnamese",
                "vietnamien",
                "Vietnamesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "vol",
                "vo",
                "Volapük",
                "volapük",
                "Volapük"
            )
        )
        languageCodes.add(LanguageCode("vot", "", "Votic", "vote", "Wotisch"))
        languageCodes.add(
            LanguageCode(
                "wak",
                "",
                "Wakashan languages",
                "wakashanes, langues",
                "Wakash-Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "wal",
                "",
                "Wolaitta; Wolaytta",
                "wolaitta; wolaytta",
                "Walamo-Sprache"
            )
        )
        languageCodes.add(LanguageCode("war", "", "Waray", "waray", "Waray"))
        languageCodes.add(
            LanguageCode(
                "was",
                "",
                "Washo",
                "washo",
                "Washo-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "wel (B)",
                "",
                "Welsh",
                "gallois",
                "Kymrisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "cym (T)",
                "cy",
                "Welsh",
                "gallois",
                "Kymrisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "wen",
                "",
                "Sorbian languages",
                "sorabes, langues",
                "Sorbisch (Andere)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "wln",
                "wa",
                "Walloon",
                "wallon",
                "Wallonisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "wol",
                "wo",
                "Wolof",
                "wolof",
                "Wolof-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "xal",
                "",
                "Kalmyk; Oirat",
                "kalmouk; oïrat",
                "Kalmückisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "xho",
                "xh",
                "Xhosa",
                "xhosa",
                "Xhosa-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "yao",
                "",
                "Yao",
                "yao",
                "Yao-Sprache (Bantusprache)"
            )
        )
        languageCodes.add(
            LanguageCode(
                "yap",
                "",
                "Yapese",
                "yapois",
                "Yapesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "yid",
                "yi",
                "Yiddish",
                "yiddish",
                "Jiddisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "yor",
                "yo",
                "Yoruba",
                "yoruba",
                "Yoruba-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "ypk",
                "",
                "Yupik languages",
                "yupik, langues",
                "Ypik-Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zap",
                "",
                "Zapotec",
                "zapotèque",
                "Zapotekisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zbl",
                "",
                "Blissymbols; Blissymbolics; Bliss",
                "symboles Bliss; Bliss",
                "Bliss-Symbol"
            )
        )
        languageCodes.add(LanguageCode("zen", "", "Zenaga", "zenaga", "Zenaga"))
        languageCodes.add(
            LanguageCode(
                "zgh",
                "",
                "Standard Moroccan Tamazight",
                "amazighe",
                "standard marocain"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zha",
                "za",
                "Zhuang; Chuang",
                "zhuang; chuang",
                "Zhuang"
            )
        )
        languageCodes.add(
            LanguageCode(
                "chi (B)",
                "",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zho (T)",
                "zh",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )

        languageCodes.add(
            LanguageCode(
                "zho",
                "zh",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )

        languageCodes.add(
            LanguageCode(
                "znd",
                "",
                "Zande languages",
                "zandé, langues",
                "Zande-Sprachen"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zul",
                "zu",
                "Zulu",
                "zoulou",
                "Zulu-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zun",
                "",
                "Zuni",
                "zuni",
                "Zuñi-Sprache"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zxx",
                "",
                "No linguistic content; Not applicable",
                "pas de contenu linguistique; non applicable",
                "Kein linguistischer Inhalt"
            )
        )
        languageCodes.add(
            LanguageCode(
                "zza",
                "",
                "Zaza; Dimili; Dimli; Kirdki; Kirmanjki; Zazaki",
                "zaza; dimili; dimli; kirdki; kirmanjki; zazaki",
                "Zazaki"
            )
        )
        languageCodes.add(
            LanguageCode(
                "chi",
                "",
                "Chinese",
                "chinois",
                "Chinesisch"
            )
        )
        languageCodes.add(LanguageCode("nar", "", "Narrative", "Narratif", "Erzählung"))

    }
}