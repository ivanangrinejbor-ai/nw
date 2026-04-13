package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector3;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class GodRaysEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position; attribute vec2 a_texCoord0; varying vec2 v_texCoords; void main() { v_texCoords = a_texCoord0; gl_Position = a_position; }";
    private static final String FSH = Gdx.files.internal("shaders/god_rays.frag").readString();

    private Texture depthTexture;
    private Camera camera;
    private Vector3 sunDirection = new Vector3(1, -1.5f, 1);
    public float exposure = 0.3f;
    public float decay = 0.95f;
    public float density = 0.8f;
    public float weight = 0.6f;

    public GodRaysEffect() {
        super(new ShaderProgram(VSH, FSH));
        if (!program.isCompiled()) Gdx.app.error("GodRays", program.getLog());
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(Texture depthTexture) { this.depthTexture = depthTexture; }
    public void setSunDirection(Vector3 dir) { this.sunDirection.set(dir); }

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

        if (camera != null) {
            Vector3 sunPosWorld = new Vector3(camera.position).sub(new Vector3(sunDirection).scl(1000f));
            Vector3 sunPosScreen = new Vector3(sunPosWorld);
            camera.project(sunPosScreen);

            float sunX = sunPosScreen.x / Gdx.graphics.getWidth();
            float sunY = sunPosScreen.y / Gdx.graphics.getHeight();

            if (sunPosScreen.z < 0) {
                sunX = -100f; sunY = -100f;
            }

            program.setUniformf("u_sunPos", sunX, sunY);
            program.setUniformf("u_farPlane", camera.far);
        }

        program.setUniformf("u_exposure", exposure);
        program.setUniformf("u_decay", decay);
        program.setUniformf("u_density", density);
        program.setUniformf("u_weight", weight);

        program.end();
        renderShader(context, dst);
    }
}