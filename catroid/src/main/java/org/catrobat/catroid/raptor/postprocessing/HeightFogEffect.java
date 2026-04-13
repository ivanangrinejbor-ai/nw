package org.catrobat.catroid.raptor.postprocessing;

import android.opengl.Matrix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class HeightFogEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = a_position;\n" +
            "}";

    private static final String FSH = Gdx.files.internal("shaders/height_fog.frag").readString();

    private com.badlogic.gdx.graphics.Texture depthTexture;
    private Camera camera;
    public Color fogColor = new Color(0.5f, 0.6f, 0.7f, 1.0f);
    public float fogDensity = 0.01f;
    public float heightFalloff = 0.05f;
    public float fogHeight = 0.0f;

    public Matrix4 tmpMat = new Matrix4();
    public Matrix4 tmpMat2 = new Matrix4();


    public HeightFogEffect() {
        super(new ShaderProgram(VSH, FSH));
        if (!program.isCompiled()) Gdx.app.error("HeightFog", program.getLog());
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(com.badlogic.gdx.graphics.Texture depthTexture) { this.depthTexture = depthTexture; }

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

        if (camera != null) {
            program.setUniformMatrix("u_invProjection", tmpMat.set(camera.projection).inv());
            program.setUniformMatrix("u_invView", tmpMat2.set(camera.view).inv());

            program.setUniformf("u_cameraPos", camera.position);
            program.setUniformf("u_farPlane", camera.far);
        }

        program.setUniformf("u_fogColor", fogColor.r, fogColor.g, fogColor.b);
        program.setUniformf("u_fogDensity", fogDensity);
        program.setUniformf("u_heightFalloff", heightFalloff);
        program.setUniformf("u_fogHeight", fogHeight);

        program.end();
        renderShader(context, dst);
    }
}