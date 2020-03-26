
package com.xiaominfc.opus_recorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Message;
import top.oply.opuslib.OpusRecorder;

public class AudioRecordHandler implements Runnable {

	private volatile boolean isRecording;
	private final Object mutex = new Object();
	private static final int frequency = 8000;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private static final float MAX_SOUND_RECORD_TIME = 60.0f;
	public static int packagesize = 160;// 320
	private String fileName = null;
	private float recordTime = 0;
	private long startTime = 0;
	private long endTime = 0;
	private long maxVolumeStart = 0;
	private long maxVolumeEnd = 0;
	private static AudioRecord recordInstance = null;
	private OpusRecorder opusRecorder;

	public AudioRecordHandler(String fileName) {
		super();
		this.fileName = fileName;
	}

	public void run() {
		try {
			android.util.Log.i("run", "0");
			synchronized (mutex) {
				android.util.Log.i("run", "1");
				while (!this.isRecording) {
					android.util.Log.i("run", "2");
					try {
						android.util.Log.i("run", "3");
						mutex.wait();
						android.util.Log.i("run", "4");
					} catch (InterruptedException e) {
						throw new IllegalStateException("Wait() interrupted!", e);
					}
				}
			}
			try {
				android.util.Log.i("run", "5");
				OpusRecorder.getInstance().startRecording(this.fileName);
				android.util.Log.i("run", "6");
				recordTime = 0;
				startTime = System.currentTimeMillis();
				maxVolumeStart = System.currentTimeMillis();
				android.util.Log.i("run", String.valueOf(this.isRecording));
				while (this.isRecording) {
					android.util.Log.i("run", "7");
					endTime = System.currentTimeMillis();
					recordTime = (float) ((endTime - startTime) / 1000.0f);
					if (recordTime >= MAX_SOUND_RECORD_TIME) {
						// timeover
						break;
					}
					maxVolumeEnd = System.currentTimeMillis();
				}
			} catch (Exception e) {
				throw e;
			} finally {
				android.util.Log.i("run", "8");
				OpusRecorder.getInstance().stopRecording();
				android.util.Log.i("run", "9");
				if (recordInstance != null) {
					android.util.Log.i("run", "10");
					recordInstance.stop();
					android.util.Log.i("run", "11");
					recordInstance.release();
					android.util.Log.i("run", "12");
					recordInstance = null;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

	}

	private void setMaxVolume(short[] buffer, int readLen) {
		try {
			if (maxVolumeEnd - maxVolumeStart < 100) {
				return;
			}
			maxVolumeStart = maxVolumeEnd;
			int max = 0;
			for (int i = 0; i < readLen; i++) {
				if (Math.abs(buffer[i]) > max) {
					max = Math.abs(buffer[i]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public float getRecordTime() {
		return recordTime;
	}

	public void setRecordTime(float len) {
		recordTime = len;
	}

	public void setRecording(boolean isRec) {
		android.util.Log.i("setRecording", "0");
		synchronized (mutex) {
			android.util.Log.i("setRecording", "1");
			this.isRecording = isRec;
			android.util.Log.i("setRecording", "2");
			if (this.isRecording) {
				android.util.Log.i("setRecording", "3");
				mutex.notify();
				android.util.Log.i("setRecording", "4");
			}
		}
	}

	public boolean isRecording() {
		android.util.Log.i("isRecording", "0");
		synchronized (mutex) {
			android.util.Log.i("isRecording", "1");
			return isRecording;
		}
	}
}
