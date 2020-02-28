package core.renderer;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.util.function.Consumer;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.opengl.GL11;

import core.shader.ScreenShaderProgram;
import engine.Window;

public class VectorRenderer {

    long vgContext;
    NVGColor color;
    ScreenShaderProgram shader = new ScreenShaderProgram("screenShader.vert", "screenShader.frag");

    public VectorRenderer() {
        init();
    }

    public void init() {

        vgContext = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        if (vgContext == NULL) {
            System.err.println("Could not init nanoVG.");
            System.exit(-1);
        }

        color = NVGColor.create();

        color.r(1.0f);
        color.g(0.0f);
        color.b(1.0f);
        color.a(0.5f);

        nvgCreateFont(vgContext, "REGULAR", "res/open_sans_font.ttf");
    }

    /**
     * Renders the given function to the currently bound framebuffer
     * 
     * @see <a href="https://lwjglgamedev.gitbooks.io/3d-game-development-with
     *      -lwjgl/content/chapter24/chapter24.html">https://lwjglgamedev.
     *      gitbooks.io/3d-game-development-with
     *      -lwjgl/content/chapter24/chapter24.html</a>
     * @param func
     *            NanoVG draw call (without begin/end frame)
     */
    public void render(Consumer<Long> func) {
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        nvgBeginFrame(vgContext, Window.width, Window.height, 1);

        func.accept(vgContext);

        nvgEndFrame(vgContext);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }
    
    public void setColor(float r, float g, float b, float a) {
        NanoVG.nvgRGBAf(r, g, b, a, color);
    }
}
