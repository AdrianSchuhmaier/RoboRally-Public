package core.renderer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import core.math.Vector2f;
import core.math.Vector3f;
import core.model.Model;
import core.shader.ScreenShaderProgram;
import core.shader.ShaderProgram;
import core.texture.Texture;
import core.window.Timer;
import core.window.Window;
import utilities.SettingsManager;

/**
 * @author Adrian Schuhmaier
 */
public class SortedRenderer {

    public final Vector3f backColor = new Vector3f(1, 1, 0);

    /**
     * Data structure for faster rendering:</br>
     * Switching ShaderPrograms takes the longest.</br>
     * Switching Textures is still slow.</br>
     * Switching models (setting uniforms) is fast.
     */
    protected Map<ShaderProgram, Map<Texture, List<Model>>> renderModels;
    protected ScreenShaderProgram screenShader;

    /* ===== Framebuffers ===== */
    /** The scene framebuffer */
    protected int sceneFramebuffer;
    /** The effects framebuffer */
    protected int effectFramebuffer;
    /**
     * The result framebuffer (used by {@link #renderResultToScreen()})
     */
    protected int resultFramebuffer;
    /**
     * An extra framebuffer to copy to (eg. when needing several post-process
     * stages)
     */
    protected int spareFramebuffer;
    /** The mirrored framebuffer */
    protected int mirrorFramebuffer;

    /* ===== Textures & Renderbuffers ===== */
    /**
     * Color Texture of {@link #sceneFramebuffer}
     */
    public int sceneColor;
    /** Color Texture of {@link #effectFramebuffer} */
    protected int effectColor;
    /**
     * Color Texture of {@link #resultFramebuffer}
     */
    protected int resultColor;
    /**
     * Color Texture of {@link #spareFramebuffer}
     */
    protected int spareColor;
    /**
     * Color Texture of {@link #mirrorFramebuffer}
     */
    protected int mirrorColor;
    /** Extra Texture containing R:fresnel, G:effectfilter, B:mirrormask */
    protected int specularAndGlow;
    /**
     * Depth and Stencil Texture of {@link #sceneFramebuffer},
     * {@link #effectFramebuffer} and {@link #resultFramebuffer}
     */
    protected int depthAndStencil;

    /**
     * Depth and Stencil Texture of {@link #mirrorFramebuffer}
     */
    protected int depthAndStencilReflection;

    /* ===== Mirroring ===== */
    /** Consumer to control the camera to be reflected */
    protected Consumer<Boolean> mirrorFunction;

