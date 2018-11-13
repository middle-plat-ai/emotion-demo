package com.demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class Emotion {

	private double[] draw;     //原始double分类数据
	private double[][] frame;  //分帧存储

	private double[] wi;     //谐波频率
	private double[][] F;    //幅值

	public int wlen = 2;	//	     wlen 帧长，取2的指数次个，方便fft
	public int inc = 2;	    //	     inc  帧移
	public int fn;          //       f 分帧数

	private double[] vol; 
	private int[] zcr;
	private double[] pitch;
	
	private String jsonResult = "";
	
	public String getEmotion(byte[] b) {
	
		double[] raw = new double[b.length/2];				
		ByteArray2DoubleArray(raw, b);
		enframe(raw);
		volume();
		zcr();
		fft();
		
		
		
		if(zcr[0] > 100)
			jsonResult = "{\"emotion\":\"SADNESS/UNCERTAINTY/BOREDOM\"}";
		
		else if(zcr[0] > 50)
			jsonResult = "{\"emotion\":\"ANGER/DISLIKE/STRESS\"}";
		
		else if(zcr[0] > 30)
			jsonResult = "{\"emotion\":\"NEUTRAL\"}";	
		
		else if(zcr[0] > 20)
			jsonResult ="{\"emotion\":\"HAPPINESS/ENTHUSIASM/FRIENDLINESS\"}";
	
		else
			jsonResult ="{\"emotion\":\"WARMTH/CALMNESS\"}";
			
		return jsonResult;
	}

	//分帧
	public void enframe(double[] x) {		
		draw = x;
		int len = draw.length;
		System.out.println("draw len is " + len);

		int diff = len - wlen;
		if(diff < 0) {
			System.out.println("len is too short");
			return;
		}

		fn = diff / inc + 1;
		frame = new double[wlen][fn];     //分帧存储，列为总帧数，行为单帧数据
		System.out.println("fn: " + fn );
		for(int i = 0; i < fn; i++) {
			for(int j = 0; j < wlen; j++) {
				frame[j][i] = draw[i * inc + j];
			}
		}
	}

	public void fft() {		
		// 基频
		double w = 2 * Math.PI / wlen;
		// 倍频
		wi = new double[wlen];
		for(int i = 0; i < wlen; i++) {
			wi[i] = w * i;
		}

		int N = wlen;
		F =  new double[N][fn];   //列为总帧数，每行存储单帧的幅频值
		Complex[] x = new Complex[N];    
  
		for(int i = 0; i < fn; i++) {						
			//分帧转换成复数
			for (int m = 0; m < N; m++) {
				x[m] = new Complex(frame[m][i], 0);		
			}			
			FFT.show(x, "x");
			
			//fft
		    Complex[] y = FFT.fft(x);		
			FFT.show(y, "y");
			
			//求出幅频值
			for (int m = 0; m < N; m++) {			
				F[m][i] = y[m].abs();
			}  	
		}

		System.out.println("F");
	}

	public void volume() {
		vol = new double[fn];
		for(int i = 0; i < fn; i++) {
			for(int j = 0; j < wlen; j++) {
				vol[i] += Math.abs(frame[j][i]);
			}
		}
	}

	public void energy() {
		vol = new double[fn];
		for(int i = 0; i < fn; i++) {
			for(int j = 0; j < wlen; j++) {
				vol[i] += frame[j][i] * frame[j][i];
			}
		}
	}

	public void zcr() {
		zcr = new int[fn];
		for(int i = 0; i < fn; i++) {
			for(int j = 0; j < wlen -1 ; j++) {				
				double tmp = frame[j][i] * frame[j+1][i];
				if(tmp < 0)				
					zcr[i] += 1;
			}
		}
	}



	public void ByteArray2DoubleArray(double[] doubleArray, byte[] byteArray) {
		System.out.println("doubleArray " + doubleArray.length);
		System.out.println("byteArray " + byteArray.length);

		for (int i = 0; i < doubleArray.length; i++) {
			byte bl = byteArray[2 * i];
			byte bh = byteArray[2 * i + 1];
			short s = (short) ((bh & 0x00FF) << 8 | bl & 0x00FF);

			doubleArray[i] = s / 32768f; // 32768 = 2^15  归一化
		}
	}

	@SuppressWarnings({ "resource" })
	public static void main(String[] args) throws IOException  {		

		Emotion et = new Emotion();

		String pcmFilePath = "/home/swair/Desktop/test123.wav";
		File file = new File(pcmFilePath);
		Long filelength = file.length();  

		FileInputStream fs;		   
		fs = new FileInputStream(file);		

		byte[] b = new byte[filelength.intValue()];  
		double[] raw = new double[filelength.intValue()/2];

		fs.read(b);		
		et.ByteArray2DoubleArray(raw, b);	

		et.enframe(raw);

		et.volume();

		et.zcr();

		et.fft();
	
	}
}
