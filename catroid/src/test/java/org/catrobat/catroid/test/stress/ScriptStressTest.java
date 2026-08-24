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

package org.catrobat.catroid.test.stress;

import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.common.ThreadScheduler;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.utils.PerformanceTracker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class ScriptStressTest {

	private ThreadScheduler scheduler;
	private Actor actor;

	@Before
	public void setUp() {
		actor = new Look(new Sprite());
		scheduler = new ThreadScheduler(actor);
		// Reset global counter so tests are independent
		PerformanceTracker.activeThreads.set(0);
	}

	/** Creates a mock ScriptSequenceAction tied to a unique Script instance. */
	private ScriptSequenceAction makeAction(boolean finishesImmediately) {
		Script script = mock(Script.class);
		ScriptSequenceAction action = Mockito.mock(ScriptSequenceAction.class);
		when(action.getScript()).thenReturn(script);
		// act() returns true → action is "done" this tick; false → keeps running
		when(action.act(anyFloat())).thenReturn(finishesImmediately);
		return action;
	}

	/** Creates N distinct actions all configured to run continuously. */
	private List<ScriptSequenceAction> makeNDistinctActions(int n) {
		List<ScriptSequenceAction> actions = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			actions.add(makeAction(false));
		}
		return actions;
	}

	@Test
	public void st01_singleBlockStartsAndRuns() {
		ScriptSequenceAction action = makeAction(false);
		scheduler.startThread(action);
		scheduler.tick(1f);

		assertEquals("One action should be in the actor", 1, actor.getActions().size);
		verify(action, times(1)).act(anyFloat());
	}

	@Test
	public void st02_tenDistinctBlocksAllRun() {
		List<ScriptSequenceAction> actions = makeNDistinctActions(10);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}
		scheduler.tick(1f);

		assertEquals(10, actor.getActions().size);
		for (ScriptSequenceAction a : actions) {
			verify(a, times(1)).act(anyFloat());
		}
	}

	@Test
	public void st03_fiftyDistinctBlocksAllRun() {
		List<ScriptSequenceAction> actions = makeNDistinctActions(50);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}
		scheduler.tick(1f);

		assertEquals(50, actor.getActions().size);
		for (ScriptSequenceAction a : actions) {
			verify(a, atLeastOnce()).act(anyFloat());
		}
	}

	@Test
	public void st04_sixtyDistinctBlocksAllRun() {
		final int COUNT = 60;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		scheduler.tick(1f);
		scheduler.tick(1f);
		scheduler.tick(1f);

		assertEquals("All 60 threads must remain active", COUNT, actor.getActions().size);
		for (ScriptSequenceAction a : actions) {
			verify(a, times(3)).act(anyFloat());
		}
	}

	@Test
	public void st05_oneHundredDistinctBlocksAllRun() {
		final int COUNT = 100;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}
		scheduler.tick(1f);

		assertEquals(COUNT, actor.getActions().size);
		for (ScriptSequenceAction a : actions) {
			verify(a, times(1)).act(anyFloat());
		}
	}

	@Test(timeout = 10_000)
	public void st06_fiveHundredDistinctBlocksNoOOM() {
		final int COUNT = 500;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}
		scheduler.tick(1f);
		scheduler.tick(1f);

		assertEquals("All 500 threads stay alive (continuous)", COUNT, actor.getActions().size);
	}

	@Test
	public void st07_sameScriptDeduplicatedInStartQueue() {
		Script sharedScript = mock(Script.class);

		ScriptSequenceAction action1 = mock(ScriptSequenceAction.class);
		when(action1.getScript()).thenReturn(sharedScript);
		when(action1.act(anyFloat())).thenReturn(false);

		ScriptSequenceAction action2 = mock(ScriptSequenceAction.class);
		when(action2.getScript()).thenReturn(sharedScript);
		when(action2.act(anyFloat())).thenReturn(false);

		scheduler.startThread(action1);
		scheduler.startThread(action2); // should evict action1 from start queue

		scheduler.tick(1f);

		verify(action1, never()).act(anyFloat());
		verify(action2, times(1)).act(anyFloat());
		assertEquals(1, actor.getActions().size);
	}

	@Test
	public void st08_largeScaleDeduplication_sixtyPairs() {
		final int PAIRS = 60;
		List<ScriptSequenceAction> firstBatch = new ArrayList<>(PAIRS);
		List<ScriptSequenceAction> secondBatch = new ArrayList<>(PAIRS);

		for (int i = 0; i < PAIRS; i++) {
			Script sharedScript = mock(Script.class);

			ScriptSequenceAction a1 = mock(ScriptSequenceAction.class);
			when(a1.getScript()).thenReturn(sharedScript);
			when(a1.act(anyFloat())).thenReturn(false);
			firstBatch.add(a1);

			ScriptSequenceAction a2 = mock(ScriptSequenceAction.class);
			when(a2.getScript()).thenReturn(sharedScript);
			when(a2.act(anyFloat())).thenReturn(false);
			secondBatch.add(a2);
		}

		// Enqueue first batch, then second batch (same scripts → evict first)
		for (ScriptSequenceAction a : firstBatch) {
			scheduler.startThread(a);
		}
		for (ScriptSequenceAction a : secondBatch) {
			scheduler.startThread(a);
		}

		scheduler.tick(1f);

		assertEquals("De-duplication must leave exactly " + PAIRS + " threads", PAIRS, actor.getActions().size);

		// First batch actions must NOT have run (they were evicted before tick)
		for (ScriptSequenceAction a : firstBatch) {
			verify(a, never()).act(anyFloat());
		}
		// Second batch actions must each have run exactly once
		for (ScriptSequenceAction a : secondBatch) {
			verify(a, times(1)).act(anyFloat());
		}
	}

	@Test
	public void st09_executionOrderMatchesInsertionOrder() {
		final int COUNT = 20;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		scheduler.tick(1f);

		// Verify each action ran at least once — ordering confirmed by the
		// fact that GDX Array is ordered and startQueue is processed in order.
		// Mockito InOrder verifies the *relative* order of mock invocations.
		org.mockito.InOrder inOrder = Mockito.inOrder(actions.toArray());
		for (ScriptSequenceAction a : actions) {
			inOrder.verify(a).act(anyFloat());
		}
	}

	@Test
	public void st10_stopOneScriptAtScalePreservesRest() {
		final int COUNT = 30;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		// Pick the 15th action as the one to stop
		ScriptSequenceAction targetAction = actions.get(14);
		Script targetScript = targetAction.getScript();

		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		// tick 1: all 30 move from startQueue → actor, then all 30 execute act()
		scheduler.tick(1f);
		// Queue target for removal (added to stopQueue)
		scheduler.stopThreadsWithScript(targetScript);
		// tick 2: run loop executes ALL actions (including target) then stopQueue drains target
		scheduler.tick(1f);
		// tick 3: target is gone; only 29 remain
		scheduler.tick(1f);

		// Target ran in tick1 and tick2 (removed after tick2's run loop), so 2 calls total
		verify(targetAction, times(2)).act(anyFloat());
		// All others ran 3 times (tick1 + tick2 + tick3)
		for (ScriptSequenceAction a : actions) {
			if (a != targetAction) {
				verify(a, times(3)).act(anyFloat());
			}
		}
		assertEquals("29 threads should remain", COUNT - 1, actor.getActions().size);
	}

	@Test
	public void st11_suspendedStateBlocksAllThreadsAtScale() {
		final int COUNT = 60;
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		scheduler.setState(ThreadScheduler.SUSPENDED);
		scheduler.tick(1f);
		scheduler.tick(1f);

		// Suspended: actions flushed from startQueue → added to actor, but NOT executed
		assertEquals("All threads added to actor even when suspended", COUNT, actor.getActions().size);
		for (ScriptSequenceAction a : actions) {
			verify(a, never()).act(anyFloat());
		}
	}

	@Test
	public void st12_allThreadsFinishedSentinelAfterDrain() {
		final int COUNT = 10;
		List<ScriptSequenceAction> actions = new ArrayList<>(COUNT);
		for (int i = 0; i < COUNT; i++) {
			actions.add(makeAction(true)); // finishes immediately
		}
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		assertFalse("Before tick: threads still pending", scheduler.haveAllThreadsFinished());

		scheduler.tick(1f); // all complete on first tick → removed from actor

		assertTrue("After drain: all threads finished", scheduler.haveAllThreadsFinished());
	}

	@Test
	public void st13_performanceTrackerCounterAccumulatesAtScale() {
		final int COUNT = 50;
		PerformanceTracker.activeThreads.set(0);
		List<ScriptSequenceAction> actions = makeNDistinctActions(COUNT);
		for (ScriptSequenceAction a : actions) {
			scheduler.startThread(a);
		}

		// tick() order: addAndGet(actions.size) THEN startThreadsInStartQueue.
		// On tick 1: actions.size == 0 (startQueue not yet flushed) so counter += 0,
		// then startQueue flushed → COUNT actions added to actor.
		// On tick 2: actions.size == COUNT → counter += COUNT.
		scheduler.tick(1f);
		long counterAfterTick1 = PerformanceTracker.activeThreads.get();
		// After tick1 the flush happened; actor now has COUNT threads.
		// The counter itself reflects whatever was in actor BEFORE the flush (0 on first tick).
		assertEquals("Actor should have COUNT threads after first tick", COUNT, actor.getActions().size);

		scheduler.tick(1f);
		long counterAfterTick2 = PerformanceTracker.activeThreads.get();
		// Tick 2 sampled actions.size = COUNT before running → counter incremented by COUNT
		assertEquals("Counter should have increased by COUNT on second tick",
				counterAfterTick1 + COUNT, counterAfterTick2);

		scheduler.tick(1f);
		long counterAfterTick3 = PerformanceTracker.activeThreads.get();
		assertEquals("Counter keeps accumulating",
				counterAfterTick2 + COUNT, counterAfterTick3);
	}

	@Test
	public void st14_reStartAfterCompletionWorks() {
		ScriptSequenceAction action = makeAction(true); // completes on first act()
		scheduler.startThread(action);
		scheduler.tick(1f); // runs and finishes → removed from actor

		assertTrue("No threads should remain after drain", scheduler.haveAllThreadsFinished());

		// Re-queue the same action (new act() call will still return true)
		scheduler.startThread(action);
		assertFalse("Pending restart: not all threads finished yet", scheduler.haveAllThreadsFinished());

		scheduler.tick(1f);
		// verify: act() was called exactly twice total (once per startThread)
		verify(action, times(2)).act(anyFloat());
	}

	@Test
	public void st15_mixedCompleteAndContinuousThreads() {
		final int FINISHING = 25;
		final int CONTINUOUS = 25;

		List<ScriptSequenceAction> finishers = new ArrayList<>(FINISHING);
		for (int i = 0; i < FINISHING; i++) {
			finishers.add(makeAction(true));
		}
		List<ScriptSequenceAction> runners = new ArrayList<>(CONTINUOUS);
		for (int i = 0; i < CONTINUOUS; i++) {
			runners.add(makeAction(false));
		}

		for (ScriptSequenceAction a : finishers) scheduler.startThread(a);
		for (ScriptSequenceAction a : runners) scheduler.startThread(a);

		scheduler.tick(1f);

		// After first tick: finishers done and removed, runners still active
		assertEquals("Only continuous threads should remain", CONTINUOUS, actor.getActions().size);

		scheduler.tick(1f);
		// Finishers ran exactly once; runners ran twice
		for (ScriptSequenceAction a : finishers) {
			verify(a, times(1)).act(anyFloat());
		}
		for (ScriptSequenceAction a : runners) {
			verify(a, times(2)).act(anyFloat());
		}
	}
}
