package com.IMExtraction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;


/*
	- Hereda de ImageDisplayer
	- 2 buffers, one with pixel data, one with offsets. Of course, only the pixel data is
		displayed by passing it to Imagedisplayer
	- Select sections and MOVE such sections. Updates both buffers
	- The buffer is a collection of nxm Pixels() wich have info about color, type of 
	- Receives the order of pixels at construction (?) This must be updatable (RGBA...)

	- At save: Creates the binary with:
		+ size of the image we created.
			* Takes the topleft and bottom right pixels and creates a rectangle to export
			* everything with no pixels is just a (0.0.0.0) that does not save, as the offset
				must be put as -1 (invalid)
		+ The selected order of pixels
		+ each pixel and its offset
	reates the image as png in RGBA
		-> It must save the orignal 
*/

/*
	Maybe we DO create a pixel object (color, offset and x,y coords)
	and have a buffer for drawing
	how to move?
		The imageDisplayer func returns the section. In such section we obtain all the Pixels()
			that are in it
		As we move them, their position in the image gets updated and they update the buffer
		These pixels receive a reference to the buffer they are to modify
*/




public class LeftPanel extends JPanel {

	private CopyBuffer copyBuffer;
	private ImageEditor imageEditor;

	public LeftPanel() {
		imageEditor = new ImageEditor();

		setUpPasteLogic();

		// 5. Layout the main panel
		this.setLayout(new BorderLayout());

		JPanel imageWrapper = new JPanel(new BorderLayout());
		imageWrapper.add(imageEditor, BorderLayout.CENTER);
		add(imageWrapper);


		JButton exportButton = new JButton("Export Image");
		exportButton.addActionListener(e -> {
			this.imageEditor.exportImage();
		});
		add(exportButton, BorderLayout.SOUTH);

	}

	public void setCopyBuffer(CopyBuffer cb) {
		this.copyBuffer = cb;
	}

	private void setUpPasteLogic() {
		this.setFocusable(true);
		this.setRequestFocusEnabled(true);

		// Add a mouse listener so clicking the image focuses it
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				requestFocusInWindow();
			}
		});
		InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap am = getActionMap();

		// Define the KeyStroke (Control + V)
		// Use getMenuShortcutKeyMaskEx() to support Command+C on Mac automatically
		KeyStroke pasteKey = KeyStroke.getKeyStroke(KeyEvent.VK_V, 
								Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

		// Map the stroke to an ID
		im.put(pasteKey, "pasteBinary");

		// Map the ID to an Action
		am.put("pasteBinary", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Ctrl+V pressed! Executing paste logic...");
				imageEditor.makePaste(copyBuffer);
			}
		});
	}

}