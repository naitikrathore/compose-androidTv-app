// ILanguageMapperInterface.aidl
package com.cltv.mal.interfaces;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.LanguageCode;
interface ILanguageMapperInterface {
    String getLanguageName(in String languageCode);
    String getPreferredLanguageName(in String languageCode);
    String getLanguageCodeByCountryCode(in String countryCode);
    int getTxtDigitalLanguageMapByCountryCode(in String countryCode);
    int getTxtDigitalLanguageMapByPosition(in int position);
    LanguageCode[] getLanguageCodes();
    LanguageCode getDefaultLanguageCode();
    String getLanguageCode(in String trackLanguage);
}