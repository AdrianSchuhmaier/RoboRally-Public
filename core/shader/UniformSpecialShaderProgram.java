package core.shader;

import java.util.function.Consumer;

/**
 * This class holds a ShaderProgram that renders a quad to the screen using its
 * extra uniforms to make the shader work.
 * 
 * @author Adrian Schuhmaier
 *
 */
public class UniformSpecialShaderProgram extends ScreenShaderProgram {

    /** Consumer binding the wanted uniforms */
    protected Consumer<Void> uniforms;

    /**
     * @param vertexShaderName
     *            path to a screen quad vertex shader
     * @param fragmentShaderName
     *            path to a screen quad fragment shader (e.g. post-processing
     *            shader)
     */
    public UniformSpecialShaderProgram(String vertexShaderName, String fragmentShaderName) {
        super(vertexShaderName, fragmentShaderName);
        this.uniforms = (nothing) -> {
        };
    }

    /**
     * @param vertexShaderName
     *            path to a screen quad vertex shader
     * @param geometryShaderName
     *            not used in this implementation
     * @param fragmentShaderName
     *            path to a screen quad fragment shader (e.g. post-processing
     *            shader)
     */
    public UniformSpecialShaderProgram(String vertexShaderName, String geometryShaderName, String fragmentShaderName) {
        super(vertexShaderName, geometryShaderName, fragmentShaderName);
        this.uniforms = (nothing) -> {
        };
    }

    /**
     * @param vertexShaderName
     *            path to a screen quad vertex shader
     * @param fragmentShaderName
     *            path to a screen quad fragment shader (e.g. post-processing
     *            shader)
     * @param uniforms
     *            Consumer binding the wanted uniforms
     */
    public UniformSpecialShaderProgram(String vertexShaderName, String fragmentShaderName, Consumer<Void> uniforms) {
        super(vertexShaderName, fragmentShaderName);
        this.uniforms = uniforms;
    }

    public void setUniforms(Consumer<Void> uniforms) {
        this.uniforms = uniforms;
    }

    public void render() {
        if (uniforms != null)
            uniforms.accept(null);
    }

}
