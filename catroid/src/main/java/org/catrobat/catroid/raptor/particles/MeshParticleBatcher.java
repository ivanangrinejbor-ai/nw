package org.catrobat.catroid.raptor.particles;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;

import org.catrobat.catroid.raptor.ParticleSystem3DComponent;


public class MeshParticleBatcher implements Disposable {

    private static final int MAX_VERTS_PER_PARTICLE = 128;
    private static final int FLOATS_PER_VERT = 7;


    private float[] srcPositions;
    private float[] srcNormals;
    private float[] srcUVs;
    private short[] srcIndices;
    private int srcVertCount;
    private int srcIdxCount;


    private Mesh batchedMesh;
    private float[] batchedVertices;
    private short[] batchedIndices;
    private int maxParticles;

    private ShaderProgram shader;
    private boolean glInitialized = false;


    private final Vector3 tmpV = new Vector3();
    private final Quaternion tmpQ = new Quaternion();
    private final Matrix4 tmpMat = new Matrix4();

    public MeshParticleBatcher(int maxParticles) {
        this.maxParticles = maxParticles;
    }





    public void loadPrimitive(ParticleSystem3DComponent.MeshType type) {
        ModelBuilder mb = new ModelBuilder();
        Model model;

        com.badlogic.gdx.graphics.g3d.Material mat = new com.badlogic.gdx.graphics.g3d.Material();
        long attrs = VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal |
                VertexAttributes.Usage.TextureCoordinates;

        switch (type) {
            case SPHERE_LOW:
                model = mb.createSphere(1f, 1f, 1f, 6, 6, mat, attrs);
                break;
            case CYLINDER_LOW:
                model = mb.createCylinder(1f, 1f, 1f, 8, mat, attrs);
                break;
            case CUBE:
            default:
                model = mb.createBox(1f, 1f, 1f, mat, attrs);
                break;
        }

        extractMeshData(model);
        model.dispose();


        invalidateGL();
    }


    public void loadFromModel(Model model) {
        if (model == null) {
            loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
            return;
        }
        extractMeshData(model);


        invalidateGL();
    }


    private void invalidateGL() {
        if (batchedMesh != null) {
            batchedMesh.dispose();
            batchedMesh = null;
        }
        batchedVertices = null;
        batchedIndices = null;
        glInitialized = false;
    }

    private void extractMeshData(Model model) {
        Mesh sourceMesh = null;
        MeshPart sourcePart = null;


        sourcePart = findFirstMeshPart(model.nodes);

        if (sourcePart != null) {
            sourceMesh = sourcePart.mesh;
        }


        if (sourceMesh == null && model.meshParts != null && model.meshParts.size > 0) {
            sourcePart = model.meshParts.get(0);
            sourceMesh = sourcePart.mesh;
        }


        if (sourceMesh == null && model.meshes != null && model.meshes.size > 0) {
            sourceMesh = model.meshes.get(0);

            sourcePart = new MeshPart();
            sourcePart.mesh = sourceMesh;
            sourcePart.offset = 0;
            sourcePart.size = sourceMesh.getNumIndices();
            sourcePart.primitiveType = GL20.GL_TRIANGLES;
        }

        if (sourceMesh == null) {
            Gdx.app.error("MeshBatcher", "No mesh found in model, falling back to cube");
            loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
            return;
        }

        Gdx.app.log("MeshBatcher", "Found mesh: " + sourceMesh.getNumVertices() + " verts, "
                + sourceMesh.getNumIndices() + " indices");


        int vertexSize = sourceMesh.getVertexSize() / 4;
        float[] allVerts = new float[sourceMesh.getNumVertices() * vertexSize];
        sourceMesh.getVertices(allVerts);


        int totalIndices = sourceMesh.getNumIndices();
        short[] allIndices;

        if (totalIndices > 0) {
            allIndices = new short[totalIndices];
            sourceMesh.getIndices(allIndices);
        } else {

            allIndices = new short[sourceMesh.getNumVertices()];
            for (int i = 0; i < allIndices.length; i++) {
                allIndices[i] = (short) i;
            }
            totalIndices = allIndices.length;
        }


        VertexAttribute posAttr = sourceMesh.getVertexAttribute(VertexAttributes.Usage.Position);
        VertexAttribute normAttr = sourceMesh.getVertexAttribute(VertexAttributes.Usage.Normal);
        VertexAttribute uvAttr = sourceMesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates);

        int posOffset = posAttr != null ? posAttr.offset / 4 : -1;
        int normOffset = normAttr != null ? normAttr.offset / 4 : -1;
        int uvOffset = uvAttr != null ? uvAttr.offset / 4 : -1;

