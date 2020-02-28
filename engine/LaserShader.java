package engine;

import java.nio.FloatBuffer;
import java.util.HashMap;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import constants.LaserColor;
import core.math.Vector3f;
import core.shader.UniformSpecialShaderProgram;
import core.texture.TextureLoader;
import core.window.Timer;
import utilities.Console;

public class LaserShader extends UniformSpecialShaderProgram {

    /** HashMap(LaserColor, int[vao, vbo, vertexCount]) */
    private HashMap<LaserColor, int[]> laser;
    private int textureID;

    public LaserShader(String vertexShaderName, String geometryShaderName, String fragmentShaderName,
            String textureName) {
        super(vertexShaderName, geometryShaderName, fragmentShaderName);
        textureID = TextureLoader.loadTexture(textureName).getTextureID();
        laser = new HashMap<LaserColor, int[]>();
        for (LaserColor color : LaserColor.values()) {
            // initialize openGL object
            int vao = GL30.glGenVertexArrays();
            GL30.glBindVertexArray(vao);
            int vbo = GL15.glGenBuffers();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
            // setup attrib location
            GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 12, 0);
            GL20.glEnableVertexAttribArray(0);

            laser.put(color, new int[] { vao, vbo, 0 });
            GL30.glBindVertexArray(0);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

        }
    }

    /**
     * Creates the OpenGL equivalent of the given lasers.</br>
     * <b>Shall only be called within the Thread with proper OpenGL context</b>
     * 
     * @param positions
     *            [start1, end1, start2, end2, ...]
     * @param laserColor
     */
    public void setLasers(Vector3f[] positions, LaserColor laserColor) {
        if (positions.length % 2 != 0) {
            Console.err(this.getClass(), "Unable to render lasers");
        } else {
            int[] glObject = this.laser.get(laserColor);
            glObject[2] = positions.length;
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glObject[1]);
            FloatBuffer buffer = MemoryUtil.memAllocFloat(3 * positions.length);
            for (int i = 0; i < positions.length; i++) {
                buffer.put(new float[] { positions[i].x, positions[i].y, positions[i].z });
            }
            buffer.flip();
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

            MemoryUtil.memFree(buffer);
        }
    }

    /**
     * Clears the laser of the given color (so there is nothing being rendered)
     * 
     * @param laserColor
     */
    public void clearLasers(LaserColor laserColor) {
        setLasers(new Vector3f[0], laserColor);
    }

    /**
     * Clears all lasers so none are rendered
     */
    public void clearAllLasers() {
        for (LaserColor laserColor : LaserColor.values()) {
            clearLasers(laserColor);
        }
    }

    @Override
    public void render() {
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_MAX);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureID);
        for (LaserColor color : laser.keySet()) {
            Vector3f rgb = color.getRGB();
            FloatBuffer proj = Camera.getInstance().getProjectionMatrix().getBuffer();
            FloatBuffer view = Camera.getInstance().getViewMatrix().getBuffer();
            GL20.glUniform3f(this.getUniformLocation("laserColor"), rgb.x, rgb.y, rgb.z);
            GL20.glUniform1f(this.getUniformLocation("time"), (float) Timer.getInstance().getTime());
            GL20.glUniformMatrix4fv(this.getUniformLocation("projectionMatrix"), false, proj);
            GL20.glUniformMatrix4fv(this.getUniformLocation("viewMatrix"), false, view);
            GL30.glBindVertexArray(laser.get(color)[0]);
            GL11.glDrawArrays(GL11.GL_LINES, 0, laser.get(color)[2]);
            GL30.glBindVertexArray(0);
            MemoryUtil.memFree(proj);
            MemoryUtil.memFree(view);
        }
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDepthMask(true);
        GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD);
    }

}
