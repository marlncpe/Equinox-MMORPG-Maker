import java.io.IOException;

import com.pi.common.contants.SectorConstants;
import com.pi.common.contants.TileConstants;
import com.pi.common.database.GraphicsObject;
import com.pi.common.database.Sector;
import com.pi.common.database.Tile;
import com.pi.common.database.Tile.TileLayer;
import com.pi.common.database.def.EntityDef;
import com.pi.common.database.io.SectorIO;
import com.pi.server.database.Paths;
import com.pi.server.database.io.EntityDefIO;

public class EmptySector {
    public static Sector create() {
	Sector sec = new Sector();
	sec.setSectorLocation(0, 0, 0);
	for (int x = 0; x < SectorConstants.SECTOR_WIDTH; x++)
	    for (int y = 0; y < SectorConstants.SECTOR_HEIGHT; y++) {
		Tile t = new Tile();
		GraphicsObject obj = new GraphicsObject();
		obj.setGraphic(2);
		obj.setPosition(0, 32, 32, 32);
		t.setLayer(TileLayer.GROUND, obj);
		sec.setLocalTile(x, y, t);
	    }
	for (int x = 0; x < 4; x++)
	    for (int y = 0; y < 5; y++) {
		GraphicsObject obj = new GraphicsObject();
		obj.setGraphic(2);
		obj.setPosition(x * TileConstants.TILE_WIDTH, (5 + y)
			* TileConstants.TILE_HEIGHT, 32, 32);
		sec.getLocalTile(5 + x, 5 + y).setLayer(TileLayer.FRINGE1, obj);
	    }
	return sec;
    }

    public static void main(String[] args) throws IOException {
	try {
	    SectorIO.write(Paths.getSectorFile(0, 0, 0), create());
	} catch (IOException e) {
	    e.printStackTrace();
	}
	EntityDef d =new EntityDef();
	d.setGraphic(3);
	d.setPosition(0,0,128,192);
	EntityDefIO.write(Paths.getEntityDef(0),d);
    }
}
