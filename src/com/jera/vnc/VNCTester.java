package com.jera.vnc;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

import org.python.util.PythonInterpreter;

/**
 * Created by IntelliJ IDEA.
 * User: jbrewer
 * Date: Sep 13, 2003
 * Time: 4:40:34 PM
 * To change this template use Options | File Templates.
 */
public class VNCTester {
	private static PythonInterpreter ourInterpreter;
	private static JTextPane textPane = new JTextPane();
	private static VNC vnc;

	public static void showScriptFrame(VNC theVNC) {
		vnc = theVNC;

		textPane.setText("sut.clickAt(187, 19)\n\nsut.sleep(35000)\n\nsut.clickAt(319, 58)\nsut.type(\"This is a test.\")\n");

		JFrame scriptFrame = new JFrame("Script Frame");
		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		scriptFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JButton(new AbstractAction("Run") {
			public void actionPerformed(ActionEvent e) {
				Thread thread = new Thread(new Runnable() {
					public void run() {
						runScript();
					}
				});
				thread.start();
			}
		}));
		scriptFrame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		scriptFrame.pack();
		scriptFrame.show();

		Thread initializationThread = new Thread(new Runnable() {
			public void run() {
				initialize();
			}
		});
		initializationThread.start();
	}

	private static void initialize() {
		ourInterpreter = new PythonInterpreter();
		ourInterpreter.set("sut", vnc);
	}

	public static void runScript() {
		ourInterpreter.exec(textPane.getText());
	}
}
