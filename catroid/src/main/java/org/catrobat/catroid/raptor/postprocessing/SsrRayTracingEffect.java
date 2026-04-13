package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class SsrRayTracingEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = a_position;\n" +
            "}";

    private static final String FSH = Gdx.files.internal("shaders/raytracing_ssr.frag").readString();

    private Texture depthTexture;
    private Texture materialTexture;
    private Camera camera;

    private int steps = 20;
    private float reflectivity = 1.0f;
    private float edgeFade = 0.15f;
    private float thickness = 0.5f;
    private float maxDistance = 50.0f;
    private float stride = 0.5f;
    public Matrix4 tmpMat = new Matrix4();

    public SsrRayTracingEffect() {
        super(new ShaderProgram(VSH, FSH));
        if (!program.isCompiled()) {
            Gdx.app.error("SsrRayTracingEffect", "Shader error:\n" + program.getLog());
        }
    }

    public void setParams(int steps, float reflectivity, float thickness, float maxDistance, float stride, float edgeFade) {
        this.steps = steps;
        this.reflectivity = reflectivity;
        this.thickness = thickness;
        this.maxDistance = maxDistance;
        this.stride = stride;
        this.edgeFade = edgeFade;
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(Texture depthTexture) { this.depthTexture = depthTexture; }
    public void setMaterialTexture(Texture materialTexture) { this.materialTexture = materialTexture; }

    @Override
    public void rebind() {
        super.rebind();
        program.begin();
        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_materialTexture", 2);
        program.end();
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        VfxFrameBuffer src = buffers.getSrcBuffer();
        VfxFrameBuffer dst = buffers.getDstBuffer();

        program.begin();

        if (depthTexture != null) depthTexture.bind(1);
        if (materialTexture != null) materialTexture.bind(2);
        src.getTexture().bind(0);

        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_materialTexture", 2);

        if (camera != null) {
            program.setUniformMatrix("u_projectionMatrix", camera.projection);
            program.setUniformMatrix("u_invProjectionMatrix", tmpMat.set(camera.projection).inv());
            program.setUniformMatrix("u_viewMatrix", camera.view);
            program.setUniformf("u_farPlane", camera.far);
        }

        program.setUniformf("u_reflectivityMulti", reflectivity);
        program.setUniformf("u_edgeFade", edgeFade);
        program.setUniformf("u_thickness", thickness);
        program.setUniformf("u_maxDistance", maxDistance);
        program.setUniformf("u_stride", stride);
        program.setUniformi("u_maxSteps", steps);
        program.end();

        renderShader(context, dst);
    }
}