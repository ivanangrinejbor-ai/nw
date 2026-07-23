package org.catrobat.catroid.content;

import org.catrobat.catroid.content.bricks.ScriptBrick;
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrickV2;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.eventids.UserDefinedBrickV2EventId;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserDefinedScriptV2 extends Script {
    private static final long serialVersionUID = 2L;

    private UUID userDefinedBrickID;
    private String blockName;
    private List<String> parameterNames;
    private transient Map<String, Object> currentParams = new ConcurrentHashMap<>();

    public UserDefinedScriptV2(UUID userDefinedBrickID, String blockName, List<String> parameterNames) {
        this.userDefinedBrickID = userDefinedBrickID;
        this.blockName = blockName;
        this.parameterNames = parameterNames != null ? parameterNames : new ArrayList<>();
    }

    public UserDefinedScriptV2() {
    }

    public String getBlockName() {
        return blockName;
    }

    public List<String> getParameterNames() {
        return parameterNames;
    }

    public UUID getUserDefinedBrickID() {
        return userDefinedBrickID;
    }

    @Override
    public ScriptBrick getScriptBrick() {
        if (scriptBrick == null) {
            scriptBrick = new UserDefinedReceiverBrickV2(this);
        }
        return scriptBrick;
    }

    @Override
    public EventId createEventId(Sprite sprite) {
        return new UserDefinedBrickV2EventId(userDefinedBrickID);
    }

    public void setParamValues(Map<String, Object> values) {
        if (currentParams == null) {
            currentParams = new ConcurrentHashMap<>();
        }
        currentParams.clear();
        if (values != null) {
            currentParams.putAll(values);
        }
    }

    public Object getParamValue(String name) {
        if (currentParams == null) {
            return null;
        }
        return currentParams.get(name);
    }

    public void setParamValue(String name, Object val) {
        if (currentParams == null) {
            currentParams = new ConcurrentHashMap<>();
        }
        if (val != null) {
            currentParams.put(name, val);
        } else {
            currentParams.remove(name);
        }
    }
}
