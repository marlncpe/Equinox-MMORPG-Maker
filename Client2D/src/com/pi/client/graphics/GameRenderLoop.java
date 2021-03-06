package com.pi.client.graphics;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Iterator;

import com.pi.client.Client;
import com.pi.client.constants.Constants;
import com.pi.client.entity.ClientEntity;
import com.pi.common.constants.TileConstants;
import com.pi.common.database.GraphicsObject;
import com.pi.common.database.Location;
import com.pi.common.database.def.ItemDef;
import com.pi.common.database.def.entity.EntityDef;
import com.pi.common.database.def.entity.HealthDefComponent;
import com.pi.common.database.world.Sector;
import com.pi.common.database.world.Tile;
import com.pi.common.database.world.TileLayer;
import com.pi.common.game.entity.Entity;
import com.pi.common.game.entity.comp.HealthComponent;
import com.pi.common.game.entity.comp.ItemLinkageComponent;
import com.pi.common.util.Filter;
import com.pi.common.util.FilteredIterator;
import com.pi.graphics.device.IGraphics;
import com.pi.graphics.device.Renderable;

/**
 * The render loop for the actual game.
 * 
 * @author Westin
 * 
 */
public class GameRenderLoop implements Renderable {
	/**
	 * The client instance this render loop is bound to.
	 */
	private final Client client;
	/**
	 * A temporary field to allow multiple render methods to work together.
	 */
	private IGraphics g;
	/**
	 * The current tile view window.
	 */
	private Rectangle tileView = new Rectangle();
	/**
	 * The x coordinate the current tile view was calculated at.
	 */
	private int currentTileViewX = -1;
	/**
	 * The z coordinate the current tile view was calculated at.
	 */
	private int currentTileViewZ = -1;
	/**
	 * The x offset the current rendering should be based on.
	 */
	private int currentTileViewXOff = -1;
	/**
	 * The z offset the current rendering should be based on.
	 */
	private int currentTileViewZOff = -1;

	/**
	 * The window width the current tile view was calculated at.
	 */
	private int currentTileViewWidth = -1;
	/**
	 * The window height the current tile view was calculated at.
	 */
	private int currentTileViewHeight = -1;
	/**
	 * The render distance for entities.
	 */
	private int renderDistance = 0;

	/**
	 * Creates the game render loop for a specified client.
	 * 
	 * @param sClient
	 *            the bound client
	 */
	public GameRenderLoop(final Client sClient) {
		this.client = sClient;
	}

