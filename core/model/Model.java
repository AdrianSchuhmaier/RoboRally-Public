package core.model;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import core.math.Axis;
import core.math.Matrix4f;
import core.math.Vector3f;
import core.model.Model;
import core.shader.ShaderProgram;
import core.texture.Texture;

/**
 * This Class holds a 3D model with reference to its VAO, textureIDs and with
 * loc/rot/scale data.
 * 
 * @author Adrian Schuhmaier
 *
 */
public class Model {

    /** how many textures the model can hold */
    protected final int MAX_TEXTURE_COUNT = 4;

    protected int vao;
    protected int vertexCount; // technically speaking its the indicesCount

    /**
     * if the modelMatrix was changed since the last updateModelMatrix() call
     */
    protected boolean modelMatrixChanged;
    /** the model matrix (=transformation matrix) of the model */
    protected Matrix4f modelMatrix;
    /** the position of the model's origin point in world space */
    protected Vector3f position;
    /** the euler rotation of the model around its origin in world space */
    protected Vector3f rotation;
    /** the scale of the model */
    protected Vector3f scale;

    /** the textures the model holds */
    private Texture[] textures;

    /**
     * Constructor, do not use it to create a Model. Use the ModelLoader to
     * create a Model.
     */
    public Model() {
        this.modelMatrix = new Matrix4f();
        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);
        this.scale = new Vector3f(1, 1, 1);
        this.modelMatrixChanged = true;
        this.textures = new Texture[MAX_TEXTURE_COUNT];
        Texture placeHolderTex = new Texture(0);
        for (int i = 0; i < MAX_TEXTURE_COUNT; i++) {
            textures[i] = placeHolderTex;
        }
    }

    /**
     * Renders the model to the currently bound Framebuffer with the Shader
     * currently in use.
     */
    public void render() {
        GL11.glDrawElements(GL11.GL_TRIANGLES, vertexCount, GL11.GL_UNSIGNED_INT, 0);
    }

    /**
     * Prepares a model for the rendering - can be called once before rendering
     * several model copies.
     * 
     * @return Consumer, that is executed directly before the rendering.
     */
    public Consumer<ShaderProgram> prepare() {
        GL30.glBindVertexArray(vao);
        bindTextures();
        Consumer<ShaderProgram> additionalUniforms = (shader) -> {
        };
        return additionalUniforms;
    }

    /**
     * Cleans up the context after rendering the model to not be accidentally
     * modified.
     */
    public void cleanUp() {
        GL30.glBindVertexArray(0);
    }

    /**
     * @return modelMatrix of the model
     */
    public Matrix4f getModelMatrix() {
        if (modelMatrixChanged) {
            updateModelMatrix();
        }
        return modelMatrix;
    }

    /**
     * Translates the model's position by the translation vector.
     * 
     * @param vector
     *            translation vector
     */
    public void translate(Vector3f vector) {
        this.position = this.position.add(vector);
        modelMatrixChanged = true;
    }

    /**
     * Sets the model's position to the new position.
     * 
     * @param vector
     *            new position
     */
    public void setPosition(Vector3f vector) {
        this.position.x = vector.x;
        this.position.y = vector.y;
        this.position.z = vector.z;
        modelMatrixChanged = true;
    }

    /**
     * Rotates the model by the given angle around the given axis.
     * 
     * @param angle
     *            how much the model is rotated
     * @param axis
     *            of the rotation
     */
    public void rotate(float angle, Axis axis) {
        switch (axis) {
        case X:
            rotation.x += angle;
            rotation.x %= 360;
            break;
        case Y:
            rotation.y += angle;
            rotation.y %= 360;
            break;
        case Z:
            rotation.z += angle;
            rotation.z %= 360;
            break;
        }
        modelMatrixChanged = true;
    }

    /**
     * Sets the model's rotation to the new rotation.
     * 
     * @param rotation
     *            new rotation
     */
    public void setRotation(Vector3f rotation) {
        this.rotation.x = rotation.x;
        this.rotation.y = rotation.y;
        this.rotation.z = rotation.z;
    }

    /**
     * Scales the model.
     * 
     * @param scale
     *            factor to scale by.
     */
    public void scale(float scale) {
        this.scale = this.scale.scale(scale);
        modelMatrixChanged = true;
    }

    /**
     * Scales the model.
     * 
     * @param scale
     *            value to change the scale with.
     */
    public void scaleAbs(float scale) {
        this.scale = this.scale.add(new Vector3f(scale, scale, scale));
        if (this.scale.x < 0) {
            this.scale = new Vector3f(0, 0, 0);
        }
        modelMatrixChanged = true;
    }

    /**
     * Sets the model's scale to the new scale.
     * 
     * @param scale
     *            the new scale
     */
    public void setScale(float scale) {
        this.scale.x = scale;
        this.scale.y = scale;
        this.scale.z = scale;
    }

    /**
     * Sets the model's scale to the new scale.
     * 
     * @param scale
     *            the new scale
     */
    public void setScale(Vector3f scale) {
        this.scale.x = scale.x;
        this.scale.y = scale.y;
        this.scale.z = scale.z;
    }

    /**
     * Generates a new updated modelMatrix.
     */
    private void updateModelMatrix() {
        Matrix4f modelMatrix = new Matrix4f();
        modelMatrix = modelMatrix.multiply(Matrix4f.translate(position.x, position.y, position.z));
        modelMatrix = modelMatrix.multiply(Matrix4f.rotate(rotation.x, 1, 0, 0));
        modelMatrix = modelMatrix.multiply(Matrix4f.rotate(rotation.y, 0, 1, 0));
        modelMatrix = modelMatrix.multiply(Matrix4f.rotate(rotation.z, 0, 0, 1));
        modelMatrix = modelMatrix.multiply(Matrix4f.scale(scale.x, scale.y, scale.z));
        this.modelMatrix = modelMatrix;
        modelMatrixChanged = false;
    }

    /**
     * Creates a new Model object with the same data references without
     * reloading and replicating into graphics memory.
     * 
     * @return new Model Object representing the same model
     */
    public Model copy() {
        Model copy = new Model();
        copy.vao = this.vao;
        copy.vertexCount = this.vertexCount;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Model other = (Model) obj;
        if (vao != other.vao)
            return false;
        return true;
    }

    /**
     * Sets the model texture at the given index to the given texture.
     * 
     * @param index
     *            of the texture
     * @param texture
     *            to set to the given index
     */
    public void setTexture(int index, Texture texture) {
        if (index < 0 || index >= MAX_TEXTURE_COUNT) {
            System.err.println("Can't set model texture with index " + index);
        }
        this.textures[index] = texture;
    }

    /**
     * 
     * @param index
     * @return
     */
    public Texture getTexture(int index) {
        if (index < 0 || index >= MAX_TEXTURE_COUNT) {
            System.err.println("Can't get model texture with index " + index);
        }
        return this.textures[index];
    }

    /**
     * 
     * @param args
     *            what textures to bind
     */
    public void bindTextures(int... args) {

        if (args.length == 0) {
            args = new int[] { 0, 1, 2, 3 };
        }

        for (int i : args) {
            if (i >= 0 && i < MAX_TEXTURE_COUNT) {
                GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);

                Texture tex = textures[i];

                GL11.glBindTexture(GL11.GL_TEXTURE_2D, tex.getTextureID());
            }
        }
    }

    public int getVao() {
        return vao;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public Texture[] getTextures() {
        return textures;
    }

    public Vector3f getPosition() {
        return new Vector3f(position.x, position.y, position.z);
    }

    public float getScale() {
        return scale.y;
    }
}
