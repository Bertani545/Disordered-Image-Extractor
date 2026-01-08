package com.IMExtraction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;

/*
	- Hereda de ImageDisplayer
	- Manages the file
	- 1 buffer -> It's updated with the toolbar (size, section of the file)
	- Receives the order of pixels at construction (?) This must be updatable (RGBA...)

	- Select sections and COPY such sections.
		+ Creates 2 buffers, with pixel info and offset (saves it in clipboard(?))
		+ it only pastes to the other one
		+ Maybe jut add a button that says "SEND" to move the section to the other panel
			-> This creates the Pixel() classes
	


*/

public class RightPanel extends JPanel {

	private ImageDisplayer imageDisplayer;
	private CopyBuffer copyBuffer;

	public RightPanel() {
		imageDisplayer = new ImageDisplayer();

		setUpCopyLogic();

		JPanel topNav = createTopOptions();

		// 5. Layout the main panel
		this.setLayout(new BorderLayout());
		add(topNav, BorderLayout.NORTH);

		JPanel imageWrapper = new JPanel(new BorderLayout());
		imageWrapper.add(imageDisplayer, BorderLayout.CENTER);
		add(imageWrapper);

	}

	public void setCopyBuffer(CopyBuffer cb) {
		this.copyBuffer = cb;
	}

	private void setUpCopyLogic() {
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

		// Define the KeyStroke (Control + C)
		// Use getMenuShortcutKeyMaskEx() to support Command+C on Mac automatically
		KeyStroke copyKey = KeyStroke.getKeyStroke(KeyEvent.VK_C, 
								Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

		// Map the stroke to an ID
		im.put(copyKey, "copyBinary");

		// Map the ID to an Action
		am.put("copyBinary", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("Ctrl+C pressed! Executing copy logic...");
				imageDisplayer.makeCopy(copyBuffer);
			}
		});
	}

	private JPanel createTopOptions() {
		JPanel topNav = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topNav.setPreferredSize(new Dimension(getWidth(), 60));


		JButton retreatLineBtn = new JButton("\u2770\u2770");
		retreatLineBtn.addActionListener(e -> {
			this.imageDisplayer.updateLine(-32);
		});
		topNav.add(retreatLineBtn);
		retreatLineBtn = new JButton("\u2770");
		retreatLineBtn.addActionListener(e -> {
			this.imageDisplayer.updateLine(-1);
		});
		topNav.add(retreatLineBtn);


		JButton advanceLineBtn = new JButton("\u2771");
		advanceLineBtn.addActionListener(e -> {
			this.imageDisplayer.updateLine(1);
		});
		topNav.add(advanceLineBtn);
		advanceLineBtn = new JButton("\u2771\u2771");
		advanceLineBtn.addActionListener(e -> {
			this.imageDisplayer.updateLine(32);
		});
		topNav.add(advanceLineBtn);
		
		
		JButton retreatPlaneBtn = new JButton("\u2190");
		retreatPlaneBtn.addActionListener(e -> {
			this.imageDisplayer.updatePlane(-1);
		});
		topNav.add(retreatPlaneBtn);
		JButton advancePlaneBtn = new JButton("\u2794");
		advancePlaneBtn.addActionListener(e -> {
			this.imageDisplayer.updatePlane(1);
		});
		topNav.add(advancePlaneBtn);
		

		JButton decreaseWidthBtn = new JButton("-W");
		decreaseWidthBtn.addActionListener(e -> {
			this.imageDisplayer.updateWidth(-1);
		});
		topNav.add(decreaseWidthBtn);
		JButton addWidthBtn = new JButton("+W");
		addWidthBtn.addActionListener(e -> {
			this.imageDisplayer.updateWidth(1);
		});
		topNav.add(addWidthBtn);
		

		JButton decreaseHeightBtn = new JButton("-H");
		decreaseHeightBtn.addActionListener(e -> {
			this.imageDisplayer.updateHeight(-32);
		});
		topNav.add(decreaseHeightBtn);
		JButton addHeightBtn = new JButton("+H");
		addHeightBtn.addActionListener(e -> {
			this.imageDisplayer.updateHeight(32);
		});
		topNav.add(addHeightBtn);
		

		JButton decreasePixelBtn = new JButton("-P");
		decreasePixelBtn.addActionListener(e -> {
			this.imageDisplayer.updateDataStartingPoint(-4);
		});
		topNav.add(decreasePixelBtn);
		JButton advancePixelBtn = new JButton("+P");
		advancePixelBtn.addActionListener(e -> {
			this.imageDisplayer.updateDataStartingPoint(4);
		});
		topNav.add(advancePixelBtn);
		

		JButton decreaseByteBtn = new JButton("-B");
		decreaseByteBtn.addActionListener(e -> {
			this.imageDisplayer.updateDataStartingPoint(-1);
		});
		topNav.add(decreaseByteBtn);
		JButton advanceByteBtn = new JButton("+B");
		advanceByteBtn.addActionListener(e -> {
			this.imageDisplayer.updateDataStartingPoint(1);
		});
		topNav.add(advanceByteBtn);
		

		return topNav;
	}

	public void setUp() {
		// Must be update size first
		this.imageDisplayer.setInitialSize();
		this.imageDisplayer.updateOffset(0);
		this.revalidate();
	}


	public void updateFile(byte[] fileBytes) {
		this.imageDisplayer.updateFile(fileBytes);
	}

	public void display() {
		this.imageDisplayer.display();
	}
}