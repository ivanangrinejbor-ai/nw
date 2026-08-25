package org.catrobat.catroid.content;

import android.os.SystemClock;
import android.util.Log;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.eventids.IntervalEventId;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class IntervalScriptTrigger {
	private static final String TAG = IntervalScriptTrigger.class.getSimpleName();

	private final Formula secondsFormula;
	private long lastTick = 0;
	private double elapsedMs = 0;
	private boolean started = false;

	public IntervalScriptTrigger(Formula secondsFormula) {
		this.secondsFormula = secondsFormula;
	}

	void evaluateAndTriggerActions(Sprite sprite) {
		long now = SystemClock.uptimeMillis();
		if (!started) {
			started = true;
			lastTick = now;
			return;
		}

		long delta = now - lastTick;
		lastTick = now;
		elapsedMs += delta;

		double intervalMs;
		try {
			Scope scope = new Scope(ProjectManager.getInstance().getCurrentProject(), sprite, null);
			double seconds = secondsFormula.interpretDouble(scope);
			if (seconds <= 0) {
				return;
			}
			intervalMs = seconds * 1000.0;
		} catch (InterpretationException e) {
			Log.e(TAG, Log.getStackTraceString(e));
			return;
		}

		if (elapsedMs >= intervalMs) {
			elapsedMs = 0;
			sprite.look.fire(new EventWrapper(new IntervalEventId(secondsFormula), false));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof IntervalScriptTrigger)) {
			return false;
		}
		return secondsFormula.equals(((IntervalScriptTrigger) o).secondsFormula);
	}

	@Override
	public int hashCode() {
		return secondsFormula.hashCode();
	}
}
