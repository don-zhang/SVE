package com.ryong21.io;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.ryong21.pcm2wav.PCM2WAV;
import com.ryong21.pcm2wav.WaveHeader;

import android.os.Environment;
import android.util.Log;

public class PcmWriter implements Runnable {
	private final Object mutex = new Object();
	private volatile boolean isRecording;
	private rawData rawData;
	private File pcmFile;
	DataOutputStream dataOutputStreamInstance;
	private List<rawData> list;
	private LinkedList<Byte> wavDatas = new LinkedList<Byte>();

	public PcmWriter() {
		super();
		pcmFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.pcm");
		list = Collections.synchronizedList(new LinkedList<rawData>());
	}

	public void init() {
		BufferedOutputStream bufferedStreamInstance = null;

		if (pcmFile.exists()) {
			pcmFile.delete();
		}

		try {
			pcmFile.createNewFile();
		} catch (IOException e) {
			throw new IllegalStateException("Cannot create file: " + pcmFile.toString());
		}

		try {
			bufferedStreamInstance = new BufferedOutputStream(new FileOutputStream(pcmFile));
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Cannot Open File", e);
		}

		dataOutputStreamInstance = new DataOutputStream(bufferedStreamInstance);

	}

	public void run() {
		while (this.isRecording()) {

			if (list.size() > 0) {
				rawData = list.remove(0);
				byte[] temp = new byte[2];
				try {

					for (int i = 0; i < rawData.size; ++i) {
						temp[0] = (byte) rawData.pcm[i];
						temp[1] = (byte) (rawData.pcm[i] >> 8);
						dataOutputStreamInstance.write(temp[0]);
						dataOutputStreamInstance.write(temp[1]);
						wavDatas.add(temp[0]);
						wavDatas.add(temp[1]);
					}

				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}
		stop();
		convert();
	}

	public void putData(short[] buf, int size) {
		rawData data = new rawData();
		data.size = size;
		System.arraycopy(buf, 0, data.pcm, 0, size);
		list.add(data);
	}

	public void stop() {
		try {
			dataOutputStreamInstance.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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

	class rawData {
		int size;
		short[] pcm = new short[1024 * 10];
	}

	public void convert() {
		/*
		 * //间接转换 String src =
		 * Environment.getExternalStorageDirectory().getAbsolutePath() +
		 * "/test.pcm"; String target =
		 * Environment.getExternalStorageDirectory().getAbsolutePath() +
		 * "/test.wav"; try { PCM2WAV.convertAudioFiles(src, target); } catch
		 * (Exception e) { e.printStackTrace(); }
		 */

		// 直接转换
		try {
			int PCMSize = wavDatas.size();
			WaveHeader header = PCM2WAV.getWaveHeader(PCMSize);
			byte[] headers = header.getHeader();

			// wav文件
			File wavFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.wav");

			if (wavFile.exists()) {
				wavFile.delete();
			}

			wavFile.createNewFile();

			BufferedOutputStream wavBufferedStreamInstance = new BufferedOutputStream(new FileOutputStream(wavFile));

			DataOutputStream wavDataOutputStreamInstance = new DataOutputStream(wavBufferedStreamInstance);

			//写入wav头，固定44字节
			wavDataOutputStreamInstance.write(headers);

			//写入pcm文件内容
			for (byte bytes : wavDatas) {
				wavDataOutputStreamInstance.write(bytes);
			}

			wavDataOutputStreamInstance.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
		Log.i("wuchao", "convert success");
	}
}
