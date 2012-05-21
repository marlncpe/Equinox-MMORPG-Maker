package com.pi.server.world;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Map;

import com.pi.common.database.Sector;
import com.pi.common.database.SectorLocation;
import com.pi.common.database.io.DatabaseIO;
import com.pi.common.net.ByteBufferOutputStream;
import com.pi.common.net.PacketOutputStream;
import com.pi.common.net.packet.Packet4Sector;
import com.pi.common.net.packet.Packet5SectorRequest;
import com.pi.common.net.packet.Packet6BlankSector;
import com.pi.server.Server;
import com.pi.server.ServerThread;
import com.pi.server.client.Client;
import com.pi.server.database.Paths;

public class SectorManager extends ServerThread {
	public final static int sectorExpiry = 300000; // 5 Minutes

	private LinkedList<SectorLocation> loadQueue = new LinkedList<SectorLocation>();
	private Hashtable<SectorLocation, SectorStorage> map = new Hashtable<SectorLocation, SectorStorage>();

	public SectorManager(Server server) {
		super(server);
		this.mutex = new Object();
		start();
	}

	public void requestSector(int clientID, Packet5SectorRequest req) {
		synchronized (mutex) {
			SectorStorage sec = getSectorStorage(req.baseX, req.baseY,
					req.baseZ);
			if (sec != null && (sec.data != null || sec.empty)) {
				if (sec.empty) {
					Packet6BlankSector packet = new Packet6BlankSector();
					packet.baseX = req.baseX;
					packet.baseY = req.baseY;
					packet.baseZ = req.baseZ;
					server.getClientManager().getClient(clientID)
							.getNetClient().send(packet);
				} else {
					Sector sector = sec.data;
					if (sector.getRevision() != req.revision) {

						Packet4Sector packet = new Packet4Sector();
						packet.sector = sector;

						Client cli = server.getClientManager().getClient(
								clientID);
						// cli.getNetClient().sendRaw(sec.pack);
						cli.getNetClient().send(packet);
					}
				}
			}
		}
	}

	public Sector getSector(int x, int y, int z) {
		SectorStorage ss = getSectorStorage(x, y, z);
		return ss != null ? ss.data : null;
	}

	public boolean isEmptySector(int x, int y, int z) {
		SectorStorage ss = getSectorStorage(x, y, z);
		return ss != null ? ss.empty : false;
	}

	private SectorStorage getSectorStorage(int x, int y, int z) {
		synchronized (mutex) {
			SectorLocation p = new SectorLocation(x, y, z);
			SectorStorage sS = map.get(p);
			if (sS == null || (sS.data == null && !sS.empty)) {
				if (!loadQueue.contains(p)) {// TODO FASTER
					loadQueue.addLast(p);
					mutex.notify();
				}
				return null;
			}
			return sS;
		}
	}

	public void setSector(Sector sector) {
		synchronized (mutex) {
			SectorStorage sec = map.get(sector.getSectorLocation());
			if (sec == null)
				sec = new SectorStorage();
			sec.lastUsed = System.currentTimeMillis();
			sec.data = sector;
			sec.updatePacketData();
			map.put(sector.getSectorLocation(), sec);
			server.getWorld().getSectorWriter().writeSector(sector);
		}
	}

	@Override
	public void loop() {
		synchronized (mutex) {
			if (loadQueue.size() <= 0) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
				}
			} else {
				doRequest();
				removeExpired();
			}
		}
	}

	private void removeExpired() {
		for (SectorLocation i : map.keySet()) {
			if (System.currentTimeMillis() - map.get(i).lastUsed > sectorExpiry) {
				map.remove(i);
				server.getLog().fine("Dropped sector: " + i.toString());
			}
		}
	}

	private void doRequest() {
		SectorLocation oldestSector = loadQueue.removeFirst();
		SectorStorage sX = map.get(oldestSector);
		if (sX == null || (sX.data == null && !sX.empty)) {
			if (sX == null)
				sX = new SectorStorage();
			try {
				sX.data = (Sector) DatabaseIO.read(
						Paths.getSectorFile(oldestSector), Sector.class);
				sX.updatePacketData();
				sX.empty = false;
				sX.lastUsed = System.currentTimeMillis();
				server.getLog().finer(
						"Loaded sector " + oldestSector.toString());
				map.put(oldestSector, sX);
			} catch (FileNotFoundException e) {
				sX.data = null;
				sX.empty = true;
				sX.lastUsed = System.currentTimeMillis();
				map.put(oldestSector, sX);
				server.getLog().finest(
						"Flagged as empty: " + oldestSector.toString());
			} catch (IOException e) {
				server.getLog().printStackTrace(e);
			}
		}
	}

	public static class SectorStorage {
		public long lastUsed;
		public Sector data;
		public byte[] pack;
		public boolean empty = false;

		public void updatePacketData() {
			Packet4Sector p = new Packet4Sector();
			try {
				ByteBufferOutputStream bO = new ByteBufferOutputStream(
						p.getPacketLength());
				PacketOutputStream pO = new PacketOutputStream(bO);
				p.writePacket(pO);
				pO.close();
				pack = bO.getByteBuffer().array();
			} catch (Exception e) {
				pack = null;
			}
		}
	}

	public static class ClientSectorRequest {
		public int clientId;
		public int revision;
		public int baseX, baseY, baseZ;

		public ClientSectorRequest(int client, Packet5SectorRequest req) {
			this.clientId = client;
			this.revision = req.revision;
			this.baseX = req.baseX;
			this.baseY = req.baseY;
			this.baseZ = req.baseZ;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ClientSectorRequest) {
				ClientSectorRequest req = (ClientSectorRequest) o;
				return this.clientId == req.clientId && this.baseX == req.baseX
						&& this.baseY == req.baseY && this.baseZ == req.baseZ;
			}
			return false;
		}
	}

	public Map<SectorLocation, SectorStorage> loadedMap() {
		synchronized (mutex) {
			return Collections.unmodifiableMap(map);
		}
	}
}
