package com.mediatek.dtv.tvinput.atsc3tuner.common;
import android.os.Bundle;
/*
        AIDL Interface for setting preferred language
*/

interface IPreferredLanguage{

    /**
    * Sets  Language preference
        * @bundle keys :-
        * IDefines.KEY_PREF_LANG_UI (String) :  preferred language of the Receiver User Interfaces
        * IDefines.KEY_PREF_LANG_AUDIO (String) :  preferred language for the audio output
        * IDefines.KEY_PREF_LANG_SUBTITLE (String) :  preferred language of the closed captions or subtitles
        * IDefines.KEY_PREF_LANG_EI_AUDIO (String) :  preferred language of aeas emergency Audio service
        * IDefines.KEY_PREF_LANG_VID_DESC(String) :  preferred language for video description service
        * IDefines.KEY_PREF_LANG_AUD_DESC (String) :  preferred language of audio description service
    */
    void setLangPreference(in Bundle langPrefs);

    /**
    * Get  Language preference
        * @bundle keys :-
        * IDefines.KEY_PREF_LANG_UI (String) :  preferred language of the Receiver User Interfaces
        * IDefines.KEY_PREF_LANG_AUDIO (String) :  preferred language for the audio output
        * IDefines.KEY_PREF_LANG_SUBTITLE (String) :  preferred language of the closed captions or subtitles
        * IDefines.KEY_PREF_LANG_EI_AUDIO (String) :  preferred language of aeas emergency Audio service
        * IDefines.KEY_PREF_LANG_VID_DESC(String) :  preferred language for video description service
        * IDefines.KEY_PREF_LANG_AUD_DESC (String) :  preferred language of audio description service
    */
    android.os.Bundle getLangPreference();

}