    /**
     * 
     */
    public SortedRenderer() {
        renderModels = new HashMap<ShaderProgram, Map<Texture, List<Model>>>();
        Window.reflect = false;
        screenShader = new ScreenShaderProgram("screenShader.vert", "screenShader.frag");
        int windowWidth = Window.width;
        int windowHeight = Window.height;
        int[] drawBuffers = new int[] { GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1 };

        // gen framebuffers
        sceneFramebuffer = GL30.glGenFramebuffers();
        effectFramebuffer = GL30.glGenFramebuffers();
        resultFramebuffer = GL30.glGenFramebuffers();
        spareFramebuffer = GL30.glGenFramebuffers();
        mirrorFramebuffer = GL30.glGenFramebuffers();
        // gen textures & renderbuffers
        sceneColor = GL11.glGenTextures();
        effectColor = GL11.glGenTextures();
        resultColor = GL11.glGenTextures();
        spareColor = GL11.glGenTextures();
        mirrorColor = GL11.glGenTextures();
        specularAndGlow = GL11.glGenTextures();
        depthAndStencil = GL30.glGenRenderbuffers();
        depthAndStencilReflection = GL30.glGenRenderbuffers();

        // configure textures
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneColor);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, windowHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, effectColor);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, windowHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, resultColor);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, windowWidth, windowHeight, 0, GL11.GL_RGB,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, spareColor);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, windowHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularAndGlow);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, windowWidth, windowHeight, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);

        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // configure renderbuffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);

        // configure framebuffers
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, sceneFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, sceneColor, 0);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, specularAndGlow,
                0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthAndStencil);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, windowWidth, windowHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencil);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencil);
        GL20.glDrawBuffers(drawBuffers);
        // check the buffer for completeness
        if (!(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)) {
            System.err.println("sceneFramebuffer incomplete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, effectFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, effectColor, 0);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencil);
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        // check the buffer for completeness
        if (!(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)) {
            System.err.println("effectFramebuffer incomplete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, resultFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, resultColor, 0);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencil);
        // check the buffer for completeness
        if (!(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)) {
            System.err.println("resultFramebuffer incomplete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, spareFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, spareColor, 0);
        // check the buffer for completeness
        if (!(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)) {
            System.err.println("spareFramebuffer incomplete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, 0);
    }

    /**
     * Clears the framebuffers, prepares stencil
     */
    public void prepare() {
        GL11.glClearColor(backColor.x, backColor.y, backColor.z, 0.0f);
        // clear color, depth and stencil buffer
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
    }

    // ===========================================================================
    // Model Rendering
    // ===========================================================================

    /**
     * Renders all Models from the {@link #renderModels internal model
     * data-structure} with {@link #renderModels(ShaderProgram, Map)
     * renderModels(ShaderProgram, Map)}. </br>
     * If reflection is enabled, it renders to both the
     * {@link.Framebuffer#SCENE scene framebuffer} and the
     * {@link.Framebuffer#MIRROR mirror framebuffer}, using the
     * {@link #mirrorFunction mirrorFunction} to coordinate the views
     */
    public void renderModels() {
        synchronized (this) {
            for (ShaderProgram shader : renderModels.keySet()) {
                int fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
                renderModels(shader, renderModels.get(shader));
                if (Window.reflect) {
                    bindFramebuffer(Framebuffer.MIRROR);
                    mirrorFunction.accept(true);
                    renderModels(shader, renderModels.get(shader));
                    mirrorFunction.accept(false);
                }
                GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
            }
        }
    }

    /**
     * Renders the Models from the textureModelMap with the given shader.
     * 
     * @param shader
     *            ShaderProgram to render the models with
     * @param textureModelMap
     */
    public void renderModels(ShaderProgram shader, Map<Texture, List<Model>> textureModelMap) {
        synchronized (textureModelMap) {
            shader.use();

            for (Texture texture : textureModelMap.keySet()) {

                List<Model> modelList = textureModelMap.get(texture);

                for (Model model : modelList) {

                    // prepare AND set additional uniforms
                    model.prepare().accept(shader);

                    // set ModelMatrix
                    FloatBuffer buffer = (model).getModelMatrix().getBuffer();
                    GL20.glUniformMatrix4fv(shader.getUniformLocation("modelMatrix"), false, buffer);
                    MemoryUtil.memFree(buffer);

                    if (GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) == mirrorFramebuffer) {
                        GL20.glUniform1i(shader.getUniformLocation("isReflection"), 1);
                    } else {
                        GL20.glUniform1i(shader.getUniformLocation("isReflection"), 0);
                    }

                    GL20.glUniform1f(shader.getUniformLocation("time"), (float) Timer.getInstance().getTime());

                    model.render();
                }
                if (modelList.size() > 0)
                    modelList.get(0).cleanUp();
            }
        }
    }

    // ===========================================================================
    // Mirror Functionality
    // ===========================================================================

    /**
     * Clears the currently bound framebuffer.
     */
    public void clearFramebuffer() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
    }

    /**
     * Clears all Framebuffers of the Renderer.
     */
    public void clearFramebuffers() {
        for (Framebuffer f : Framebuffer.values()) {
            bindFramebuffer(f);
            clearFramebuffer();
        }
    }

    /**
     * Binds the specified framebuffer.
     * 
     * @param framebuffer
     */
    public void bindFramebuffer(Framebuffer framebuffer) {
        int reference = 0;

        switch (framebuffer) {
        case SCENE:
            reference = sceneFramebuffer;
            break;
        case EFFECTS:
            reference = effectFramebuffer;
            break;
        case RESULT:
            reference = resultFramebuffer;
            break;
        case MIRROR:
            reference = mirrorFramebuffer;
            break;
        case SPARE:
            reference = spareFramebuffer;
            break;
        case DEFAULT:
            reference = 0;
            break;
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, reference);
    }

    /**
     * Returns the textureID of the given framebuffer (does <b>not</b> work on
     * {@link core.renderer.SortedRenderer.Framebuffer#DEFAULT
     * Framebuffer.DEFAULT}).
     * 
     * @param framebuffer
     * @return textureID
     */
    public int getFramebufferTexture(Framebuffer framebuffer) {
        switch (framebuffer) {
        case SCENE:
            return sceneColor;
        case EFFECTS:
            return effectColor;
        case RESULT:
            return resultColor;
        case MIRROR:
            return mirrorColor;
        case SPARE:
            return spareColor;
        default:
            return 0;
        }
    }

    /**
     * @param mirrorFunction
     *            Consumer
     */
    public void enableMirrorAction(Consumer<Boolean> mirrorFunction) {

        // mirrorFramebuffer = GL30.glGenFramebuffers();
        // mirrorColor = GL11.glGenTextures();

        // configure texture
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mirrorColor);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, Window.width, Window.height, 0, GL11.GL_RGBA,
                GL11.GL_UNSIGNED_BYTE, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        // configure framebuffers
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, mirrorFramebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, mirrorColor, 0);
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthAndStencilReflection);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, Window.width, Window.height);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencilReflection);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
                depthAndStencilReflection);

        // check the buffer for completeness
        if (!(GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) == GL30.GL_FRAMEBUFFER_COMPLETE)) {
            System.err.println("sceneFramebuffer incomplete!");
        }

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);

        this.mirrorFunction = mirrorFunction;
        Window.reflect = true;
    }

    /**
     * Specifies, whether rendering the reflection or not.
     * 
     * @param use
     */
    public void useMirror(boolean use) {
        if (mirrorFunction == null && use) {
            throw new IllegalStateException(
                    "Cannot use mirror without a mirror function. Call enableMirrorAction() before you use the mirror.");
        } else {
            Window.reflect = use;
        }
    }

    // ===========================================================================
    // Screen Shader
    // ===========================================================================

    /**
     * Renders with the given ScreenShader
     * 
     * @param shader
     */
    public void renderScreenShader(ScreenShaderProgram shader) {
        int fbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        shader.use();
        shader.render();
        if (Window.reflect) {
            mirrorFunction.accept(true);
            bindFramebuffer(Framebuffer.MIRROR);
            shader.render();
            mirrorFunction.accept(false);
        }
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
    }

    /**
     * Renders the texture with the default screenShader.
     * 
     * @param textureID
     */
    public void renderTexture(int textureID) {
        screenShader.use();
        screenShader.renderTexture(textureID);
    }

    /**
     * Renders the texture with the default screenShader, using the given
     * positions.
     * 
     * @param textureID
     * @param positions
     *            [Vector2f(xPos, yPos), Vector2f(width, height)]
     */
    public void renderTexture(int textureID, Vector2f[] positions) {
        screenShader.use();
        screenShader.renderTexture(textureID, positions);
    }

    /**
     * Renders the texture with the default screenShader.
     * 
     * @param texture
     */
    public void renderTextureToScreen(Texture texture) {
        renderTextureToScreen(texture.getTextureID());
    }

    /**
     * Renders the texture with the default screenShader to screen.
     * 
     * @param textureID
     */
    public void renderTextureToScreen(int textureID) {
        bindFramebuffer(Framebuffer.DEFAULT);
        screenShader.use();
        screenShader.renderTexture(textureID);
    }

    /**
     * Renders the texture with the default screenShader to screen, using the
     * given positions.
     * 
     * @param textureID
     * @param positions
     *            [Vector2f(xPos, yPos), Vector2f(width, height)]
     */
    public void renderTextureToScreen(int textureID, Vector2f[] positions) {
        bindFramebuffer(Framebuffer.DEFAULT);
        screenShader.use();
        screenShader.renderTexture(textureID, positions);
    }

    /**
     * Renders the {@link.Framebuffer#RESULT result} to screen.
     */
    public void renderResultToScreen() {
        renderTextureToScreen(resultColor);
    }

    /**
     * Renders from the {@link core.renderer.SortedRenderer.Framebuffer#SCENE
     * scene framebuffer},
     * {@link core.renderer.SortedRenderer.Framebuffer#EFFECTS effects
     * framebuffer}, and {@link core.renderer.SortedRenderer.Framebuffer#MIRROR
     * mirror framebuffer} to the
     * {@link core.renderer.SortedRenderer.Framebuffer#RESULT result
     * framebuffer} using the given shader.
     * 
     * @param shader
     *            post-process-shader
     */
    public void postProcessCombine(ScreenShaderProgram shader) {

        bindFramebuffer(Framebuffer.RESULT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        ScreenShaderProgram shaderUse;
        if (shader == null) {
            shaderUse = screenShader;
        } else {
            shaderUse = shader;
        }
        shaderUse.use();
        GL20.glUniform2f(shaderUse.getUniformLocation("inverseTextureSize"), 1f / (float) Window.width,
                1f / (float) Window.height);
        GL20.glUniform1i(shaderUse.getUniformLocation("fxaa"), Window.fxaa ? 1 : 0);
        GL20.glUniform1i(shaderUse.getUniformLocation("reflect"), Window.reflect ? 1 : 0);
        GL20.glUniform1f(shaderUse.getUniformLocation("reflectiveness"), SettingsManager.reflectiveness);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, sceneColor);
        GL13.glActiveTexture(GL13.GL_TEXTURE1);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, mirrorColor);
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, effectColor);
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularAndGlow);

        shader.render();
    }

    /**
     * Renders from the {@link core.renderer.SortedRenderer.Framebuffer#SPARE
     * spare framebuffer} and the additionally given <b>framebuffers</b> to the
     * {@link core.renderer.SortedRenderer.Framebuffer#RESULT result
     * framebuffer} using the given shader.
     * 
     * @param shader
     *            post-process-shader
     * @param strength
     *            how strong the postProcess takes effect (0 = nothing, 1 =
     *            full)
     * @param framebuffers
     *            max. 3 additional Framebuffers (shall <b>not</b> be
     *            {@link core.renderer.SortedRenderer.Framebuffer#DEFAULT
     *            Framebuffer.DEFAULT}).</br>
     *            (If a framebuffer is
     *            {@link core.renderer.SortedRenderer.Framebuffer#SPARE
     *            Framebuffer.SPARE}, the {@link #specularAndGlow glow}-Texture
     *            will be used)
     */
    public void postProcess(ScreenShaderProgram shader, float strength, Framebuffer... framebuffers) {
        copyFramebuffer(Framebuffer.RESULT, Framebuffer.SPARE);
        bindFramebuffer(Framebuffer.RESULT);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        ScreenShaderProgram shaderUse;
        if (shader == null) {
            shaderUse = screenShader;
        } else {
            shaderUse = shader;
        }
        shaderUse.use();
        GL20.glUniform2f(shaderUse.getUniformLocation("inverseTextureSize"), 1f / (float) Window.width,
                1f / (float) Window.height);
        GL20.glUniform1i(shaderUse.getUniformLocation("fxaa"), Window.fxaa ? 1 : 0);
        GL20.glUniform1i(shaderUse.getUniformLocation("reflect"), Window.reflect ? 1 : 0);
        GL20.glUniform1f(shaderUse.getUniformLocation("reflectiveness"), SettingsManager.reflectiveness);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, resultColor);

        for (int i = 0; i < Math.min(framebuffers.length, 3); i++) {
            if (framebuffers[i] == Framebuffer.DEFAULT) {
                System.err.println("Cannot perform post-processing with the DEFAULT framebuffer.");
            } else if (framebuffers[i] == Framebuffer.SPARE) {
                GL13.glActiveTexture(GL13.GL_TEXTURE1 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, specularAndGlow);
            } else {
                GL13.glActiveTexture(GL13.GL_TEXTURE1 + i);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, getFramebufferTexture(framebuffers[i]));
            }
        }
        shader.use();
        GL20.glUniform1f(shaderUse.getUniformLocation("strength"), strength);
        shader.render();
    }

    /**
     * Copies the color of the given framebuffer to the
     * {@link core.renderer.SortedRenderer.Framebuffer#SPARE Framebuffer.SPARE}
     * (does <b>not</b> work on
     * {@link core.renderer.SortedRenderer.Framebuffer#DEFAULT
     * Framebuffer.DEFAULT}).
     * 
     * @param framebuffer
     */
    public void copyFramebuffer(Framebuffer source, Framebuffer target) {
        bindFramebuffer(target);
        screenShader.use();
        screenShader.renderTexture(getFramebufferTexture(source));
    }

    // ===========================================================================
    // Context Management
    // ===========================================================================

    /**
     * Sets, whether stencil is used.
     * 
     * @param use
     */
    public void useStencil(boolean use) {
        if (use) {
            GL11.glEnable(GL11.GL_STENCIL_TEST);
        } else {
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }
    }

    /**
     * Sets, whether rendering writes to stencil.
     * 
     * @param write
     */
    public void writeStencil(boolean write) {
        if (write) {
            GL11.glStencilMask(0xFF);
        } else {
            GL11.glStencilMask(0x00);
        }
    }

    // ===========================================================================
    // Model Management
    // ===========================================================================

    /**
     * Adds the given model with the given shader to the {@link #renderModels
     * internal model data-structure}.
     * 
     * @param model
     * @param shader
     */
    public void addRenderModel(Model model, ShaderProgram shader) {
        synchronized (this) {
            Texture modelTex = model.getTexture(0);

            // if there are no renderModels
            if (renderModels == null) {
                renderModels = new HashMap<ShaderProgram, Map<Texture, List<Model>>>();
            }

            // if the shader is not yet in the Map
            if (!renderModels.containsKey(shader)) {
                // add the shader
                renderModels.put(shader, new HashMap<Texture, List<Model>>());
            }

            // get the texMap to the given shader
            Map<Texture, List<Model>> texMap = renderModels.get(shader);

            // if the texMap does not contain the models texture
            if (!texMap.containsKey(modelTex)) {
                // add the model texture
                texMap.put(modelTex, new ArrayList<Model>());
            }

            // get the modelList to the given texture
            List<Model> modelList = texMap.get(modelTex);

            // add the model
            modelList.add(model);
        }
    }

    /**
     * Adds the given models with the given shader to the {@link #renderModels
     * internal model data-structure}.
     * 
     * @param models
     * @param shader
     */
    public void addRenderModels(List<Model> models, ShaderProgram shader) {
        synchronized (this) {
            for (Model model : models) {
                addRenderModel(model, shader);
            }
        }
    }

    /**
     * Removes the given model from the {@link #renderModels internal model
     * data-structure}.
     * 
     * @param modelToRemove
     */
    public void removeRenderModel(Model modelToRemove) {
        synchronized (this) {
            for (Map<Texture, List<Model>> map : renderModels.values()) {
                List<Model> modelList = map.get(modelToRemove.getTexture(0));
                if (modelList == null)
                    continue;
                modelList.remove(modelToRemove);

            }
        }
    }

    /**
     * Removes the given models from the {@link #renderModels internal model
     * data-structure}.
     * 
     * @param modelsToRemove
     *            ModelList of the models to be removed
     */
    public void removeRenderModels(List<Model> modelsToRemove) {
        synchronized (this) {
            for (Model modelToRemove : new ArrayList<>(modelsToRemove)) {
                removeRenderModel(modelToRemove);
            }
        }
    }

    /**
     * Clears the {@link #renderModels internal model data-structure}.
     */
    public void clearRenderModels() {
        synchronized (this) {
            renderModels.clear();
        }
    }

    // ===========================================================================
    // Framebuffer enum
    // ===========================================================================

    /**
     * This Enum represents the different framebuffers present in the
     * {@link SortedRenderer Class}.
     * 
     * @author Adrian Schuhmaier
     */
    public enum Framebuffer {
        /**
         * Default Framebuffer - the canvas that is shown in the window. </br>
         */
        DEFAULT,
        /**
         * The SCENE framebuffer for all scene objects to be rendered to.
         */
        SCENE,
        /**
         * The EFFECTS Framebuffer for all Effects that want to be overlayed
         * separately.
         */
        EFFECTS,
        /**
         * Result Framebuffer - the {@link #postProcess(ScreenShaderProgram)
         * postProcess-method} renders to it.
         */
        RESULT,
        /**
         * Spare framebuffer, used if an extra one is needed.
         */
        SPARE,
        /**
         * Mirror Framebuffer for the mirrored scene used for reflections.
         */
        MIRROR;
    }
}