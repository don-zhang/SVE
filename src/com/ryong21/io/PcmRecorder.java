package com.ryong21.io;

import com.ryong21.encode.Encoder;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public class PcmRecorder implements Runnable {

	private volatile boolean isRecording;
	private final Object mutex = new Object();
	private static final int frequency = 8000;
	private static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;

	public PcmRecorder() {
		super();
	}

	public void run() {
		
		Encoder encoder = new Encoder();
		Thread encodeThread = new Thread (encoder);
		encoder.setRecording(true);
		encodeThread.start();
		
		PcmWriter pcmWriter = new PcmWriter();
		pcmWriter.init();
		Thread writerThread = new Thread (pcmWriter);
		pcmWriter.setRecording(true);
		writerThread.start();
				
		synchronized (mutex) {
			while (!this.isRecording) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					throw new IllegalStateException("Wait() interrupted!", e);
				}
			}
		}
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

		int bufferRead = 0;
		int bufferSize = AudioRecord.getMinBufferSize(frequency,
				AudioFormat.CHANNEL_IN_DEFAULT, audioEncoding);
		
		short[] tempBuffer = new short[bufferSize * 2];
		AudioRecord recordInstance = new AudioRecord(
				MediaRecorder.AudioSource.MIC, frequency,
				AudioFormat.CHANNEL_IN_DEFAULT, audioEncoding, bufferSize * 2);

		recordInstance.startRecording();
		
		while (this.isRecording) {
			
			bufferRead = recordInstance.read(tempBuffer, 0, bufferSize * 2);
			if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			} else if (bufferRead == AudioRecord.ERROR_BAD_VALUE) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_BAD_VALUE");
			} else if (bufferRead == AudioRecord.ERROR_INVALID_OPERATION) {
				throw new IllegalStateException(
						"read() returned AudioRecord.ERROR_INVALID_OPERATION");
			}
			
			if(encoder.isIdle()){
				encoder.putData(System.currentTimeMillis(), tempBuffer, bufferRead);
			}else {
			}	
			
			pcmWriter.putData(tempBuffer, bufferRead);
				
		}
		recordInstance.stop();
		encoder.setRecording(false);
		
		pcmWriter.setRecording(false);
	}

	public void setRecording(boolean isRecording) {
		synchronized (mutex) {
			this.isRecording = isRecording;
			if (this.isRecording) {
				mutex.notify();
			}
		}
	}

	public boolean isRecording() {
		synchronized (mutex) {
			return isRecording;
		}
	}
}
