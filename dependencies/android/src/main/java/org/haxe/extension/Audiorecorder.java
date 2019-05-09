package org.haxe.extension;


import android.app.Activity;
import android.content.res.AssetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.util.Log;


import java.io.IOException;
import java.lang.Throwable;

import org.haxe.lime.HaxeObject;

/* 
	You can use the Android Extension class in order to hook
	into the Android activity lifecycle. This is not required
	for standard Java code, this is designed for when you need
	deeper integration.
	
	You can access additional references from the Extension class,
	depending on your needs:
	
	- Extension.assetManager (android.content.res.AssetManager)
	- Extension.callbackHandler (android.os.Handler)
	- Extension.mainActivity (android.app.Activity)
	- Extension.mainContext (android.content.Context)
	- Extension.mainView (android.view.View)
	
	You can also make references to static or instance methods
	and properties on Java classes. These classes can be included 
	as single files using <java path="to/File.java" /> within your
	project, or use the full Android Library Project format (such
	as this example) in order to include your own AndroidManifest
	data, additional dependencies, etc.
	
	These are also optional, though this example shows a static
	function for performing a single task, like returning a value
	back to Haxe from Java.
*/
public class Audiorecorder extends Extension {


	public static void sampleMethod (int inputValue, final HaxeObject callback) {
		callback.call ("action", new Object[] { inputValue * 100 });
	}

	private static final String TAG = "trace[java]";
	private static int RECORDER_SAMPLERATE = 8000;
	private static int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	private static int bufferSize;
	
	private static AudioRecord recorder = null;
	private static Thread recordingThread = null;
	private static boolean isRecording = false;
	
	//todo change for update listener
	public static String startRecording(final HaxeObject callback) {
		if (isRecording)
			return ",,";
			
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING) * 2;

		recorder = initFirstGood();
		/*new AudioRecord(MediaRecorder.AudioSource.MIC,
				RECORDER_SAMPLERATE, RECORDER_CHANNELS,
				RECORDER_AUDIO_ENCODING, bufferSize);
		*/
		recorder.startRecording();
		isRecording = true;
		recordingThread = new Thread(new Runnable() {
			public void run() {
				byte sData[] = new byte[bufferSize];

				while (isRecording) {
					// gets the voice output from microphone to byte format
					recorder.read(sData, 0, bufferSize);
					Log.i(TAG,"Got data" + sData.toString());
					try {
						//pass data to haxe
						callback.call("action", new Object[] {sData});
					} catch (Throwable e) {
						e.printStackTrace();
						//call error
					}
				}
			}
		}, "AudioRecorder Thread");
		recordingThread.start();
		return RECORDER_SAMPLERATE+","+getChannels(RECORDER_CHANNELS)+","+getFormat(RECORDER_AUDIO_ENCODING);
	}
	
	private static int getChannels(int i){
		if (i==AudioFormat.CHANNEL_IN_STEREO)
			return 2;
		if (i==AudioFormat.CHANNEL_IN_MONO)
			return 1;
		return 0;
	}
	
	private static int getFormat(int i){
		if (i==AudioFormat.ENCODING_PCM_8BIT)
			return 1;
		if (i==AudioFormat.ENCODING_PCM_16BIT)
			return 2;
		return 4;
	}
	
	private static AudioRecord initFirstGood(){
		int[] mSampleRates = new int[] { /*44100, 11025, 22050, 16000,*/ 8000, 11025, 16000, 22050, 44100 };
		short [] aformats = new short[] { AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT };
		short [] chConfigs = new short[] { AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO };
		Log.i(TAG, aformats + "");
		Log.i(TAG, chConfigs + "");
		for (int rate : mSampleRates) {
			RECORDER_SAMPLERATE=rate;
			for (short audioFormat : aformats) {
				RECORDER_AUDIO_ENCODING=audioFormat;
				for (short channelConfig : chConfigs) {
					RECORDER_CHANNELS=channelConfig;
					try {
						Log.d(TAG, "Attempting rate " + rate + "Hz, bits: " + audioFormat + ", channel: " + channelConfig);
						int bufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat);
						if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
							Log.d(TAG, "Buffer size OK "+bufferSize);
							return new AudioRecord(MediaRecorder.AudioSource.MIC,
								rate, channelConfig,
								audioFormat, bufferSize);
						}
					}catch(Throwable e){
						Log.e(TAG,e+"");
					}
				}
			}
		}
		return null;
	}

	public static void stopRecording() {
		// stops the recording activity
		if (null != recorder) {
			isRecording = false;
			recorder.stop();
			recorder.release();
			recorder = null;
			recordingThread = null;
		}
	}
	
}