package com.IMExtraction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.Arrays;
import javax.swing.JFileChooser;
import java.io.File;


/*
	- Opens the file and passes to rightpanel
	- Input offset and passes it to rightpanel
	- Creates insrances of left and right panel
	- Doesn't do much more
	- Selection of codec for both panels
*/




public class ProgramGUI extends JFrame {

	private byte[] fileContents = new byte[0];
	private RightPanel rightPanel;
	private LeftPanel leftPanel;
	private CopyBuffer copyBuffer;

	public ProgramGUI() {

		copyBuffer = new CopyBuffer();

		//Create and set up the window.
		setTitle("Image Extractor");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(1000, 600);
		setLayout(new BorderLayout());

		// 1. TOP NAVIGATION BAR (RED)
		JPanel topNav = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topNav.setBackground(Color.RED);
		topNav.setPreferredSize(new Dimension(getWidth(), 60));
		
		JButton fileButton = new JButton("New File");
		fileButton.addActionListener(e -> {
			String path = "";
			path = getFileToExplore();
			if (path != "") {
				readFile(path);
				display();
			}
			
		});
		
		topNav.add(fileButton);
		add(topNav, BorderLayout.NORTH);

		// 2. MAIN CONTENT AREA (SPLIT IN TWO)
		JPanel centerPanel = new JPanel(new GridLayout(1, 2, 20, 0)); // 20px gap
		centerPanel.setBackground(Color.BLACK);
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));


		// RIGHT PANEL
		rightPanel = new RightPanel();
		rightPanel.setCopyBuffer(this.copyBuffer);

		// LEFT PANEL
		leftPanel = new LeftPanel();
		leftPanel.setCopyBuffer(this.copyBuffer);
		
		centerPanel.add(leftPanel);
		centerPanel.add(rightPanel);

		add(centerPanel, BorderLayout.CENTER);


		//Display the window.
		pack();


	}

	private String getFileToExplore() {
		JFileChooser fileChooser = new JFileChooser();
		File workingDirectory = new File(System.getProperty("user.dir"));
		fileChooser.setCurrentDirectory(workingDirectory);
		fileChooser.setDialogTitle("Select a file to analyze");
		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();
			return selectedFile.getAbsolutePath();
		}
		return "";
	}

	public void readFile(String pathString) {
		Path path = Paths.get(pathString);
		try {
			this.fileContents = Files.readAllBytes(path); // Read all bytes
		} catch (IOException e) {
			this.fileContents = new byte[0];
		}
		rightPanel.updateFile(this.fileContents);
		//leftPanel.reset()
	}

	public void display() {
		rightPanel.setUp();
		//System.out.println("We are here");
	}

	private static JPanel createInterchangeablePanel() {
		JPanel p = new JPanel();
		p.setBackground(new Color(128, 142, 155)); // Gray color
		p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		return p;
	}
}