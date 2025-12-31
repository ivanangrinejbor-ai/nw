package org.catrobat.catroid.content;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class VmMonitorActor extends Actor {
    private Texture vmTexture;
    private ShaderProgram shader;

    public VmMonitorActor(ShaderProgram shader) {
        this.shader = shader;
    }

    public void setTexture(Texture texture) {
        this.vmTexture = texture;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (vmTexture == null) return;

        ShaderProgram previousShader = batch.getShader();
        batch.setShader(shader);

        batch.disableBlending();

        batch.setColor(1, 1, 1, 1);
        batch.draw(vmTexture, getX(), getY(), getWidth(), getHeight());

        batch.enableBlending();

        batch.setShader(previousShader);
    }
}