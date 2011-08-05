package com.pi.client.world;

import java.awt.Point;
import java.io.File;
import java.io.IOException;
import java.util.*;

import com.pi.client.Client;
import com.pi.client.database.Paths;
import com.pi.common.database.Sector;
import com.pi.common.database.io.SectorIO;
import com.pi.common.net.packet.Packet5SectorRequest;

public class SectorManager extends Thread {
    public final static int sectorExpiry = 60000; // 1 Minute
    public final static int serverRequestExpiry = 30000; // 30 seconds
    private List<Point> blankSectors = new ArrayList<Point>();
    private final Client client;
    private boolean running = true;

    private Map<Point, Long> loadQueue = new HashMap<Point, Long>();
    private Map<Point, SectorStorage> map = new HashMap<Point, SectorStorage>();
    private Map<Point, Long> sentRequests = new HashMap<Point, Long>();

    public SectorManager(Client client) {
	super("SectorManager");
	this.client = client;
	start();
    }

    public synchronized Sector getSector(int x, int y) {
	Point p = new Point(x, y);
	SectorStorage sS = map.get(p);
	if ((sS == null || sS.data == null) && !blankSectors.contains(p)) {
	    loadQueue.put(p, System.currentTimeMillis());
	    return null;
	}
	return sS.data;
    }

    public synchronized void setSector(Sector sector) {
	SectorStorage sec = map.get(sector.getSectorLocation());
	if (sec == null)
	    sec = new SectorStorage();
	sec.lastUsed = System.currentTimeMillis();
	sec.data = sector;
	map.put(sector.getSectorLocation(), sec);
	client.getWorld().getSectorWriter().writeSector(sector);
    }

    @Override
    public void run() {
	while (running) {
	    doRequest();
	    removeExpired();
	}
	client.getLog().fine("Killing Sector Manager Thread");
    }

    private synchronized void removeExpired() {
	for (Point i : map.keySet()) {
	    if (System.currentTimeMillis() - map.get(i).lastUsed > sectorExpiry) {
		map.remove(i);
	    }
	}
    }

    private synchronized void doRequest() {
	long oldestTime = Long.MAX_VALUE;
	Point oldestSector = null;
	for (Point i : loadQueue.keySet()) {
	    long requestTime = loadQueue.get(i);
	    if (System.currentTimeMillis() - requestTime > sectorExpiry) {
		loadQueue.remove(i);
	    } else {
		if (oldestTime > requestTime) {
		    oldestTime = requestTime;
		    oldestSector = i;
		}
	    }
	}
	if (oldestSector != null) {
	    loadQueue.remove(oldestSector);
	    SectorStorage sX = new SectorStorage();
	    File f = Paths.getSectorFile(oldestSector.x, oldestSector.y);
	    int revision = -1;
	    if (f.exists()) {
		try {
		    sX.data = SectorIO.read(f);
		    revision = sX.data.getRevision();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	    if (client.getNetwork() != null
		    && client.getNetwork().getSocket() != null
		    && client.getNetwork().getSocket().isConnected()) {
		if (sentRequests.get(oldestSector) == null
			|| sentRequests.get(oldestSector).longValue()
				+ serverRequestExpiry < System
				    .currentTimeMillis()) {
		    sentRequests.put(oldestSector, System.currentTimeMillis());
		    Packet5SectorRequest pack = new Packet5SectorRequest();
		    pack.baseX = oldestSector.x;
		    pack.baseY = oldestSector.y;
		    pack.revision = revision;
		    client.getNetwork().send(pack);
		    sX.lastUsed = System.currentTimeMillis();
		    map.put(oldestSector, sX);
		}
	    }
	}
    }

    public void dispose() {
	running = false;
	try {
	    join();
	} catch (InterruptedException e) {
	    e.printStackTrace();
	    System.exit(0);
	}
    }

    public void flagSectorAsBlack(Point p) {
	if (!blankSectors.contains(p))
	    blankSectors.add(p);
    }

    private static class SectorStorage {
	public long lastUsed;
	public Sector data;
    }
}
