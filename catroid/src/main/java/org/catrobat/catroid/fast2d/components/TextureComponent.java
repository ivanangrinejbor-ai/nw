package org.catrobat.catroid.fast2d.components;

import com.artemis.PooledComponent;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TextureComponent extends PooledComponent {
    public TextureRegion region = null;
    public String textureName = "";
    public Color color = new Color(Color.WHITE);
    public float width = 0f, height = 0f;
    public float originX = 0f, originY = 0f;

    @Override
    protected void reset() {
        region = null;
        textureName = "";
        color.set(Color.WHITE);
        width = 0f; height = 0f;
        originX = 0f; originY = 0f;
    }
}