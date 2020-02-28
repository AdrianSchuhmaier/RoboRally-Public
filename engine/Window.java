package engine;

/**
 * A singleton class that manages the glfw window and the user input.
 * 
 * @author Adrian Schuhmaier
 */
public class Window extends core.window.Window {

    boolean yInvert;
    double yacc = 2.5;

    private static String[] windowTitle = new String[] { "Sehr Erbärmliche Kompetenz", "Schlaflos Einsatz-Kommando",
            "Sopra-Existenz-Krise", "Soft, Ehrlich, Kompetent", "Sopra-Einsatz-Kommando", "Sechs Erfahrene K-Oten",
            "Sehr erschöpfter Kai", "SEKs sells"};

    protected static final String WINDOW_TITLE = "SEK - " + random();
    private static Window instance;

    /**
     * Constructor
     */
    protected Window() {
        super();
        yInvert = true;
    }

    public static String random() {
        return windowTitle[(int) (Math.random() * windowTitle.length)];
    };

    /** Returns the Window instance */
    public static Window getInstance() {
        if (instance == null) {
            instance = new Window();
        }
        return instance;
    }

    /** Creates the window in the given size */
    @Override
    public void create(int width, int height) {
        super.create(width, height);
        setWindowTitle(WINDOW_TITLE);
    }

}