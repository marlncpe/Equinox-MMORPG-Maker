package com.pi.common;

import java.io.*;

import javax.swing.*;

public class PILogViewer extends JFrame {
    private static final long serialVersionUID = 1L;
    public PrintStream logOut;

    public PILogViewer(String string) {
	super(string);
	setSize(500, 500);
	setLocation(250, 50);
	setVisible(true);
	final JScrollPane tSPane = new JScrollPane();
	final JTextPane tPane = new JTextPane();
	tSPane.setSize(500, 450);
	tSPane.setLocation(0, 0);
	add(tSPane);
	tPane.setSize(500, 450);
	tPane.setLocation(0, 0);
	tSPane.add(tPane);
	logOut = new PrintStream(new OutputStream() {

	    @Override
	    public void write(int arg0) throws IOException {
		tPane.setText(tPane.getText() + ((char) arg0));
	    }

	    @Override
	    public void write(byte[] byts, int off, int len) {
		tPane.setText(tPane.getText() + new String(byts, off, len));
	    }
	});
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
