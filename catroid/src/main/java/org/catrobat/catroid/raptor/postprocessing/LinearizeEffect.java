package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;

public class LinearizeEffect extends ShaderVfxEffect implements ChainVfxEffect {

    public LinearizeEffect() {
        super(VfxGLUtils.compileShader(
                Gdx.files.internal("shaders/screenspace.vert"),
                Gdx.files.internal("shaders/linearize.frag")
        ));
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        buffers.getSrcTexture().bind(0);
        program.begin();
        program.setUniformi("u_texture0", 0);
        program.end();
        renderShader(context, buffers.getDstBuffer());
    }
}