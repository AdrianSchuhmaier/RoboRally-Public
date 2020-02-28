package core.objects;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import core.math.Vector2f;

/**
 * 
 * This class holds a set amounts of lines in a line group. Setting OpenGL
 * buffers, updating and rendering is also handled here.
 * 
 * @author Adrian Schuhmaier
 * @author Maximilian Blasi
 *
 */
public class LineGroup {

    private int vao;
    private int vbo;

    /**
     * array of the positions (line1 start, line1 end, line 2 start, line2 end,
     * ... )
     */
    protected Vector2f[] positions;

    public LineGroup(int lineCount) {
        positions = new Vector2f[lineCount * 2];

        vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);

        vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer dataContainer = MemoryUtil.memAllocFloat(lineCount * 4);
        dataContainer.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, dataContainer, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(dataContainer);

        // setup attrib location
        GL20.glVertexAttribPointer(0, 2, GL11.GL_FLOAT, false, 8, 0);
        GL20.glEnableVertexAttribArray(0);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    public void render() {

        for (int i = 0; i < positions.length; i += 2) {
            GL11.glDrawArrays(GL11.GL_LINES, i, 2);
        }

    }

    public void prepare() {
        GL30.glBindVertexArray(vao);
    }

    public void cleanUp() {
        GL30.glBindVertexArray(0);
    }

    /**
     * Updates the OpenGL Buffers that hold the positions with the information
     * in the positions array.
     */
    protected void updatePositionsBuffer() {
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);

        FloatBuffer data = MemoryUtil.memAllocFloat(positions.length * 2);
        for (int i = 0; i < positions.length; i++) {
            data.put(positions[i].x);
            data.put(positions[i].y);
        }
        data.flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, data, GL15.GL_STATIC_DRAW);
        MemoryUtil.memFree(data);

        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
    }

}
