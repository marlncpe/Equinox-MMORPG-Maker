package com.pi.client.net;

import javax.swing.JOptionPane;

import com.pi.client.Client;
import com.pi.common.database.SectorLocation;
import com.pi.common.game.Entity;
import com.pi.common.net.NetHandler;
import com.pi.common.net.packet.Packet;
import com.pi.common.net.packet.Packet0Disconnect;
import com.pi.common.net.packet.Packet10LocalEntityID;
import com.pi.common.net.packet.Packet2Alert;
import com.pi.common.net.packet.Packet2Alert.AlertType;
import com.pi.common.net.packet.Packet4Sector;
import com.pi.common.net.packet.Packet6BlankSector;
import com.pi.common.net.packet.Packet7EntityMove;
import com.pi.common.net.packet.Packet8EntityDispose;

public class NetClientHandler extends NetHandler {
    private final NetClientClient netClient;
    private final Client client;

    public NetClientHandler(NetClientClient netClient, Client client) {
	this.client = client;
	this.netClient = netClient;
    }

    @Override
    public void process(Packet p) {
    }

    public void process(Packet0Disconnect p) {
	JOptionPane.showMessageDialog(null, ((Packet0Disconnect) p).reason
		+ "\n" + ((Packet0Disconnect) p).details);
	netClient.dispose();
    }

    public void process(Packet2Alert p) {
	if (p.alertType.equals(AlertType.MAIN_MENU)
		&& client.getDisplayManager() != null
		&& client.getDisplayManager().getRenderLoop() != null
		&& client.getDisplayManager().getRenderLoop().getMainMenu() != null) {
	    if (p.message.equalsIgnoreCase("Login sucessfull")) {
		client.setInGame(true);
	    } else {
		client.getDisplayManager().getRenderLoop().getMainMenu()
			.alert(p.message);
	    }
	}
    }

    public void process(Packet4Sector p) {
	client.getWorld().getSectorManager().setSector(p.sector);
    }

    public void process(Packet6BlankSector p) {
	client.getWorld()
		.getSectorManager()
		.flagSectorAsBlack(
			new SectorLocation(p.baseX, p.baseY, p.baseZ));
    }

    public void process(Packet7EntityMove p) {
	Entity ent = client.getEntityManager().getEntity(p.entityID);
	if (ent == null)
	    ent = new Entity();
	ent.setLocation(p.moved);
	ent.setLayer(p.entityLayer);
	client.getEntityManager().saveEntity(ent);
    }

    public void process(Packet8EntityDispose p) {
	client.getEntityManager().deRegisterEntity(p.entityID);
    }

    public void process(Packet10LocalEntityID p) {
	client.getLog().info("LocalID: " + p.entityID);
	client.getEntityManager().setLocalEntityID(p.entityID);
    }
}
