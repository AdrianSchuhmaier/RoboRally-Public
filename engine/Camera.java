package engine;

import core.math.Matrix4f;
import core.math.Vector3f;

/**
 * @author Adrian Schuhmaier
 */
public class Camera extends core.objects.Camera {

    private static Camera instance;

    private float distance;

    private float minPitch;
    private float maxPitch;
    private float minDistance;
    private float maxDistance;
    private int boardWidth;
    private int boardLength;
    private float space;
    private float percentZoom;

    private Vector3f center;

    /**
     * Initializes and allocates the matrices buffer
     * 
     * @param boardWidth
     *            number of tiles in the x direction
     * @param boardLength
     *            number of tiles in the z direction
     * @return new initialized Camera
     */
    public Camera(int boardWidth, int boardLength) {
        super();
        instance = this;

        // TODO: improve camera init
        int longerSide = Math.max(boardWidth, boardLength);
        this.minPitch = 5;
        this.maxPitch = 89;
        this.rotation = 0;
        this.pitch = 20;
        this.distance = longerSide * 0.9f;
        this.minDistance = longerSide * 0.1f;
        this.maxDistance = 1.5f * longerSide;
        this.center = new Vector3f();
        this.center.x = (boardWidth - 1) / 2f;
        this.center.z = (boardLength - 1) / 2f;
        this.boardLength = boardLength;
        this.boardWidth = boardWidth;
        this.space = this.maxDistance - this.minDistance;
        zoom(0f);
    }

    /**
     * creates a new camera with default board width
     */
    public Camera() {
        this(1, 1);
    }

    /**
     * sets the dimensions of the map to the given values
     * 
     * @param boardWidth
     * @param boardLength
     */
    public void setMapDimensions(int boardWidth, int boardLength) {
        int longerSide = Math.max(boardWidth, boardLength);
        this.distance = longerSide * 0.9f;
        this.minDistance = longerSide * 0.1f;
        this.maxDistance = 1.5f * longerSide;
        this.center = new Vector3f();
        this.center.x = (boardWidth - 1) / 2f;
        this.center.z = (boardLength - 1) / 2f;
        this.boardLength = boardLength;
        this.boardWidth = boardWidth;
        this.space = this.maxDistance - this.minDistance;
        zoom(0f);
    }

    /**
     * returns the instance of this camera
     * 
     * @return
     */
    public static Camera getInstance() {
        return instance;
    }

    /**
     * @param delta
     *            added camera rotation (in degrees)
     */
    public void rotate(float delta) {
        this.rotation += delta;
        rotation %= 360;

        changed = true;
    }

    /**
     * @param delta
     *            added pitch (in degrees)
     */
    public void pitch(float delta) {
        this.pitch += delta;

        if (pitch > maxPitch) {
            pitch = maxPitch;
        } else if (pitch < minPitch) {
            pitch = minPitch;
        }

        changed = true;
    }

    /**
     * @param delta
     *            added zoom (higher means closer to the object)
     */
    public void zoom(float delta) {
        this.distance -= delta;
        if (this.distance > maxDistance) {
            this.distance = maxDistance;
        } else if (this.distance <= minDistance) {
            this.distance = minDistance;
        }
        this.percentZoom = (this.distance - this.minDistance) / this.space;
        translateX(0);
        translateY(0);
        changed = true;
    }

    /**
     * used for moving the camera on the field
     * 
     * @param dx
     */
    public void translateX(float dx) {
        this.center.x += Math.cos(Math.toRadians(-rotation)) * dx;
        this.center.z += Math.sin(Math.toRadians(-rotation)) * dx;
        if (this.center.x > (this.boardWidth - ((this.boardWidth / 2f) * percentZoom))) {
            this.center.x = (this.boardWidth - ((this.boardWidth / 2f) * percentZoom));
        } else if (this.center.x < (this.boardWidth / 2f) * percentZoom) {
            this.center.x = (this.boardWidth / 2f) * percentZoom;
        }
        if (this.center.z > this.boardLength - ((this.boardLength / 2f) * percentZoom)) {
            this.center.z = this.boardLength - ((this.boardLength / 2f) * percentZoom);
        } else if (this.center.z < (this.boardLength / 2f) * percentZoom) {
            this.center.z = (this.boardLength / 2f) * percentZoom;
        }
        changed = true;
    }

    /**
     * Used for the movement of the camera on the Y Axis
     * 
     * @param dy
     */
    public void translateY(float dy) {
        this.center.x -= Math.sin(Math.toRadians(rotation)) * dy;
        this.center.z -= Math.cos(Math.toRadians(rotation)) * dy;
        if (this.center.z > this.boardLength - ((this.boardLength / 2f) * percentZoom)) {
            this.center.z = this.boardLength - ((this.boardLength / 2f) * percentZoom);
        } else if (this.center.z < (this.boardLength / 2f) * percentZoom) {
            this.center.z = (this.boardLength / 2f) * percentZoom;
        }
        if (this.center.x > ((this.boardWidth - ((this.boardWidth / 2f) * percentZoom)))) {
            this.center.x = (this.boardWidth - ((this.boardWidth / 2f) * percentZoom));
        } else if (this.center.x < (this.boardWidth / 2f) * percentZoom) {
            this.center.x = (this.boardWidth / 2f) * percentZoom;
        }
        changed = true;
    }

    /** Overridden to build in the center translation. Nothing more. */
    @Override
    protected Matrix4f genViewMatrix() {

        // refreshes the position
        position = calcPosition();

        // calculate the camera's coordinate system
        Vector3f[] coordSys = genCoordSystem(position.subtract(center));
        Vector3f right, up, forward;
        forward = coordSys[0];
        right = coordSys[1];
        up = coordSys[2];

        // we move the world, not the camera
        Vector3f position = this.position.negate();

        return genViewMatrix(forward, right, up, position);
    }

    /**
     * Calculates the camera's position based on the angles and the distance
     * from space origin (+upOffset).
     * 
     * @return camera position
     */
    private Vector3f calcPosition() {
        float y = (float) (distance * Math.sin(Math.toRadians(pitch)));
        float xzDistance = (float) (distance * Math.cos(Math.toRadians(pitch)));
        float x = (float) (xzDistance * Math.sin(Math.toRadians(rotation)));
        float z = (float) (xzDistance * Math.cos(Math.toRadians(rotation)));
        return new Vector3f(x + center.x, y + center.y, z + center.z);
    }

    /**
     * return the center point of the camera
     * 
     * @return
     */
    public Vector3f getCenter() {
        return center;
    }

    /**
     * return the distance of this camera
     * 
     * @return
     */
    public float getDistance() {
        return distance;
    }

    /**
     * get the percentage how far the camera is zoomed out
     * 
     * @return
     */
    public float getPercentZoom() {
        return percentZoom;
    }

    /**
     * returns the distance between the closest and the farthest possible camera
     * 
     * @return
     */
    public float getSpace() {
        return space;
    }

}
