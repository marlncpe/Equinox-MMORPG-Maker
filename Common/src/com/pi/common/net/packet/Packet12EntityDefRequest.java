package com.pi.common.net.packet;

import java.io.IOException;

import com.pi.common.net.PacketInputStream;
import com.pi.common.net.PacketOutputStream;

public class Packet12EntityDefRequest extends Packet {
	public int defID;

	@Override
	public void writeData(PacketOutputStream pOut) throws IOException {
		pOut.writeInt(defID);
	}

	@Override
	public void readData(PacketInputStream pIn) throws IOException {
		defID = pIn.readInt();
	}

	@Override
	public int getID() {
		return 12;
	}

	@Override
	public int getLength() {
		return 4;
	}
}
