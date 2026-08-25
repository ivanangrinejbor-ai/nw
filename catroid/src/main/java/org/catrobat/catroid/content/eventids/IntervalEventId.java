package org.catrobat.catroid.content.eventids;

import org.catrobat.catroid.formulaeditor.Formula;

public class IntervalEventId extends EventId {
	final Formula secondsFormula;

	public IntervalEventId(Formula secondsFormula) {
		this.secondsFormula = secondsFormula;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof IntervalEventId)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		IntervalEventId that = (IntervalEventId) o;
		return secondsFormula != null ? secondsFormula.equals(that.secondsFormula)
				: that.secondsFormula == null;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (secondsFormula != null ? secondsFormula.hashCode() : 0);
		return result;
	}
}
