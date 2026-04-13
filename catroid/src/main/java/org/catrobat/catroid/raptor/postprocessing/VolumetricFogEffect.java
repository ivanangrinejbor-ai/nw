package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class VolumetricFogEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = a_position;\n" +
            "}";

    private static final String FSH = Gdx.files.internal("shaders/volumetric_fog.frag").readString();

    private Texture depthTexture;
    private Texture shadowTexture;
    private Camera camera;

    private final Matrix4 lightProjView = new Matrix4();
    private final Vector3 lightDir = new Vector3(1, -1, 1).nor();
    private final Color lightColor = new Color(Color.WHITE);

    private float density = 0.5f;
    private float scattering = 0.6f;
    private int steps = 30;
    private float maxDistance = 150.0f;

    public VolumetricFogEffect() {
        super(new ShaderProgram(VSH, FSH));
        if (!program.isCompiled()) {
            Gdx.app.error("VolumetricFog", "Shader error:\n" + program.getLog());
        }
    }

    public void setParams(float density, float scattering, int steps, float maxDistance) {
        this.density = density;
        this.scattering = scattering;
        this.steps = steps;
        this.maxDistance = maxDistance;
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(Texture depthTexture) { this.depthTexture = depthTexture; }

    public void setShadowMap(Texture shadowTexture, Matrix4 lightCombined) {
        this.shadowTexture = shadowTexture;
        this.lightProjView.set(lightCombined);
    }

    public void setLightParams(Vector3 dir, Color color) {
        this.lightDir.set(dir).nor();
        this.lightColor.set(color);
    }

    @Override
    public void rebind() {
        super.rebind();
        program.begin();
        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_shadowTexture", 2);
        program.end();
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        VfxFrameBuffer src = buffers.getSrcBuffer();
        VfxFrameBuffer dst = buffers.getDstBuffer();

        program.begin();

        if (depthTexture != null) depthTexture.bind(1);
        if (shadowTexture != null) shadowTexture.bind(2);
        src.getTexture().bind(0);

        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_shadowTexture", 2);

        if (camera != null) {
            program.setUniformMatrix("u_invProjViewMatrix", camera.invProjectionView);
            program.setUniformf("u_cameraPos", camera.position);
            program.setUniformf("u_farPlane", camera.far);
        }

        program.setUniformMatrix("u_lightProjView", lightProjView);
        program.setUniformf("u_lightDir", lightDir);
        program.setUniformf("u_lightColor", lightColor.r, lightColor.g, lightColor.b);

        program.setUniformf("u_density", density);
        program.setUniformf("u_scattering", scattering);
        program.setUniformf("u_maxDistance", maxDistance);
        program.setUniformi("u_steps", steps);

        program.end();

        renderShader(context, dst);
    }
}