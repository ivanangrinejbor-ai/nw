package org.catrobat.catroid.raptor;

import java.util.ArrayList;
import java.util.List;

public class PrefabComponent implements Component {
    public String prefabFilePath;
    public transient List<String> spawnedInstances = new ArrayList<>();

    public PrefabComponent() {}
}