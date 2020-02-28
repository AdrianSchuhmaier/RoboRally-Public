package core.window;

import org.lwjgl.glfw.GLFW;

import utilities.Console;

/**
 * 
 * A timer based on the (precise) timing information of the glfw window
 * 
 * @author Adrian Schuhmaier
 * 
 */
public class Timer {

    private static Timer instance;

    /** time at last frame */
    private static double lastFrame;

    /** frames per second */
    private static double frameTime;

    /** the time of the program start */
    private static double startTime;

    /** the time of the last printed fps */
    private static double lastPrint;

    /** counts the frames in the printFPS delay window */
    private static int frameCount;

    private Timer() {
    }

    public static Timer getInstance() {
        if (instance == null) {
            instance = new Timer();
        }
        return instance;
    }

    public double getTime() {
        return GLFW.glfwGetTime();
    }

    public float getDelta() {
        double time = getTime();
        return (float) (time - lastFrame);

//        return delta;
    }

    public void start() {
        startTime = getTime();
        lastFrame = startTime;
        lastPrint = startTime;
        frameCount = 0;
    }

    public void startFrame() {
        double now = getTime();
        frameTime = now - lastFrame;
        lastFrame = now;
        frameCount++;
    }

    public float timeRunning() {
        return (float) (getTime() - startTime);
    }

    public int getFPS() {
        return (int) Math.round(1 / frameTime);
    }

    public void printFPS(float delay) {
        double now = getTime();
        if ((now - lastPrint) > delay) {
            lastPrint = now;
            Console.out(this.getClass(), "FPS " + frameCount / delay);
            frameCount = 0;
        }
    }
}
