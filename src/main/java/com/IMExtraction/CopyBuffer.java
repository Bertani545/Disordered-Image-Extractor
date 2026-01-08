package com.IMExtraction;

import java.awt.image.BufferedImage;

public class CopyBuffer {
	private Pixel[] pixels;
	private int height;
	private int width;
	public CopyBuffer() {}


	public void updateData(int w, int h, Pixel[] pixels) {
		if (pixels.length != w * h) {
			this.pixels = null;
			this.height = 0;
			this.width = 0;
		}
		this.pixels = pixels;
		this.height = h;
		this.width = w;
	}


	public Pixel[] getPixels() {
		return this.pixels;
	}
	public int getHeight() {
		return this.height;
	}
	public int getWidth() {
		return this.width;
	}
}
