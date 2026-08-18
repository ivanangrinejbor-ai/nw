/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.physics;

import com.badlogic.gdx.graphics.g2d.Batch;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.physics.PhysicsLook;
import org.catrobat.catroid.physics.PhysicsObject;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class PhysicsRagdollTest {

	private static final float DELTA = 0.6f;

	private PhysicsWorld physicsWorld;
	private Sprite sprite;
	private PhysicsLook look;
	private PhysicsObject physicsObject;
	private Batch batch;

	@Before
	public void setUp() throws Exception {
		Project project = new Project(MockUtil.mockContextForProject(), "RagdollProject");
		Scene scene = project.getDefaultScene();
		sprite = new Sprite("RagdollSprite");
		scene.addSprite(sprite);
		ProjectManager.getInstance().setCurrentProject(project);
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);
		ProjectManager.getInstance().setCurrentlyEditedScene(scene);

		physicsWorld = new PhysicsWorld(1280, 720, project);
		look = new PhysicsLook(sprite, physicsWorld);
		sprite.look = look;
		physicsObject = physicsWorld.getPhysicsObject(sprite);
		batch = Mockito.mock(Batch.class);
	}

	private void makeDynamic() {
		physicsObject.setType(PhysicsObject.Type.DYNAMIC);
	}

	private void step(float seconds) {
		for (int i = 0; i < Math.max(1, (int) (seconds / (1.0f / 60.0f))); i++) {
			physicsWorld.step(1.0f / 60.0f);
		}
	}

	@Test
	public void testBodyExistsAndDefaultsToNone() {
		assertTrue("Physics body must exist for ragdoll sprite", physicsObject != null);
		assertEquals("Default type must be NONE (static, no gravity)", PhysicsObject.Type.NONE,
				physicsObject.getType());
	}

	@Test
	public void testRagdollOffSettersMoveBodyDirectly() {
		makeDynamic();
		look.setPosition(100, 100);
		assertTrue(Math.abs(physicsObject.getX() - 100) < DELTA);
		assertTrue(Math.abs(physicsObject.getY() - 100) < DELTA);
	}

	@Test
	public void testRagdollMode1SettersDoNotMoveBody() {
		makeDynamic();
		look.setPosition(100, 100);
		sprite.ragdollMode = 1;
		look.setPosition(300, 300);
		assertTrue("Ragdoll setters must NOT write into the body",
				Math.abs(physicsObject.getX() - 100) < DELTA);
		assertTrue(Math.abs(physicsObject.getY() - 100) < DELTA);
	}

	@Test
	public void testRagdollMode1BodyFallsWithGravity() {
		makeDynamic();
		look.setPosition(0, 500);
		sprite.ragdollMode = 1;
		step(0.5f);
		assertTrue("Ragdoll body must keep falling with gravity, y=" + physicsObject.getY(),
				physicsObject.getY() < 490f);
	}

	@Test
	public void testRagdollMode1ActorFollowsBodyPosition() {
		makeDynamic();
		look.setPosition(0, 500);
		sprite.ragdollMode = 1;
		step(0.5f);
		look.draw(batch, 1.0f);
		assertTrue("Actor must be drawn at the body position",
				Math.abs(look.getX() - physicsObject.getX()) < DELTA);
		assertTrue(Math.abs(look.getY() - physicsObject.getY()) < DELTA);
	}

	@Test
	public void testRagdollFollowBodyIsDraggedTowardScriptTarget() {
		makeDynamic();
		look.setPosition(0, 0);
		sprite.ragdollMode = 2;
		look.setPosition(300, 0);

		float firstX = physicsObject.getX();
		for (int i = 0; i < 20; i++) {
			look.draw(batch, 1.0f);
			step(0.05f);
		}
		assertTrue("Body must be dragged toward the target, firstX=" + firstX + " now=" + physicsObject.getX(),
				physicsObject.getX() > 200f && physicsObject.getX() < 315f);
	}

	@Test
	public void testRagdollFollowNewTargetChangesDirection() {
		makeDynamic();
		look.setPosition(0, 0);
		sprite.ragdollMode = 2;
		look.setPosition(300, 0);
		for (int i = 0; i < 10; i++) {
			look.draw(batch, 1.0f);
			step(0.05f);
		}
		float xAfterRight = physicsObject.getX();
		assertTrue(xAfterRight > 100f);

		look.setPosition(-300, 0);
		for (int i = 0; i < 20; i++) {
			look.draw(batch, 1.0f);
			step(0.05f);
		}
		assertTrue("Body must turn around when the script target moves, now=" + physicsObject.getX(),
				physicsObject.getX() < -150f);
	}

	@Test
	public void testRagdollFollowActorDrawnOnBodyNotOnTarget() {
		makeDynamic();
		look.setPosition(0, 0);
		sprite.ragdollMode = 2;
		look.setPosition(300, 0);
		step(0.05f);
		look.draw(batch, 1.0f);
		assertTrue("Actor must follow the body, not jump to the target",
				Math.abs(look.getX() - physicsObject.getX()) < DELTA);
		assertTrue(Math.abs(look.getX() - 300) > 10f);
	}

	@Test
	public void testRagdollHangupFreezesBodyWhenInvisible() {
		makeDynamic();
		look.setPosition(0, 500);
		sprite.ragdollMode = 1;
		look.setLookVisible(false);
		step(0.5f);
		assertTrue("Invisible ragdoll body hangs (static, no gravity), y=" + physicsObject.getY(),
				Math.abs(physicsObject.getY() - 500) < DELTA);

		look.setLookVisible(true);
		step(0.5f);
		assertTrue("Ragdoll body must resume falling when visible again, y=" + physicsObject.getY(),
				physicsObject.getY() < 490f);
	}

	@Test
	public void testRagdollOffSettersMoveBodyAfterDisabling() {
		makeDynamic();
		look.setPosition(100, 100);
		sprite.ragdollMode = 1;
		look.setPosition(300, 300);
		assertTrue(Math.abs(physicsObject.getX() - 100) < DELTA);

		sprite.ragdollMode = 0;
		look.setPosition(150, 150);
		assertTrue("Disabling ragdoll must restore direct body control",
				Math.abs(physicsObject.getX() - 150) < DELTA);
		assertTrue(Math.abs(physicsObject.getY() - 150) < DELTA);
	}
}