// IAsyncScheduledRecordingListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
import com.cltv.mal.model.entities.ScheduledRecording;
oneway interface IAsyncScheduledRecordingListener {
   void onResponse(in ScheduledRecording[] result);
}