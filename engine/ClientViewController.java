package engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Consumer;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NanoVG;
import org.lwjgl.opengl.GL20;

import ai.WorldAnalyser;
import ai.WorldSimulator;
import ai.world.Field;
import ai.world.World;
import client.MainClient;
import client.User;
import constants.CardType;
import constants.LaserColor;
import constants.Orientation;
import constants.Role;
import core.math.Axis;
import core.math.Matrix4f;
import core.math.Vector2f;
import core.math.Vector3f;
import core.math.Vector4f;
import core.model.Model;
import core.model.ModelLoader;
import core.renderer.SortedRenderer;
import core.renderer.VectorRenderer;
import core.renderer.SortedRenderer.Framebuffer;
import core.shader.ScreenShaderProgram;
import core.shader.ShaderProgram;
import core.texture.Texture;
import core.texture.TextureLoader;
import core.window.Interpolator;
import core.window.Timer;
import leveleditor.src.engine.math.vec2i;
import leveleditor.src.engine.math.vec3;
import gui.ClickableField;
import gui.ClickableOverlay;
import gui.DirectionSelection;
import gui.UIView;
import javafx.scene.shape.Polygon;
import gui.Draggable;
import gui.Droppable;
import gui.MenuView;
import gui.Shape;
import logic.Card;
import logic.Robot;
import logic.RobotController;
import map.Checkpoint;
import map.HealthField;
import map.Laser;
import map.Map;
import map.Portal;
import utilities.SettingsManager;
import utilities.TurnTimer;

/**
 * A class that visualizes the game state and handles all <br>
 * textures, shader and models
 */
public class ClientViewController {

    /**
     * show cardNames on textures;
     */
    private static final boolean SHOW_CARD_NAMES = false;
    /**
     * the total time each animation takes
     */
    public static float totalAnimationTime = 0.5f;
    /**
     * the map that is visualized
     */
    private Map map;
    /**
     * a list of robots that are visualized on the map
     */
    private ArrayList<RobotController> robotList;
    /**
     * a list of users that are displayed in a user list
     */
    private HashMap<Integer, User> users = null;
    /**
     * Cards that show on display
     */
    private Draggable[] draggables = new Draggable[9];
    /**
     * Left Border of the cardSelectTable
     */
    public static int selectTableX = Window.width / 4;
    /**
     * Top Border of the cardSelectTable
     */
    public static int selectTableY = Window.height / 12;
    /**
     * Width of the cardSelectTable
     */
    public static int selectTableWidth = Window.width / 2;
    /**
     * Height of the cardSelectTable
     */
    public static int selectTableHeight = Window.height / 5;
    /**
     * height where the vertical middle of a card should be
     */
    public static int draggablePositionY = Window.height / 6 * 5;
    /**
     * if there are Cards to select
     */
    public static boolean cardselect = false;
    /**
     * if player wants to hide cardselection
     */
    public boolean hideCardselect = false;
    /**
     * orientation the player has chosen
     */
    public static Orientation orientation = null;
    /**
     * World to simmulate cards
     */
    World world = null;
    /**
     * a list of robots that are visualized on the map
     */
    public static ArrayList<ClickableOverlay> overlays = new ArrayList<ClickableOverlay>();
    /**
     * a list of player uids that are displayed in the user list
     */
    private LinkedList<String> playerUid = new LinkedList<String>();
    /**
     * a list of player names that are displayed in the user list
     */
    private LinkedList<String> playerName = new LinkedList<String>();
    /**
     * a list of the health stats of all players, that are displayed in the user
     * list
     */
    private LinkedList<Integer> playerHealth = new LinkedList<Integer>();
    /**
     * a list of the current checkpoints of all players, that are displayed in
     * the user list
     */
    private LinkedList<Integer> playerCps = new LinkedList<Integer>();
    /**
     * an array that contains the color of every user that is displayed in the
     * user list
     */
    private Vector3f[] playerColors;
    /**
     * the width of the map
     */
    private int mapDimX;
    /**
     * the height of the map
     */
    private int mapDimZ;

    private boolean startTimer;
    /**
     * a timer that is used to display the time till the next round result
     * message
     */
    TurnTimer tTimer;

    /**
     * MenuView menuView
     */
    MenuView menuView;

    /**
     * MainClient mainClient
     */
    MainClient mainClient;

    /**
     * the camera that views the playing field
     */
    Camera camera;

    // Shader

    /**
     * the shader that is used to display the background (fancy stars)
     */
    ScreenShaderProgram backgroundShader;
    /**
     * the basic shader program
     */
    ShaderProgram shaderProgram;
    /**
     * the shader that is used to display the border
     */
    ShaderProgram borderShaderProgram;
    /**
     * the shader that is used to display checkpoints
     */
    ShaderProgram checkpointShaderProgram;
    /**
     * the shader that is used to display walls
     */
    ShaderProgram wallShaderProgram;
    /**
     * the shader that is used to display robots
     */
    ShaderProgram roboShaderProgram;
    /**
     * the shader that is used to display the health fields
     */
    ShaderProgram healthFieldShaderProgram;
    /**
     * the shader that is used to display portal effects
     */
    ShaderProgram portalShaderProgram;
    /**
     * the shader that is used to display portals and laserModels
     */
    ShaderProgram matcapShaderProgram;
    /**
     * the shader that is used to display the laser
     */
    LaserShader laserShaderProgram;

    // Models
    /**
     * the basic tile model
     */
    final String TILE_MODEL_PROTO = "res/tile.obj";
    /**
     * the basic border around the playing field
     */
    final String BORDER_MODEL_PROTO = "res/border.obj";
    /**
     * the basic checkpoint model
     */
    final String CHECKPOINT_MODEL_PROTO = "res/checkpointSmall.obj";
    /**
     * the basic laser model
     */
    final String LASER_MODEL_PROTO = "res/laser.obj";
    /**
     * the basic portal exit model
     */
    final String PORTAL_MODEL_PROTO = "res/portal_out.obj";
    /**
     * the poral effect model
     */
    final String PORTAL_EFFECT_MODEL_PROTO = "res/portal_effect.obj";
    /**
     * the basic health field model
     */
    final String HEALTH_FIELD_MODEL_PROTO = "res/healthField.obj";
    /**
     * the basic wall model
     */
    final String WALL_MODEL_PROTO = "res/RoboWand.obj";
    /**
     * the basic robot model
     */
    final String ROBOT_PROTO = "res/gardeningrobo.obj";

    // Textures and matcaps
    /**
     * the texture of a tile
     */
    final String TILE = "res/tile.png";
    /**
     * the texture of a checkpoint tile
     */
    final String CHECKPOINT_TILE = "res/checkpointTile.png";
    /**
     * The matcaps for the checkpointModels
     */
    // used by the player perspective only
    final String VISITED_CP_TEX = "res/matcaps/dark_green.png";
    final String ACTIVE_CP_TEX = "res/matcaps/light_green.png";
    final String NEXT_CP_TEX = "res/matcaps/blue.png";
    final String NOT_VISITED_CP_TEX = "res/matcaps/red.png";

    // used by the spectator perspective only
    final String START_CP_TEX = "res/matcaps/light_green.png";
    final String VISITED_BY_ANYONE_CP_TEX = "res/matcaps/blue.png";
    final String VISITED_BY_NOONE_CP_TEX = "res/matcaps/red.png";

    // used by both perspectives
    final String FINISH_CP_TEX = "res/matcaps/golden.png";
    final String ROBOT_TEX = "res/gardeningrobo_tex.png";
    final String ROBOT_TEX2 = "res/matcaps/matcap_robo.png";

    /**
     * indicates if the color of a checkpoint has to be changed
     */
    public boolean checkpointsChanged = true;
    /**
     * the textures for the laser models
     */
    final String LASER_MODEL_TEX = "res/matcaps/laser.png";
    final String BLUE_LASER_TEX = "res/blueLaserTex.png";
    final String RED_LASER_TEX = "res/redLaserTex.png";
    /**
     * the textures for the portal effects
     */
    final String PORTAL_IN_TEX = "res/portal_effect.png";
    final String PORTAL_OUT_TEX = "res/portal_effect_reverse.png";
    final String PORTAL_DEPTH = "res/portal_effect_depth.png";
    /**
     * the texture of the health field
     */
    final String HEALTH_FIELD_TEX = "res/healthFieldTex.png";
    /**
     * the texture indicating that the card selection is valid
     */
    final String VALID_SELECTION_TEX = "res/cardsValid.png";
    /**
     * the texture indicating that the card selection is invalid
     */
    final String INVALID_SELECTION_TEX = "res/cardsInvalid.png";
    /**
     * model renderer
     */
    static SortedRenderer renderer;
    /**
     * text renderer
     */
    static VectorRenderer vRenderer;
    /**
     * lists the models which have to be removed in the next turn (checkpoint
     * models)
     */
    ArrayList<Model> modelsToBeRemovedList;
    /**
     * lists the robo models
     */
    ArrayList<Model> roboModels = new ArrayList<Model>();
    /**
     * height at which the lasers hang on the walls, in length units
     */
    final float LASER_HEIGHT = 0.1f;
    /**
     * length of the laser shooter (how long it is measured from the line where
     * it is positioned), in length units
     */
    final float SHOOTER_LENGTH = 0.36f;
    /**
     * if the {@link #laserShooting} was changed
     */
    private boolean laserChanged = true;
    /**
     * if the lasers are shooting
     */
    private boolean laserShooting = true;

    /** value the interpolator uses to rotate the camera */
    public int rotate;

    /** value the interpolator uses to pitch the camera */
    public int pitch;

    /** value the interpolator uses to zoom */
    public int zoom;

    /** value the interpolator uses to move the camera */
    public int moveX;

    /** value the interpolator uses to move the camera */
    public int moveZ;

    /** the selected card while dragging and droping */
    private Draggable selectedDraggable = null;

    /** radius of the credits minigames objects */
    public final static int KUEK_RAD = Window.height / 20;

    /** pos of the credits minigames objects */
    static LinkedList<Droppable> kueks = new LinkedList<Droppable>();

    /** credits?! */
    private static LinkedList<Texture> creditTo = new LinkedList<Texture>();

    /** boolean if textures drops */
    private static boolean creditsDropping = false;
    /**  */
    private static int creditIterator = 0;
    private static int creditToIterator = 0;

    public static Texture TEXTUR_KUEK;
    public static Texture TEXTUR_KAI;
    public static Texture TEXTUR_NIK;
    public static Texture TEXTUR_LIX;
    public static Texture TEXTUR_MAXI;
    private static Texture TEXTUR_SCHREDD;

    /** score */
    private static int creditsScore = 0;
    /** secret score (if enought, game starts after credits) */
    private static int secretCreditScore = 0;

    /** boolean if schredder should be activated */
    public static boolean creditsMiGaEnabled = false;

    public static boolean creditsFinished = true; // false;

    /** difficulty of the credits */
    public static float kuekSpeed = 3.5f;

    /** difficulty of the credits */
    public static float kuekSinMod = 0.0f;

    /** position of the schredder */
    public static float schreddX = Window.width / 2;

    /** position of the schredder */
    public static float schreddY = Window.height - Window.height / 20;

    /** width of the schredder */
    private static float schreddWidth = Window.width / 10;

    /** displays the destination of currently selected cards */
    private RoboModel destinationPreview;

    /** the world object used to simulate end positions of the user */
    private World simulatedWorld;

    /** all fields on the map */
    private Model[][] tileModels;

    public Texture[] tut = new Texture[10];

    /** a clickable overlay that dislays the selectable orientations */
    private DirectionSelection directionSelection;

    /** the last temporary visible selection */
    private ClickableField lastTemporarySelection = null;

    /**
     * used to test if a clickable overlay was clicked on purpose or on accident
     */
    private Shape temporaryShape;