        if (posOffset < 0) {
            Gdx.app.error("MeshBatcher", "Mesh has no position attribute!");
            loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);
            return;
        }


        srcVertCount = Math.min(sourceMesh.getNumVertices(), MAX_VERTS_PER_PARTICLE);


        int idxOffset = (sourcePart != null && sourcePart.offset >= 0) ? sourcePart.offset : 0;
        int idxSize = (sourcePart != null && sourcePart.size > 0) ? sourcePart.size : totalIndices;


        if (idxOffset + idxSize > totalIndices) {
            idxSize = totalIndices - idxOffset;
        }

        srcIdxCount = idxSize;

        srcPositions = new float[srcVertCount * 3];
        srcNormals = new float[srcVertCount * 3];
        srcUVs = new float[srcVertCount * 2];
        srcIndices = new short[srcIdxCount];


        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < srcVertCount; i++) {
            int base = i * vertexSize;
            float vx = allVerts[base + posOffset];
            float vy = allVerts[base + posOffset + 1];
            float vz = allVerts[base + posOffset + 2];

            srcPositions[i * 3] = vx;
            srcPositions[i * 3 + 1] = vy;
            srcPositions[i * 3 + 2] = vz;

            if (vx < minX) minX = vx; if (vx > maxX) maxX = vx;
            if (vy < minY) minY = vy; if (vy > maxY) maxY = vy;
            if (vz < minZ) minZ = vz; if (vz > maxZ) maxZ = vz;

            if (normOffset >= 0) {
                srcNormals[i * 3] = allVerts[base + normOffset];
                srcNormals[i * 3 + 1] = allVerts[base + normOffset + 1];
                srcNormals[i * 3 + 2] = allVerts[base + normOffset + 2];
            } else {
                srcNormals[i * 3 + 1] = 1f;
            }
            if (uvOffset >= 0) {
                srcUVs[i * 2] = allVerts[base + uvOffset];
                srcUVs[i * 2 + 1] = allVerts[base + uvOffset + 1];
            }
        }


        float sizeX = maxX - minX, sizeY = maxY - minY, sizeZ = maxZ - minZ;
        float maxDim = Math.max(sizeX, Math.max(sizeY, sizeZ));
        if (maxDim > 0.001f) {
            float scale = 1.0f / maxDim;
            float cx = (minX + maxX) * 0.5f;
            float cy = (minY + maxY) * 0.5f;
            float cz = (minZ + maxZ) * 0.5f;
            for (int i = 0; i < srcVertCount; i++) {
                srcPositions[i * 3]     = (srcPositions[i * 3]     - cx) * scale;
                srcPositions[i * 3 + 1] = (srcPositions[i * 3 + 1] - cy) * scale;
                srcPositions[i * 3 + 2] = (srcPositions[i * 3 + 2] - cz) * scale;
            }
        }


        for (int i = 0; i < srcIdxCount; i++) {
            int idx = allIndices[idxOffset + i] & 0xFFFF;
            srcIndices[i] = (short) Math.min(idx, srcVertCount - 1);
        }

        Gdx.app.log("MeshBatcher", "Extracted: " + srcVertCount + " verts, " + srcIdxCount
                + " indices, normalized to unit size");
    }


    private MeshPart findFirstMeshPart(Iterable<Node> nodes) {
        for (Node node : nodes) {
            if (node.parts != null && node.parts.size > 0) {
                for (NodePart np : node.parts) {
                    if (np.meshPart != null && np.meshPart.mesh != null
                            && np.meshPart.mesh.getNumVertices() > 0) {
                        return np.meshPart;
                    }
                }
            }
            if (node.hasChildren()) {
                MeshPart found = findFirstMeshPart(node.getChildren());
                if (found != null) return found;
            }
        }
        return null;
    }



    public void ensureGLReady() {
        if (glInitialized) return;
        if (srcPositions == null) loadPrimitive(ParticleSystem3DComponent.MeshType.CUBE);

        int totalVerts = maxParticles * srcVertCount;
        int totalIndices = maxParticles * srcIdxCount;


        if (totalVerts > 65000) {
            maxParticles = 65000 / srcVertCount;
            totalVerts = maxParticles * srcVertCount;
            totalIndices = maxParticles * srcIdxCount;
        }

        batchedVertices = new float[totalVerts * FLOATS_PER_VERT];
        batchedIndices = new short[totalIndices];

        batchedMesh = new Mesh(false, totalVerts, totalIndices,
                new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
                new VertexAttribute(VertexAttributes.Usage.Normal, 3, "a_normal"),
                new VertexAttribute(VertexAttributes.Usage.ColorPacked, 4, "a_color"));

        buildShader();
        glInitialized = true;
    }

    private void buildShader() {
        String vert =
                "attribute vec3 a_position;\n" +
                        "attribute vec3 a_normal;\n" +
                        "attribute vec4 a_color;\n" +
                        "uniform mat4 u_projViewTrans;\n" +
                        "uniform vec3 u_lightDir;\n" +
                        "uniform vec3 u_ambientColor;\n" +
                        "varying vec4 v_color;\n" +
                        "void main() {\n" +
                        "    float NdotL = max(dot(a_normal, -u_lightDir), 0.0);\n" +
                        "    vec3 diffuse = a_color.rgb * (u_ambientColor + vec3(NdotL));\n" +
                        "    v_color = vec4(diffuse, a_color.a);\n" +
                        "    gl_Position = u_projViewTrans * vec4(a_position, 1.0);\n" +
                        "}\n";

        String frag =
                "#ifdef GL_ES\n" +
                        "precision mediump float;\n" +
                        "#endif\n" +
                        "varying vec4 v_color;\n" +
                        "void main() {\n" +
                        "    gl_FragColor = v_color;\n" +
                        "    if (gl_FragColor.a < 0.01) discard;\n" +
                        "}\n";

        shader = new ShaderProgram(vert, frag);
        if (!shader.isCompiled()) {
            Gdx.app.error("MeshBatcherShader", shader.getLog());
            shader = null;
        }
    }





    public void batch(int count,
                      float[] posX, float[] posY, float[] posZ,
                      float[] size,
                      float[] rotX, float[] rotY, float[] rotZ,
                      float[] colorR, float[] colorG, float[] colorB, float[] colorA,
                      float[] velX, float[] velY, float[] velZ,
                      boolean alignToVelocity) {

        if (!glInitialized || srcPositions == null) return;

        int particlesToRender = Math.min(count, maxParticles);
        int vOffset = 0;
        int iOffset = 0;

        for (int p = 0; p < particlesToRender; p++) {
            float s = size[p];


            if (alignToVelocity && (velX[p] != 0 || velY[p] != 0 || velZ[p] != 0)) {
                tmpV.set(velX[p], velY[p], velZ[p]).nor();
                tmpQ.setFromCross(Vector3.Z, tmpV);
            } else {

                tmpQ.setEulerAngles(rotY[p], rotX[p], rotZ[p]);
            }

            float px = posX[p], py = posY[p], pz = posZ[p];
            float packedColor = Color.toFloatBits(colorR[p], colorG[p], colorB[p], colorA[p]);


            float qx = tmpQ.x, qy = tmpQ.y, qz = tmpQ.z, qw = tmpQ.w;
            float xx = qx * qx, yy = qy * qy, zz = qz * qz;
            float xy = qx * qy, xz = qx * qz, yz = qy * qz;
            float wx = qw * qx, wy = qw * qy, wz = qw * qz;

            float r00 = 1 - 2*(yy+zz), r01 = 2*(xy-wz), r02 = 2*(xz+wy);
            float r10 = 2*(xy+wz), r11 = 1 - 2*(xx+zz), r12 = 2*(yz-wx);
            float r20 = 2*(xz-wy), r21 = 2*(yz+wx), r22 = 1 - 2*(xx+yy);

            int vertBase = p * srcVertCount;

            for (int v = 0; v < srcVertCount; v++) {
                float lx = srcPositions[v*3]   * s;
                float ly = srcPositions[v*3+1] * s;
                float lz = srcPositions[v*3+2] * s;


                float wx2 = r00*lx + r01*ly + r02*lz + px;
                float wy2 = r10*lx + r11*ly + r12*lz + py;
                float wz2 = r20*lx + r21*ly + r22*lz + pz;


                float nx = srcNormals[v*3], ny = srcNormals[v*3+1], nz = srcNormals[v*3+2];
                float wnx = r00*nx + r01*ny + r02*nz;
                float wny = r10*nx + r11*ny + r12*nz;
                float wnz = r20*nx + r21*ny + r22*nz;

                batchedVertices[vOffset++] = wx2;
                batchedVertices[vOffset++] = wy2;
                batchedVertices[vOffset++] = wz2;
                batchedVertices[vOffset++] = wnx;
                batchedVertices[vOffset++] = wny;
                batchedVertices[vOffset++] = wnz;
                batchedVertices[vOffset++] = packedColor;
            }

            for (int i = 0; i < srcIdxCount; i++) {
                batchedIndices[iOffset++] = (short)(srcIndices[i] + vertBase);
            }
        }

        this.lastVertOffset = vOffset;
        this.lastIdxOffset = iOffset;
        this.lastParticleCount = particlesToRender;
    }

    private int lastVertOffset = 0;
    private int lastIdxOffset = 0;
    private int lastParticleCount = 0;


    public void render(Camera camera, Vector3 lightDir, Color ambientColor,
                       boolean additive) {
        if (!glInitialized || shader == null || lastParticleCount == 0) return;

        batchedMesh.setVertices(batchedVertices, 0, lastVertOffset);
        batchedMesh.setIndices(batchedIndices, 0, lastIdxOffset);

        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);

        if (additive) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            Gdx.gl.glDepthMask(false);
        } else {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            Gdx.gl.glDepthMask(true);
        }

        shader.begin();
        shader.setUniformMatrix("u_projViewTrans", camera.combined);
        shader.setUniformf("u_lightDir", lightDir.x, lightDir.y, lightDir.z);
        shader.setUniformf("u_ambientColor", ambientColor.r, ambientColor.g, ambientColor.b);

        batchedMesh.render(shader, GL20.GL_TRIANGLES, 0, lastIdxOffset);
        shader.end();

        if (additive) {
            Gdx.gl.glDepthMask(true);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        }
    }

    @Override
    public void dispose() {
        if (batchedMesh != null) { batchedMesh.dispose(); batchedMesh = null; }
        if (shader != null) { shader.dispose(); shader = null; }
        glInitialized = false;
    }
}