package com.mediatek.dtv.tvinput.atsc3tuner.common;

/*
	AIDL Interface for setting preferred language 
*/

interface IDialogEnhancement{
  	
  	/**
		 * Sets  Language preference
		 * @param
		 * option (int) :  Dialog enhancement option in db , refer IDefines.aidl
	*/	
	void setDEOption(int option );
}
