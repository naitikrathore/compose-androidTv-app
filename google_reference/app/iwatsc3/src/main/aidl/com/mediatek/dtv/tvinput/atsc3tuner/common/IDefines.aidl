package com.mediatek.dtv.tvinput.atsc3tuner.common;

interface IDefines {

     /** ScanType **/
     const int AUTO_SCAN = 0;
     const int MANUAL_SCAN = 1;

     /** ScanMode **/
     const int SCAN_MODE_DVB_DTV  = 1;
     const int SCAN_MODE_DVB_ATV  = 2;
     const int SCAN_MODE_DVB_ATSC  = 3;
     const int SCAN_MODE_DVB_ISDB_ANTENNA  = 4;
     const int SCAN_MODE_DVB_ISDB_CABLE  = 5;
     const int SCAN_MODE_DVB_ATSC3  = 6;

     /** TunerType **/
     const int TUNER_TYPE_PASSTHROUGH = -1;
     const int TUNER_TYPE_DTV_CABLE = 0;
     const int TUNER_TYPE_DTV_ANTENNA = 1;
     const int TUNER_TYPE_DTV_SATELLITE = 2;
     const int TUNER_TYPE_ATV = 8;
     const int TUNER_TYPE_ATV_CABLE = 9;
     const int TUNER_TYPE_OTHERS = 10;
     const int TUNER_TYPE_ATSC_CABLE = 11;
     const int TUNER_TYPE_ATSC_ANTENA = 12;
     const int TUNER_TYPE_ISDB_CABLE = 13;
     const int TUNER_TYPE_ISDB_ANTENA = 14;

     /** update scan **/
     const int QUICK_SCAN = 1;
     const int FULL_SCAN = 0;

     /** FullScanType **/
     const int FULL_SCAN_TYPE_DTV_CABLE = 1;
     const int FULL_SCAN_TYPE_DTV_ANTENNA = 2;
     const int FULL_SCAN_TYPE_ATV_CABLE = 4;
     const int FULL_SCAN_TYPE_ATV_ANTENNA = 8;
     const int FULL_SCAN_TYPE_ALL = 0;

     /** Scan Param bundle Keys **/
     const  String KEY_SCAN_TYPE = "scantype";
     const  String KEY_SCAN_MODE = "scanmode";
     const  String KEY_TUNER_TYPE = "tunertype";
     const  String KEY_QUICK_SCAN = "quickscan";
     const  String KEY_FULLSCAN_TYPE = "fullscantype";
     const  String KEY_FREQUENCY = "frequency";
     const  String KEY_BANDWIDTH = "bandwidth";
     const  String KEY_MODULATION = "modulation";
     const  String KEY_SYMBOLRATE = "symbolrate";
     
     /** Binder type **/
     const int BINDER_TYPE_SCAN = 0;
     const int BINDER_TYPE_CLOCK = 1;
     const int BINDER_TYPE_CC = 2;
     const int BINDER_TYPE_PL = 3;
     const int BINDER_TYPE_DE = 4;
     const int BINDER_TYPE_AE = 5;
     const int BINDER_TYPE_BROWSER_STATUS = 6;
	
    /* Dialog enhancement option */	
	const int DE_OPTION_OFF = 0;
	const int DE_OPTION_LOW = 4;
	const int DE_OPTION_MEDIUM = 8 ;
	const int DE_OPTION_HIGH = 12	;

	/* CC Settings bundle param keys */
	const  String  KEY_CAPTIONS_DISPLAY = "captions_display_key";
    const  String KEY_CAPTIONS_SERVICES = "captions_services";
    const  String KEY_CAPTIONS_ADVANCED = "captions_advanced_selection";
    const  String KEY_CAPTIONS_TEXTSIZE = "captions_text_size";
    const  String KEY_CAPTIONS_FONT_FAMILY = "captions_font_family";
    const  String KEY_CAPTIONS_TEXT_COLOR = "captions_text_color";
    const  String KEY_CAPTIONS_TEXT_OPACITY = "captions_font_opacity";
    const  String KEY_CAPTIONS_EDGE_TYPE = "captions_edge_type";
    const  String KEY_CAPTIONS_EDGE_COLOR = "captions_edge_color";
    const  String KEY_CAPTIONS_BACKGROUND_COLOR = "captions_background_color";
    const  String KEY_CAPTIONS_BACKGROUND_OPACITY = "captions_background_opacity";

