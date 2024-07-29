package com.mediatek.dtv.tvinput.atsc3tuner.common;
import android.os.Bundle;

interface IClosedCaption {
    /**
     * Get CC Settings from TIS.
     * @return
     *	(Bundle) :
            *   key:
          	    KEY_CAPTIONS_DISPLAY (int) : 0 -> disable , 1 -> enable
                KEY_CAPTIONS_TEXTSIZE (float) : Default 1
                KEY_CAPTIONS_TEXT_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA
                KEY_CAPTIONS_EDGE_TYPE (int) : IDefines.EDGE_NONE, IDefines.EDGE_UNIFORM, IDefines.EDGE_DROP, IDefines.EDGE_RAISED, IDefines.EDGE_DEPRESSED
                KEY_CAPTIONS_EDGE_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA
                KEY_CAPTIONS_BACKGROUND_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA
     *
     */
	Bundle getCCSettings();

    /**
     * Set CC Settings to TIS.
     *	@param
     *	(Bundle) :
                *   key:
              	    KEY_CAPTIONS_DISPLAY (int) : 0 -> disable , 1 -> enable
                    KEY_CAPTIONS_TEXTSIZE (float) : Default 1
                    KEY_CAPTIONS_TEXT_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA
                    KEY_CAPTIONS_EDGE_TYPE (int) : IDefines.EDGE_NONE, IDefines.EDGE_UNIFORM, IDefines.EDGE_DROP, IDefines.EDGE_RAISED, IDefines.EDGE_DEPRESSED
                    KEY_CAPTIONS_EDGE_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA
                    KEY_CAPTIONS_BACKGROUND_COLOR (String) : IDefines.CC_WHITE, IDefines.CC_BLACK, IDefines.CC_RED, IDefines.CC_YELLOW, IDefines.CC_GREEN, IDefines.CC_CYAN, IDefines.CC_BLUE, IDefines.CC_MAGENTA

     * @return
     *	boolean : Whether the setting is successful. (True): Yes  (False): No
     *
     */
	boolean setCCSettings(in Bundle param);

}