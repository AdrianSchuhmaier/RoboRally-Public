package core.shader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL32;

import core.shader.ShaderProgram;

/**
 * 
 * This class contains a OpenGL shader program that it creates from shader
 * files.
 * 
 * @author Adrian Schuhmaier
 *
 */
public class ShaderProgram {

    public static final int MATRICES_BUFFER_BINDING = 1;
    public static final int DIMENSIONS_BUFFER_BINDING = 2;
    protected static final String SHADER_PATH = "res/shaders/";

    private int programID;

    public int getProgramID() {
        return programID;
    }

    /**
     * Constructor - loads and compiles the shader files and returns the
     * shaderProgram.
     * 
     * @param vertexShaderName
     *            file name of the vertex shader code
     * @param fragmentShaderName
     *            file name of the fragment shader code
     */
    public ShaderProgram(String vertexShaderName, String fragmentShaderName) {

        int vertexShaderID = loadShader(vertexShaderName, GL20.GL_VERTEX_SHADER);
        int fragmentShaderID = loadShader(fragmentShaderName, GL20.GL_FRAGMENT_SHADER);
        this.programID = loadProgram(vertexShaderID, fragmentShaderID);

        // do the uniform block bindings
        int index;
        if ((index = GL31.glGetUniformBlockIndex(programID, "Matrices")) != GL31.GL_INVALID_INDEX) {
            // bind the shaders programs uniform block to its binding point
            GL31.glUniformBlockBinding(programID, index, ShaderProgram.MATRICES_BUFFER_BINDING);
        }

        if ((index = GL31.glGetUniformBlockIndex(programID, "Dimensions")) != GL31.GL_INVALID_INDEX) {
            // bind the shaders programs uniform block to its binding point
            GL31.glUniformBlockBinding(programID, index, ShaderProgram.DIMENSIONS_BUFFER_BINDING);
        }
    }

    /**
     * Constructor - loads and compiles the shader files and returns the
     * shaderProgram.
     * 
     * @param vertexShaderName
     *            file name of the vertex shader code
     * @param geometryShaderName
     *            file name of the geometry shader code
     * @param fragmentShaderName
     *            file name of the fragment shader code
     */
    public ShaderProgram(String vertexShaderName, String geometryShader, String fragmentShaderName) {

        int vertexShaderID = loadShader(vertexShaderName, GL20.GL_VERTEX_SHADER);
        int geometryShaderID = loadShader(geometryShader, GL32.GL_GEOMETRY_SHADER);
        int fragmentShaderID = loadShader(fragmentShaderName, GL20.GL_FRAGMENT_SHADER);
        this.programID = loadProgram(vertexShaderID, geometryShaderID, fragmentShaderID);

        // do the uniform block bindings
        int index;
        if ((index = GL31.glGetUniformBlockIndex(programID, "Matrices")) != GL31.GL_INVALID_INDEX) {
            // bind the shaders programs uniform block to its binding point
            GL31.glUniformBlockBinding(programID, index, ShaderProgram.MATRICES_BUFFER_BINDING);
        }

        if ((index = GL31.glGetUniformBlockIndex(programID, "Dimensions")) != GL31.GL_INVALID_INDEX) {
            // bind the shaders programs uniform block to its binding point
            GL31.glUniformBlockBinding(programID, index, ShaderProgram.DIMENSIONS_BUFFER_BINDING);
        }
    }

    /**
     * Loads shaderCode from the given shaderName and compiles it.
     * 
     * @param shaderName
     *            file name of the shader
     * @param type
     *            shader type (GL_VERTEX_SHADER or GL_FRAGMENT_SHADER)
     * @return shaderID
     */
    private int loadShader(String shaderName, int type) {

        // create a new shader in openGL
        int shaderID = GL20.glCreateShader(type);

        CharSequence shaderSource = readFromFile(shaderName);

        // set shaderSource as the openGL shaderSource for the shaderID
        GL20.glShaderSource(shaderID, shaderSource);

        // compile the shader
        GL20.glCompileShader(shaderID);

        // test compileation status
        if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetShaderInfoLog(shaderID, 512));
            System.err.println("Could not compile shader.");
            System.exit(-1);
        }

        System.out.println("\"" + shaderName + "\" compiled successfully");

        return shaderID;
    }

    private CharSequence readFromFile(String shaderName) {
        return readFromFile(shaderName, false);
    }

    private CharSequence readFromFile(String shaderName, boolean ignoreFail) {
        StringBuilder shaderSource = new StringBuilder();

        // read the code from the source file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(SHADER_PATH + shaderName));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#include")) {
                    try {
                        String newPath = line.split(" ")[1];
                        shaderSource.append(readFromFile(newPath, true)).append("\n");
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("\"" + shaderName + "\" #include statement skipped.");
                    }
                } else {
                    shaderSource.append(line).append("\n");
                }
            }
            reader.close();
        } catch (IOException e) {
            if (ignoreFail) {
                System.out.println("\"" + shaderName + "\" Shader file import skipped.");
                return "";
            } else {
                System.err.println("Could not load shader file.");
                e.printStackTrace();
                System.exit(1);
            }
        }
        return shaderSource;
    }

    /**
     * Attaches required shaders to the program and links it
     *
     * @param vertexShaderID
     * @param fragmentShaderID
     */
    private int loadProgram(int vertexShaderID, int fragmentShaderID) {

        // create a new shader program in openGL
        int programID = GL20.glCreateProgram();

        // attach both shaders to the shader program
        GL20.glAttachShader(programID, vertexShaderID);
        GL20.glAttachShader(programID, fragmentShaderID);

        // link the program
        GL20.glLinkProgram(programID);

        // test link status
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetProgramInfoLog(programID, 500));
            System.err.println("Could not compile shader.");
            System.exit(-1);
        }

        // delete the shaders as they are linked into the program
        GL20.glDeleteShader(vertexShaderID);
        GL20.glDeleteShader(fragmentShaderID);

        return programID;
    }

    /**
     * Attaches required shaders to the program and links it
     *
     * @param vertexShaderID
     * @param geometryShaderID
     * @param fragmentShaderID
     */
    private int loadProgram(int vertexShaderID, int geometryShaderID, int fragmentShaderID) {

        // create a new shader program in openGL
        int programID = GL20.glCreateProgram();

        // attach both shaders to the shader program
        GL20.glAttachShader(programID, vertexShaderID);
        GL20.glAttachShader(programID, geometryShaderID);
        GL20.glAttachShader(programID, fragmentShaderID);

        // link the program
        GL20.glLinkProgram(programID);

        // test link status
        if (GL20.glGetProgrami(programID, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            System.out.println(GL20.glGetProgramInfoLog(programID, 500));
            System.err.println("Could not compile shader.");
            System.exit(-1);
        }

        // delete the shaders as they are linked into the program
        GL20.glDeleteShader(vertexShaderID);
        GL20.glDeleteShader(geometryShaderID);
        GL20.glDeleteShader(fragmentShaderID);

        return programID;
    }

    /** Specifies this shader program as the one to use for openGL draw calls */
    public void use() {
        GL20.glUseProgram(programID);
    }

    /**
     * Returns the uniform location of a uniform variable in the shader program
     * (either vertex or fragment shader).
     * 
     * @param name
     *            of the uniform
     * @return uniform location of the specified variable
     */
    public int getUniformLocation(String name) {
        return GL20.glGetUniformLocation(this.programID, name);
    }
}
