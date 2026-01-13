package com.IMExtraction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/*
	- Creates all the functionality to display a buffer of raw data as images
	- Displays in RGBA (using different codecs)
	- Selection too
*/

public class ImageDisplayer extends JPanel{

	private byte[] currentData;
	private BufferedImage image;
	WritableRaster raster;
	Pixel.Encoding activeEncoding = Pixel.Encoding.RGBA;
	private ImagePanel imagePanel;
	private JScrollBar fileScroller;

	private int currentOffset = 0x0;
	private int width = 1;
	private int height = 1;
	private int lineOffset = 0;

	public ImageDisplayer(){
		this.setLayout(new BorderLayout());
		this.setFocusable(true);
		this.setRequestFocusEnabled(true);

		this.imagePanel = new ImagePanel();

		JPanel toolbar = new JPanel();
		JComboBox<Pixel.Encoding> encodingPicker = new JComboBox<>(Pixel.Encoding.values());

		encodingPicker.addActionListener(e -> {
			activeEncoding = (Pixel.Encoding) encodingPicker.getSelectedItem();
			display(); // Re-render with new encoding
		});

		toolbar.add(new JLabel("Encoding: "));
		toolbar.add(encodingPicker);
		add(toolbar, BorderLayout.NORTH);
		JScrollPane viewer = new JScrollPane(imagePanel);
		add(viewer, BorderLayout.CENTER);


	}

	
	public void setInitialSize() {
		int pixLen = this.currentData.length / Pixel.SIZE;
		if (pixLen < 256) {
			updateSize(pixLen, 1);
		} else {
			updateSize(256, Math.min(pixLen / 256, 512));
		}
	}

	public void updateSize(int width, int height) {
		this.width = Math.max(width, 1);
		this.height = Math.max(height, 1);
		this.imagePanel.deleteSelection();
		this.rebuildScroller();
		this.updateScroller();
		this.display();
	}

	public void updateOffset(int offset) {
		int currSize = this.width * this.height * Pixel.SIZE;
		this.currentOffset = Math.min(Math.max(offset, 0), this.currentData.length - currSize);
		this.updateScroller();
		this.display();
	}
	public void updateOffsetNoUpdate(int offset) {
		int currSize = this.width * this.height * Pixel.SIZE;
		this.currentOffset = Math.min(Math.max(offset, 0), this.currentData.length - currSize);
		this.display();
	}

	public void updateFile(byte[] fileBytes) {
		this.currentData = fileBytes;

		this.currentOffset = 0;
		this.lineOffset = 0;
		if (this.fileScroller != null) 
			remove(this.fileScroller);
		revalidate();
		//TODO: Re-draw
	}

	private void rebuildScroller() {

		if (this.fileScroller != null) 
			remove(this.fileScroller);
		int max = this.currentData.length / Pixel.SIZE / this.width;
		if (max <= 512) return; // No scroll bar neccesary
		this.fileScroller = new JScrollBar(JScrollBar.VERTICAL, 0, 1, 0, max - this.height);
		
		this.fileScroller.addAdjustmentListener(e -> {
			int currentValue = e.getValue();

			int newOffset = currentValue * Pixel.SIZE * this.width;
			updateOffsetNoUpdate(newOffset);
		});
		add(this.fileScroller, BorderLayout.WEST);
		revalidate();
	}

	private void updateScroller() {
		if (this.fileScroller == null) return;
		this.fileScroller.setValue(this.currentOffset / Pixel.SIZE / this.width);
	}

	public void display() {
		if (this.currentData == null) return;
		int totalPixels = this.height * this.width;
		this.image = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);

		int[] internalPixels = ((java.awt.image.DataBufferInt) image.getRaster().getDataBuffer()).getData();

