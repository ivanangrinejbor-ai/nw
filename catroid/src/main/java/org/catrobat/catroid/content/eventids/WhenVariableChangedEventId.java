package org.catrobat.catroid.content.eventids;

public class WhenVariableChangedEventId extends EventId {

    public static final int VARIABLE_CHANGED = 107;

    private final String variableName;

    public WhenVariableChangedEventId(String variableName) {
        this.variableName = variableName;
    }

    public String getVariableName() {
        return variableName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WhenVariableChangedEventId)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        WhenVariableChangedEventId that = (WhenVariableChangedEventId) o;
        return variableName != null ? variableName.equals(that.variableName) : that.variableName == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (variableName != null ? variableName.hashCode() : 0);
        return result;
    }
}