    //For scan listener
    const String KEY_SCAN_RESULT = "scan_result";
    const String KEY_SCAN_PROGRESS = "scan_progress";
    const String KEY_EVENT_TYPE_TV_SERVICE_NUMBER  = "number_channel_scanned";    
    //onEvent types

    const int KEY_EVENT_TYPE_QUALITY = 0;
    const int KEY_EVENT_TYPE_STRENGTH = 1;
    const int KEY_EVENT_TYPE_FREQ = 2;
    const int KEY_EVENT_TYPE_TV = 3;
    const int KEY_EVENT_TYPE_RADIO = 4;
    
    //for scanlistener onEvent

    const String KEY_SIGNAL_QUALITY = "signal_quality";
    const String KEY_SIGNAL_STRENGTH = "signal_strength";
    const String KEY_FREQ_CHANGED = "change_freq";

    //For scan progress/results

    const String KEY_TV_SERVICE = "tv_service";
    const String KEY_RADIO_SERVICE = "radio_service";

    //for scan abort

    const String KEY_ROUTE = "route_id";
    const String KEY_BROADCAST = "broadcast_type";

    //Preferred language key

    const String KEY_PREF_LANG_UI = "0";
    const String KEY_PREF_LANG_AUDIO = "1";
    const String KEY_PREF_LANG_SUBTITLE = "2";
    const String KEY_PREF_LANG_EI_AUDIO = "3";
    const String KEY_PREF_LANG_VID_DESC = "4";
    const String KEY_PREF_LANG_AUD_DESC = "5";

     // CC color codes
      const String CC_WHITE = "ffffff";
      const String CC_BLACK = "0" ;
      const String CC_RED = "ff0000" ;
      const String CC_YELLOW = "ffff00" ;
      const String CC_GREEN = "ff00";
      const String CC_CYAN = "ffff" ;
      const String CC_BLUE = "ff";
      const String CC_MAGENTA = "ff00ff" ;

      //CC edge type
      const int EDGE_NONE = 0;
      const int EDGE_UNIFORM = 1;
      const int EDGE_DROP = 2;
      const int EDGE_RAISED = 3;
      const int EDGE_DEPRESSED = 4;
      const int EDGE_LEFT_DROP = 5 ; 
      const int EDGE_RIGHT_DROP = 6 ;

    //CC Attributes 
    const int ATTR_SOLID = 0 ;
    const int ATTR_TRANSPARENT = 1;
    const int ATTR_TRANSLUCENT = 2;
    const int ATTR_FLASHING = 4;

    //CC penstyle 
    const String PEN_STYLE = "pen_style";
    const int PEN_NORMAL = 0;
    const int PEN_ITALIC = 1;
    const int PEN_UNDERLINE = 2;

    //CC penSize
    const String PEN_SIZE = "pen_size";
    const int PEN_STANDARD = 0;
    const int PEN_SMALL = 1;
    const int PEN_LARGE = 2;

    //CC FontStyle
    const int FONT_DEFAULT = 0;
    const int MONOSPACED_WITH_SERIFS = 1;
    const int PROPORTIONALLY_SPACED_WITH_SERIFS = 2;
    const int MONOSPACED_WITHOUT_SERIFS = 3;
    const int PROPORTIONALLY_SPACED_WITHOUT_SERIFS = 4;
    const int FONT_CASUAL = 5;
    const int FONT_CURSIVE = 6;
    const int SMALL_CAPITALS = 7;