		for (int i = 0; i < totalPixels; i++) {
			int dataIdx = this.currentOffset + this.lineOffset + (i * Pixel.SIZE);
			if (dataIdx + 3 >= currentData.length) break;
			// Ensure we don't run off the end of the byte array
			if (dataIdx + 3 >= currentData.length) break;

			// Pack the bytes directly into the internal image array
			byte[] pixel = new byte[4];
			pixel[0] = currentData[dataIdx];
			pixel[0 + 1] = currentData[dataIdx + 1];
			pixel[0 + 2] = currentData[dataIdx + 2];
			pixel[0 + 3] = currentData[dataIdx + 3];

			internalPixels[i] = Pixel.packPixelForJava(
				pixel,
				activeEncoding
			);
		}
		imagePanel.setImage(image);
	}

	// Simple inner class to handle the drawing
	class ImagePanel extends JPanel {
		private BufferedImage img;
		private Rectangle selection;
		private Point startPoint;
		private double zoomFactor = 1.0;

		public ImagePanel() {
			this.setFocusable(true);
			this.setRequestFocusEnabled(true);
			// Listen for mouse wheel movement
			addMouseWheelListener(e -> {
				if (e.getWheelRotation() < 0) {
					zoomFactor *= 1.1; // Zoom In
				} else {
					zoomFactor /= 1.1; // Zoom Out
				}
				// Cap the zoom so it doesn't disappear or get too huge
				zoomFactor = Math.max(0.1, Math.min(zoomFactor, 10.0));
				
				revalidate(); // Inform ScrollPane that size changed
				repaint();

				if (getParent() instanceof JViewport) {
						getParent().revalidate();
					}
			});
			// Selection Logic 
			MouseAdapter ma = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					if (img == null) return;
					startPoint = e.getPoint();
					startPoint.x++;
					startPoint.y++;
					startPoint.x /= zoomFactor;
					startPoint.y /= zoomFactor;
					validatePoint(startPoint);
					selection = new Rectangle(startPoint);
					
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (img == null) return;
					Point currPoint = e.getPoint();
					validatePoint(currPoint);
					int x =  (int) Math.min(startPoint.x, (currPoint.x + 1) / zoomFactor);
					int y =  (int) Math.min(startPoint.y, (currPoint.y + 1) / zoomFactor);
					int w =  (int) Math.abs(startPoint.x - (currPoint.x + 1) / zoomFactor);
					int h =  (int) Math.abs(startPoint.y - (currPoint.y + 1) / zoomFactor);
					selection.setBounds(x, y, w, h);
					repaint();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (img == null) return;
					repaint();
				}
			};
			addMouseListener(ma);
			addMouseMotionListener(ma);
		}
		public void deleteSelection() {
			this.selection = null;
		}

		public Rectangle getSelection() {
			return this.selection;
		}
		private void validatePoint(Point old) {
			double maxW = this.img.getWidth() * this.zoomFactor;
			double maxH = this.img.getHeight() * this.zoomFactor;
			old.x = Math.max(0, Math.min((int) maxW, old.x));
			old.y = Math.max(0, Math.min((int) maxH, old.y));	
		}

		public void setImage(BufferedImage img) { this.img = img; repaint(); }
		public void update() {repaint();}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (img != null) {
				Graphics2D g2 = (Graphics2D) g;
			
				// This makes the zoom look "crisp" (pixelated) instead of blurry
				// Very important for analyzing binary data!
				g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
									RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

				// Apply the zoom transformation
				g2.scale(zoomFactor, zoomFactor);
				g2.drawImage(img, 0, 0, null);
				if (selection != null) {
					float[] dash = {2.0f};
					float dashwidth = Math.min(Math.max(0.01f, 2.0f / (float) zoomFactor), 5.0f);
					BasicStroke dashedStroke = new BasicStroke(dashwidth, 
											BasicStroke.CAP_BUTT, 
											BasicStroke.JOIN_MITER, 
											10.0f, dash, 0.0f);
					g2.setStroke(dashedStroke);
					g2.setColor(new Color(1.0f, 0f, 0f, 0.5f));
					g2.drawRect(selection.x, selection.y, selection.width, selection.height);
				}

			}
			
		}
		@Override
		public Dimension getPreferredSize() {
			if (img == null) return new Dimension(200, 200);
			// We must tell the JScrollPane how big the zoomed image is
			int w = (int) (img.getWidth() * zoomFactor);
			int h = (int) (img.getHeight() * zoomFactor);
			return new Dimension(w, h);
		}
	}

	public void makeCopy(CopyBuffer copyBuffer) {
		Rectangle rect = this.imagePanel.getSelection();
		if (rect == null) return;
		if (rect.width <= 0 || rect.height <= 0) return;
		BufferedImage subImage = this.image.getSubimage(rect.x, rect.y, rect.width, rect.height);
		copyToClipboard(subImage);

		// ------------------------------- ADD THE OFFSET COPY LOGIC
		int rW = rect.width;
		int rH = rect.height;
		int rowSizeInBytes = rW * Pixel.SIZE;
		int stride = this.width * Pixel.SIZE;

		Pixel[] pixels = new Pixel[rW * rH];
		for (int i = 0; i < rH; i++) {
			int rowOffset = this.currentOffset + ((rect.y + i) * stride) + (rect.x * Pixel.SIZE);
			for (int j = 0; j < rW; j++) {
				int pixelOffset = rowOffset + Pixel.SIZE * j;
				byte[] pixelData = Arrays.copyOfRange(this.currentData, pixelOffset, pixelOffset + 4);
				pixels[rW * i + j] = new Pixel(pixelOffset, pixelData, this.activeEncoding);
			}
		}
		//copyBuffer.updateData(subImage);
		copyBuffer.updateData(rW, rH, pixels);
	}
	private void copyToClipboard(BufferedImage img) {
	Transferable transferable = new Transferable() {
			@Override
			public DataFlavor[] getTransferDataFlavors() { return new DataFlavor[]{DataFlavor.imageFlavor}; }
			@Override
			public boolean isDataFlavorSupported(DataFlavor flavor) { return DataFlavor.imageFlavor.equals(flavor); }
			@Override
			public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
				if (flavor.equals(DataFlavor.imageFlavor)) return img;
				throw new UnsupportedFlavorException(flavor);
			}
		};
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, null);
	}

	public void updateLine(int dx) {
		if (this.currentData == null) return;
		int newOffset = this.currentOffset + dx * Pixel.SIZE * this.width;
		this.updateOffset(newOffset);
	}

	public void updatePlane(int dx) {
		if (this.currentData == null) return;
		int newOffset = this.currentOffset + dx * Pixel.SIZE * this.width * this.height;
		this.updateOffset(newOffset);
	}

	public void updateHeight(int dy) {
		if (this.currentData == null) return;
		this.updateSize(this.width, this.height + dy);
	}

	public void updateWidth(int dx) {
		if (this.currentData == null) return;
		this.updateSize(this.width + dx, this.height);
	}

	public void updateDataStartingPoint(int dx) {
		if (this.currentData == null) return;
		this.updateOffsetNoUpdate(this.currentOffset + dx);
	}

	public void updateLineOffset(int dx) {
		if (this.currentData == null) return;
		this.lineOffset += dx;
		this.lineOffset = (this.lineOffset + this.width * Pixel.SIZE) % this.width * Pixel.SIZE;
		display();
	}
}

// Starts as ARGB, as we change, we start to see what is their encoding
// Then we save as RGBA (for png) and save in the export the original encoding to retrieve it