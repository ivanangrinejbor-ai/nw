package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;

public class ExposureEffect extends ShaderVfxEffect implements ChainVfxEffect {
    private float exposure = 1.0f;

    public ExposureEffect() {
        super(VfxGLUtils.compileShader(
                Gdx.files.internal("shaders/screenspace.vert"),
                Gdx.files.internal("shaders/exposure.frag")
        ));
    }

    public void setExposure(float exposure) {
        this.exposure = exposure;
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        buffers.getSrcTexture().bind(0);

        program.begin();
        program.setUniformi("u_texture0", 0);
        program.setUniformf("u_exposure", this.exposure);
        program.end();

        renderShader(context, buffers.getDstBuffer());
    }
}