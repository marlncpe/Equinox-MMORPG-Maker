package com.pi.server.logic.entity;

import com.pi.common.constants.Direction;
import com.pi.server.Server;
import com.pi.server.entity.ServerEntity;

/**
 * Entity logic class that randomly moves.
 * 
 * @author Westin
 * 
 */
public class RandomEntityLogic extends EntityLogic {
	/**
	 * The random movement chance. Higher has a greater chance. 1 is constantly
	 * moving.
	 */
	private static final float RANDOM_MOVEMENT_CHANCE = .1f;

	/**
	 * The minimum time in milliseconds between movement checks.
	 */
	private static final long MOVEMENT_CHECK_TIME = 500;

	/**
	 * The last movement check.
	 */
	private long lastMovementCheck = -1;

	/**
	 * Create a random entity logic instance for the given entity wrapper and
	 * server.
	 * 
	 * @param sEntity the entity wrapper
	 * @param sServer the server
	 */
	public RandomEntityLogic(final ServerEntity sEntity,
			final Server sServer) {
		super(sEntity, sServer);
	}

	@Override
	public void doLogic() {
		if (getServerEntity().isStillMoving()
				|| lastMovementCheck + MOVEMENT_CHECK_TIME > System
						.currentTimeMillis()) {
			return;
		}
		lastMovementCheck = System.currentTimeMillis();
		if (getRandom().nextFloat() < RANDOM_MOVEMENT_CHANCE) {
			// It would appear that we should move randomly.
			Direction d = null;
			for (int i = 0; i < 3
					&& (d == null || getEntity().getDir()
							.getInverse() == d); i++) {
				// The inverse thing is to try to not just have entities moving
				// back and forth. Three tries.
				d =
						Direction.values()[getRandom().nextInt(
								Direction.values().length)];
			}
			tryMove(d);
		}
	}
}
