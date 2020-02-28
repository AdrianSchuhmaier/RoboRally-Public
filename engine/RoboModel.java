package engine;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import core.math.Vector3f;
import core.model.Model;
import core.shader.ShaderProgram;

public class RoboModel extends Model {

    private Vector3f playerColor;
    private boolean isPreview;

    public RoboModel(Model model, Vector3f playerColor) {
        super();
        this.playerColor = playerColor;
        this.vao = model.getVao();
        this.vertexCount = model.getVertexCount();
    }

    public RoboModel(Model model) {
        super();
        this.playerColor = new Vector3f(1, 0, 1);
        this.vao = model.getVao();
        this.vertexCount = model.getVertexCount();
    }

    @Override
    public Consumer<ShaderProgram> prepare() {
        super.prepare();
        Consumer<ShaderProgram> additionalUniforms = (shader) -> {
            FloatBuffer buffer = playerColor.getBuffer();
            GL20.glUniform3fv(shader.getUniformLocation("playerColor"), buffer);
            GL20.glUniform1i(shader.getUniformLocation("isPreview"), isPreview ? GL11.GL_TRUE : GL11.GL_FALSE);
            MemoryUtil.memFree(buffer);
        };
        return additionalUniforms;
    }

    public RoboModel copy(RoboModel model) {
        return new RoboModel((Model) model, model.getPlayerColor());
    }

    @Override
    public RoboModel copy() {
        RoboModel copy = new RoboModel(this);
        return copy;
    }

    public void setPlayerColor(Vector3f color) {
        playerColor = color;
    }

    public Vector3f getPlayerColor() {
        return playerColor;
    }

    public void setPreview() {
        this.isPreview = true;
    }
}
