/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.physics;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.content.Sprite;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@LunoClass
public class PhysicsCollisionListener implements ContactListener {

	public static final String COLLISION_MESSAGE_ESCAPE_CHAR = "\t";
	public static final String COLLISION_MESSAGE_CONNECTOR = "<" + COLLISION_MESSAGE_ESCAPE_CHAR
			+ "-" + COLLISION_MESSAGE_ESCAPE_CHAR + ">";

	private PhysicsWorld physicsWorld;

	public PhysicsCollisionListener(PhysicsWorld physicsWorld) {
		this.physicsWorld = physicsWorld;
	}

	private Map<CollidingSprites, PhysicalCollision> collidingSpritesToCollisionMap = new HashMap<>();

	/**
	 * Deferred event queue for collision events that must not fire during Box2D's world step.
	 * Events are queued in beginContact/endContact and flushed after world.step() completes.
	 */
	private final Queue<Runnable> deferredEvents = new ConcurrentLinkedQueue<>();

	private void registerContact(Sprite sprite1, Sprite sprite2) {
		CollidingSprites collidingSprites = new CollidingSprites(sprite1, sprite2);
		if (!collidingSpritesToCollisionMap.containsKey(collidingSprites)) {
			collidingSpritesToCollisionMap.put(collidingSprites, new PhysicalCollision(collidingSprites));
		}
		collidingSpritesToCollisionMap.get(collidingSprites).increaseContactCounter();
	}

	private void unregisterContact(Sprite sprite1, Sprite sprite2) {
		CollidingSprites collidingSprites = new CollidingSprites(sprite1, sprite2);
		if (collidingSpritesToCollisionMap.containsKey(collidingSprites)) {
			PhysicalCollision physicalCollision = collidingSpritesToCollisionMap.get(collidingSprites);
			physicalCollision.decreaseContactCounter();

			if (physicalCollision.getContactCounter() == 0) {
				physicalCollision.sendBounceOffEvents();
				collidingSpritesToCollisionMap.remove(collidingSprites);
			}
		}
	}

	@Override
	public void beginContact(Contact contact) {
		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();

		if (a.getUserData() instanceof Sprite && b.getUserData() instanceof PhysicsBoundaryBox.BoundaryBoxIdentifier) {
			Sprite sprite = (Sprite) a.getUserData();
			PhysicsBoundaryBox.BoundaryBoxIdentifier boxId = (PhysicsBoundaryBox.BoundaryBoxIdentifier) b.getUserData();
			deferredEvents.add(() -> physicsWorld.bouncedOnEdge(sprite, boxId));
		} else if (a.getUserData() instanceof PhysicsBoundaryBox.BoundaryBoxIdentifier && (b.getUserData() instanceof Sprite)) {
			Sprite sprite = (Sprite) b.getUserData();
			PhysicsBoundaryBox.BoundaryBoxIdentifier boxId = (PhysicsBoundaryBox.BoundaryBoxIdentifier) a.getUserData();
			deferredEvents.add(() -> physicsWorld.bouncedOnEdge(sprite, boxId));
		} else if (a.getUserData() instanceof Sprite && b.getUserData() instanceof Sprite) {
			Sprite sprite1 = (Sprite) a.getUserData();
			Sprite sprite2 = (Sprite) b.getUserData();
			registerContact(sprite1, sprite2);
		}
	}

	@Override
	public void endContact(Contact contact) {
		Body a = contact.getFixtureA().getBody();
		Body b = contact.getFixtureB().getBody();

		if (a.getUserData() instanceof Sprite && b.getUserData() instanceof Sprite) {
			Sprite sprite1 = (Sprite) a.getUserData();
			Sprite sprite2 = (Sprite) b.getUserData();
			deferredEvents.add(() -> unregisterContact(sprite1, sprite2));
		}
	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	/** Processes all deferred collision events accumulated during the last world.step(). */
	public void flushDeferredEvents() {
		Runnable event;
		while ((event = deferredEvents.poll()) != null) {
			event.run();
		}
	}
}
