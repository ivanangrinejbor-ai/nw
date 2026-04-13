package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class DepthOfFieldEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = a_position;\n" +
            "}";

    private static final String FSH = Gdx.files.internal("shaders/dof.frag").readString();

    private Texture depthTexture;
    private Camera camera;

    public float focusDistance = 15.0f;
    public float focusRange = 5.0f;
    public float blurSize = 0.02f;
    public float transition = 10.0f;

    public DepthOfFieldEffect() {
        super(new ShaderProgram(VSH, FSH));
        if (!program.isCompiled()) Gdx.app.error("DoF", program.getLog());
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(Texture depthTexture) { this.depthTexture = depthTexture; }

    @Override
    public void rebind() {
        super.rebind();
        program.begin();
        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.end();
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        VfxFrameBuffer src = buffers.getSrcBuffer();
        VfxFrameBuffer dst = buffers.getDstBuffer();

        program.begin();

        if (depthTexture != null) depthTexture.bind(1);
        src.getTexture().bind(0);
        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);

        program.setUniformf("u_farPlane", camera.far);
        program.setUniformf("u_aspectRatio", (float) Gdx.graphics.getWidth() / (float) Gdx.graphics.getHeight());

        program.setUniformf("u_focusDistance", focusDistance);
        program.setUniformf("u_focusRange", focusRange);
        program.setUniformf("u_transition", transition);
        program.setUniformf("u_blurSize", blurSize);

        program.end();

        renderShader(context, dst);
    }
}