    /**
     * Creates and initializes all models, shaders and textures/matcaps
     * 
     * @param renderer
     *            the renderer that should render the scene
     */
    public ClientViewController(SortedRenderer render, MenuView menuView, MainClient mainClient) {
        this.menuView = menuView;
        renderer = render;
        this.mainClient = mainClient;

        // create the shaders
        backgroundShader = new ScreenShaderProgram("background.vert", "background.frag");
        // basic shader
        shaderProgram = new ShaderProgram("static.vert", "static.frag");
        shaderProgram.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(shaderProgram.getProgramID(), "tex"), 0);
        // border shader
        borderShaderProgram = new ShaderProgram("static.vert", "border.frag");
        borderShaderProgram.use();
        // checkpoint shader
        checkpointShaderProgram = new ShaderProgram("static.vert", "staticCheckpoint.frag");
        checkpointShaderProgram.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(checkpointShaderProgram.getProgramID(), "tex"), 0);
        // wall shader
        wallShaderProgram = new ShaderProgram("static.vert", "staticWall.frag");
        wallShaderProgram.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(wallShaderProgram.getProgramID(), "tex"), 0);
        // simple matcap shader (portals and laserModels)
        matcapShaderProgram = new ShaderProgram("static.vert", "simpleMatcapShader.frag");
        matcapShaderProgram.use();
        GL20.glUniform1i(GL20.glGetUniformLocation(matcapShaderProgram.getProgramID(), "tex"), 0);
        GL20.glUniform1i(GL20.glGetUniformLocation(matcapShaderProgram.getProgramID(), "tex2"), 1);
        // robot shader
        roboShaderProgram = new ShaderProgram("roboShader.vert", "roboShader.frag");
        roboShaderProgram.use();
        GL20.glUniform1i(roboShaderProgram.getUniformLocation("tex"), 0);
        GL20.glUniform1i(roboShaderProgram.getUniformLocation("tex2"), 1);
        // healthField shader
        healthFieldShaderProgram = new ShaderProgram("healthShader.vert", "healthShader.frag");
        healthFieldShaderProgram.use();
        GL20.glUniform1i(healthFieldShaderProgram.getUniformLocation("tex"), 0);
        GL20.glUniform1i(healthFieldShaderProgram.getUniformLocation("tex2"), 1);
        // portal effect shader
        portalShaderProgram = new ShaderProgram("portalShader.vert", "portalShader.frag");
        portalShaderProgram.use();
        GL20.glUniform1i(portalShaderProgram.getUniformLocation("tex"), 0);
        GL20.glUniform1i(portalShaderProgram.getUniformLocation("depth"), 1);
        // laser shader
        laserShaderProgram = new LaserShader("laserShader.vert", "laserShader.geom", "laserShader.frag",
                "res/laser.png");

        // create the camera
        camera = new Camera();
        // define inverse camera for reflections
        Consumer<Boolean> cameraReflect = reflect -> camera.setReflected(reflect);
        renderer.enableMirrorAction(cameraReflect);

        // enable camera movement
        rotate = Interpolator.getInstance().register(1, (x) -> camera.rotate(x), Interpolator.PARABEL_FUNCTION_BACK,
                190);
        pitch = Interpolator.getInstance().register(1, (x) -> camera.pitch(x), Interpolator.PARABEL_FUNCTION);
        zoom = Interpolator.getInstance().register(1, (x) -> camera.zoom(x), Interpolator.PARABEL_FUNCTION);
        moveX = Interpolator.getInstance().register(1, (x) -> camera.translateX(x), Interpolator.PARABEL_FUNCTION);
        moveZ = Interpolator.getInstance().register(1, (x) -> camera.translateY(x), Interpolator.PARABEL_FUNCTION);

        // load all models
        CardType.loadTextures();
        TEXTUR_KUEK = TextureLoader.loadTexture("res/credits/schredd/temp/kuek.png");
        TEXTUR_SCHREDD = TextureLoader.loadTexture("res/credits/schredd/temp/schredd.png");
        TEXTUR_KAI = TextureLoader.loadTexture("res/credits/schredd/temp/Gerhard.png");
        TEXTUR_NIK = TextureLoader.loadTexture("res/credits/schredd/temp/malte.png");
        TEXTUR_MAXI = TextureLoader.loadTexture("res/credits/schredd/temp/blasi.png");
        TEXTUR_LIX = TextureLoader.loadTexture("res/credits/schredd/temp/Felix.png");
        creditTo.add(0, TextureLoader.loadTexture("res/credits/Credits/RoboRally.png"));
        creditTo.add(1, TextureLoader.loadTexture("res/credits/Credits/Credits.png"));
        creditTo.add(2, TextureLoader.loadTexture("res/credits/Credits/ThanksTo.png"));
        creditTo.add(3, TextureLoader.loadTexture("res/credits/Credits/Adrian.png"));
        creditTo.add(4, TextureLoader.loadTexture("res/credits/Credits/Sean.png"));
        creditTo.add(5, TextureLoader.loadTexture("res/credits/Credits/Benni.png"));
        creditTo.add(6, TextureLoader.loadTexture("res/credits/Credits/Jojo.png"));
        creditTo.add(7, TextureLoader.loadTexture("res/credits/Credits/Maxi.png"));
        creditTo.add(8, TextureLoader.loadTexture("res/credits/Credits/Kai.png"));

        ModelLoader.loadModel(TILE_MODEL_PROTO);
        ModelLoader.loadModel(BORDER_MODEL_PROTO);
        ModelLoader.loadModel(CHECKPOINT_MODEL_PROTO);
        ModelLoader.loadModel(LASER_MODEL_PROTO);
        ModelLoader.loadModel(PORTAL_MODEL_PROTO);
        ModelLoader.loadModel(PORTAL_EFFECT_MODEL_PROTO);
        ModelLoader.loadModel(HEALTH_FIELD_MODEL_PROTO);
        ModelLoader.loadModel(WALL_MODEL_PROTO);
        ModelLoader.loadModel(ROBOT_PROTO);

        tut[0] = TextureLoader.loadTexture("res/tutorial/Tut1.png");
        tut[1] = TextureLoader.loadTexture("res/tutorial/Tut2.png");
        tut[2] = TextureLoader.loadTexture("res/tutorial/Tut3.png");
        tut[3] = TextureLoader.loadTexture("res/tutorial/Tut4.png");
        tut[4] = TextureLoader.loadTexture("res/tutorial/Tut5.png");
        tut[5] = TextureLoader.loadTexture("res/tutorial/Tut6.png");
        tut[6] = TextureLoader.loadTexture("res/tutorial/Tut7.png");
        tut[7] = TextureLoader.loadTexture("res/tutorial/Tut8.png");
        tut[8] = TextureLoader.loadTexture("res/tutorial/Tut9.png");
        tut[9] = TextureLoader.loadTexture("res/tutorial/Tut10.png");

        // load all textures/matcaps

        TextureLoader.loadTexture(TILE);
        TextureLoader.loadTexture(CHECKPOINT_TILE);
        TextureLoader.loadTexture(VISITED_CP_TEX);
        TextureLoader.loadTexture(ACTIVE_CP_TEX);
        TextureLoader.loadTexture(NEXT_CP_TEX);
        TextureLoader.loadTexture(NOT_VISITED_CP_TEX);
        TextureLoader.loadTexture(START_CP_TEX);
        TextureLoader.loadTexture(VISITED_BY_ANYONE_CP_TEX);
        TextureLoader.loadTexture(VISITED_BY_NOONE_CP_TEX);
        TextureLoader.loadTexture(FINISH_CP_TEX);

        TextureLoader.loadTexture(LASER_MODEL_TEX);
        TextureLoader.loadTexture(BLUE_LASER_TEX);
        TextureLoader.loadTexture(RED_LASER_TEX);

        TextureLoader.loadTexture(PORTAL_IN_TEX);
        TextureLoader.loadTexture(PORTAL_OUT_TEX);
        TextureLoader.loadTexture(PORTAL_DEPTH);

        TextureLoader.loadTexture(HEALTH_FIELD_TEX);

        TextureLoader.loadTexture(ROBOT_TEX);
        TextureLoader.loadTexture(ROBOT_TEX2);

        TextureLoader.loadTexture(VALID_SELECTION_TEX);
        TextureLoader.loadTexture(INVALID_SELECTION_TEX);

        // initialize text renderer
        vRenderer = new VectorRenderer();

        modelsToBeRemovedList = new ArrayList<Model>();
    }

    /**
     * displays the given map on the screen
     * 
     * @param map
     *            the map that should be displayed
     */
    public void displayMap(Map map) {
        this.map = map;
        mapDimX = map.getWidth();
        mapDimZ = map.getHeight();
        // set camera movement borders
        camera.setMapDimensions(mapDimX, mapDimZ);
        // start turn timer with card selection time
        tTimer = new TurnTimer(menuView.getMenuLogic(), map);
        new Thread(tTimer).start();
        // initialize all models from the map
        checkpointsChanged = true;
        addMapModelsToRenderer();
        startLasers();
        simulatedWorld = new World(map, null);

        // direction selection arrows
        directionSelection = new DirectionSelection(
                new Vector2f(map.getCheckpoints()[0].getPosition()[0], map.getCheckpoints()[0].getPosition()[1]));
        directionSelection.setVisible(false);
        directionSelection.setAction((x) -> {
            directionSelection.select(x);
            updatePreviewPosition();
        });

        overlays.add(directionSelection);

        // click fields for every field
        for (int i = 1; i <= mapDimX; i++) {
            for (int j = 1; j <= mapDimZ; j++) {
                final int x = i - 1;
                final int y = j - 1;
                ClickableField field = new ClickableField(new Vector2f(i, j));
                field.setAction((index) -> {
                    if (lastTemporarySelection != null)
                        lastTemporarySelection.getDirectionSelection().setDirectionsVisible(new boolean[4]);
                    lastTemporarySelection = field;
                    Orientation direction = Orientation.UP;
                    if (world == null || mainClient.getMe().ROLE.equals(Role.SPECTATOR))
                        return;
                    if (index == 0) {
                        field.getDirectionSelection().setDirectionsVisible(world.getFields()[x][y].reachable);
                        return;
                    } else if (index == 1) {
                        setSelectedCards(world.getFields()[x][y].paths[2]);
                        direction = world.getFields()[x][y].neededOrientation[2];
                    } else if (index == 2) {
                        setSelectedCards(world.getFields()[x][y].paths[1]);
                        direction = world.getFields()[x][y].neededOrientation[1];
                    } else if (index == 3) {
                        setSelectedCards(world.getFields()[x][y].paths[0]);
                        direction = world.getFields()[x][y].neededOrientation[0];
                    } else if (index == 4) {
                        setSelectedCards(world.getFields()[x][y].paths[3]);
                        direction = world.getFields()[x][y].neededOrientation[3];
                    }
                    switch (direction) {
                    case DOWN:
                        directionSelection.select(0);
                        break;
                    case RIGHT:
                        directionSelection.select(1);
                        break;
                    case UP:
                        directionSelection.select(2);
                        break;
                    case LEFT:
                        directionSelection.select(3);
                        break;
                    }
                    updatePreviewPosition();
                });
                overlays.add(field);
            }
        }
    }

    /**
     * returns the selected orientation
     */
    public Orientation getSelectedOrientation() {
        return directionSelection.getSelection();
    }

    /**
     * displays the given robots on the screen
     * 
     * @param robots
     *            the robots that should be displayed
     */
    public void displayRobots(ArrayList<RobotController> robots) {
        this.robotList = robots;
        // generate distinct colors for all robots
        playerColors = generateColors(robotList.size());
        // initialize all robot models
        addRobotModelsToRenderer();
    }

    /**
     * removes all models from the renderer
     */
    public void clearModels() {
        renderer.clearRenderModels();
        stopLasers();
        overlays.clear();
    }

    /**
     * updates the data which is used to display the player list
     * 
     * @param userMap
     *            the list of users that should be displayed
     */
    public void displayUsers(HashMap<Integer, User> userMap) {
        // clear previous user lists
        if (users != null) {
            users.clear();
            playerUid.clear();
            playerHealth.clear();
            playerName.clear();
            playerCps.clear();
        }
        this.users = userMap;
        int i = 0;
        // add information for every user
        for (Integer uid : users.keySet()) {
            if (users.get(uid).getRobot() != null && users.get(uid).getRobot().isAlive()) {
                playerUid.add(String.valueOf(uid));
                playerName.add(users.get(uid).getName());
                playerHealth.add(users.get(uid).getRobot().getLives());
                playerColors[i] = users.get(uid).getRobot().model.getPlayerColor();
                playerCps.add(users.get(uid).getRobot().getCheckpoint());
                i++;
            }
        }
    }

    /**
     * initializes all robot models from the robotModel list
     */
    private void addRobotModelsToRenderer() {
        roboModels.clear();
        // initialize every robot
        for (int i = 0; i < robotList.size(); i++) {
            RobotController robot = robotList.get(i);
            // create a new model
            RoboModel robotModel = new RoboModel(ModelLoader.loadModel(ROBOT_PROTO)).copy();
            // set position of the model to the position of the robot
            robotModel.translate(new Vector3f(robot.getFieldX(), 0, robot.getFieldY()));
            // set up orientation
            switch (robot.getOrientation()) {
            case LEFT:
                robotModel.rotate(90, Axis.Y);
                break;
            case DOWN:
                robotModel.rotate(-90, Axis.Y);
                // No break
            case RIGHT:
                robotModel.rotate(-90, Axis.Y);
                break;
            default:
                // Nothing, already facing up
            }
            // set textures/matcaps
            robotModel.setTexture(0, TextureLoader.loadTexture(ROBOT_TEX));
            robotModel.setTexture(1, TextureLoader.loadTexture(ROBOT_TEX2));
            // add distinct player color to the model
            robotModel.setPlayerColor(playerColors[i]);
            robotModel.scale(0);
            robot.model = robotModel;
            roboModels.add(robotModel);
            // enable robot animations
            robot.moveX = Interpolator.getInstance().register(totalAnimationTime,
                    (x) -> robot.model.translate(new Vector3f(x, 0, 0)), Interpolator.PARABEL_FUNCTION, true, true);
            robot.moveY = Interpolator.getInstance().register(totalAnimationTime,
                    (y) -> robot.model.translate(new Vector3f(0, y, 0)), Interpolator.PARABEL_FUNCTION, true, true);
            robot.moveZ = Interpolator.getInstance().register(totalAnimationTime,
                    (z) -> robot.model.translate(new Vector3f(0, 0, z)), Interpolator.PARABEL_FUNCTION, true, true);
            robot.rotate = Interpolator.getInstance().register(totalAnimationTime,
                    (angle) -> robot.model.rotate(angle, Axis.Y), Interpolator.PARABEL_FUNCTION, true, true);
            robot.scale = Interpolator.getInstance().register(totalAnimationTime,
                    (scale) -> robot.model.scaleAbs(scale), Interpolator.PARABEL_FUNCTION, false, true);
            // used for events where no animations are needed. Calling this does

        }
        // add all models to the renderer
        renderer.addRenderModels(roboModels, roboShaderProgram);
    }

    /**
     * cleans everything opened by the clientViewController
     */
    public void cleanUp() {
        // close timer thread
        if (tTimer != null) {
            tTimer.close();
        }
        // clean up the window
        Window.getInstance().close();
        Window.getInstance().cleanUp();
    }

    /**
     * sets vRenderer to the given VectorRenderer
     * 
     * @param vr
     *            the vectorRenderer that should be used, should not be null
     */
    public void setVectorRenderer(VectorRenderer vr) {
        if (vr != null) {
            vRenderer = vr;
        }
    }

    /**
     * creates and initializes all models, that are contained in the map and
     * gives them to the renderer
     */
    private void addMapModelsToRenderer() {
        // create lists for walls, checkpoints and tiles
        ArrayList<Model> tileModelList = new ArrayList<Model>();
        ArrayList<Model> wallModelList = new ArrayList<Model>();
        ArrayList<Model> checkpointModelList = new ArrayList<Model>();
        ArrayList<Model> laserModelList = new ArrayList<Model>();
        ArrayList<Model> portalEntryModelList = new ArrayList<Model>();
        ArrayList<Model> portalExitModelList = new ArrayList<Model>();
        ArrayList<Model> portalEffectModelList = new ArrayList<Model>();
        ArrayList<Model> healthFieldModelList = new ArrayList<Model>();

        boolean[][] rightWalls = map.getRightWallsArray();
        boolean[][] downWalls = map.getDownWallsArray();
        boolean[][] checkpointArray = map.getCheckpointsArray();
        Laser[] lasers = map.getLasers();
        Portal[] portals = map.getPortals();
        HealthField[] healthFields = map.getHealthFields();

        Model border = ModelLoader.loadModel(BORDER_MODEL_PROTO).copy();
        border.setPosition(new Vector3f(0.5f, 0, 0.5f));
        border.setScale(new Vector3f(mapDimX, (mapDimX + mapDimZ) / 5, mapDimZ));
        renderer.addRenderModel(border, borderShaderProgram);

        // initialize all tiles
        ModelLoader.loadModel(TILE_MODEL_PROTO).setTexture(0, TextureLoader.loadTexture(TILE));
        tileModels = new Model[mapDimX + 1][mapDimZ + 1];
        // for every position on the map
        for (int x = 0; x <= mapDimX; x++) {
            for (int z = 0; z <= mapDimZ; z++) {
                // don't display fields with position <1
                if (x > 0 && z > 0) {
                    // create new model
                    Model tileModel = ModelLoader.loadModel(TILE_MODEL_PROTO).copy();
                    // set position of the model to the position of the tile
                    tileModel.translate(new Vector3f(x, 0, z));
                    // set texture
                    if (checkpointArray[x][z]) {
                        tileModel.setTexture(0, TextureLoader.loadTexture(CHECKPOINT_TILE));
                    } else {
                        tileModel.setTexture(0, TextureLoader.loadTexture(TILE));
                    }
                    tileModels[x][z] = tileModel;
                    tileModelList.add(tileModel);
                }
                // if there is a wall below the current field, it's added
                if (downWalls[x][z]) {
                    // create new model
                    Model downWallModel = ModelLoader.loadModel(WALL_MODEL_PROTO).copy();
                    // set position of the wall model to the position of the map
                    downWallModel.translate(new Vector3f(x, 0f, z + 0.5f));
                    wallModelList.add(downWallModel);
                }

                // if there is a wall right of the current field, it's added
                if (rightWalls[x][z]) {
                    // create new model
                    Model rightWallModel = ModelLoader.loadModel(WALL_MODEL_PROTO).copy();
                    // set position of the wall model to the position of the map
                    rightWallModel.translate(new Vector3f(x + 0.5f, 0f, z));
                    rightWallModel.rotate(90, Axis.Y);
                    wallModelList.add(rightWallModel);
                }
            }
        }

        updateCheckpoints(true);

        for (Laser laser : lasers) {
            Model laserModel = ModelLoader.loadModel(LASER_MODEL_PROTO).copy();
            switch (laser.getDirection()) {
            case UP:
                laserModel.rotate(180, Axis.Y);
                laserModel.translate(new Vector3f(0, 0, 0.5f));
                break;
            case RIGHT:
                laserModel.rotate(90, Axis.Y);
                laserModel.translate(new Vector3f(-0.5f, 0, 0));
                break;
            case DOWN:
                laserModel.rotate(0, Axis.Y);
                laserModel.translate(new Vector3f(0, 0, -0.5f));
                break;
            case LEFT:
                laserModel.rotate(-90, Axis.Y);
                laserModel.translate(new Vector3f(0.5f, 0, 0));
                break;
            }
            if (laser.isPiercing()) {
                laserModel.setTexture(0, TextureLoader.loadTexture(LASER_MODEL_TEX));
                laserModel.setTexture(1, TextureLoader.loadTexture(BLUE_LASER_TEX));
            } else {
                laserModel.setTexture(0, TextureLoader.loadTexture(LASER_MODEL_TEX));
                laserModel.setTexture(1, TextureLoader.loadTexture(RED_LASER_TEX));
            }
            laserModel.translate(new Vector3f(laser.getFieldX(), 0, laser.getFieldY()));
            laserModelList.add(laserModel);
        }

        for (Portal portal : portals) {
            Model portalEntryModel = ModelLoader.loadModel(PORTAL_MODEL_PROTO).copy();
            Model portalExitModel = ModelLoader.loadModel(PORTAL_MODEL_PROTO).copy();
            Model portalEffectModelIn = ModelLoader.loadModel(PORTAL_EFFECT_MODEL_PROTO).copy();
            Model portalEffectModelOut = ModelLoader.loadModel(PORTAL_EFFECT_MODEL_PROTO).copy();

            Vector3f entryPos = new Vector3f(portal.getStartFieldX(), 0f, portal.getStartFieldY());
            Vector3f exitPos = new Vector3f(portal.getEndFieldX(), 0f, portal.getEndFieldY());
            portalEntryModel.translate(entryPos);
            portalEffectModelIn.translate(entryPos);
            portalExitModel.translate(exitPos);
            portalEffectModelOut.translate(exitPos);

            portalEntryModel.setTexture(0, TextureLoader.loadTexture(LASER_MODEL_TEX));
            portalExitModel.setTexture(0, TextureLoader.loadTexture(LASER_MODEL_TEX));
            portalEffectModelIn.setTexture(0, TextureLoader.loadTexture(PORTAL_IN_TEX));
            portalEffectModelIn.setTexture(1, TextureLoader.loadTexture(PORTAL_DEPTH));
            portalEffectModelOut.setTexture(0, TextureLoader.loadTexture(PORTAL_OUT_TEX));
            portalEffectModelOut.setTexture(1, TextureLoader.loadTexture(PORTAL_DEPTH));

            portalEntryModelList.add(portalEntryModel);
            portalExitModelList.add(portalExitModel);
            portalEffectModelList.add(portalEffectModelIn);
            portalEffectModelList.add(portalEffectModelOut);
        }

        for (HealthField healthField : healthFields) {
            Model healthFieldModel = ModelLoader.loadModel(HEALTH_FIELD_MODEL_PROTO).copy();

            healthFieldModel.translate(new Vector3f(healthField.getFieldX(), 0f, healthField.getFieldY()));
            healthFieldModel.scale(0.5f);
            healthFieldModel.setTexture(0, TextureLoader.loadTexture(START_CP_TEX));
            healthFieldModel.setTexture(1, TextureLoader.loadTexture(HEALTH_FIELD_TEX));
            healthFieldModelList.add(healthFieldModel);
        }

        // preview
        destinationPreview = new RoboModel(ModelLoader.loadModel(ROBOT_PROTO)).copy();
        // destinationPreview.scale(0);
        destinationPreview.setPreview();
        destinationPreview.scale(0);
        destinationPreview.setTexture(0, TextureLoader.loadTexture(ROBOT_TEX));
        destinationPreview.setTexture(1, TextureLoader.loadTexture(ROBOT_TEX2));
        // destinationPreview.translate(new Vector3f(1,0,1));
        destinationPreview.setPlayerColor(new Vector3f(1, 1, 1));

        renderer.addRenderModel(destinationPreview, roboShaderProgram);

        renderer.addRenderModels(tileModelList, shaderProgram);
        renderer.addRenderModels(wallModelList, wallShaderProgram);
        renderer.addRenderModels(checkpointModelList, checkpointShaderProgram);
        renderer.addRenderModels(laserModelList, matcapShaderProgram);
        renderer.addRenderModels(portalEntryModelList, matcapShaderProgram);
        renderer.addRenderModels(portalExitModelList, matcapShaderProgram);
        renderer.addRenderModels(portalEffectModelList, portalShaderProgram);
        renderer.addRenderModels(healthFieldModelList, healthFieldShaderProgram);
    }

    /**
     * updates the visualization for all checkpoints
     * 
     * @param firstRound
     *            whether it is before the first round of the game when the
     *            method is called
     */
    public void updateCheckpoints(boolean firstRound) {
        if (checkpointsChanged) {
            if (!modelsToBeRemovedList.isEmpty()) {
                renderer.removeRenderModels(modelsToBeRemovedList);
                modelsToBeRemovedList.clear();
            }
            ArrayList<Model> checkpointModelList = new ArrayList<Model>();
            // user perspective
            if (mainClient.getMe().ROLE.equals(Role.USER)) {
                for (Checkpoint cp : map.getCheckpoints()) {
                    // create new model
                    Model checkpointModel = ModelLoader.loadModel(CHECKPOINT_MODEL_PROTO).copy();
                    int myActiveCP = firstRound ? 0 : robotList.get(0).getCheckpoint();
                    if (cp.getNumber() == map.getCheckpoints().length - 1) {
                        // display the last checkpoint differently
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(FINISH_CP_TEX));
                    } else if (cp.getNumber() == myActiveCP) {
                        // display my current checkpoint differently
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(ACTIVE_CP_TEX));
                    } else if (cp.getNumber() == myActiveCP + 1) {
                        // display my next checkpoint differently
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(NEXT_CP_TEX));
                    } else if (cp.getNumber() < myActiveCP) {
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(VISITED_CP_TEX));
                    } else if (cp.getNumber() > myActiveCP + 1) {
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(NOT_VISITED_CP_TEX));
                    }
                    // set position of the model to the same position as the
                    // checkpoint
                    checkpointModel.translate(new Vector3f(cp.getPosition()[0], 1.5f, cp.getPosition()[1]));
                    checkpointModelList.add(checkpointModel);
                    modelsToBeRemovedList.add(checkpointModel);
                }
                // spectator perspective
            } else {
                // Display checkpoints from a spectator perspectiv
                for (Checkpoint cp : map.getCheckpoints()) {
                    // create new model
                    Model checkpointModel = ModelLoader.loadModel(CHECKPOINT_MODEL_PROTO).copy();
                    if (cp.getNumber() == 0) {
                        // display the first checkpoint differently
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(START_CP_TEX));
                    } else if (cp.getNumber() == map.getCheckpoints().length - 1) {
                        // display the last checkpoint differently
                        checkpointModel.setTexture(0, TextureLoader.loadTexture(FINISH_CP_TEX));
                    } else {
                        if (cp.getVisitedByAnyone()) {
                            checkpointModel.setTexture(0, TextureLoader.loadTexture(VISITED_BY_ANYONE_CP_TEX));
                        } else {
                            checkpointModel.setTexture(0, TextureLoader.loadTexture(VISITED_BY_NOONE_CP_TEX));
                        }
                    }
                    // set position of the model to the same position as the
                    // checkpoint
                    checkpointModel.translate(new Vector3f(cp.getPosition()[0], 1.5f, cp.getPosition()[1]));
                    checkpointModelList.add(checkpointModel);
                    modelsToBeRemovedList.add(checkpointModel);
                }

            }
            renderer.addRenderModels(checkpointModelList, checkpointShaderProgram);
            checkpointsChanged = false;
        }
    }

    /**
     * renders all added models
     */
    public void render() {
        camera.updateUniformBlock();
        updateLaserBeams();

        renderer.bindFramebuffer(Framebuffer.SCENE);
        renderer.renderScreenShader(backgroundShader);
        renderer.renderModels();
        renderer.bindFramebuffer(Framebuffer.EFFECTS);
        renderer.renderScreenShader(laserShaderProgram);

    }

    public void startLasers() {
        laserShooting = true;
        laserChanged = true;
    }

    public void stopLasers() {
        laserShooting = false;
        laserChanged = true;
    }

    /**
     * gives the coordinates of the start and end points of all the laser beams
     * to the renderer
     */
    private void updateLaserBeams() {
        if (!laserChanged || map == null) {
            return;
        }
        if (!laserShooting) {
            laserShaderProgram.clearAllLasers();
            laserChanged = false;
            return;
        }

        Laser[] lasers = map.getLasers();
        int nrOfPiercingLasers = 0;
        for (Laser laser : lasers) {
            if (laser.isPiercing()) {
                nrOfPiercingLasers++;
            }
        }
        Vector3f[] piercingLaserPositions = new Vector3f[2 * nrOfPiercingLasers];
        Vector3f[] unpiercingLaserPositions = new Vector3f[2 * (lasers.length - nrOfPiercingLasers)];

        int i = 0;
        int j = 0;
        for (Laser laser : lasers) {
            float startPointX = laser.getFieldX();
            float startPointY = laser.getFieldY();
            switch (laser.getDirection()) {
            case UP:
                startPointY += SHOOTER_LENGTH;
                break;
            case RIGHT:
                startPointX -= SHOOTER_LENGTH;
                break;
            case DOWN:
                startPointY -= SHOOTER_LENGTH;
                break;
            case LEFT:
                startPointX += SHOOTER_LENGTH;
                break;
            }
            if (laser.isPiercing()) {
                piercingLaserPositions[i] = new Vector3f(startPointX, LASER_HEIGHT, startPointY);
                i++;
                piercingLaserPositions[i] = findLaserBeamEndpoint(laser);
                i++;
            } else {
                unpiercingLaserPositions[j] = new Vector3f(startPointX, LASER_HEIGHT, startPointY);
                j++;
                unpiercingLaserPositions[j] = findLaserBeamEndpoint(laser);
                j++;
            }
        }
        laserShaderProgram.setLasers(piercingLaserPositions, LaserColor.BLUE);
        laserShaderProgram.setLasers(unpiercingLaserPositions, LaserColor.RED);
        laserChanged = false;
    }

    /**
     * returns the coordinates of the end point of a laser, used by
     * updateLaserBeams()
     * 
     * @param laser
     * @return laserEndpoint
     */
    private Vector3f findLaserBeamEndpoint(Laser laser) {
        boolean[][] rightWalls = map.getRightWallsArray();
        boolean[][] downWalls = map.getDownWallsArray();
        boolean[][] robots = new boolean[mapDimX + 1][mapDimZ + 1];
        if (robotList != null) {
            for (RobotController robot : robotList) {
                if (!robot.isOnMap() || robot.getFieldX() < 0 || robot.getFieldX() > map.getWidth()
                        || robot.getFieldY() < 0 || robot.getFieldY() > map.getHeight()) {
                    continue;
                }
                robots[robot.getFieldX()][robot.getFieldY()] = true;
            }
        }
        float laserEndX = laser.getFieldX();
        float laserEndZ = laser.getFieldY();

        switch (laser.getDirection()) {
        case UP:
            laserEndZ = 0.5f;
            for (int i = laser.getFieldY(); i > 0; i--) {
                if (robots[laser.getFieldX()][i] && !laser.isPiercing()) {
                    laserEndZ = i;
                    break;
                }
                if (downWalls[laser.getFieldX()][i - 1]) {
                    laserEndZ = i - 0.5f;
                    break;
                }
            }
            break;
        case RIGHT:
            laserEndX = mapDimX + 0.5f;
            for (

                    int i = laser.getFieldX(); i < mapDimX; i++) {
                if (robots[i][laser.getFieldY()] && !laser.isPiercing()) {
                    laserEndX = i;
                    break;
                }
                if (rightWalls[i][laser.getFieldY()]) {
                    laserEndX = i + 0.5f;
                    break;
                }
            }
            break;
        case DOWN:
            laserEndZ = mapDimZ + 0.5f;
            for (int i = laser.getFieldY(); i < mapDimZ; i++) {
                if (robots[laser.getFieldX()][i] && !laser.isPiercing()) {
                    laserEndZ = i;
                    break;
                }
                if (downWalls[laser.getFieldX()][i]) {
                    laserEndZ = i + 0.5f;
                    break;
                }
            }
            break;
        case LEFT:
            laserEndX = 0.5f;
            for (int i = laser.getFieldX(); i > 0; i--) {
                if (robots[i][laser.getFieldY()] && !laser.isPiercing()) {
                    laserEndX = i;
                    break;
                }
                if (rightWalls[i - 1][laser.getFieldY()]) {
                    laserEndX = i - 0.5f;
                    break;
                }
            }
            break;
        }
        return new Vector3f(laserEndX, LASER_HEIGHT, laserEndZ);
    }

    /**
     * highlights reachable Fields
     */
    public void displayReachableFields() {
        if(mainClient.getRole()!=Role.USER)
            return;
        // clear all models
        for (int i = 0; i < tileModels.length; i++) {
            for (int j = 0; j < tileModels[i].length; j++) {
                if (tileModels[i][j] != null) {
                    renderer.removeRenderModel(tileModels[i][j]);
                }
            }
        }

        // readd all models with new textures
        boolean[][] cpArray = map.getCheckpointsArray();
        Field[][] fields = world.getFields();
        for (int i = 0; i < fields.length; i++) {
            for (int j = 0; j < fields[i].length; j++) {
                Model newTileModel = ModelLoader.loadModel(TILE_MODEL_PROTO).copy();
                newTileModel.translate(new Vector3f(i + 1, 0, j + 1));
                if (fields[i][j].reachable[0] || fields[i][j].reachable[1] || fields[i][j].reachable[2]
                        || fields[i][j].reachable[3]) {

                    if (cpArray[i + 1][j + 1]) {
                        newTileModel.setTexture(0, TextureLoader.loadTexture(CHECKPOINT_TILE));
                    } else {
                        newTileModel.setTexture(0, TextureLoader.loadTexture(TILE));
                    }
                }
                renderer.addRenderModel(newTileModel, shaderProgram);
                tileModels[i + 1][j + 1] = newTileModel;
            }

        }

    }

    /**
     * tests if a clickableOverlay is clicked at the given position
     * 
     * @param x
     *            the x position
     * @param y
     *            the y position
     * @param mouseButton
     *            the mouse button that was clicked<br>
     *            <i>0: left mouse Button<br>
     *            <i>1: right mouse Button<br>
     *            <i>2: middle mouse Button
     * @return if a clickable overlay was hit at the given position
     */
    public boolean checkForClickableOverlays(int x, int y, int mouseButton) {
        ArrayList<Shape> onPosition = new ArrayList<>();
        for (ClickableOverlay overlay : new ArrayList<>(overlays)) {
            if (!overlay.isVisible())
                continue;
            for (Shape shape : overlay.getShapes()) {
                if (!shape.isVisible())
                    continue;
                if (inBonds(new Vector2f(x, y), shape)) {
                    onPosition.add(shape);
                }
            }
        }
        if (onPosition.size() > 0) {
            if (mouseButton == 0 && Collections.max(onPosition) == temporaryShape)
                Collections.max(onPosition).click();
            else if (mouseButton == -1)
                temporaryShape = Collections.max(onPosition);
        } else {
            temporaryShape = null;
        }
        return onPosition.size() > 0;
    }

    /**
     * renders all visible clickable overlays to the screen
     */
    private void renderClickableOverlays() {
        for (ClickableOverlay overlay : new ArrayList<>(overlays)) {
            if (!overlay.isVisible())
                continue;
            for (Shape shape : new ArrayList<>(overlay.getShapes())) {
                if (!shape.isVisible())
                    continue;
                if (shape.getColor().w == 0)
                    continue;
                Vector2f[] positions = shape.getPositions();
                Vector2f[] screenPositions = new Vector2f[positions.length];
                for (int i = 0; i < positions.length; i++) {
                    screenPositions[i] = toScreenCoords(
                            new Vector4f(positions[i].x, overlay.getHeight(), positions[i].y, 1));
                }
                renderPolygon(shape.getColor(), screenPositions);
            }
        }

    }

    public void clearReachableFields() {
        if(mainClient.getRole()!=Role.USER)
            return;
        for (int i = 0; i < tileModels.length; i++) {
            for (int j = 0; j < tileModels[i].length; j++) {
                if (tileModels[i][j] != null) {
                    renderer.removeRenderModel(tileModels[i][j]);
                }
            }
        }

        ArrayList<Model> newTileModelList = new ArrayList<Model>();
        boolean[][] checkpointArray = map.getCheckpointsArray();
        ModelLoader.loadModel(TILE_MODEL_PROTO).setTexture(0, TextureLoader.loadTexture(TILE));
        tileModels = new Model[mapDimX + 1][mapDimZ + 1];
        // for every position on the map
        for (int x = 0; x <= mapDimX; x++) {
            for (int z = 0; z <= mapDimZ; z++) {
                // don't display fields with position <1
                if (x > 0 && z > 0) {
                    // create new model
                    Model tileModel = ModelLoader.loadModel(TILE_MODEL_PROTO).copy();
                    // set position of the model to the position of the tile
                    tileModel.translate(new Vector3f(x, 0, z));
                    // set texture
                    if (checkpointArray[x][z]) {
                        tileModel.setTexture(0, TextureLoader.loadTexture(CHECKPOINT_TILE));
                    } else {
                        tileModel.setTexture(0, TextureLoader.loadTexture(TILE));
                    }
                    tileModels[x][z] = tileModel;
                    newTileModelList.add(tileModel);
                }
            }
        }
        renderer.addRenderModels(newTileModelList, shaderProgram);
    }

    /**
     * give him points and a color thats all he needs. then you have a fancy
     * Polygon
     * 
     * @param pos
     * @param col
     */
    public void renderPolygon(Vector4f col, Vector2f... pos) {
        if (col == null || pos == null || !(pos.length > 0))
            return;
        Consumer<Long> polygon = (context) -> {
            NVGColor color = NVGColor.create();
            NanoVG.nvgRGBAf(col.x, col.y, col.z, col.w, color);
            NVGColor tempColor = NVGColor.create();
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);

            NanoVG.nvgSave(context);
            NanoVG.nvgBeginPath(context);
            if (pos.length > 0) {
                if (pos[0] == null)
                    return;
                NanoVG.nvgMoveTo(context, pos[0].x, pos[0].y);
                for (Vector2f positionI : pos) {
                    if (positionI == null)
                        return;
                    NanoVG.nvgLineTo(context, positionI.x, positionI.y);
                }
            }
            NanoVG.nvgClosePath(context);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgFill(context);
            NanoVG.nvgRestore(context);

            NanoVG.nvgRGBAf(0.7f, 0.2f, 0.7f, 1f, color);
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);
        };
        vRenderer.render(polygon);
    }

    /**
     * tests if a point is inside a shape
     * 
     * @param pos
     * @return true if the given cords are inside the given shape
     */
    public boolean inBonds(Vector2f cords, Shape shape) {
        Vector2f[] positions = shape.getPositions();
        if (positions.length == 0)
            return false;
        double[] coords = new double[positions.length * 2];

        for (int i = 0; i < positions.length; i++) {
            if (positions[i] == null)
                continue;
            Vector2f screenCords = toScreenCoords(new Vector4f(positions[i].x, shape.getHeight(), positions[i].y, 1));
            if (screenCords == null)
                return false;
            coords[2 * i] = (int) screenCords.x;
            coords[2 * i + 1] = (int) screenCords.y;
        }

        Polygon polygon = new Polygon(coords);
        return polygon.contains(cords.x, cords.y);
    }

    /**
     * displays a user list on the screen Displays the turn timer. Displays a
     * user list on the screen with color, UID, name, healthpoints and last
     * Checkpoint reached for each player. In the card selection phase in player
     * perspective, it also displays the card selection. In the debug mode,
     * displays the fps counter.
     */
    public void renderHUD() {
        renderer.bindFramebuffer(Framebuffer.DEFAULT);
        if (SettingsManager.debug)
            renderText(vRenderer, "" + Timer.getInstance().getFPS(), UIView.CHAT_WINDOW_POS_X, 0, 60f,
                    NanoVG.NVG_ALIGN_RIGHT | NanoVG.NVG_ALIGN_TOP);
        // test if the countdown should already be displayed
        if (startTimer) {
            // set text color to black
            vRenderer.setColor(1.f, .0f, 1.f, 1f);
            try {
                renderText(vRenderer, "" + tTimer.getLeftTime(), Window.width / 2, 5, 60f,
                        NanoVG.NVG_ALIGN_MIDDLE | NanoVG.NVG_ALIGN_TOP);
            } catch (NullPointerException npE) {
            } catch (IndexOutOfBoundsException ioobE) {
            }
        }
        switch (menuView.getState()) {
        case CREDITS:
            renderCredits();
            break;
        case TUTORIAL:
            renderTut();
            break;
        case GAME_RUNNING:
        case LOBBY:
        case LOBBY_GAME_LEADER:
        case PAUSE:
        case CARDSELECT:
            // overlays
            renderClickableOverlays();
            // render number of checkpoint in top of it
            renderCheckpointNumbers();
            // renders unique Names for the Portals
            renderPortalIdentifiers();
            // renders the damage number of the lasers
            renderLaserDMG();

            if (users == null)
                break;
            synchronized (users) {
                // render playernames on top of the robots
                renderPlayerNames();
                // render user list
                renderUserList();

                // render cards to select
                if (cardselect && !hideCardselect && draggables.length > 0) {
                    // selecttable
                    renderCardSelectTable();
                    // Cards (Draggables)
                    renderCards();
                    // show whether the current card selection is valid
                    renderCardVerifier();
                }
            }
            break;
        default:
        }
    }

    /**
     * displays all player names on the screen
     */
    private void renderPlayerNames() {
        synchronized (users) {
            if (this.users != null) {
                final float fotSize = 25f;
                for (int uid : new HashMap<>(users).keySet()) {
                    if (users != null && users.get(uid) != null && users.get(uid).getRobot() != null
                            && users.get(uid).getRobot().model != null) {
                        Vector3f pos3f = users.get(uid).getRobot().model.getPosition();
                        Vector4f pos4f = new Vector4f(pos3f.x, pos3f.y + .5f, pos3f.z, 1);
                        Vector2f sc = toScreenCoords(pos4f);
                        if (sc != null && users.get(uid) != null && users.get(uid).getRobot().isOnMap()) {
                            renderInGameText(users.get(uid).getName(), sc.x, sc.y, fotSize);
                        }
                    }
                }
            }
        }
    }

    /**
     * renders an identifier over all portals and displays witch portal is the
     * start portal or the end portal
     */
    private void renderPortalIdentifiers() {
        if (map != null) {
            float fotSize = 15f;
            Portal[] portals = map.getPortals();
            ArrayList<Portal> portalMap = new ArrayList<Portal>();
            for (Portal portal : portals) {
                portalMap.add(portal);
                Vector4f pos4f1 = new Vector4f(portal.getEndFieldX(), 0.5f, portal.getEndFieldY(), 1);
                Vector2f sc1 = toScreenCoords(pos4f1);
                if (sc1 != null) {
                    if (portalMap.indexOf(portal) < 26) {
                        renderInGameText("End " + (char) (portalMap.indexOf(portal) + 65), sc1.x, sc1.y, fotSize);
                    } else {
                        renderInGameText("End " + (char) (portalMap.indexOf(portal) / 26 + 64) + ""
                                + (char) (portalMap.indexOf(portal) - (portalMap.indexOf(portal) / 26) * 26 + 65),
                                sc1.x, sc1.y, fotSize);
                    }
                }
                Vector4f pos4f2 = new Vector4f(portal.getStartFieldX(), 0.5f, portal.getStartFieldY(), 1);
                Vector2f sc2 = toScreenCoords(pos4f2);
                if (sc2 != null) {
                    if (portalMap.indexOf(portal) < 26) {
                        renderInGameText("Start " + (char) (portalMap.indexOf(portal) + 65), sc2.x, sc2.y, fotSize);
                    } else {
                        renderInGameText("Start " + (char) (portalMap.indexOf(portal) / 26 + 64) + ""
                                + (char) (portalMap.indexOf(portal) - (portalMap.indexOf(portal) / 26) * 26 + 65),
                                sc2.x, sc2.y, fotSize);
                    }
                }
            }
        }
    }

    /**
     * displays the Tutorial
     */
    private void renderTut() {
        Consumer<Long> tutorial = (context) -> {
            Vector2f[] pos = { new Vector2f(0, Window.height - 50), new Vector2f(Window.width, -(Window.height - 50)) };
            renderer.renderTexture(tut[UIView.counter].getTextureID(), pos);
        };
        vRenderer.render(tutorial);
    }

    /**
     * renders the number of each checkpoint over the checkpoint
     */
    private void renderCheckpointNumbers() {
        if (map != null && map.getCheckpoints() != null) {
            float fontSize = 20f;
            for (Checkpoint cp : map.getCheckpoints()) {
                Vector4f pos4f = new Vector4f(cp.getPosition()[0], 1.5f, cp.getPosition()[1], 1);
                Vector2f sc = toScreenCoords(pos4f);
                if (sc != null)
                    renderInGameText("" + cp.getNumber(), sc.x, sc.y, fontSize);
            }
        }
    }

    /**
     * renders the creditsMiniGame
     */
    public void renderCredits() {
        if (creditIterator > 150) {
            if (!creditsDropping) {
                spawnName(1);
                if (creditToIterator < creditTo.size() - 1) {
                    creditToIterator++;
                } else {
                    creditsFinished = true;
                }
            }
            creditsDropping = true;
        }
        if (kuekSpeed >= 71) {
            creditsFinished = true;
            creditsMiGaEnabled = false;
        }
        // first 3 pics (fullscreen)
        if (!creditsDropping) {
            Consumer<Long> fullScreenCreditPic = (context) -> {
                Vector2f[] positions = { new Vector2f(0, Window.height), new Vector2f(Window.width, -Window.height) };
                renderer.renderTexture(creditTo.get(creditToIterator).getTextureID(), positions);
            };
            vRenderer.render(fullScreenCreditPic);
            if ((creditIterator != 0) && (creditIterator % 50) == 0) {
                if (creditToIterator < creditTo.size()) {
                    creditToIterator++;
                }
            }
        } else { // everything after that

            // shows chicks
            if (creditsFinished) {
                if (creditsMiGaEnabled) {
                    letTheChicksFlow();
                    // show schredder
                    Consumer<Long> schredd = (context) -> {
                        Vector2f[] positions = { new Vector2f(schreddX - schreddWidth / 2, schreddY + schreddWidth / 6),
                                new Vector2f(schreddWidth, -schreddWidth / 3) };
                        renderer.renderTexture(TEXTUR_SCHREDD.getTextureID(), positions);
                    };
                    vRenderer.render(schredd);

                    // show scorebox
                    Consumer<Long> scorebox = (context) -> {
                        NVGColor color = NVGColor.create();
                        NanoVG.nvgRGBAf(1.0f, 1.0f, 1.0f, 0.7f, color);
                        NVGColor tempColor = NVGColor.create();
                        NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);

                        // draw a rectangle
                        NanoVG.nvgBeginPath(context);
                        NanoVG.nvgRect(context, 0, 0, 150, 50);
                        NanoVG.nvgFillColor(context, color);
                        NanoVG.nvgFill(context);

                        NanoVG.nvgRGBAf(0.7f, 0.2f, 0.7f, 1f, color);
                        NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);
                    };
                    vRenderer.render(scorebox);
                    // render Text
                    renderText(vRenderer, "Score: " + creditsScore, 0, 0, 30f,
                            NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                    renderText(vRenderer, "Speed: " + kuekSpeed, 0, 30f, 20f,
                            NanoVG.NVG_ALIGN_LEFT | NanoVG.NVG_ALIGN_TOP);
                }
            } else { // show creditnames dropping
                letTheNamesFlow();
            }
        }

        // shows whatever is in kueks
        for (Droppable kueke : new LinkedList<Droppable>(kueks)) {
            Droppable kuek = kueks.get(kueks.indexOf(kueke));
            Consumer<Long> kuk = (context) -> {
                Vector2f[] positions = { kuek, kuek.dimension };
                renderer.renderTexture(kuek.tex.getTextureID(), positions);
            };
            vRenderer.render(kuk);
            // changes pos
            kuek.y += kuekSpeed;
            kuek.x += Math.sin(kuek.y * 50) * 300 * kuekSinMod / kuekSpeed;
            schreddWidth *= (schreddWidth < Window.width / 6) ? 1 : 0.99983;
        }
        creditIterator++;
    }

    private static void spawnName(int amm) {
        for (int i = 0; i < Math.max(0, amm); i++) {
            if (creditToIterator <= creditTo.size() - 1) {
                kueks.add(new Droppable((float) ((Window.width - 800) * Math.random()), 0, 800, 450,
                        creditTo.get(creditToIterator)));
            }
        }
    }

    private static void spawnKuek(int amm) {
        for (int i = 0; i < amm; i++) {
            kueks.add(new Droppable((float) ((Window.width - KUEK_RAD * 2) * Math.random()), 0,
                    (Math.random() < 0.05) ? ((Math.random() < 0.5) ? ((Math.random() < 0.5) ? TEXTUR_LIX : TEXTUR_NIK)
                            : ((Math.random() < 0.5) ? TEXTUR_KAI : TEXTUR_MAXI)) : TEXTUR_KUEK));
        }
    }

    private void letTheNamesFlow() {
        for (Droppable kueke : new LinkedList<Droppable>(kueks)) {
            Droppable namePos = kueks.get(kueks.indexOf(kueke));

            boolean cought = ((namePos.x - namePos.dimension.x / 2 < schreddX + schreddWidth / 2)
                    && (namePos.x + namePos.dimension.x / 2 > schreddX - schreddWidth / 2)
                    && (namePos.y > schreddY - schreddWidth / 3 / 2));
            // removes it if out of window
            if (namePos.y + namePos.dimension.y > Window.height) {
                kueks.remove(namePos);
                spawnName(1);
                if (creditToIterator < creditTo.size()) {
                    creditToIterator++;
                } else {
                    creditsFinished = true;
                }
            }
            // adds secretScore if cought
            if (cought && !namePos.cought) {
                secretCreditScore++;
                namePos.cought = true;
                if (secretCreditScore >= 6) {
                    creditsMiGaEnabled = true;
                }
            }
        }
    }

    private void letTheChicksFlow() {
        // spawns new chicks
        if (kueks.size() < 8) {
            spawnKuek((int) (Math.random() + kuekSpeed * 0.01f));
        }

        // if kueke dies speed *= 1.01 if cought speed *= 0.97
        for (Droppable kueke : new LinkedList<Droppable>(kueks)) {
            Droppable kuek = kueks.get(kueks.indexOf(kueke));

            boolean cought = ((kuek.x - kuek.dimension.x / 2 < schreddX + schreddWidth / 2)
                    && (kuek.x + kuek.dimension.x / 2 > schreddX - schreddWidth / 2)
                    && (kuek.y > schreddY - schreddWidth / 3 / 2));
            // if cought or out of screen remove it and add score
            if (kuek.y + kuek.dimension.y > Window.height || cought) {
                kueks.remove(kuek);
                if (cought && !kuek.cought) {
                    if (kuek.tex.equals(TEXTUR_KAI)) {
                        kuekSpeed = Math.max(kuekSpeed * 0.666f, 1.0f);
                    } else if (kuek.tex.equals(TEXTUR_LIX)) {
                        schreddWidth *= (schreddWidth < Window.width / 4) ? 1.37 : 1.0125;
                    } else if (kuek.tex.equals(TEXTUR_MAXI)) {
                        kuekSinMod++;
                    } else if (kuek.tex.equals(TEXTUR_NIK)) {
                        creditsScore *= 0.5f + ((Math.random() + 0.7) / 2);
                    } else {
                        creditsScore += 1 + (3 * kuekSinMod);
                    }
                    kuekSpeed = Math.min(71f, kuekSpeed * 0.97f);
                    kuek.cought = true;
                } else {
                    kuekSpeed = Math.min(100f, kuekSpeed * 1.002f);
                }
            }
        }
        kuekSpeed = Math.min(100f, kuekSpeed * 1.003f);
        kuekSinMod = Math.max(0, kuekSinMod * 0.9931f);

    }

    private void renderInGameText(String text, float x, float y, float fontSize) {
        Consumer<Long> func = (context) -> {
            float[] bounds = new float[4];
            float length = NanoVG.nvgTextBounds(context, x, y, text, bounds) * fontSize / 15 + 5;
            NVGColor color = NVGColor.create();
            NanoVG.nvgRGBAf(1.0f, 1.0f, 1.0f, 0.7f, color);
            NVGColor tempColor = NVGColor.create();
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);

            // draw a rectangle
            NanoVG.nvgBeginPath(context);
            NanoVG.nvgRect(context, x - length / 2, y - fontSize / 2, length, fontSize);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgFill(context);

            NanoVG.nvgRGBAf(0.7f, 0.2f, 0.7f, 1f, color);
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);
        };
        vRenderer.render(func);
        renderText(vRenderer, text, x, y, fontSize, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_MIDDLE);
    }

    /**
     * renders a rectangle where Cards should be dragged to
     */
    private void renderCardSelectTable() {
        // print box where selected cards have to go
        Consumer<Long> cardTable = (context) -> {
            NVGColor color = NVGColor.create();
            NanoVG.nvgRGBAf(0.2f, 0.2f, 0.2f, 0.7f, color);
            NVGColor tempColor = NVGColor.create();
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);

            // draw a rectangle
            NanoVG.nvgBeginPath(context);
            NanoVG.nvgRect(context, selectTableX, selectTableY, selectTableWidth, selectTableHeight);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgFill(context);

            NanoVG.nvgRGBAf(0.7f, 0.2f, 0.7f, 1f, color);
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), color.a(), tempColor);
        };
        vRenderer.render(cardTable);
    }

    /**
     * render the Cards (the Draggables) the user got dealed
     */
    private void renderCards() {
        for (int i = 0; i < draggables.length; i++) {
            if (draggables[i] != null) {
                Draggable card = draggables[i];
                int width = card.width;
                int height = Math.abs(card.height);
                int posx = card.x;
                int posy = card.y;
                Texture texture = CardType.getCardTexture(card.card.getType());
                Consumer<Long> func = (context) -> {

                    // draw the texture of the card
                    Vector2f[] pos = { new Vector2f(posx, posy + height), new Vector2f(width, -height) };
                    renderer.renderTexture(texture.getTextureID(), pos);

                    if (SHOW_CARD_NAMES)
                        renderText(vRenderer, card.card.getType().toString(), posx + width / 2, posy, 15f,
                                NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_TOP);
                    renderText(vRenderer, String.valueOf(card.card.getValue()), posx + width / 2, posy + height,
                            (float) height / 6, NanoVG.NVG_ALIGN_CENTER | NanoVG.NVG_ALIGN_BOTTOM);
                };
                vRenderer.render(func);
            }
        }
    }

    /**
     * render weather the selected Cards are valid or not
     */
    private void renderCardVerifier() {
        Card[] cardSel = getCardsToSend();
        Texture validInvalidTex;
        if (Card.cardSelectionIsValid(cardSel, !mainClient.getUsers().get(mainClient.getUid()).getRobot().isOnMap())) {
            validInvalidTex = TextureLoader.loadTexture(VALID_SELECTION_TEX);
        } else {
            validInvalidTex = TextureLoader.loadTexture(INVALID_SELECTION_TEX);
        }
        Consumer<Long> func = (context) -> {
            Vector2f[] pos = {
                    new Vector2f((Window.width - 180) - 15,
                            (Window.height - 53)
                                    + 30) /* depending on "Karten senden" button */,
                    new Vector2f(30, -30) /* sidelength of the tex */ };
            renderer.renderTexture(validInvalidTex.getTextureID(), pos);
        };
        vRenderer.render(func);
    }

    /**
     * render the userList ingame
     */
    private void renderUserList() {
        if (!playerUid.isEmpty() && !playerName.isEmpty() && !playerHealth.isEmpty() && !playerCps.isEmpty()
                && this.users != null && !this.users.isEmpty() && map != null && map.getCheckpoints().length > 0) {
            // update all user information
            String[] uids = new String[users.size()];
            String[] nameArray = new String[users.size()];
            Integer[] healthArray = new Integer[users.size()];
            String[] cpArray = new String[playerCps.size()];
            String nrOfCps = String.valueOf(map.getCheckpoints().length - 1);

            playerUid.toArray(uids);
            playerName.toArray(nameArray);
            playerHealth.toArray(healthArray);
            int i = 0;
            for (Integer integer : new LinkedList<>(playerCps)) {
                cpArray[i] = String.valueOf(integer);
                i++;
            }

            // render the player list
            renderPlayerList(vRenderer, 0, 0, 400, 20, 20, playerColors, uids, nameArray, healthArray, cpArray,
                    nrOfCps);
        }
    }

    public void applyColor() {
        playerColors = generateColors(robotList.size());
        renderer.removeRenderModels(roboModels);
        roboModels.clear();
        addRobotModelsToRenderer();
        render();
    }

    /**
     * Divides the RGB color spectre into 'nrOfPlayers'-many parts and returns
     * those colors as an RGB-Vector array. In the user perspective, the user
     * gets the color from SettingsManager.color and the RGB spectre is divided
     * with an according offset.
     * 
     * @param nrOfPlayers
     * @return colors
     */
    private Vector3f[] generateColors(int nrOfPlayers) {
        Vector3f[] colors = new Vector3f[nrOfPlayers];

        float[] hue = new float[nrOfPlayers];

        // set all parameters for the colors
        for (int i = 0; i < nrOfPlayers; i++) {
            hue[i] = i * (360f / nrOfPlayers) + SettingsManager.color;
            hue[i] %= 360;
        }

        // transfer color data into RGB
        for (int i = 0; i < nrOfPlayers; i++) {
            int h = (int) hue[i] / 60; // floor the number
            float f = hue[i] / 60 - h;
            float r = 0;
            float g = 0;
            float b = 0;

            if (h == 0) {
                r = 1f;
                g = f;
                b = 0f;
            } else if (h == 1) {
                r = 1 - f;
                g = 1f;
                b = 0f;
            } else if (h == 2) {
                r = 0f;
                g = 1f;
                b = f;
            } else if (h == 3) {
                r = 0f;
                g = 1 - f;
                b = 1f;
            } else if (h == 4) {
                r = f;
                g = 0f;
                b = 1f;
            } else if (h == 5) {
                r = 1f;
                g = 0f;
                b = 1 - f;
            }
            colors[i] = new Vector3f(r, g, b);
        }
        return colors;
    }

    /**
     * Converts a 3D position into 2D screen coordinates
     * 
     * @param position
     * @return
     */
    private Vector2f toScreenCoords(Vector4f position) {
        Vector4f pos = new Vector4f().add(position);
        pos = camera.getViewMatrix().multiply(pos);
        return Matrix4f.perspectiveDivision(pos, camera.getFOV(), camera.getAspectRatio());
    }

    /**
     * Renders the Text
     * 
     * @param vRenderer
     * @param text
     * @param x
     * @param y
     * @param size
     * @param flags
     */
    private static void renderText(VectorRenderer vRenderer, String text, float x, float y, float size, int flags) {
        Consumer<Long> func = (context) -> {
            NVGColor color = NVGColor.create();
            NanoVG.nvgRGBAf(0f, 0f, 0f, 1f, color);

            NanoVG.nvgFontSize(context, size);
            NanoVG.nvgFontFace(context, "REGULAR");
            NanoVG.nvgTextAlign(context, flags);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgText(context, x, y, text);
        };
        vRenderer.render(func);
    }

    /**
     * Renders Rectangles
     * 
     * @param vRenderer
     * @param x
     * @param y
     * @param width
     * @param height
     * @param color
     */
    private static void renderRectangle(VectorRenderer vRenderer, float x, float y, float width, float height,
            NVGColor color) {
        Consumer<Long> func = (context) -> {

            NanoVG.nvgBeginPath(context);
            NanoVG.nvgRect(context, x, y, width, height);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgFill(context);
        };

        vRenderer.render(func);
    }

    /**
     * Renders the player List with the matching color to the robot
     * 
     * @param vRenderer
     * @param x
     * @param y
     * @param width
     * @param fontSize
     * @param gapSize
     * @param playerColors
     * @param uids
     * @param names
     * @param healthpoints
     * @param checkpointNrs
     * @param maxCpNr
     */
    private static void renderPlayerList(VectorRenderer vRenderer, float x, float y, float width, float fontSize,
            float gapSize, Vector3f[] playerColors, String[] uids, String[] names, Integer[] healthpoints,
            String[] checkpointNrs, String maxCpNr) {
        Consumer<Long> func = (context) -> {
            NVGColor color = NVGColor.create();
            NanoVG.nvgRGBAf(0.5f, 1f, 0.2f, 1f, color);

            float height = uids.length * fontSize;
            // draw the rectangle
            NanoVG.nvgBeginPath(context);
            NanoVG.nvgRect(context, x, y, width, height);
            NanoVG.nvgRGBAf(color.r(), color.g(), color.b(), 0.2f, color);
            NanoVG.nvgFillColor(context, color);
            NanoVG.nvgFill(context);

            NanoVG.nvgFontSize(context, fontSize);

            float colorRectWidth = 20; // Width of the rectangles which
            // represent the player colors
            float textFromRect = 10.0f; // How much space there is from the
            // uid
            // text
            // to the right edge of the color
            // rectangle
            float textX = x + textFromRect + colorRectWidth; // x dim of
            // where
            // the uid text
            // starts
            float textY = y; // y dim of where the uid text starts
            float maxUidPixels = 0.0f; // how many pixels the longest of
            // uids
            // needs
            float maxCpPixels = 0.0f; // how many pixels the longest of cp
            // numbers needs
            float hpBarPixels = 6.6f * fontSize; // 9*0.6+8*0.15 = 6.6 with
            // hpSideLength:=0.6 and
            // gap
            // between hps is 0.15
            final float[] bounds = new float[4];
            final float[] bounds2 = new float[4];

            for (int i = 0; i < uids.length; i++) {
                if (uids[i] != null) {
                    float uidPixels = NanoVG.nvgTextBounds(context, textX, textY, uids[i], bounds);
                    if (uidPixels > maxUidPixels) {
                        maxUidPixels = uidPixels;
                    }

                    float cpPixels = NanoVG.nvgTextBounds(context, textX, textY, checkpointNrs[i] + "/" + maxCpNr,
                            bounds2);
                    if (cpPixels > maxCpPixels) {
                        maxCpPixels = cpPixels;
                    }
                }
            }

            // the player names get the rest of the list width after the
            // other
            // components got the space they needed. Player names have to be
            // cut
            // off if too long.

            float maxNamePixels = width - 2 * textFromRect - colorRectWidth - maxUidPixels - 3 * gapSize
                    - hpBarPixels/* (for health points) */
                    - maxCpPixels;

            float maxNamePixelsTemp = maxNamePixels;
            for (int i = 0; i < uids.length; i++) {
                if (uids[i] != null) {
                    maxNamePixels = maxNamePixelsTemp;
                    String cutName = names[i];
                    boolean wasCut = false;

                    NanoVG.nvgFontSize(context, fontSize);
                    float namePixels = NanoVG.nvgTextBounds(context, textX + maxUidPixels + gapSize,
                            textY + (i + 0.8f) * fontSize, cutName, bounds);
                    if (namePixels > maxNamePixels) {
                        maxNamePixels = maxNamePixels - NanoVG.nvgTextBounds(context, textX + maxUidPixels + gapSize,
                                textY + (i + 0.8f) * fontSize, "...", bounds);
                        wasCut = true;
                    }
                    while (namePixels > maxNamePixels) {
                        cutName = cutName.substring(0, cutName.length() - 1);
                        namePixels = NanoVG.nvgTextBounds(context, textX + maxUidPixels + gapSize,
                                textY + (i + 0.8f) * fontSize, cutName, bounds);
                    }
                    if (wasCut) {
                        cutName = cutName + "...";
                    }
                    maxNamePixels = maxNamePixelsTemp;

                    // player colors
                    NanoVG.nvgRGBAf(playerColors[i].x, playerColors[i].y, playerColors[i].z, 1f, color);
                    renderRectangle(vRenderer, x, y + i * fontSize, colorRectWidth, fontSize, color);
                    // player uids
                    NanoVG.nvgRGBAf(0.0f, 0.0f, 0.0f, 1.0f, color); // color for
                    // the
                    // text
                    renderText(vRenderer, uids[i], textX, textY + (i + 0.8f) * fontSize, fontSize,
                            NanoVG.NVG_ALIGN_LEFT);
                    // player names
                    renderText(vRenderer, cutName, textX + gapSize + maxUidPixels, textY + (i + 0.8f) * fontSize,
                            fontSize, NanoVG.NVG_ALIGN_LEFT);
                    // player hps
                    NanoVG.nvgRGBAf(0.4f, 0.85f, 0.0f, 1f, color); // color for
                    // the
                    // healthpoints
                    renderHealthBar(vRenderer, textX + 2 * gapSize + maxUidPixels + maxNamePixels,
                            textY + (i + 0.55f) * fontSize, 0.6f * fontSize, healthpoints[i]);
                    // player checkpoints
                    renderText(vRenderer, checkpointNrs[i] + "/" + maxCpNr,
                            textX + 3 * gapSize + maxUidPixels + maxNamePixels + hpBarPixels,
                            textY + (i + 0.8f) * fontSize, fontSize, NanoVG.NVG_ALIGN_LEFT);
                }
            }
        };
        vRenderer.render(func);

    }

    /**
     * Renders the Health bars
     * 
     * @param vRenderer
     * @param x
     * @param y
     * @param sideLength
     * @param lifepoints
     */
    private static void renderHealthBar(VectorRenderer vRenderer, float x, float y, float sideLength, int lifepoints) {
        Consumer<Long> func = (context) -> {
            NVGColor colorGreen = NVGColor.create();
            NanoVG.nvgRGBAf(0.5f, 1f, 0.2f, 1f, colorGreen);
            NVGColor colorRed = NVGColor.create();
            NanoVG.nvgRGBAf(1.0f, 0.2f, 0.5f, 0.2f, colorRed);

            NVGColor tempColor = NVGColor.create();
            NanoVG.nvgRGBAf(colorGreen.r(), colorGreen.g(), colorGreen.b(), colorGreen.a(), tempColor);
            float relativeX = x; // after one hp circle was drawn, the xPosition
            // has to be increased for the next hp
            // circle
            float gap = 0.25f * sideLength; // gap between hp circles
            for (int i = 1; i <= 9; i++) {
                if (i <= lifepoints) {
                    colorGreen = NVGColor.create();
                    NanoVG.nvgRGBAf(0.5f, 1f, 0.2f, 1f, colorGreen);

                    // draw a circle
                    NanoVG.nvgBeginPath(context);
                    NanoVG.nvgCircle(context, relativeX, y, 0.5f * sideLength);
                    NanoVG.nvgFillColor(context, colorGreen);
                    NanoVG.nvgFill(context);

                    // draw the circle stroke
                    NanoVG.nvgRGBAf(0.75f * colorGreen.r(), 0.75f * colorGreen.g(), 0.75f * colorGreen.b(),
                            colorGreen.a(), colorGreen);
                    // the color shall be similar, but a bit darker for the
                    // stroke
                    NanoVG.nvgStrokeWidth(context, 0.1f * sideLength);
                    NanoVG.nvgStrokeColor(context, colorGreen);
                    NanoVG.nvgStroke(context);
                    NanoVG.nvgRGBAf(tempColor.r(), tempColor.g(), tempColor.b(), tempColor.a(), colorGreen);

                    // draw the small shimmer circle
                    NanoVG.nvgBeginPath(context);
                    NanoVG.nvgCircle(context, relativeX + 0.2f * sideLength, y - 0.2f * sideLength, 0.15f * sideLength);
                    NanoVG.nvgRGBAf(1.6f * colorGreen.r(), 1.6f * colorGreen.g(), 1.6f * colorGreen.b(),
                            0.6f * colorGreen.a(), colorGreen);
                    NanoVG.nvgFillColor(context, colorGreen);
                    NanoVG.nvgFill(context);

                    relativeX = relativeX + sideLength + gap;
                    NanoVG.nvgRGBAf(tempColor.r(), tempColor.g(), tempColor.b(), tempColor.a(), colorGreen);
                } else {
                    colorRed = NVGColor.create();
                    NanoVG.nvgRGBAf(1.0f, 0.2f, 0.5f, 0.2f, colorRed);
                    // draw a circle
                    NanoVG.nvgBeginPath(context);
                    NanoVG.nvgCircle(context, relativeX, y, 0.5f * sideLength);
                    NanoVG.nvgFillColor(context, colorRed);
                    NanoVG.nvgFill(context);

                    relativeX = relativeX + sideLength + gap;
                    NanoVG.nvgRGBAf(tempColor.r(), tempColor.g(), tempColor.b(), tempColor.a(), colorRed);
                }
            }
        };
        vRenderer.render(func);
    }

    /**
     * resets the timer
     */
    public void reset(boolean firstRound) {
        if (tTimer != null && users != null)
            tTimer.resetTimer(users.size(), firstRound);
    }

    /**
     * resets the local copy of users in ClientViewController
     */
    public void removeAllUsersFromHUD() {
        users = null;
        playerHealth.clear();
        playerName.clear();
        playerUid.clear();
        playerCps.clear();

    }

    /**
     * sets the startTimer value to the given boolean
     * 
     * @param startTimer
     *            new value of start timer
     */
    public void setStartTimer(boolean startTimer) {
        this.startTimer = startTimer;
        reset(true);
    }

    /**
     * Getter
     * 
     * @return tTimer TurnTimer
     */
    public TurnTimer gettTimer() {
        return tTimer;
    }

    /**
     * Getter
     * 
     * @return all robots that are displayed
     */
    public ArrayList<RobotController> getRobotList() {
        return robotList;
    }

    /**
     * en- or disables reflections
     * 
     * @param act
     */
    public void setMirror(boolean act) {
        renderer.useMirror(act);
    }

    /**
     * moves a card from the given position to the next position if its selected
     * and at the given position
     * 
     * @param x
     *            the old x position of the card
     * @param y
     *            the old y position of the card
     * @param newX
     *            the new x position of the card
     * @param newY
     *            the new y position of the card
     * @return if a card changed position
     */
    public boolean translateCards(int x, int y, int newX, int newY) {
        if (!cardselect || hideCardselect || UIView.CARD_SENT)
            return false;
        int inBounds = 0;
        boolean success = false;
        try {
            for (Draggable card : draggables) {
                inBounds += (!card.outOfBoundsOf(selectTableX, selectTableY, selectTableWidth, selectTableHeight)) ? 1
                        : 0;
            }
            success = selectedDraggable.translate(x, y, newX, newY, inBounds >= 5);
            if (success) {
                boolean resort = false;
                Draggable temp = null;
                for (int i = 0; i < draggables.length; i++) {
                    if (draggables[i].equals(selectedDraggable)) {
                        resort = true;
                        temp = draggables[i];
                    }
                    if (resort) {
                        if (i + 1 < draggables.length) {
                            draggables[i] = draggables[i + 1];
                        } else {
                            draggables[i] = temp;
                        }
                    }
                }
            }
        } catch (NullPointerException npE) {
        }
        updatePreviewPosition();
        return success;
    }

    /** update the position of the preview model */
    private void updatePreviewPosition() {
        Card[] selection = getCardsToSend();
        Robot robot = mainClient.getUsers().get(mainClient.getUid()).getRobot();
        if (!robot.isOnMap() && directionSelection.getSelection() == null) {
            return;
        }
        int[] checkPos = map.getCheckpoints()[robot.getCheckpoint()].getPosition();
        vec3 pos = WorldSimulator.simulate(simulatedWorld, selection,
                new vec2i(robot.isOnMap() ? robot.getFieldX() - 1 : checkPos[0] - 1,
                        robot.isOnMap() ? robot.getFieldY() - 1 : checkPos[1] - 1),
                robot.isOnMap() ? robot.getOrientation() : directionSelection.getSelection());
        destinationPreview.setPosition(new Vector3f(pos.x + 1, 0, pos.y + 1));
        destinationPreview.setRotation(new Vector3f(0, Orientation.fromArrayIndex((int) pos.z).getRotationDegree(), 0));
        destinationPreview.setScale(0.9f);
    }

    /**
     * hides the preview robot
     */
    public void hidePreview() {
        destinationPreview.scale(0);
    }

    public void selectCard(int posx, int posy) {
        if (!cardselect || hideCardselect || UIView.CARD_SENT)
            return;
        int inBounds = 0;
        try {
            for (Draggable card : draggables) {
                inBounds += (!card.outOfBoundsOf(selectTableX, selectTableY, selectTableWidth, selectTableHeight)) ? 1
                        : 0;
            }
            for (Draggable card : draggables) {
                if (card.outOfBoundsOf(selectTableX, selectTableY, selectTableWidth, selectTableHeight)) {
                    int x = (inBounds + 1) * (selectTableWidth / 6) + selectTableX;
                    int y = (selectTableHeight / 2) + selectTableY;
                    if (card.translate(posx, posy, x, y, inBounds >= 5)) {
                        // if (SettingsManager.updateCardsInstant)
                        organiseCards();
                        updatePreviewPosition();
                        return;
                    }
                } else {
                    int x = (9 - inBounds + 1) * (Window.width / 6);
                    int y = (Window.height / 2);
                    if (card.translate(posx, posy, x, y, false)) {
                        // if (SettingsManager.updateCardsInstant)
                        organiseCards();
                        updatePreviewPosition();
                        return;
                    }
                }
            }
        } catch (NullPointerException npE) {
        }
    }

    /**
     * selects the given cards and moves them in the selection window
     * 
     * @param selectedCards
     *            the cards that should be selected
     */
    public void setSelectedCards(Card[] selectedCards) {
        ArrayList<Draggable> allDraggableList = new ArrayList<Draggable>();
        for (Draggable drag : draggables) {
            allDraggableList.add(drag);
        }
        ArrayList<Draggable> selectedDraggableList = new ArrayList<Draggable>();
        ArrayList<Draggable> unselectedDraggableList = new ArrayList<Draggable>();

        // divide cards on onTable or not
        for (Card card : selectedCards) {
            int index = allDraggableList.indexOf(new Draggable(0, 0, card));
            if (index != -1)
                selectedDraggableList.add(allDraggableList.get(index));
        }
        for (Draggable card : draggables) {
            if (!selectedDraggableList.contains(card)) {
                unselectedDraggableList.add(card);
            }
        }
        // add cards to right list (ordered)
        int selSize = selectedDraggableList.size();
        int unselSize = unselectedDraggableList.size();
        for (int i = 0; i < selSize; i++) {
            Draggable temp = selectedDraggableList.get(i);
            // set their new positions
            temp.x = (i + 1) * (selectTableWidth / 6) + selectTableX - temp.width / 2;
            temp.y = (selectTableHeight / 2) + selectTableY - temp.height / 2;
        }
        for (int i = 0; i < unselSize; i++) {
            Draggable temp = unselectedDraggableList.get(i);
            // set their new positions
            temp.x = (i + 1) * (Window.width / (unselSize + 1)) - temp.width / 2;
            temp.y = Window.height / 4 * 3 - temp.height / 2;
        }
        organiseCards();
        cardselect = true;
        hideCardselect = false;
        updatePreviewPosition();
    }

    /**
     * formats all selected cards nicely
     */
    public void organiseCards() {
        ArrayList<Draggable> selectedDraggableList = new ArrayList<Draggable>();
        ArrayList<Draggable> unselectedDraggableList = new ArrayList<Draggable>();
        try {
            // divide cards on onTable or not
            for (Draggable card : draggables) {
                if (!card.outOfBoundsOf(selectTableX, selectTableY, selectTableWidth, selectTableHeight)) {
                    selectedDraggableList.add(card);
                } else {
                    unselectedDraggableList.add(card);
                }
            }
            // add cards to right list (ordered)
            int selSize = selectedDraggableList.size();
            int unselSize = unselectedDraggableList.size();
            for (int i = 0; i < selSize; i++) {
                Draggable temp = getLeftestCard(selectedDraggableList);
                selectedDraggableList.remove(temp);
                // set their new positions
                temp.x = (i + 1) * (selectTableWidth / 6) + selectTableX - temp.width / 2;
                temp.y = (selectTableHeight / 2) + selectTableY - temp.height / 2;
            }
            for (int i = 0; i < unselSize; i++) {
                Draggable temp = getHighestCard(unselectedDraggableList);
                unselectedDraggableList.remove(temp);
                // set their new positions
                temp.x = (i + 1) * (Window.width / (unselSize + 1)) - temp.width / 2;
                temp.y = draggablePositionY - temp.height / 2;
            }
        } catch (NullPointerException npE) {
        }
    }

    public void updateCards(Card[] cards) {
        if (cards != null) {
            for (int i = 0; i < cards.length; i++) {
                if (draggables[i] == null) {
                    draggables[i] = new Draggable(0, 0, cards[i]);
                } else {
                    draggables[i].card = cards[i];
                }
                int width = draggables[i].width;
                int height = draggables[i].height;
                int posx = (i + 1) * (Window.width / 10) - width / 2;
                int posy = Window.height / 4 * 3 - height / 2;
                draggables[i].x = posx;
                draggables[i].y = posy;
            }
            cardselect = true;
            hideCardselect = false;
            directionSelection.updatePos(
                    new Vector2f(map.getCheckpoints()[mainClient.getMyRobot().getCheckpoint()].getPosition()));
            directionSelection.setVisible(!mainClient.getMyRobot().isOnMap());
            world = WorldAnalyser.getReachableFields(map, mainClient.getMyRobot(), cards);
            // reachable fields
            displayReachableFields();
            organiseCards();
        } else {
            for (int i = 0; i < draggables.length; i++) {
                if (draggables[i] != null)
                    draggables[i] = null;
            }
        }
    }

    /**
     * tests and returns which cards were selected by the user
     * 
     * @return the cards that were selected by the user
     */
    public Card[] getCardsToSend() {
        ArrayList<Draggable> draggableList = new ArrayList<Draggable>();
        Draggable[] draggableArray = new Draggable[5];
        Card[] cards = new Card[5];
        try {
            for (Draggable card : draggables) {
                if (!card.outOfBoundsOf(selectTableX, selectTableY, selectTableWidth, selectTableHeight)) {
                    draggableList.add(card);
                }
            }
            for (int i = 0; i < draggableArray.length; i++) {
                draggableArray[i] = getLeftestCard(draggableList);
                draggableList.remove(draggableArray[i]);
                // Console.out(this.getClass(), "Card_" + i + " is " +
                // draggableArray[i].card.getType() + " with value " +
                // draggableArray[i].card.getValue());
            }
            for (int i = 0; i < cards.length; i++) {
                cards[i] = draggableArray[i].card;
            }
        } catch (NullPointerException npE) {
            // npE.printStackTrace();
        }
        return cards;
    }

    /**
     * returns the selected card thats on the left
     * 
     * @param cardList
     *            the cards that the card should be selected from
     * @return the card with the lowest y position
     */
    public Draggable getLeftestCard(ArrayList<Draggable> cardList) {
        Draggable temp = null;
        for (Draggable card : cardList) {
            temp = card.returnLeftCardOf(temp);
        }
        return temp;
    }

    /**
     * returns the selected card thats on the left
     * 
     * @param cardList
     *            the cards that the card should be selected from
     * @return the card with the lowest y position
     */
    public Draggable getHighestCard(ArrayList<Draggable> cardList) {
        Draggable temp = null;
        for (Draggable card : cardList) {
            temp = card.higherCardOf(temp);
        }
        return temp;
    }

    /**
     * hide or show the card select state
     * 
     * @param forceHidden
     */
    public void toggleHideCardselect(boolean forceHidden) {
        hideCardselect = (forceHidden) ? true : !hideCardselect;
        organiseCards();
    }

    /**
     * resizes the select field size after the window size changed
     */
    public static void updateSelectTableBounds() {
        selectTableX = Window.width / 4;
        selectTableY = Window.height / 12;
        selectTableWidth = Window.width / 2;
        selectTableHeight = Window.height / 5;
        draggablePositionY = Window.height / 6 * 5;
    }

    /**
     * resizes the select field size after the window size changed
     */
    public static void updateCreditBounds() {
        kueks.clear();
        creditToIterator = 0;
        creditsMiGaEnabled = false;
        creditsDropping = false;
        creditsFinished = false;
        creditsScore = 0;
        secretCreditScore = 0;
        creditIterator = 0;
        kuekSpeed = 3.5f;
        kuekSinMod = 0.0f;
        schreddWidth = Window.width / 10;
        schreddX = Window.width / 2;
        schreddY = Window.height - Window.height / 20;
    }

    /**
     * tests which card was clicked on
     * 
     * @param x
     *            the x position of the card
     * @param y
     *            the y position of the card
     */
    public void lockCard(int x, int y) {
        if (!cardselect || hideCardselect)
            return;
        if (selectedDraggable != null)
            return;
        for (int i = draggables.length - 1; i >= 0; i--) {
            if (draggables[i] != null && draggables[i].x <= x && x <= draggables[i].x + draggables[i].width
                    && draggables[i].y <= y && y <= draggables[i].y + draggables[i].height) {
                selectedDraggable = draggables[i];
                return;
            }
        }
    }

    /**
     * frees the selected card
     */
    public void releaseCard() {
        selectedDraggable = null;
    }

    /**
     * sets the menu state to the given state whilst displaying a fix state
     * 
     * @param state
     *            the new state
     */
    public void closeConfirmationWindow() {
        menuView.endGame();
    }

    /**
     * updates the button for fxaa
     * 
     * @param enabled
     */
    public void updateAntiAliasing(boolean enabled) {
        menuView.updateAntiAliasing(enabled);
    }

    /**
     * This method renders the damage number of the Lasers over their start
     * location according to their orientation
     */
    private void renderLaserDMG() {
        if (map != null) {
            float fotSize = 15f;
            Laser[] lasers = map.getLasers();
            ArrayList<Laser> laserMap = new ArrayList<Laser>();
            for (Laser laser : lasers) {
                laserMap.add(laser);
                Vector4f pos4f;

                if (laser.getDirection() == Orientation.UP) {
                    pos4f = new Vector4f(laser.getFieldX(), 0.5f, laser.getFieldY() + 0.4f, 1);
                } else if (laser.getDirection() == Orientation.DOWN) {
                    pos4f = new Vector4f(laser.getFieldX(), 0.5f, laser.getFieldY() - 0.4f, 1);
                } else if (laser.getDirection() == Orientation.LEFT) {
                    pos4f = new Vector4f(laser.getFieldX() + 0.4f, 0.5f, laser.getFieldY(), 1);
                } else {
                    pos4f = new Vector4f(laser.getFieldX() - 0.4f, 0.5f, laser.getFieldY(), 1);
                }

                Vector2f sc = toScreenCoords(pos4f);
                if (sc != null) {
                    renderInGameText("" + laser.getDamage(), sc.x, sc.y, fotSize);
                }
            }
        }
    }

    /**
     * removes all stuff that was displayed in the last round
     */
    public void clearRound() {
        world = null;
        cardselect = false;
        hideCardselect = true;
        directionSelection.setVisible(false);
        if (lastTemporarySelection != null)
            lastTemporarySelection.getDirectionSelection().setDirectionsVisible(new boolean[4]);
        updateCards(null);
        clearReachableFields();
        hidePreview();
    }

}