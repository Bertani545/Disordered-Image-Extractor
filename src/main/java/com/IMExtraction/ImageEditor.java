package com.IMExtraction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.image.WritableRaster;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import javax.swing.JFileChooser;


/*
	- Creates all the functionality to edit images
	- Displays in RGBA (using different codecs)
	- Selection too
*/
/*
	Dos capas:
		Capa seleccionada y capa en el canvas
		La capa seleccionada se mueve independiente
		Cuando se presiona enter, se fusiona con la de abajo

		La capa de abajo puede ser una imagen normal
		Al seleccionar, se obtiene la informacion de la imagen (subimg) y se crea
		un panel editable
		Luego, con enter se fusiona con la de abajo

		Así solo necesitamos 2 JPanels que muestren imagenes


*/

public class ImageEditor extends JPanel{

	private ImagePanel imagePanel;
	private BottomLayer bottomLayer;
	private TopLayer topLayer;

	public ImageEditor(){
		this.setLayout(new BorderLayout());
		this.setFocusable(true);
		this.setRequestFocusEnabled(true);

		this.imagePanel = new ImagePanel();
		this.bottomLayer = new BottomLayer();

		JScrollPane viewer = new JScrollPane(imagePanel);
		InputMap im = viewer.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "none");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "none");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "none");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "none");

		add(viewer, BorderLayout.CENTER);


		setupKeyboardActions();
	}


	private void setupAction(int key, Runnable func) {
		InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = getActionMap();

		String keyName = KeyEvent.getKeyText(key);
		String actionText =  keyName + "_Pressed";
		im.put(KeyStroke.getKeyStroke(key, 0, false), actionText);
		am.put(actionText, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				func.run();
			}
		});
	}

	private void setupKeyboardActions() {
		setupAction(KeyEvent.VK_ENTER, () -> {
			mergeLayers();
		});
		setupAction(KeyEvent.VK_ESCAPE, () -> {
			if (topLayer != null) bottomLayer.restore(topLayer, imagePanel.getOgSelection());
			topLayer = null;
			imagePanel.repaint();
			//imagePanel.setImage(bottomLayer.getBottomImage());
			
		});
		setupAction(KeyEvent.VK_DELETE, () -> {
			topLayer = null;
			imagePanel.repaint();
		});
		setupAction(KeyEvent.VK_UP, () -> {
			if (topLayer == null) return;
			topLayer.updatePosition(0, -1);
			paintLayers();
		});
		setupAction(KeyEvent.VK_DOWN, () -> {
			if (topLayer == null) return;
			topLayer.updatePosition(0, 1);
			paintLayers();
		});
		setupAction(KeyEvent.VK_RIGHT, () -> {
			if (topLayer == null) return;
			topLayer.updatePosition(1, 0);
			paintLayers();
		});
		setupAction(KeyEvent.VK_LEFT, () -> {
			if (topLayer == null) return;
			topLayer.updatePosition(-1, 0);
			paintLayers();
		});
	}

	// Simple inner class to handle the drawing
	class ImagePanel extends JPanel {
		private Rectangle selection;
		private Rectangle ogSelection;
		private Point startPoint;
		private Point mousePosition;
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
					if (topLayer != null) {
						// Move the top Layer
						mousePosition = e.getPoint();
						validatePoint(mousePosition);
					} else {
						startPoint = e.getPoint();
						startPoint.x /= zoomFactor;
						startPoint.y /= zoomFactor;
						validatePoint(startPoint);
						selection = new Rectangle(startPoint);
					}
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					Point currPoint = e.getPoint();
					validatePoint(currPoint);
					if (topLayer != null) {
						// Get movement
						double dx =  - (mousePosition.x - currPoint.x) / zoomFactor;
						double dy =  - (mousePosition.y - currPoint.y) / zoomFactor;
						mousePosition = currPoint;
						topLayer.updatePosition((int) dx, (int) dy);

					} else {
						int x =  (int) Math.min(startPoint.x, currPoint.x / zoomFactor);
						int y =  (int) Math.min(startPoint.y, currPoint.y / zoomFactor);
						int w =  (int) Math.abs(startPoint.x - currPoint.x / zoomFactor);
						int h =  (int) Math.abs(startPoint.y - currPoint.y / zoomFactor);
						selection.setBounds(x, y, w, h);
					}
					paintLayers();
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (selection == null) return;
					if (selection.width == 0 || selection.height == 0) {
						deleteSelection();
						return;
					}
					if (topLayer == null) {
						Pixel[] newTopPixels = bottomLayer.cut(selection);
						if (newTopPixels == null) {
							selection = null;
							return;
						}
						topLayer = new TopLayer(selection, newTopPixels);
						ogSelection = selection;
						deleteSelection();
					}
					
					paintLayers();
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
		public Rectangle getOgSelection() {
			return this.ogSelection;
		}
		private void validatePoint(Point old) {
			double maxW = bottomLayer.getWidth() * this.zoomFactor;
			double maxH = bottomLayer.getHeight() * this.zoomFactor;
			old.x = Math.max(0, Math.min((int) maxW, old.x));
			old.y = Math.max(0, Math.min((int) maxH, old.y));
		}

		public void update() {repaint();}
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2 = (Graphics2D) g;

			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
								RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g2.scale(zoomFactor, zoomFactor);

			if (bottomLayer != null) g2.drawImage(bottomLayer.getBottomImage(), 0, 0, null);
			float[] dash = {2.0f};
			if (topLayer != null) {
				Point p = topLayer.getPosition();
				g2.drawImage(topLayer.getTopImage(), p.x, p.y, null);
				float dashwidth = Math.min(Math.max(0.01f, 2.0f / (float) zoomFactor), 5.0f);
				BasicStroke dashedStroke = new BasicStroke(dashwidth, 
										BasicStroke.CAP_BUTT, 
										BasicStroke.JOIN_MITER, 
										10.0f, dash, 0.0f);
				g2.setStroke(dashedStroke);
				g2.setColor(new Color(1.0f, 0f, 0f, 0.5f));
				g2.drawRect(p.x, p.y, topLayer.getWidth(), topLayer.getHeight());
				/*
				g2.setColor(Color.BLACK);
				BasicStroke offsetStroke = new BasicStroke(dashwidth, 
										BasicStroke.CAP_BUTT, 
										BasicStroke.JOIN_MITER, 
										10.0f, dash, 5.0f);
				g2.setStroke(offsetStroke);
				g2.drawRect(p.x, p.y, topLayer.getWidth(), topLayer.getHeight());
				*/

			} else if (selection != null) {
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
		@Override
		public Dimension getPreferredSize() {
			if (bottomLayer == null) return new Dimension(200, 200);
			int w = (int) (bottomLayer.getWidth() * zoomFactor);
			int h = (int) (bottomLayer.getHeight() * zoomFactor);
			return new Dimension(w, h);
		}
	}


	class BottomLayer {
		private Pixel[] pixels; // Maybe transform it to a 2D matrix
		private int width = 1000;
		private int height = 1000;
		private TopLayer topLayerReference;
		private BufferedImage bottomImage;// to merge with topLayer

		public BottomLayer() {
			this.pixels = new Pixel[this.width * this.height];
			// Creates a w x h image of 0000
			this.bottomImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
			// Diplays it
			imagePanel.repaint();
		}


		public BufferedImage getBottomImage() {
			return this.bottomImage;
		}

		public Pixel[] cut(Rectangle selection) {
			Point pos = new Point(selection.x, selection.y);
			int topW = selection.width;
			int topH = selection.height;
			if (topW > this.width || topH > this.height) return null;
			if (pos.x >= this.width || pos.y >= this.height) return null;

			Pixel[] topPixels = new Pixel[topW * topH];

			// Get them
			int pixelTotal = 0;
			int initialOffset = pos.y * this.width + pos.x;
			for (int i = 0; i < topH; i++) {
				if (pos.y + i >= this.width) break; 
				for (int j = 0; j < topW; j++) {
					if (pos.x + j >= this.height) break;
					int idx = initialOffset + i * this.width + j;
					topPixels[i * topW + j] = this.pixels[idx];
					pixelTotal +=  this.pixels[idx] == null ? 0 : 1;
					this.pixels[idx] = null;
				}
			}
			this.bottomImage = getImageFromPixels();
			imagePanel.repaint();
			return pixelTotal == 0 ? null : topPixels;
		}

		public void restore(TopLayer topLayer, Rectangle selection) {
			Pixel[] topPixels = topLayer.getPixels();
			Point pos = new Point(selection.x, selection.y);
			int topW = topLayer.getWidth();
			int topH = topLayer.getHeight();

			// To merge this is ilogical
			if (topW > this.width || topH > this.height) return;
			if (pos.x >= this.width || pos.y >= this.height) return;

			// Place them
			int initialOffset = pos.y * this.width + pos.x;
			for (int i = 0; i < topH; i++) {
				if (pos.y + i >= this.width) break; 
				for (int j = 0; j < topW; j++) {
					if (pos.x + j >= this.height) break;
					int idx = initialOffset + i * this.width + j;
					Pixel topPixel = topPixels[i * topW + j];
					this.pixels[idx] = topPixel;
				}
			}

			this.bottomImage = getImageFromPixels();
			imagePanel.repaint();
		}

		public void mergeTopLayer(TopLayer topLayer) {
			Pixel[] topPixels = topLayer.getPixels();
			Point pos = topLayer.getPosition();
			int topW = topLayer.getWidth();
			int topH = topLayer.getHeight();

			// To merge this is ilogical
			if (topW > this.width || topH > this.height) return;
			if (pos.x >= this.width || pos.y >= this.height) return;

			// Place them
			int initialOffset = pos.y * this.width + pos.x;
			for (int i = 0; i < topH; i++) {
				if (pos.y + i >= this.width) break; 
				for (int j = 0; j < topW; j++) {
					if (pos.x + j >= this.height) break;
					int idx = initialOffset + i * this.width + j;
					Pixel topPixel = topPixels[i * topW + j];
					this.pixels[idx] = topPixel;
				}
			}

			this.bottomImage = getImageFromPixels();
			imagePanel.repaint();
		}

		public BufferedImage getImageFromPixels() {
			BufferedImage newImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
			int[] internalPixels = ((java.awt.image.DataBufferInt) newImage.getRaster().getDataBuffer()).getData();
			int totalPixels = this.width * this.height;
			for (int i = 0; i < totalPixels; i++) {
				internalPixels[i] = this.pixels[i] == null ? 0 : this.pixels[i].packForJava();
			}
			return newImage;
		}

		public int getWidth() {
			return this.width;
		}
		public int getHeight() {
			return this.height;
		}
		public Pixel[] getPixels() {
			return this.pixels;
		}
	}

	class TopLayer {
		private Pixel[] pixels;
		private int width;
		private int height;
		private Point position;
		private BufferedImage topImage;

		public TopLayer(CopyBuffer copyBuffer) {
			this.pixels = copyBuffer.getPixels();
			if (pixels == null) return;

			this.width = copyBuffer.getWidth();
			this.height = copyBuffer.getHeight();
			this.position = new Point(100,200); // Always starts at the corner

			// Create the image from the data
			this.topImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
			int[] internalPixels = ((java.awt.image.DataBufferInt) this.topImage.getRaster().getDataBuffer()).getData();
			int totalPixels = this.width * this.height;
			for (int i = 0; i < totalPixels; i++) {
				internalPixels[i] = pixels[i].packForJava();
			}
		}

		public TopLayer(Rectangle selection, Pixel[] newPixels) {
			this.pixels = newPixels;
			this.width = selection.width;
			this.height = selection.height;
			this.position = new Point(selection.x, selection.y);

			this.topImage = new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB);
			int[] internalPixels = ((java.awt.image.DataBufferInt) this.topImage.getRaster().getDataBuffer()).getData();
			int totalPixels = this.width * this.height;
			for (int i = 0; i < totalPixels; i++) {
				if (pixels[i] == null) {
					internalPixels[i] = 0;
				} else {
					internalPixels[i] = pixels[i].packForJava();
				}
				
			}
		}

		public int getWidth() {
			return this.width;
		}
		public int getHeight() {
			return this.height;
		}
		public Point getPosition() {
			return this.position;
		}
		public BufferedImage getTopImage() {
			return this.topImage;
		}
		public Pixel[] getPixels() {
			return this.pixels;
		}
		// UPDATE NECCESARY
		public void updatePosition(int dx, int dy) {
			this.position.x += dx;
			this.position.y += dy;
		}
	}

	public void makePaste(CopyBuffer copyBuffer) {
		this.topLayer = new TopLayer(copyBuffer);
		imagePanel.repaint();
	}

	// Called when pressing the arrow keys or mouse dragging
	// The update of the coordinates should be handled by something else
	private void paintLayers() {
		imagePanel.repaint();
	}


	// Called when pressing enter. Merges both pixels buffers to create the "final" one
	private void mergeLayers() {
		if (this.topLayer == null) return;
		this.bottomLayer.mergeTopLayer(this.topLayer);
		this.topLayer = null;
		imagePanel.repaint();
	}

	private String getSavePath() {
		JFileChooser fileChooser = new JFileChooser();
		File workingDirectory = new File(System.getProperty("user.dir"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setDialogTitle("Choose a place to save and a name for both files");
		int userSelection = fileChooser.showSaveDialog(this);

		if (userSelection == JFileChooser.APPROVE_OPTION) {
			File fileToSave = fileChooser.getSelectedFile();
			String path = fileToSave.getAbsolutePath();

			File file1 = new File(path + ".bin");
			File file2 = new File(path + ".png");

			int response = JOptionPane.YES_OPTION;
			if (file1.exists() || file2.exists()) {
				response = JOptionPane.showConfirmDialog(this, 
							"At least one of the files already exists. Do you want to replace it?", 
							"Confirm Overwrite", 
							JOptionPane.YES_NO_OPTION);
			}
			if (response == JOptionPane.NO_OPTION) path = "";
			return path;
		}
		return "";
	}


	public void exportImage() {
		// First we do the png
		// Get borders
		Pixel[] pixels = this.bottomLayer.getPixels();
		if (pixels == null) return;
		// Top
		int top = 0;
		for (int i = 0; i < pixels.length; i++) {
			if (i % bottomLayer.getWidth() == 0) top ++;
			if (pixels[i] != null) break;
		}

		int bottom = bottomLayer.getWidth() - 1;
		for (int i = pixels.length - 1; i >= 0 ; i--) {
			if (i % bottomLayer.getWidth() == 0) bottom --;
			if (pixels[i] != null) break;
		}

		int left = -1;
		for (int j = 0; j < bottomLayer.getWidth(); j++) {
			for (int i = 0; i < bottomLayer.getHeight(); i++) {
				if (pixels[i * bottomLayer.getWidth() + j] != null)
					left = j;
			}
			if (left >= 0) break;
		}

		int right = -1;
		for (int j = bottomLayer.getWidth() - 1; j >= 0; j--) {
			for (int i = bottomLayer.getHeight() - 1; i >= 0; i--) {
				if (pixels[i * bottomLayer.getWidth() + j] != null)
					right = j;
			}
			if (right >= 0) break;
		}

		// No image
		if (left == -1 || right == -1) return;

		int w = right - left + 1;
		int h = bottom - top + 1;
		if (w == 0 || h == 0) return; // No image
		BufferedImage newImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		int[] internalPixels = ((java.awt.image.DataBufferInt) newImage.getRaster().getDataBuffer()).getData();
		int totalPixels = w * h;

		// Create image and binary file with the offsets
		

		String savePath = "";
		savePath = getSavePath();
		if (savePath == "") return;
		try {
			File outputImg = new File(savePath + ".png");
			FileOutputStream outputBinary = new FileOutputStream(savePath + ".bin");
			DataOutputStream offsetData = new DataOutputStream(outputBinary);

			int idx = 0;
			for (int i = 0; i < h; i++) {
				for (int j = 0; j < w; j++) {
					int pxId = top * bottomLayer.getWidth() + left + i * bottomLayer.getWidth() + j;
					if (pixels[pxId] == null) {
						internalPixels[idx] = 0;
						offsetData.writeInt(-1);
					} else {
						internalPixels[idx] = pixels[pxId].packForJava();
						offsetData.writeInt(pixels[pxId].getOffset());
					}
					
					idx ++;
				}
			}

			ImageIO.write(newImage, "png", outputImg);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
