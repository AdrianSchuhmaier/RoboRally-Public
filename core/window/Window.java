package core.window;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryUtil.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.KHRDebug;

import core.model.ModelLoader;
import core.texture.TextureLoader;

/**
 * A singleton class that manages the glfw window and the user input.
 * 
 * @author Adrian Schuhmaier
 */
public abstract class Window {

    /** dimensions of the window (in pixels) */
    public static int width, height;
    /** if fxaa anti-aliasing is enabled */
    public static boolean fxaa;
    /** if reflection is enabled in the scene */
    public static boolean reflect;

    /** the window title */
    protected static final String WINDOW_TITLE = "Title";

    protected int swapInterval = 0;
    public static long window;
    protected static boolean fullscreen;

    protected Window() {
        window = 0;
        fullscreen = false;
        fxaa = false;
    }

    /**
     * Initializes the GLFW Context (and does not create it).</br>
     * If no longer used, clean up with {@link core.window.Window#cleanUp()}.
     */
    public void init() {

        GLFWErrorCallback.createPrint().set();
        // initialize glfw and check
        if (!glfwInit()) {
            throw new IllegalStateException("Could not create GLFW Context");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_RESIZABLE, GL11.GL_FALSE);
        // glfwWindowHint(GLFW_SAMPLES, 4);

        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_FALSE);
    }

    /**
     * Creates the window with the given size.</br>
     * Also creates the <b>GLFW and OpenGL context</b>.</br>
     * Shall not be called before {@link core.window.Window#init()}
     * 
     * @param width
     * @param height
     */
    public void create(int width, int height) {
        Window.width = width;
        Window.height = height;

        // make the window invisible while config
        // glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        // create window
        if (!fullscreen) {

            window = glfwCreateWindow(width, height, WINDOW_TITLE, 0, 0);

            // get monitor information
            GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // set the window in the midd
            glfwSetWindowPos(window, (vidMode.width() - width) / 2, (vidMode.height() - height) / 2);

        } else {

            window = glfwCreateWindow(width, height, WINDOW_TITLE, glfwGetPrimaryMonitor(), 0);

        }

        // check if the window was created
        if (window == 0) {
            glfwTerminate();
            throw new IllegalStateException("Could not create the window");
        }

        // make the glfw context current
        glfwMakeContextCurrent(window);

        // cap fps
        glfwSwapInterval(swapInterval);

        // show the window
        glfwShowWindow(window);

        setWindowIcon();

        // connect opengl context and glfw context
        GLCapabilities caps = GL.createCapabilities();
        GLUtil.setupDebugMessageCallback();

        if (caps.OpenGL43)
            glDebugMessageControl(GL_DEBUG_SOURCE_API, GL_DEBUG_TYPE_OTHER, GL_DEBUG_SEVERITY_NOTIFICATION,
                    (IntBuffer) null, false);
        else if (caps.GL_KHR_debug) {
            KHRDebug.glDebugMessageControl(KHRDebug.GL_DEBUG_SOURCE_API, KHRDebug.GL_DEBUG_TYPE_OTHER,
                    KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
        } else if (caps.GL_ARB_debug_output)
            glDebugMessageControlARB(GL_DEBUG_SOURCE_API_ARB, GL_DEBUG_TYPE_OTHER_ARB, GL_DEBUG_SEVERITY_LOW_ARB,
                    (IntBuffer) null, false);

        // set the GL viewport that is drawn inside the glfw window
        GL11.glViewport(0, 0, width, height);

        // set keybind callbacks
        setKeybinds();

        System.out.println("Window created");
    }

    public void cancelCloseRequest() {
        glfwSetWindowShouldClose(window, false);
    }

    /**
     * Closes the window (<b>not</b> the glfw context).</br>
     * This method is automatically called by
     * {@link core.window.Window#cleanUp()}.
     */
    public void close() {

        // check if the window was created
        if (window == 0) {
            System.err.println("Cannot close a non-existant window. Don't bother if you ");
        }

        glfwDestroyWindow(window);
        window = 0;

        // free all loaded textures and models
        ModelLoader.clear();
        TextureLoader.clear();

        System.out.println("Window closed");
    }

    /** Cleans up the GLFW Context */
    public void cleanUp() {

        // closes the window if it exists
        if (window != 0) {
            close();
        }

        glfwTerminate();
    }

    /** Returns if the window close is requested as a user input */
    public boolean isCloseRequested() {

        // check if the window was created
        if (window == 0) {
            throw new IllegalStateException("window not created - cannot check for closeRequested");
        }

        return glfwWindowShouldClose(window);
    }

    /** Swaps back and front buffer to show the rendered image */
    public void swapBuffers() {

        // check if the window was created
        if (window == 0) {
            throw new IllegalStateException("window not created - cannot swap buffers");
        }

        glfwSwapBuffers(window);
    }

    /** Sets the fullscreen state */
    @Deprecated
    public void setFullscreen(boolean fullscreen) {

        // check if the window was created
        if (window == 0) {
            throw new IllegalStateException("window not created - cannot set fullscreen");
        }

        if (Window.fullscreen != fullscreen) {
            Window.fullscreen = fullscreen;

            // for changing the fullscreen mode the window has to be closed and
            // created again
            this.close();
            this.create(width, height);
        }
    }

    public void setSwapInterval(int interval) {
        this.swapInterval = interval;
        glfwSwapInterval(interval);
    }

    /** Reads in all user input and triggers the callbacks */
    public void pollInput() {
        glfwPollEvents();
    }

    /** Sets the window title. */
    public void setWindowTitle(String title) {
        glfwSetWindowTitle(window, title);
    }

    /** Sets the window icon */
    protected void setWindowIcon() {
        IntBuffer w = memAllocInt(1);
        IntBuffer h = memAllocInt(1);
        IntBuffer comp = memAllocInt(1);
        ByteBuffer icon16 = null;
        ByteBuffer icon32 = null;
        try {
            icon16 = TextureLoader.getTextureBuffer("res/icon16.png");
            icon32 = TextureLoader.getTextureBuffer("res/icon32.png");
        } catch (Exception e) {
            System.err.println("Could not load window Icons");
            System.exit(-1);
        }

        try (GLFWImage.Buffer icons = GLFWImage.malloc(2)) {
            ByteBuffer pixels16 = stbi_load_from_memory(icon16, w, h, comp, 4);
            icons.position(0).width(w.get(0)).height(h.get(0)).pixels(pixels16);

            ByteBuffer pixels32 = stbi_load_from_memory(icon32, w, h, comp, 4);
            icons.position(1).width(w.get(0)).height(h.get(0)).pixels(pixels32);

            icons.position(0);
            glfwSetWindowIcon(window, icons);

            stbi_image_free(pixels32);
            stbi_image_free(pixels16);
        }
        memFree(comp);
        memFree(h);
        memFree(w);
    }

    /** Sets the keybinds */
    protected void setKeybinds() {
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {

            // ESC to close
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                glfwSetWindowShouldClose(window, true);

        });
    }

}