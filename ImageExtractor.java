import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/*
	- Opens the file and passes to rightpanel
	- Input offset and passes it to rightpanel
	- Creates insrances of left and right panel
	- Doesn't do much more
	- Selection of codec for both panels
*/



public class ImageExtractor {

	public static void main(String[] args) {
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		SwingUtilities.invokeLater(() -> {
			ProgramGUI pg = new ProgramGUI();
			pg.setVisible(true);
		});
	}

}
