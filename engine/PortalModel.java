package engine;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

import core.math.Vector3f;
import core.model.Model;
import core.shader.ShaderProgram;

public class PortalModel extends Model {

    private Vector3f portalColor;
    
    public PortalModel(Model model, Vector3f portalColor) {
        super();
        this.portalColor = portalColor;
        this.vao = model.getVao();
        this.vertexCount = model.getVertexCount();
    }

    public PortalModel(Model model) {
        super();
        this.portalColor = new Vector3f(1, 0, 1);
        this.vao = model.getVao();
        this.vertexCount = model.getVertexCount();
    }

    @Override
    public Consumer<ShaderProgram> prepare() {
        super.prepare();
        Consumer<ShaderProgram> additionalUniforms = (shader) -> {
            FloatBuffer buffer = portalColor.getBuffer();
            GL20.glUniform3fv(shader.getUniformLocation("portalColor"), buffer);
            MemoryUtil.memFree(buffer);
        };
        return additionalUniforms;
    }
    
    public PortalModel copy(PortalModel model) {
        return new PortalModel((Model)model, model.getPortalColor());
    }
    
    @Override
    public PortalModel copy() {
        PortalModel copy = new PortalModel(this);
        return copy;
    }

    public void setPortalColor(Vector3f color) {
        portalColor = color;
    }

    public Vector3f getPortalColor() {
        return portalColor;
    }
}
