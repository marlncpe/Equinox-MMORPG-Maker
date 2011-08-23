package com.pi.launcher;

import java.applet.Applet;

import com.pi.common.Disposable;

public class ClientLoader {
    private final static String appletName = "com.pi.client.Client";
    private final static String frameName = "com.pi.client.clientviewer.ClientViewerFrame";

    public static Disposable loadClientApplet(Applet bind) {
	ClientClassLoader cLoader = new ClientClassLoader();
	try {
	    bind.removeAll();
	    return (Disposable) cLoader.loadClass(appletName)
		    .getConstructor(Applet.class).newInstance(bind);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return null;
    }

    public static void runClientFrame() {
	ClientClassLoader cLoader = new ClientClassLoader();
	Object[] args = new Object[] { new String[] {} };
	try {
	    cLoader.loadClass(frameName).getMethod("main", String[].class)
		    .invoke(null, args);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
