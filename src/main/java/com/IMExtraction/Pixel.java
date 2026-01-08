package com.IMExtraction;
// At the end, they are all exported to  rgb (as in png) so
// the original format must be sotored somewhere 

public class Pixel {
	static int SIZE = 4;
	static enum Encoding { RGBA, ARGB, BGRA, ABGR }

	private int offset; // Where in original archive
	private byte[] data; // Pixel information
	private Encoding encoding; // How to encode it to display it in java and export

	public Pixel(int offset, byte[] data, Encoding encoding) {
		if (data.length != SIZE) return;

		this.offset = offset;
		this.data = data;
		this.encoding = encoding;
	}
	public Pixel() {}

	public void setInfo(int offset, byte[] data, Encoding encoding) {
		if (data.length != SIZE) return;

		this.offset = offset;
		this.data = data;
		this.encoding = encoding;
	}


	static int packPixelForJava(int b1, int b2, int b3, int b4, Encoding encoding) {
		return switch (encoding) {
			// Java's TYPE_INT_ARGB expects bits as: [Alpha 8][Red 8][Green 8][Blue 8]
			case RGBA -> (b4 << 24) | (b1 << 16) | (b2 << 8) | b3;
			case ARGB -> (b1 << 24) | (b2 << 16) | (b3 << 8) | b4;
			case BGRA -> (b4 << 24) | (b3 << 16) | (b2 << 8) | b1;
			case ABGR -> (b1 << 24) | (b4 << 16) | (b3 << 8) | b2;
		};
	}

	static int packPixelForExport(int b1, int b2, int b3, int b4, Encoding encoding) {
		return 0xFFFFFFFF;
	}

	static int packPixelForJava(byte[] b, Encoding encoding) {
		if (b.length < 4) return 0;
		int b1 = b[0] & 0xFF;
		int b2 = b[1] & 0xFF;
		int b3 = b[2] & 0xFF;
		int b4 = b[3] & 0xFF;
		return packPixelForJava(b1, b2, b3, b4, encoding);
	}

	public int packForJava() {
		return packPixelForJava(data, encoding);
	}

	public int getOffset() {
		return this.offset;
	}
}