	@Override
	public final void render(final IGraphics iG) {
		this.g = iG;
		if (client.getWorld() != null) {
			Sector sec = client.getWorld().getSector(0, 0, 0);
			if (client.getEntityManager().getLocalEntity() != null) {
				final Entity myEntity = client.getEntityManager()
						.getLocalEntity().getWrappedEntity();
				if (myEntity != null) {
					if (client.getEntityManager().getLocalEntity() != null
							&& sec != null) {
						getTileView();
						for (int tI = 0; tI < TileLayer.MAX_VALUE.ordinal(); tI++) {
							final TileLayer t = TileLayer.values()[tI];
							renderLayer(t);
							Iterator<ClientEntity> entities = new FilteredIterator<ClientEntity>(
									client.getEntityManager().getEntities(),
									new Filter<ClientEntity>() {
										@Override
										public boolean accept(
												final ClientEntity e) {
											return Location.dist(myEntity,
													e.getWrappedEntity()) <= renderDistance
													&& e.getWrappedEntity()
															.getLayer() == t;
										}
									});
							while (entities.hasNext()) {
								ClientEntity ent = entities.next();
								if (ent.getWrappedEntity().getLayer() == t) {
									renderEntity(ent);
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Renders the specified entity.
	 * 
	 * @param ent
	 *            the entity
	 */
	private void renderEntity(final ClientEntity ent) {
		ent.processMovement();
		EntityDef def = client.getDefs().getEntityLoader()
				.getDef(ent.getWrappedEntity().getEntityDef());
		if (def != null) {
			float frameWidth = def.getPositionWidth()
					/ def.getHorizontalFrames();
			float frameHeight = def.getPositionHeight() / 4;
			Point p = locationToScreen(ent.getWrappedEntity());
			p.x += ent.getXOff();
			p.y += ent.getZOff() - frameHeight + TileConstants.TILE_HEIGHT;
			g.drawImage(
					def.getGraphic(),
					p.x,
					p.y,
					(int) def.getPositionX()
							+ (int) ((int) (ent.getMovementPercent() * def
									.getHorizontalFrames()) * frameWidth),
					(int) (def.getPositionY() + (frameHeight * ent
							.getWrappedEntity().getDir().ordinal())),
					(int) frameWidth, (int) frameHeight);

			HealthComponent hC = (HealthComponent) ent.getWrappedEntity()
					.getComponent(HealthComponent.class);
			HealthDefComponent lDC = (HealthDefComponent) def
					.getComponent(HealthDefComponent.class);
			if (hC != null && lDC != null && lDC.getMaximumHealth() > 0) {
				float scalarHealth = hC.getHealth()
						/ ((float) lDC.getMaximumHealth());
				if (scalarHealth != 1.0f) {
					int goodHealthWidth = Math
							.round(((float) Constants.HEALTH_BAR_WIDTH)
									* scalarHealth);
					int barY = p.y - 5 - Constants.HEALTH_BAR_HEIGHT;
					int barX = p.x
							- ((int) (Constants.HEALTH_BAR_WIDTH - frameWidth) / 2);
					g.setColor(Color.GREEN);
					g.fillRect(barX, barY, goodHealthWidth,
							Constants.HEALTH_BAR_HEIGHT);
					g.setColor(Color.RED);
					g.fillRect(barX + goodHealthWidth, barY,
							Constants.HEALTH_BAR_WIDTH - goodHealthWidth,
							Constants.HEALTH_BAR_HEIGHT);
				}
			}
		} else if (ent.getWrappedEntity().getComponent(
				ItemLinkageComponent.class) != null) {
			ItemLinkageComponent iLC = (ItemLinkageComponent) ent
					.getWrappedEntity()
					.getComponent(ItemLinkageComponent.class);
			ItemDef iDef = client.getDefs().getItemLoader()
					.getDef(iLC.getItemID());
			if (iDef != null) {
				Point p = locationToScreen(ent.getWrappedEntity());
				p.x += ent.getXOff();
				p.y += ent.getZOff();
				g.drawImage(iDef.getGraphic(), p.x, p.y,
						(int) iDef.getPositionX(), (int) iDef.getPositionY(),
						(int) iDef.getPositionWidth(),
						(int) iDef.getPositionHeight());
			}
		}
	}

	/**
	 * Renders the specified layer using the current tile view.
	 * 
	 * @param l
	 *            the tile layer to render
	 */
	private void renderLayer(final TileLayer l) {
		Location tile = new Location(0, 0, 0);
		for (int x = tileView.x; x <= tileView.x + tileView.width; x++) {
			for (int z = tileView.y; z <= tileView.y + tileView.height; z++) {
				tile.setLocation(x, 0, z);
				Sector s = client.getWorld().getSector(tile.getSectorX(), 0,
						tile.getSectorZ());
				Point screen = locationToScreen(tile);
				if (s != null && screen != null) {
					Tile lTile = s.getGlobalTile(x, z);
					if (lTile != null) {
						GraphicsObject gO = lTile.getLayer(l);
						if (gO != null) {
							g.drawImage(gO, screen.x, screen.y);
						}
					}
				}
			}
		}
	}

	/**
	 * Converts the location in the world to a point on the screen.
	 * 
	 * @param t
	 *            the world location
	 * @return the screen location, or <code>null</code> if unable to calculate.
	 */
	private Point locationToScreen(final Location t) {
		float xT = t.x - currentTileViewX;
		float zT = t.z - currentTileViewZ;
		xT *= TileConstants.TILE_WIDTH;
		zT *= TileConstants.TILE_HEIGHT;
		xT += g.getClip().getCenterX();
		zT += g.getClip().getCenterY();
		return new Point((int) xT - currentTileViewXOff, (int) zT
				- currentTileViewZOff);
	}

	/**
	 * Returns the current tile view and recalculates it if the local entity has
	 * moved since the last calculation.
	 * 
	 * @return the current tile view
	 */
	private Rectangle getTileView() {
		Rectangle clip = g.getClip();
		ClientEntity ent = client.getEntityManager().getLocalEntity();
		if (ent != null) {
			currentTileViewXOff = ent.getXOff();
			currentTileViewZOff = ent.getZOff();
			if (currentTileViewX != ent.getWrappedEntity().x
					|| currentTileViewZ != ent.getWrappedEntity().z
					|| currentTileViewWidth != client.getApplet().getWidth()
					|| currentTileViewHeight != client.getApplet().getHeight()) {
				int tileWidth = (int) Math.ceil(clip.getWidth()
						/ TileConstants.TILE_WIDTH / 2 + 1);
				int tileHeight = (int) Math.ceil(clip.getHeight()
						/ TileConstants.TILE_HEIGHT / 2 + 1);
				renderDistance = tileWidth + tileHeight;
				tileView.setBounds(ent.getWrappedEntity().x - tileWidth,
						ent.getWrappedEntity().z - tileHeight, tileWidth * 2,
						tileHeight * 2);
				currentTileViewX = ent.getWrappedEntity().x;
				currentTileViewZ = ent.getWrappedEntity().z;
				currentTileViewWidth = client.getApplet().getWidth();
				currentTileViewHeight = client.getApplet().getHeight();
				return tileView;
			}
		}
		return tileView;
	}
}
