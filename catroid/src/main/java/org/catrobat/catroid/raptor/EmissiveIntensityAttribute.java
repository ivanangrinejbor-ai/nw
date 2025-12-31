package org.catrobat.catroid.raptor;

import com.badlogic.gdx.graphics.g3d.Attribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;

public class EmissiveIntensityAttribute extends PBRFloatAttribute {

    public final static String Alias = "emissiveIntensity";
    public final static long Type = register(Alias);

    public EmissiveIntensityAttribute(float value) {
        super(Type, value);
    }

    @Override
    public Attribute copy() {
        return new EmissiveIntensityAttribute(value);
    }
}