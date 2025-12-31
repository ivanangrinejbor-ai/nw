package org.catrobat.catroid.raptor;

import java.util.ArrayList;
import java.util.List;

public class PostProcessingComponent implements Component {
    public boolean isActive = true;
    public float qualityScale = 0.25f;

    public List<PostProcessingData> effects = new ArrayList<>();

    public PostProcessingComponent() {
        effects.add(new PostProcessingData.Bloom());
        effects.add(new PostProcessingData.Vignette());
        effects.add(new PostProcessingData.Levels());
        effects.add(new PostProcessingData.Fxaa());
    }

    public boolean hasEffect(Class<? extends PostProcessingData> effectClass) {
        if (effects == null) return false;
        for (PostProcessingData effect : effects) {
            if (effectClass.isInstance(effect) && effect.isEnabled) {
                return true;
            }
        }
        return false;
    }
}