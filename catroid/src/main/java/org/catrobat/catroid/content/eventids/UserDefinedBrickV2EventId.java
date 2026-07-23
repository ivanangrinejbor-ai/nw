package org.catrobat.catroid.content.eventids;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class UserDefinedBrickV2EventId extends EventId {
    private final UUID userDefinedBrickID;
    private final Map<String, Object> interpretedInputs;

    public UserDefinedBrickV2EventId(UUID userDefinedBrickID) {
        this(userDefinedBrickID, null);
    }

    public UserDefinedBrickV2EventId(UUID userDefinedBrickID, Map<String, Object> interpretedInputs) {
        this.userDefinedBrickID = userDefinedBrickID;
        this.interpretedInputs = interpretedInputs;
    }

    public UUID getUserDefinedBrickID() {
        return userDefinedBrickID;
    }

    public Map<String, Object> getInterpretedInputs() {
        return interpretedInputs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDefinedBrickV2EventId)) return false;
        UserDefinedBrickV2EventId that = (UserDefinedBrickV2EventId) o;
        return Objects.equals(userDefinedBrickID, that.userDefinedBrickID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userDefinedBrickID);
    }
}
