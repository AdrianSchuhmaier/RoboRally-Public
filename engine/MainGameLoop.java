package engine;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import client.MainClient;
import client.User;
import constants.MenuState;
import core.renderer.SortedRenderer;
import core.renderer.SortedRenderer.Framebuffer;
import core.renderer.UIRenderer;
import core.renderer.VectorRenderer;
import core.shader.ScreenShaderProgram;
import core.window.Interpolator;
import core.window.Timer;
import gui.MenuView;
import gui.UIController;
import logic.RobotController;
import map.*;
import utilities.SettingsManager;

/**
 * This class is the main class for the graphic<br>
 * it initializes every component and contains the game loop
 * 
 * @author Adrian Schuhmaier
 */
public class MainGameLoop implements Runnable {

    /** a class that visualizes the world */
    private ClientViewController viewController;

    /** the client that started the class */
    private MainClient mainClient;

    /** main class for menus */
    private MenuView menuView;

    /** a flag for changing the screen size */
    private boolean resolutionChanged;

    /** the screen width of the window */
    private int targetWidth = 1280;

    /** the screen height of the window */
    private int targetHeight = 720;

    /**
     * Constructor
     * 
     * @param client
     */
    public MainGameLoop(MainClient client) {
        SettingsManager.loadSettings();
        switch (SettingsManager.screenRes) {
        case 1280:
            targetWidth = 1280;
            targetHeight = 720;
            break;
        case 1600:
            targetWidth = 1600;
            targetHeight = 900;
            break;
        case 1920:
            targetWidth = 1920;
            targetHeight = 980;
            break;
        case 2556:
            targetWidth = 2556;
            targetHeight = 1440;
            break;
        default:
            targetWidth = SettingsManager.screenRes;
            targetHeight = (int) (targetWidth * 0.5625);
        }
        mainClient = client;
        menuView = new MenuView(client);
        resolutionChanged = false;
    }

    /**
     * Main game loop <br>
     * initializes every component, starts the game loop, updates the window,
     * manages events
     */
    public void run() {
        UIRenderer uiRenderer;
        UIController uiController = new UIController();
        // loop for resizing the window
        do {
            this.resolutionChanged = false;
            Window window = Window.getInstance();

            // initialize the window
            window.init();
            // create a new window with the screen size
            window.create(targetWidth, targetHeight);

            // Interval(1) => Framerate = Screen refresh rate
            window.setSwapInterval(1);

            Timer timer = Timer.getInstance();

            SortedRenderer renderer = new SortedRenderer();
            VectorRenderer vRenderer = new VectorRenderer();

            uiRenderer = new UIRenderer();
            uiRenderer.init();
            uiController.keybinds(Window.window, uiRenderer.getContext());
            menuView.setUiRenderer(uiRenderer);

            // initzialize the shader
            ScreenShaderProgram postProcessCombineShader = new ScreenShaderProgram("screenShader.vert",
                    "postProcessCombine.frag");
            postProcessCombineShader.use();
            GL20.glUniform1i(postProcessCombineShader.getUniformLocation("scene"), 0);
            GL20.glUniform1i(postProcessCombineShader.getUniformLocation("reflection"), 1);
            GL20.glUniform1i(postProcessCombineShader.getUniformLocation("effect"), 2);
            GL20.glUniform1i(postProcessCombineShader.getUniformLocation("glow"), 3);

            ScreenShaderProgram postProcessBloomShader = new ScreenShaderProgram("screenShader.vert",
                    "postProcessBloom.frag");
            postProcessBloomShader.use();
            GL20.glUniform1i(postProcessBloomShader.getUniformLocation("image"), 0);

            ScreenShaderProgram postProcessSharpenShader = new ScreenShaderProgram("screenShader.vert",
                    "postProcessSharpen.frag");
            postProcessSharpenShader.use();
            GL20.glUniform1i(postProcessSharpenShader.getUniformLocation("image"), 0);

            // ====== Start MainClient

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            viewController = new ClientViewController(renderer, menuView, mainClient);
            uiController.cvc = viewController;
            viewController.setVectorRenderer(vRenderer);

            GL11.glClearColor(1f, 1f, 1f, 1.f);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glCullFace(GL11.GL_BACK);

            NkColor clear = NkColor.create();
            Nuklear.nk_rgba(10, 10, 10, 0, clear);

            System.out.println("\nGame Loop started");
            // main window loop
            try {
                while (!window.isCloseRequested()) {
                    timer.startFrame();

                    renderer.clearFramebuffers();
                    renderer.prepare();

                    viewController.render();

                    renderer.postProcessCombine(postProcessCombineShader);

                    // postProcessBloomShader.use();
                    // for (int i = 0; i < 2; i++) {
                    // GL20.glUniform1i(postProcessBloomShader.getUniformLocation("horizontal"),
                    // 0);
                    // renderer.postProcess(postProcessBloomShader, 0.2f,
                    // Framebuffer.SPARE);
                    // GL20.glUniform1i(postProcessBloomShader.getUniformLocation("horizontal"),
                    // 1);
                    // renderer.postProcess(postProcessBloomShader, 0.2f,
                    // Framebuffer.SPARE);
                    // }

                    if (SettingsManager.sharpen > 0) {
                        renderer.postProcess(postProcessSharpenShader, SettingsManager.sharpen);
                    }
                    renderer.renderResultToScreen();

                    // render UI over the rest to the default framebuffer
                    renderer.bindFramebuffer(Framebuffer.DEFAULT);

                    viewController.renderHUD();
                    uiRenderer.render();
                    vRenderer.setColor(1.f, .0f, 1.f, 1f);

                    // swap buffers (show last rendered image)
                    window.swapBuffers();

                    // polls input and syncs to framerate
                    uiRenderer.input();
                    menuView.show();

                    Interpolator.getInstance().use();

                    if (!resolutionChanged && window.isCloseRequested()) {
                        menuView.endGame();
                        window.cancelCloseRequest();
                    }

                    mainClient.windowStarted.countDown();

                    // timer.printFPS(1);
                }
            } catch (IllegalStateException e) {
                return;
            }

            uiRenderer.cleanUp();
            viewController.cleanUp();
        } while (resolutionChanged);

        menuView.getMenuLogic().stopServer();
        mainClient.close();
        uiRenderer.shutdown();
        System.exit(0);
    }