      //Language keys
      const String LANG_CODE_ENGLISH = "eng";
      const String LANG_CODE_SPANISH ="spa";
      const String LANG_CODE_KOREAN ="kor";
      const String LANG_CODE_FRENCH_1 ="fra";
      const String LANG_CODE_FRENCH_2 ="fre";
      const String LANG_CODE_RUSSIAN ="rus";
      const String LANG_CODE_GERMAN_1= "deu";
      const String LANG_CODE_GERMAN_2= "ger";
      const String LANG_CODE_PORTUGUESE ="por";
      const String LANG_CODE_POLISH ="pol";
      const String LANG_CODE_ITALIAN ="ita";
      const String LANG_CODE_DANISH ="dan";
      const String LANG_CODE_FINNISH ="fin";
      const String LANG_CODE_SWEDISH ="swe";
      const String LANG_CODE_NORWEGIAN ="nor";
      const String LANG_CODE_ICELANDIC_1 ="ice";
      const String LANG_CODE_ICELANDIC_2 ="isl";
      const String LANG_CODE_DUTCH_1 ="dut";
      const String LANG_CODE_DUTCH_2 ="nld";
      const String LANG_CODE_GREEK_1 ="grc";
      const String LANG_CODE_GREEK_2 ="gre";
      const String LANG_CODE_GREEK_3 ="ell";
      const String LANG_CODE_TURKISH_1 ="tur";
      const String LANG_CODE_TURKISH_2 ="ota";
      const String LANG_CODE_CZECH_1 ="cze"; 
      const String LANG_CODE_CZECH_2 ="ces";
      const String LANG_CODE_HUNGARIAN ="hun";
      const String LANG_CODE_HEBREW ="heb";
      const String LANG_CODE_HINDI ="hin";
      const String LANG_CODE_ROMANIAN_1 ="rum";
      const String LANG_CODE_ROMANIAN_2 ="rom";
      const String LANG_CODE_CHINESE_1 ="zho";
      const String LANG_CODE_CHINESE_2 ="chi";
      const String LANG_CODE_VIETNAMESE ="vie";
      const String LANG_CODE_ARABIC ="ara";

    //For Signal Info Parameters
    const String INFO_SIGNAL_QUALITY = "param_signal_quality";
    const String INFO_SIGNAL_STRENGTH = "param_signal_strength";
    const String INFO_BER = "param_ber";
    const String INFO_SYMBOL_RATE = "param_symbol_rate";
    const String INFO_UEC = "param_uec";
    const String INFO_AGC = "param_agc";

    //For Aud/Vid Description service
    const int TYPE_AUDIO_EIS = 1;
    const int TYPE_VIDEO_DESC = 2;
    const int TYPE_PRIMARY_AUDIO = 3;
    const int DESC_SRVC_ENABLE = 1;
    const int DESC_SRVC_DISABLE = 0;

	//AEAT filters
    const String KEY_AEAT_FILTER_AUDIENCE = "aeat_filter_audience";
    const String KEY_AEAT_FILTER_PRIORITY = "aeat_filter_priority";
    const int  AEAT_FILTER_AUDIENCE_PUBLIC = 0;
    const int  AEAT_FILTER_AUDIENCE_RESTRICTED = 1;
    const int  AEAT_FILTER_AUDIENCE_PRIVATE = 2;
    const int  AEAT_FILTER_AUDIENCE_NONE = 3;

    const int  AEAT_FILTER_PRIORITY_MINOR  = 0;
    const int  AEAT_FILTER_PRIORITY_LOW = 1;
    const int  AEAT_FILTER_PRIORITY_MODERATE = 2;
    const int  AEAT_FILTER_PRIORITY_HIGH = 3;
    const int  AEAT_FILTER_PRIORITY_MAXIMUM = 4;
    const int  AEAT_FILTER_PRIORITY_NONE= 5;

    //IntrApp
    const String KEY_INTRAPP_KEY_LIST = "KEYLIST";
}
