package core.shader;

import java.nio.FloatBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import core.math.Vector2f;
import core.shader.ShaderProgram;
import core.window.Window;

/**
 * 
 * This class holds a shaderProgram that renders textured quads.
 * 
 * @author Adrian Schuhmaier
 *
 */
public class ScreenShaderProgram extends ShaderProgram {

    /** vertex positions of a quad */
    private final float[] positions = new float[] { 1, 1, 1, -1, -1, 1, 1, -1, -1, -1, -1, 1 };
    /** uv coordinates of a quad */
    private final float[] texCoords = new float[] { 1, 1, 1, 0, 0, 1, 1, 0, 0, 0, 0, 1 };
    /** vaoID of the screen quad in OpenGL */
    private int vao;
    /** vaoID of the alternative screen quad in OpenGL*/
    private int altVao;
    /** vboID of the alternative screen quad in OpenGL */
    private int altVbo;

    /**
     * 
     * @param vertexShaderName
     *            path to a screen quad vertex shader
     * @param fragmentShaderName
     *            path to a screen quad fragment shader (e.g. post-processing
     *            shader)
     */
    public ScreenShaderProgram(String vertexShaderName, String fragmentShaderName) {
        super(vertexShaderName, fragmentShaderName);

        this.vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // buffer the data
        FloatBuffer data = MemoryUtil.memAllocFloat(24);
        data.put(positions).put(texCoords).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);

        // positions attrib location
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(0);
        // texCoords attrib location
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 8, 48);
        GL20.glEnableVertexAttribArray(1);

        // unbind the vao
        GL30.glBindVertexArray(0);
        // unbind the vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        this.altVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(altVao);
        this.altVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, altVbo);

        // positions attrib location
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(0);
        // texCoords attrib location
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 8, 48);
        GL20.glEnableVertexAttribArray(1);

        // unbind the vao
        GL30.glBindVertexArray(0);
        // unbind the vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        // shader needs to be in use to set uniforms
        this.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(this.getProgramID(), "color"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(this.getProgramID(), "glow"), 1);
    }

    /**
     * 
     * @param vertexShaderName
     *            path to a screen quad vertex shader
     * @param geometryShaderName
     *            not used in this implementation
     * @param fragmentShaderName
     *            path to a screen quad fragment shader (e.g. post-processing
     *            shader)
     */
    public ScreenShaderProgram(String vertexShaderName, String geometryShaderName, String fragmentShaderName) {
        super(vertexShaderName, geometryShaderName, fragmentShaderName);

        this.vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        // buffer the data
        FloatBuffer data = MemoryUtil.memAllocFloat(24);
        data.put(positions).put(texCoords).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);

        // positions attrib location
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(0);
        // texCoords attrib location
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 8, 48);
        GL20.glEnableVertexAttribArray(1);

        // unbind the vao
        GL30.glBindVertexArray(0);
        // unbind the vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        this.altVao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(altVao);
        this.altVbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, altVbo);

        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);

        // positions attrib location
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(0);
        // texCoords attrib location
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 8, 48);
        GL20.glEnableVertexAttribArray(1);

        // unbind the vao
        GL30.glBindVertexArray(0);
        // unbind the vbo
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        MemoryUtil.memFree(data);

        // shader needs to be in use to set uniforms
        this.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(this.getProgramID(), "color"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(this.getProgramID(), "glow"), 1);
    }

    /**
     * Renders a texture on the screen.
     * 
     * @param textureBufferID
     *            the ID of the texture to render
     */
    final public void renderTexture(int textureBufferID) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBufferID);

        render();
    }

    /**
     * 
     * @param textureBufferID
     *            the ID of the texture to render
     * @param positions
     *            [Vector2f(xPos, yPos), Vector2f(width, height)]
     */
    final public void renderTexture(int textureBufferID, Vector2f[] positions) {
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureBufferID);

        render(positions);
    }

    /**
     * Renders fullscreen.
     */
    public void render() {
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL30.glBindVertexArray(this.vao);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GL30.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }

    /**
     * Renders to the given position.
     * 
     * @param positions
     *            [Vector2f(xPos, yPos), Vector2f(width, height)]
     */
    public void render(Vector2f[] positions) {
        this.use();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL30.glBindVertexArray(altVao);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, altVbo);

        // normalize positions
        Vector2f position = new Vector2f(positions[0].x / Window.width, -positions[0].y / Window.height);
        Vector2f size = new Vector2f(positions[1].x / Window.width, positions[1].y / Window.height);
        // convert
        position.x = 2 * position.x - 1;
        position.y = 2 * position.y + 1;
        size.x = 2 * size.x;
        size.y = 2 * size.y;

        float[] upperRight = new float[] { position.x + size.x, position.y };
        float[] lowerRight = new float[] { position.x + size.x, position.y - size.y };
        float[] upperLeft = new float[] { position.x, position.y };
        float[] lowerLeft = new float[] { position.x, position.y - size.y };

        FloatBuffer data = MemoryUtil.memAllocFloat(24);
        data.put(upperRight).put(lowerRight).put(upperLeft).put(lowerRight).put(lowerLeft).put(upperLeft).put(texCoords)
                .flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_DYNAMIC_DRAW);

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);

        GL30.glBindVertexArray(0);
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        MemoryUtil.memFree(data);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
    }
}