    /**
     * Displays the map
     * 
     * @param map
     */
    public void displayMap(Map map) {
        viewController.displayMap(map);
    }

    /**
     * Displays the users
     * 
     * @param users
     */
    public void displayUsers(HashMap<Integer, User> users) {
        viewController.displayUsers(users);
    }

    /**
     * removes all models from the renderer
     */
    public void cleanUpGame() {
        mainClient.clearParticipants();
        mainClient.updateUserList();
        viewController.clearModels();
        Interpolator.getInstance().clearAllExcept(new Integer[] { viewController.moveX, viewController.moveZ,
                viewController.pitch, viewController.rotate, viewController.zoom });
    }

    /**
     * Shows the users in the LobbyMenu
     */
    public void updateUsers(HashMap<Integer, User> users) {
        menuView.updateUsers(users);
        viewController.removeAllUsersFromHUD();
        viewController.displayUsers(users);
    }

    /**
     * displays the robots
     * 
     * @param robots
     */
    public void displayRobots(ArrayList<RobotController> robots) {
        viewController.displayRobots(robots);
    }

    /**
     * updates the checkpointModels (they change their color when reached by
     * someone)
     * 
     * @param isPlayer
     */
    public void updateCheckpointModels() {
        viewController.updateCheckpoints(false);
    }

    /**
     * Setter
     */
    public void gotConnected() {
        menuView.getMenuLogic().setState(MenuState.GAME_RUNNING, true);
    }

    /**
     * Getter
     * 
     * @return Op int
     */
    public int getOp() {
        return menuView.getOp();
    }

    /**
     * Setter
     * 
     * @param state
     */
    public void setState(MenuState state) {
        menuView.setState(state);
    }

    /**
     * Setter (in case of win event)
     * 
     * @param state
     * @param winner
     */
    public void setState(MenuState state, String winner) {
        menuView.setState(state, winner);
    }

    /**
     * Setter
     * 
     * @param firstRound
     */
    public void setReset(boolean firstRound) {
        viewController.reset(firstRound);
    }

    /**
     * Setter
     */
    public void setStartTimer() {
        viewController.setStartTimer(mainClient.getStartTimer());
    }

    /**
     * to set the resolution
     * 
     * @param targetWidth
     * @param targetHeight
     */
    public void setTargetWidth(int targetWidth, int targetHeight) {
        this.targetWidth = targetWidth;
        this.targetHeight = targetHeight;
        this.resolutionChanged = true;
        GLFW.glfwSetWindowShouldClose(Window.window, true);
    }

    /**
     * Getter
     * 
     * @return viewController ClientViewController
     */
    public ClientViewController getViewController() {
        return viewController;
    }

    /**
     * Getter
     * 
     * @return state MenuState
     */
    public MenuState getState() {
        return menuView.getState();
    }

    /**
     * Getter
     * 
     * @return menuView
     */
    public MenuView getMenuView() {
        return menuView;
    }
}