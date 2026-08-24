package org.catrobat.catroid.test.content.actions

import org.catrobat.catroid.content.actions.GetFromPastebinAction
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GetFromPastebinActionTest {

	private fun runAction(url: Formula?, variable: UserVariable?) {
		val action = GetFromPastebinAction()
		action.url = url
		action.variable = variable
		action.act(1f)
	}

	@Test
	fun testNullUrlLeavesVariableUntouched() {
		val variable = UserVariable("v").apply { value = "INITIAL" }
		runAction(null, variable)
		assertEquals("INITIAL", variable.value)
	}

	@Test
	fun testNullUrlAndNullVariableDoesNotCrash() {
		runAction(null, null)
	}

	@Test
	fun testNullVariableDoesNotCrash() {
		runAction(Formula("not-a-url"), null)
	}

	@Test
	fun testMalformedUrlReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("not a url at all"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testEmptyUrlReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula(""), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testUrlWithoutSchemeReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("pastebin.com/raw/abc"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testFtpSchemeReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("ftp://example.com/file"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testFileSchemeReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("file:///etc/passwd"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testConnectionRefusedReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("http://127.0.0.1:1/"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testUnroutableAddressReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("https://192.0.2.1/"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testInvalidPortInUrlReturnsError() {
		val variable = UserVariable("v")
		runAction(Formula("http://127.0.0.1:99999/"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testRepeatedFailureKeepsError() {
		val variable = UserVariable("v")
		runAction(Formula("not-a-url"), variable)
		assertEquals("ERROR", variable.value)
		runAction(Formula("not-a-url"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testErrorOverwritesPreviousValue() {
		val variable = UserVariable("PREVIOUS")
		runAction(Formula("not-a-url"), variable)
		assertEquals("ERROR", variable.value)
	}

	@Test
	fun testFormulaFunctionYieldingUrlFails() {
		val variable = UserVariable("v")
		runAction(Formula("JOIN(http://127.0.0.1:1/, x)"), variable)
		assertEquals("ERROR", variable.value)
	}
}