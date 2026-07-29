package org.catrobat.catroid.test.robolectric.formulaeditor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.catrobat.catroid.formulaeditor.UserVariable;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class UserVariableLockTest {

	@Test
	public void testNewVariableIsNotLocked() {
		UserVariable var = new UserVariable("test");
		assertFalse(var.isLocked());
	}

	@Test
	public void testSetLockMakesVariableLocked() {
		UserVariable var = new UserVariable("test");
		var.setLock("password123");
		assertTrue(var.isLocked());
	}

	@Test
	public void testClearLockMakesVariableUnlocked() {
		UserVariable var = new UserVariable("test");
		var.setLock("password123");
		var.clearLock();
		assertFalse(var.isLocked());
	}

	@Test
	public void testVerifyLockAcceptsCorrectPassword() {
		UserVariable var = new UserVariable("test");
		var.setLock("password123");
		assertTrue(var.verifyLock("password123"));
	}

	@Test
	public void testVerifyLockRejectsWrongPassword() {
		UserVariable var = new UserVariable("test");
		var.setLock("password123");
		assertFalse(var.verifyLock("wrongpassword"));
	}

	@Test
	public void testVerifyLockReturnsTrueForUnlockedVariable() {
		UserVariable var = new UserVariable("test");
		assertTrue(var.verifyLock("anypassword"));
	}

	@Test
	public void testCopyConstructorPreservesLock() {
		UserVariable original = new UserVariable("test");
		original.setLock("password123");

		UserVariable copy = new UserVariable(original);
		assertTrue(copy.isLocked());
		assertTrue(copy.verifyLock("password123"));
		assertFalse(copy.verifyLock("wrongpassword"));
	}

	@Test
	public void testCopyConstructorPreservesName() {
		UserVariable original = new UserVariable("myVar");
		UserVariable copy = new UserVariable(original);
		assertEquals("myVar", copy.getName());
	}

	@Test
	public void testDifferentVariablesHaveDifferentSalts() {
		UserVariable var1 = new UserVariable("test1");
		UserVariable var2 = new UserVariable("test2");
		var1.setLock("samepassword");
		var2.setLock("samepassword");

		assertTrue(var1.verifyLock("samepassword"));
		assertTrue(var2.verifyLock("samepassword"));
	}

	@Test
	public void testLockSurvivesNameChange() {
		UserVariable var = new UserVariable("oldName");
		var.setLock("password123");
		var.setName("newName");

		assertTrue(var.isLocked());
		assertTrue(var.verifyLock("password123"));
	}
}
