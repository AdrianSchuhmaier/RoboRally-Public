package core.model;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import core.model.Model;

/**
 * Assimp ModelLoader
 * 
 * @author Adrian Schuhmaier
 */
public class ModelLoader {

    public static boolean rightHanded = true;

    private static HashMap<String, Model> models = new HashMap<>();

    
    /**
     * clears all loaded models
     */
    public static void clear(){
        models.clear();
    }
    
    /**
     * loads in the model at the given path, uploads data to OpenGL and returns
     * the Model containing references
     * 
     * @param path
     *            to the model file
     * @return Model with reference to its properties loaded into the graphics
     *         memory
     */
    public static Model loadModel(String path) {
        if (models.keySet().contains(path)) {
            return models.get(path);
        }
        System.out.println(path + " loading");

        Model model = new Model();

        AIScene scene = Assimp.aiImportFile(path,
                Assimp.aiProcess_Triangulate | Assimp.aiProcess_CalcTangentSpace | Assimp.aiProcess_GenSmoothNormals);

        if (scene == null) {
            System.err.println(path + " file could not be loaded");
            System.exit(-1);
        }

        // get the mesh from the scene
        AIMesh mesh = new AIMesh(scene.mMeshes().getByteBuffer(1288));
        model.vertexCount = mesh.mNumFaces() * 3;

        int vao, vbo, ebo;

        // generate and bind the vao
        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        model.vao = vao;

        // load indices
        ebo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ebo);
        // get buffer data
        IntBuffer indicesBuffer = getIndices(mesh);
        // load in buffer data
        GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);

        MemoryUtil.memFree(indicesBuffer);

        // load vertices
        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // get buffer data
        FloatBuffer positionsBuffer = getPositions(mesh);
        FloatBuffer normalsBuffer = getNormals(mesh);
        FloatBuffer tangentsBuffer = getTangents(mesh);
        FloatBuffer texCoordBuffer = getTexCoords(mesh);

        if (positionsBuffer == null) {
            System.err.println(path + " contains no position data");
            System.exit(-1);
        }

        if (normalsBuffer == null) {
            System.out.println(path + " contains no normal data");
            normalsBuffer = MemoryUtil.memAllocFloat(mesh.mNumVertices() * 3);
        }

        if (tangentsBuffer == null) {
            System.out.println(path + " contains no tangents data");
            tangentsBuffer = MemoryUtil.memAllocFloat(mesh.mNumVertices() * 3);
        }

        if (texCoordBuffer == null) {
            System.out.println(path + " contains no texCoord data");
            texCoordBuffer = MemoryUtil.memAllocFloat(mesh.mNumVertices() * 2);
        }

        int sizeOfVec3BufferSection = mesh.mNumVertices() * 12; // 3 floats with
                                                                // 4 bytes each

        FloatBuffer allBufferData = MemoryUtil.memAllocFloat(mesh.mNumVertices() * 11);
        // allocate vertex buffer
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, allBufferData, GL15.GL_STATIC_DRAW);

        // write into the vertex buffer
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, 0, positionsBuffer);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, sizeOfVec3BufferSection, normalsBuffer);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, sizeOfVec3BufferSection * 2, tangentsBuffer);
        GL15.glBufferSubData(GL15.GL_ARRAY_BUFFER, sizeOfVec3BufferSection * 3, texCoordBuffer);

        MemoryUtil.memFree(allBufferData);
        MemoryUtil.memFree(positionsBuffer);
        MemoryUtil.memFree(normalsBuffer);
        MemoryUtil.memFree(tangentsBuffer);
        MemoryUtil.memFree(texCoordBuffer);

        // setup attrib locations
        // 0 : positions
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
        GL20.glEnableVertexAttribArray(0);
        // 1 : normals
        GL20.glVertexAttribPointer(1, 3, GL11.GL_FLOAT, false, 12, sizeOfVec3BufferSection);
        GL20.glEnableVertexAttribArray(1);
        // 2 : tangents
        GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 12, sizeOfVec3BufferSection * 2);
        GL20.glEnableVertexAttribArray(2);
        // 3 : texCoords
        GL20.glVertexAttribPointer(3, 2, GL11.GL_FLOAT, false, 8, sizeOfVec3BufferSection * 3);
        GL20.glEnableVertexAttribArray(3);

        // unbind the vao
        GL30.glBindVertexArray(0);
        // unbind the vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        // unbind the ebo
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

        System.out.println(path + " loading successful");
        System.out.println(path + " vertex count: " + model.vertexCount);
        models.put(path, model);
        return model;
    }

    /**
     * Loads the indices data into the element array buffer
     * 
     * @param mesh
     *            that holds the indices
     * @return if the loading process was successful
     */
    private static IntBuffer getIndices(AIMesh mesh) {

        int faceCount = mesh.mNumFaces();
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(faceCount * 3);

        AIFace.Buffer faceBuffer = mesh.mFaces();

        for (int i = 0; i < faceCount; i++) {

            AIFace face = faceBuffer.get();

            indicesBuffer.put(face.mIndices());

        }

        indicesBuffer.flip();

        return indicesBuffer;

    }

    /**
     * Returns a buffer containing the mesh vertex positions.
     * 
     * @param mesh
     *            that holds the vertices
     * @return buffer
     */
    private static FloatBuffer getPositions(AIMesh mesh) {

        // get the number of vertices
        int vertexCount = mesh.mNumVertices();

        // allocate a FloatBuffer for storing position data
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount * 3);

        // get the vertices from the mesh
        AIVector3D.Buffer positionsBuffer = mesh.mVertices();

        // check if there are any positions
        if (positionsBuffer == null) {
            return null;
        }

        for (int i = 0; i < vertexCount; i++) {

            // get the current vertex position
            AIVector3D vector = positionsBuffer.get();

            float[] position;
            // store the position in a usable format
            if (!rightHanded) {
                position = new float[] { vector.x(), vector.z(), vector.y() };
            } else {
                position = new float[] { vector.x(), vector.y(), vector.z() };
            }
            // add the positions to the positionsBuffer
            buffer.put(position);
        }

        buffer.flip();

        return buffer;
    }

    /**
     * Returns a buffer containing the mesh vertex normals.
     * 
     * @param mesh
     *            that holds the vertices
     * @return buffer
     */
    private static FloatBuffer getNormals(AIMesh mesh) {
        // get the number of vertices
        int vertexCount = mesh.mNumVertices(); // every Vertex has 1 normal

        // allocate a FloatBuffer for storing normals data
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount * 3);

        // get the normals from the mesh
        AIVector3D.Buffer normalsBuffer = mesh.mNormals();

        // check if there are any normals
        if (normalsBuffer.remaining() == 0) {
            return null;
        }

        for (int i = 0; i < vertexCount; i++) {

            // get the current normal vector
            AIVector3D vector = normalsBuffer.get();

            float[] normal;
            // store in a usable format
            if (!rightHanded) {
                normal = new float[] { vector.x(), vector.z(), vector.y() };
            } else {
                normal = new float[] { vector.x(), vector.y(), vector.z() };
            }

            // add the positions to the positionsBuffer
            buffer.put(normal);
        }

        buffer.flip();

        return buffer;
    }

    /**
     * Returns a buffer containing the mesh vertex tangents.
     * 
     * @param mesh
     *            that holds the vertices
     * @return buffer
     */
    private static FloatBuffer getTangents(AIMesh mesh) {
        // get the number of vertices
        int vertexCount = mesh.mNumVertices(); // every vertex has 1 tangent
                                               // vector

        // allocate a FloatBuffer for storing tangents data
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount * 3);

        // get the normals from the mesh
        AIVector3D.Buffer tangentBuffer = mesh.mTangents();

        // check if there are any tangents
        if (tangentBuffer == null || tangentBuffer.remaining() == 0) {
            return null;
        }

        for (int i = 0; i < vertexCount; i++) {

            // get the current normal vector
            AIVector3D vector = tangentBuffer.get();

            float[] tangent;
            // store in a usable format
            if (!rightHanded) {
                tangent = new float[] { vector.x(), vector.z(), vector.y() };
            } else {
                tangent = new float[] { vector.x(), vector.y(), vector.z() };
            }

            // add the tangents to the tangentsBuffer
            buffer.put(tangent);
        }

        buffer.flip();

        return buffer;
    }

    /**
     * Returns a buffer containing the mesh vertex texture coordinates.
     * 
     * @param mesh
     *            that holds the vertices
     * @return buffer
     */
    private static FloatBuffer getTexCoords(AIMesh mesh) {

        // get the number of vertices
        int vertexCount = mesh.mNumVertices();

        // allocate a FloatBuffer for storing texCoord data
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertexCount * 2);

        // get the texCoords from the mesh
        AIVector3D.Buffer texCoordsBuffer = mesh.mTextureCoords(0);

        // check if there are any texCoords
        if (texCoordsBuffer == null) {
            return null;
        }

        for (int i = 0; i < vertexCount; i++) {

            // get the current texCoord as a vector
            AIVector3D vector = texCoordsBuffer.get();

            buffer.put(new float[] { vector.x(), -vector.y() });
        }

        buffer.flip();

        return buffer;
    }

}
