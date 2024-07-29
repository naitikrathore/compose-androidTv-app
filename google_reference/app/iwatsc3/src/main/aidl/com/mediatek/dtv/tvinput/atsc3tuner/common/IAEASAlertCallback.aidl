package com.mediatek.dtv.tvinput.atsc3tuner.common;

/*
	Defines the callback invoked by TIS to  send  A-EAS alert message to client
*/

interface IAEASAlertCallback{
	  
	  /**
	    *	Defines the callback interface to be invoked by TIS to show A-EAS message.
	    *	
	    *	@param
	    *	notificationStatus(Integer) : 1 (Show)  , 0 (Dismiss)		
	    */
	  void aeasAlert(in String aeatId , in  String eventDesc , in String aeatMessage, in int notificationStatus);
	  void dismissAeasAlert(in String aeatId , in int notificationStatus);
}