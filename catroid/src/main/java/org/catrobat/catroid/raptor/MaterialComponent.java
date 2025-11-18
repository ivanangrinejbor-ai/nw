package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.Color;

public class MaterialComponent implements Component {
    public Color baseColor = new Color(Color.WHITE);
    public String baseColorTexturePath = null;

    public float metallic = 0.0f;
    public float roughness = 1.0f;
    public String metallicRoughnessTexturePath = null;

    public String normalTexturePath = null;

    public String occlusionTexturePath = null;

    public Color emissiveColor = new Color(Color.BLACK);
    public String emissiveTexturePath = null;

    public MaterialComponent() {}
}