package org.catrobat.catroid.test.robolectric.content;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import org.catrobat.catroid.content.XmlHeader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class XmlHeaderProtectedProjectTest {

	@Test
	public void testNewHeaderIsNotProtected() {
		XmlHeader header = new XmlHeader();
		assertFalse(header.isProtectedProject());
	}

	@Test
	public void testSetProtectedProjectTrue() {
		XmlHeader header = new XmlHeader();
		header.setProtectedProject(true);
		assertTrue(header.isProtectedProject());
	}

	@Test
	public void testSetProtectedProjectFalse() {
		XmlHeader header = new XmlHeader();
		header.setProtectedProject(true);
		header.setProtectedProject(false);
		assertFalse(header.isProtectedProject());
	}

	@Test
	public void testProtectedFlagIsIndependent() {
		XmlHeader header = new XmlHeader();
		header.setProtectedProject(true);
		header.setlandscapeMode(true);
		assertTrue(header.isProtectedProject());
		assertTrue(header.islandscapeMode());
	}
}
