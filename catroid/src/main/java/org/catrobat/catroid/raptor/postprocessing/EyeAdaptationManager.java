
package org.catrobat.catroid.raptor.postprocessing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

import org.catrobat.catroid.raptor.PostProcessingData;

public class EyeAdaptationManager implements Disposable {

    private final ShaderProgram downsampleShader;
    private final ShaderProgram luminanceShader;
    private final FrameBuffer[] downsampleFBOs;
    private final Mesh quad;

    private float currentExposure = 1.0f;

    public EyeAdaptationManager() {
        String vert = "attribute vec4 a_position; attribute vec2 a_texCoord0; varying vec2 v_texCoords; void main() { gl_Position = a_position; v_texCoords = a_texCoord0; }";
        String lumFrag = "varying vec2 v_texCoords; uniform sampler2D u_texture0; void main() { vec3 c = texture2D(u_texture0, v_texCoords).rgb; float lum = dot(c, vec3(0.2126, 0.7152, 0.0722)); gl_FragColor = vec4(lum, lum, lum, 1.0); }";
        luminanceShader = new ShaderProgram(vert, lumFrag);
        if(!luminanceShader.isCompiled()) Gdx.app.error("EyeAdaptation", "Luminance shader failed: " + luminanceShader.getLog());

        String downFrag = "varying vec2 v_texCoords; uniform sampler2D u_texture0; uniform vec2 u_pixelSize; void main() { vec3 a = texture2D(u_texture0, v_texCoords + u_pixelSize * vec2(-0.5, 0.5)).rgb; vec3 b = texture2D(u_texture0, v_texCoords + u_pixelSize * vec2(0.5, 0.5)).rgb; vec3 c = texture2D(u_texture0, v_texCoords + u_pixelSize * vec2(-0.5, -0.5)).rgb; vec3 d = texture2D(u_texture0, v_texCoords + u_pixelSize * vec2(0.5, -0.5)).rgb; gl_FragColor = vec4((a+b+c+d)*0.25, 1.0); }";
        downsampleShader = new ShaderProgram(vert, downFrag);
        if(!downsampleShader.isCompiled()) Gdx.app.error("EyeAdaptation", "Downsample shader failed: " + downsampleShader.getLog());

        downsampleFBOs = new FrameBuffer[9];
        for(int i=0; i<downsampleFBOs.length; i++){
            int size = 1 << (downsampleFBOs.length - 1 - i);
            downsampleFBOs[i] = new FrameBuffer(Pixmap.Format.RGBA8888, size, size, false);
        }

        quad = new Mesh(true, 4, 0, new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"), new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
        quad.setVertices(new float[] {-1, -1, 0, 0, 1, -1, 1, 0, 1, 1, 1, 1, -1, 1, 0, 1});
    }

    public void update(float delta, Texture hdrSourceTexture, PostProcessingData.EyeAdaptation settings) {
        float avgLuminance = calculateAvgLuminance(hdrSourceTexture);


        float targetExposure = settings.targetLuminance / (avgLuminance + 0.001f);
        targetExposure = MathUtils.clamp(targetExposure, settings.minExposure, settings.maxExposure);
        currentExposure = MathUtils.lerp(currentExposure, targetExposure, delta * settings.speed);
    }

    private float calculateAvgLuminance(Texture source) {
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        Gdx.gl.glDisable(GL20.GL_BLEND);

        FrameBuffer firstFbo = downsampleFBOs[0];
        firstFbo.begin();
        luminanceShader.bind();
        source.bind(0);
        luminanceShader.setUniformi("u_texture0", 0);
        Gdx.gl.glViewport(0, 0, firstFbo.getWidth(), firstFbo.getHeight());
        quad.render(luminanceShader, GL20.GL_TRIANGLE_FAN, 0, 4);
        firstFbo.end();

        for(int i=1; i < downsampleFBOs.length; i++){
            FrameBuffer current = downsampleFBOs[i];
            FrameBuffer previous = downsampleFBOs[i-1];
            current.begin();
            downsampleShader.bind();
            previous.getColorBufferTexture().bind(0);
            downsampleShader.setUniformi("u_texture0", 0);
            downsampleShader.setUniformf("u_pixelSize", 1.0f / previous.getWidth(), 1.0f / previous.getHeight());
            Gdx.gl.glViewport(0, 0, current.getWidth(), current.getHeight());
            quad.render(downsampleShader, GL20.GL_TRIANGLE_FAN, 0, 4);
            current.end();
        }

        FrameBuffer finalFbo = downsampleFBOs[downsampleFBOs.length - 1];
        finalFbo.begin();
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, 1, 1, true);
        finalFbo.end();

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        return (pixels[0] & 0xff) / 255f;
    }

    public float getCurrentExposure() { return currentExposure; }

    @Override
    public void dispose() {
        luminanceShader.dispose();
        downsampleShader.dispose();
        for(FrameBuffer fbo : downsampleFBOs) fbo.dispose();
        quad.dispose();
    }
}