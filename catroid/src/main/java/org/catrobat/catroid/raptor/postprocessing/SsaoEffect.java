package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;

public class SsaoEffect extends ShaderVfxEffect implements ChainVfxEffect {

    private static final String VSH = "attribute vec4 a_position;\n" +
            "attribute vec2 a_texCoord0;\n" +
            "varying vec2 v_texCoords;\n" +
            "void main() {\n" +
            "    v_texCoords = a_texCoord0;\n" +
            "    gl_Position = a_position;\n" +
            "}";

    private static final String FSH = Gdx.files.internal("shaders/ssao.frag").readString();

    private Texture depthTexture;
    private Texture noiseTexture;
    private Camera camera;
    private float[] kernel = new float[16 * 3];

    private float radius = 0.5f;
    private float intensity = 1.0f;
    private float bias = 0.05f;
    public Matrix4 tmpMat = new Matrix4();

    public SsaoEffect() {
        super(new ShaderProgram(VSH, FSH));
        createNoiseTexture();
        generateKernel();
    }

    public void setCamera(Camera camera) { this.camera = camera; }
    public void setDepthTexture(Texture depthTexture) { this.depthTexture = depthTexture; }

    public void setParams(float radius, float intensity, float bias) {
        this.radius = radius;
        this.intensity = intensity;
        this.bias = bias;
    }

    private void generateKernel() {
        for (int i = 0; i < 16; i++) {
            Vector3 v = new Vector3(MathUtils.random()*2-1, MathUtils.random()*2-1, MathUtils.random()).nor();
            float scale = (float)i / 16f;
            scale = MathUtils.lerp(0.1f, 1.0f, scale * scale);
            v.scl(scale);
            kernel[i*3] = v.x; kernel[i*3+1] = v.y; kernel[i*3+2] = v.z;
        }
    }

    private void createNoiseTexture() {
        Pixmap pixmap = new Pixmap(4, 4, Pixmap.Format.RGB888);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                pixmap.setColor(MathUtils.random(), MathUtils.random(), 0, 1);
                pixmap.drawPixel(x, y);
            }
        }
        noiseTexture = new Texture(pixmap);
        noiseTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        noiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        pixmap.dispose();
    }

    @Override
    public void rebind() {
        super.rebind();
        program.begin();
        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_noiseTexture", 2);
        program.end();
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        VfxFrameBuffer src = buffers.getSrcBuffer();
        VfxFrameBuffer dst = buffers.getDstBuffer();

        program.begin();


        if (noiseTexture != null) {
            noiseTexture.bind(2);
        }
        if (depthTexture != null) {
            depthTexture.bind(1);
        }
        src.getTexture().bind(0);


        program.setUniformi("u_texture0", 0);
        program.setUniformi("u_depthTexture", 1);
        program.setUniformi("u_noiseTexture", 2);

        if (camera != null) {
            program.setUniformMatrix("u_projectionMatrix", camera.projection);
            program.setUniformMatrix("u_invProjectionMatrix", tmpMat.set(camera.projection).inv());
            program.setUniformMatrix("u_viewMatrix", camera.view);
            program.setUniformf("u_farPlane", camera.far);
        }

        for(int i = 0; i < 16; i++) {
            program.setUniformf("u_kernel[" + i + "]", kernel[i*3], kernel[i*3+1], kernel[i*3+2]);
        }

        program.setUniformf("u_noiseScale", (float)Gdx.graphics.getWidth() / 4f, (float)Gdx.graphics.getHeight() / 4f);
        program.setUniformf("u_radius", radius);
        program.setUniformf("u_intensity", intensity);
        program.setUniformf("u_bias", bias);

        program.end();
        renderShader(context, dst);
    }
}