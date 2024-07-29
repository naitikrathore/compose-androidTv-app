package com.iwedia.cltv.platform.mk5;

/**
 * This class is general data structure and only used for storing the data from callback
 *
 * please refer class: TvCallbackHandler
 *
 * @author mtk40707
 *
 */
public final class TvCallbackData {
    public int param1;
    public int param2;
    public int param3;
    public int param4;
    public String paramStr1;
    public long paramLong1;
    public boolean paramBool1;
    public boolean paramBool2;
    public Object paramObj1;
    public Object paramObj2;

    public TvCallbackData(){
        param1 = -1;
        param2 = -1;
        param3 = -1;
        param4 = -1;
        paramStr1 = null;
        paramLong1 = -1;
        paramBool1 = false;
        paramBool2 = false;
        paramObj1 = null;
        paramObj2 = null;
    }
}
