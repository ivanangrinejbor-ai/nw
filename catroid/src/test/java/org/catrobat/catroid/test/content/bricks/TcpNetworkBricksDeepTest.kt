package org.catrobat.catroid.test.content.bricks

import android.view.View
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.ActionFactory
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.actions.ScriptSequenceAction
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.content.bricks.CheckPortBrick
import org.catrobat.catroid.content.bricks.GetFromPastebinBrick
import org.catrobat.catroid.content.bricks.ListenTcpServerBrick
import org.catrobat.catroid.content.bricks.SendToTcpServerBrick
import org.catrobat.catroid.content.bricks.SetTcpServerClientLimitBrick
import org.catrobat.catroid.content.bricks.SetTcpServerTimeoutBrick
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class TcpNetworkBricksDeepTest {

	private lateinit var sprite: Sprite
	private lateinit var sequence: ScriptSequenceAction

	private fun roundTrip(brick: Brick): Brick {
		val xstream = XstreamSerializer.getInstance().getXstream()
		val xml = xstream.toXML(brick)
		return xstream.fromXML(xml) as Brick
	}

	@Before
	fun setUp() {
		val project = Project(MockUtil.mockContextForProject(), "Project")
		val scene = Scene("Currently playing scene", project)
		sprite = Sprite("Sprite")
		scene.addSprite(sprite)
		project.addScene(scene)
		ProjectManager.getInstance().setCurrentProject(project)
		ProjectManager.getInstance().setCurrentlyEditedScene(Scene())
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene)
		sequence = ScriptSequenceAction(Mockito.mock(Script::class.java))
	}

	@Test
	fun testPastebinConstructors() {
		val empty = GetFromPastebinBrick()
		assertNotNull(empty.getFormulaWithBrickField(Brick.BrickField.URL))
		val fromString = GetFromPastebinBrick("https://pastebin.com/raw/abc")
		assertEquals("https://pastebin.com/raw/abc",
			fromString.getFormulaWithBrickField(Brick.BrickField.URL).interpretString(null))
		val fromFormula = GetFromPastebinBrick(Formula("https://pastebin.com/raw/xyz"))
		assertEquals("https://pastebin.com/raw/xyz",
			fromFormula.getFormulaWithBrickField(Brick.BrickField.URL).interpretString(null))
	}

	@Test
	fun testPastebinCloneKeepsFormulaAndVariable() {
		val variable = UserVariable("content")
		val brick = GetFromPastebinBrick("https://pastebin.com/raw/abc")
		brick.setUserVariable(variable)
		val clone = brick.clone() as GetFromPastebinBrick
		assertEquals("https://pastebin.com/raw/abc",
			clone.getFormulaWithBrickField(Brick.BrickField.URL).interpretString(null))
		assertEquals(variable, clone.getUserVariable())
	}

	@Test
	fun testPastebinRoundTrip() {
		val variable = UserVariable("content")
		val brick = GetFromPastebinBrick("https://pastebin.com/raw/abc")
		brick.setUserVariable(variable)
		val restored = roundTrip(brick) as GetFromPastebinBrick
		assertEquals("https://pastebin.com/raw/abc",
			restored.getFormulaWithBrickField(Brick.BrickField.URL).interpretString(null))
		assertEquals("content", restored.getUserVariable().name)
	}

	@Test
	fun testPastebinRoundTripWithoutVariable() {
		val brick = GetFromPastebinBrick("https://pastebin.com/raw/abc")
		val restored = roundTrip(brick) as GetFromPastebinBrick
		assertNull(restored.getUserVariable())
	}

	@Test
	fun testCheckPortConstructors() {
		val fromString = CheckPortBrick("8080")
		assertEquals("8080", fromString.getFormulaWithBrickField(Brick.BrickField.PORT).interpretString(null))
		val fromFormula = CheckPortBrick(Formula("9090"))
		assertEquals("9090", fromFormula.getFormulaWithBrickField(Brick.BrickField.PORT).interpretString(null))
	}

	@Test
	fun testCheckPortCloneKeepsFormulaAndVariable() {
		val brick = CheckPortBrick("8080")
		brick.setUserVariable(UserVariable("portInUse"))
		val clone = brick.clone() as CheckPortBrick
		assertEquals("8080", clone.getFormulaWithBrickField(Brick.BrickField.PORT).interpretString(null))
		assertEquals("portInUse", clone.getUserVariable().name)
	}

	@Test
	fun testCheckPortRoundTrip() {
		val brick = CheckPortBrick("8080")
		brick.setUserVariable(UserVariable("portInUse"))
		val restored = roundTrip(brick) as CheckPortBrick
		assertEquals("8080", restored.getFormulaWithBrickField(Brick.BrickField.PORT).interpretString(null))
		assertEquals("portInUse", restored.getUserVariable().name)
	}

	@Test
	fun testLimitConstructors() {
		val fromInt = SetTcpServerClientLimitBrick(10)
		assertEquals("10", fromInt.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
		val fromString = SetTcpServerClientLimitBrick("5")
		assertEquals("5", fromString.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
		val fromFormula = SetTcpServerClientLimitBrick(Formula("7"))
		assertEquals("7", fromFormula.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testLimitCloneKeepsFormula() {
		val brick = SetTcpServerClientLimitBrick(3)
		val clone = brick.clone() as SetTcpServerClientLimitBrick
		assertEquals("3", clone.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testLimitRoundTrip() {
		val restored = roundTrip(SetTcpServerClientLimitBrick(42)) as SetTcpServerClientLimitBrick
		assertEquals("42", restored.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testTimeoutConstructors() {
		val fromInt = SetTcpServerTimeoutBrick(30)
		assertEquals("30", fromInt.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
		val fromString = SetTcpServerTimeoutBrick("60")
		assertEquals("60", fromString.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testTimeoutCloneKeepsFormula() {
		val brick = SetTcpServerTimeoutBrick(15)
		val clone = brick.clone() as SetTcpServerTimeoutBrick
		assertEquals("15", clone.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testTimeoutRoundTrip() {
		val restored = roundTrip(SetTcpServerTimeoutBrick(120)) as SetTcpServerTimeoutBrick
		assertEquals("120", restored.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
	}

	@Test
	fun testSendVisibleFieldsDefaultOne() {
		assertEquals(1, SendToTcpServerBrick("a").getVisibleFields())
	}

	@Test
	fun testSendVisibleFieldsRange() {
		val brick = SendToTcpServerBrick("a")
		for (value in intArrayOf(2, 3, 4, 4, 4, 1)) {
			brick.setVisibleFields(value)
			assertEquals(value, brick.getVisibleFields())
		}
	}

	@Test
	fun testSendCloneKeepsFieldsAndFormulas() {
		val brick = SendToTcpServerBrick("one")
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_2, Formula("two"))
		brick.setVisibleFields(2)
		val clone = brick.clone() as SendToTcpServerBrick
		assertEquals(2, clone.getVisibleFields())
		assertEquals("one", clone.getFormulaWithBrickField(Brick.BrickField.VALUE).interpretString(null))
		assertEquals("two", clone.getFormulaWithBrickField(Brick.BrickField.VALUE_2).interpretString(null))
	}

	@Test
	fun testSendRoundTripKeepsVisibleFields() {
		val brick = SendToTcpServerBrick("one")
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_2, Formula("two"))
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_3, Formula("three"))
		brick.setVisibleFields(3)
		val restored = roundTrip(brick) as SendToTcpServerBrick
		assertEquals(3, restored.getVisibleFields())
		assertEquals("three", restored.getFormulaWithBrickField(Brick.BrickField.VALUE_3).interpretString(null))
	}

	@Test
	fun testSendBackwardCompatDefaultsToOneVisibleField() {
		val restored = roundTrip(SendToTcpServerBrick("one")) as SendToTcpServerBrick
		assertEquals(1, restored.getVisibleFields())
	}

	@Test
	fun testSendActionReceivesOneValue() {
		val actionFactory = Mockito.mock(ActionFactory::class.java)
		sprite.setActionFactory(actionFactory)
		SendToTcpServerBrick("only").addActionToSequence(sprite, sequence)
		Mockito.verify(actionFactory).createSendToTcpServerAction(
			Mockito.eq(sprite), Mockito.any(), Mockito.anyList())
	}

	@Test
	fun testSendActionReceivesAllVisibleValues() {
		val actionFactory = Mockito.mock(ActionFactory::class.java)
		sprite.setActionFactory(actionFactory)
		val brick = SendToTcpServerBrick("1")
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_2, Formula("2"))
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_3, Formula("3"))
		brick.setFormulaWithBrickField(Brick.BrickField.VALUE_4, Formula("4"))
		brick.setVisibleFields(4)
		brick.addActionToSequence(sprite, sequence)
		Mockito.verify(actionFactory).createSendToTcpServerAction(
			Mockito.eq(sprite), Mockito.any(), Mockito.anyList())
	}

	@Test
	fun testListenVariableAccessors() {
		val brick = ListenTcpServerBrick(UserVariable("main"))
		assertEquals("main", brick.getVariable(0).name)
		brick.setVariable(1, UserVariable("extra1"))
		brick.setVariable(2, UserVariable("extra2"))
		assertEquals("extra1", brick.getVariable(1).name)
		assertEquals("extra2", brick.getVariable(2).name)
		assertNull(brick.getVariable(3))
	}

	@Test
	fun testListenSetVariableOutOfOrderGrowsList() {
		val brick = ListenTcpServerBrick()
		brick.setVariable(3, UserVariable("fourth"))
		assertEquals("fourth", brick.getVariable(3).name)
		assertNull(brick.getVariable(1))
		assertNull(brick.getVariable(2))
	}

	@Test
	fun testListenVisibleVariablesDefaultOne() {
		assertEquals(1, ListenTcpServerBrick().getVisibleVariables())
	}

	@Test
	fun testListenCloneKeepsVariablesAndResetsSpinners() {
		val brick = ListenTcpServerBrick(UserVariable("main"))
		brick.setVariable(1, UserVariable("extra"))
		brick.setVisibleVariables(2)
		val clone = brick.clone() as ListenTcpServerBrick
		assertEquals(2, clone.getVisibleVariables())
		assertEquals("main", clone.getVariable(0).name)
		assertEquals("extra", clone.getVariable(1).name)
	}

	@Test
	fun testListenRoundTripKeepsVariables() {
		val brick = ListenTcpServerBrick(UserVariable("main"))
		brick.setVariable(1, UserVariable("extra1"))
		brick.setVariable(2, UserVariable("extra2"))
		brick.setVisibleVariables(3)
		val restored = roundTrip(brick) as ListenTcpServerBrick
		assertEquals(3, restored.getVisibleVariables())
		assertEquals("main", restored.getVariable(0).name)
		assertEquals("extra1", restored.getVariable(1).name)
		assertEquals("extra2", restored.getVariable(2).name)
	}

	@Test
	fun testListenRoundTripWithoutVariables() {
		val restored = roundTrip(ListenTcpServerBrick()) as ListenTcpServerBrick
		assertEquals(1, restored.getVisibleVariables())
		assertNull(restored.getVariable(0))
	}

	@Test
	fun testListenActionReceivesVariables() {
		val actionFactory = Mockito.mock(ActionFactory::class.java)
		sprite.setActionFactory(actionFactory)
		val brick = ListenTcpServerBrick(UserVariable("main"))
		brick.setVariable(1, UserVariable("extra"))
		brick.setVisibleVariables(2)
		brick.addActionToSequence(sprite, sequence)
		Mockito.verify(actionFactory).createListenTcpServerAction(
			Mockito.eq(sprite), Mockito.any(), Mockito.anyList())
	}

	@Test
	fun testAllSixBricksInflateViews() {
		val context = RuntimeEnvironment.getApplication()
		val bricks = listOf(
			GetFromPastebinBrick("https://pastebin.com/raw/x"),
			CheckPortBrick("8080"),
			SetTcpServerClientLimitBrick(10),
			SetTcpServerTimeoutBrick(30),
			SendToTcpServerBrick("okay"),
			ListenTcpServerBrick()
		)
		for (brick in bricks) {
			assertNotNull("инфлейт ${brick.javaClass.simpleName}", brick.getView(context))
		}
	}

	@Test
	fun testSendViewHidesExtraFieldsInitially() {
		val view = SendToTcpServerBrick("okay").getView(RuntimeEnvironment.getApplication())
		assertEquals(View.GONE, view.findViewById<View>(R.id.brick_send_tcp_edit2).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.brick_send_tcp_edit3).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.brick_send_tcp_edit4).visibility)
	}

	@Test
	fun testSendViewAddButtonIncrementsToTwelve() {
		val brick = SendToTcpServerBrick("okay")
		val view = brick.getView(RuntimeEnvironment.getApplication())
		val addButton = view.findViewById<View>(R.id.brick_send_tcp_add)
		for (expected in 2..12) {
			addButton.performClick()
			assertEquals(expected, brick.getVisibleFields())
		}
		addButton.performClick()
		assertEquals("после лимита поля не добавляются", 12, brick.getVisibleFields())
	}

	@Test
	fun testSendViewShowsFieldsAfterIncrement() {
		val brick = SendToTcpServerBrick("okay")
		val view = brick.getView(RuntimeEnvironment.getApplication())
		val addButton = view.findViewById<View>(R.id.brick_send_tcp_add)
		addButton.performClick()
		addButton.performClick()
		assertEquals(View.VISIBLE, view.findViewById<View>(R.id.brick_send_tcp_edit2).visibility)
		assertEquals(View.VISIBLE, view.findViewById<View>(R.id.brick_send_tcp_edit3).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.brick_send_tcp_edit4).visibility)
	}

	@Test
	fun testListenViewHidesExtraSpinnersInitially() {
		val view = ListenTcpServerBrick().getView(RuntimeEnvironment.getApplication())
		assertEquals(View.GONE, view.findViewById<View>(R.id.listen_tcp_spinner2).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.listen_tcp_spinner3).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.listen_tcp_spinner4).visibility)
	}

	@Test
	fun testListenViewAddButtonIncrementsToTwelve() {
		val brick = ListenTcpServerBrick()
		val view = brick.getView(RuntimeEnvironment.getApplication())
		val addButton = view.findViewById<View>(R.id.brick_listen_tcp_add)
		for (expected in 2..12) {
			addButton.performClick()
			assertEquals(expected, brick.getVisibleVariables())
		}
		addButton.performClick()
		assertEquals(12, brick.getVisibleVariables())
	}

	@Test
	fun testListenViewShowsSpinnersAfterIncrement() {
		val brick = ListenTcpServerBrick()
		val view = brick.getView(RuntimeEnvironment.getApplication())
		val addButton = view.findViewById<View>(R.id.brick_listen_tcp_add)
		addButton.performClick()
		assertEquals(View.VISIBLE, view.findViewById<View>(R.id.listen_tcp_spinner2).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.listen_tcp_spinner3).visibility)
	}

	@Test
	fun testListenViewWithExistingVariableSelectsIt() {
		val variable = UserVariable("received")
		val brick = ListenTcpServerBrick(variable)
		val view = brick.getView(RuntimeEnvironment.getApplication())
		assertNotNull(view.findViewById(R.id.listen_tcp_spinner1))
		assertTrue(brick.getUserVariable() == variable)
	}

	@Test
	fun testSendViewAfterSerializeRestoresVisibility() {
		val brick = SendToTcpServerBrick("okay")
		brick.setVisibleFields(3)
		val restored = roundTrip(brick) as SendToTcpServerBrick
		val view = restored.getView(RuntimeEnvironment.getApplication())
		assertEquals(View.VISIBLE, view.findViewById<View>(R.id.brick_send_tcp_edit3).visibility)
		assertEquals(View.GONE, view.findViewById<View>(R.id.brick_send_tcp_edit4).visibility)
	}
}