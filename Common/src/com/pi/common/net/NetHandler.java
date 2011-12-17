package com.pi.common.net;

import java.lang.reflect.Method;

import com.pi.common.net.packet.Packet;

public abstract class NetHandler {
    protected abstract void process(Packet p);

    public final void processPacket(Packet p) {
	try {
	    Method m = getClass().getMethod("process", p.getClass());
	    m.invoke(this, p);
	    return;
	} catch (Exception e) {
	    System.err.println("No custom method for packet: " + p.getName());
	}
	process(p);
    }
}
