package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;

public class AutoLensFlareEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private float threshold = 0.8f;
    private float intensity = 2.0f;
    private float dispersal = 0.4f;
    private float distortion = 0.02f;
    private float size = 1f;

    public AutoLensFlareEffect() {
        super(VfxGLUtils.compileShader(
                Gdx.files.internal("shaders/screenspace.vert"),
                Gdx.files.internal("shaders/auto_flare.frag")
        ));
    }

    @Override
    public void resize(int width, int height) {



        super.resize((int)(width / size), (int)(height / size));
    }

    public void setSize(float size) {
        this.size = size;
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        setUniform("u_threshold", threshold);
        setUniform("u_intensity", intensity);
        setUniform("u_dispersal", dispersal);
        setUniform("u_distortion", distortion);
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        VfxFrameBuffer src = buffers.getSrcBuffer();
        VfxFrameBuffer dst = buffers.getDstBuffer();

        try {




            src.getTexture().bind(0);
            program.bind();
            program.setUniformi("u_texture0", 0);


            context.getViewportMesh().render(program);
        } catch (Exception e) {
            Gdx.app.error("AutoLensFlare", "Render error", e);
        }
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public void setIntensity(float intensity) {
        this.intensity = intensity;
    }

    public void setDispersal(float dispersal) {
        this.dispersal = dispersal;
    }
}