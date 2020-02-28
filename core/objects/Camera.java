package core.objects;

import java.nio.FloatBuffer;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;

import core.math.Matrix4f;
import core.math.Vector3f;
import core.math.Vector4f;
import core.shader.ShaderProgram;
import core.window.Window;

/**
 * 
 * Abstract Camera Class holds basic camera attributes as well as a uniform
 * block and a dynamic matrices change.
 * 
 * @author Adrian Schuhmaier
 *
 */
public abstract class Camera {

	/** field of view */
	protected float fov;
	/** aspect ratio */
	protected float aspectRatio;
	/** near clipping distance (for the projection matrix) */
	protected float nearClipping;
	/** far clipping distance (for the projection matrix) */
	protected float farClipping;

	/** position of the camera */
	protected Vector3f position;
	/** orientation variable */
	protected float rotation, pitch;
	/** if the camera has changed since the last updateUniformBlock() call */
	protected boolean changed;
	/** if the camera is reflected over the xz plane */
	protected boolean reflected;

	/** uniform buffer holding the view and projection matrix */
	protected int matricesUniformBuffer;
	/** uniform buffer holding the dimensions of the window */
	protected int dimensionsUniformBuffer;

	/**
	 * Initializes and allocates the matrices buffer. </br>
	 * Shall not be called without OpenGLContext ready.
	 * 
	 * @param boardWidth
	 *            number of tiles in the x direction
	 * @param boardLength
	 *            number of tiles in the z direction
	 * 
	 * @return new initialized Camera
	 */
	public Camera() {

		this.fov = 70;
		this.aspectRatio = 16f / 9f;
		this.nearClipping = 0.1f;
		this.farClipping = 90f;
		this.rotation = 0;
		this.pitch = 20;
		this.position = new Vector3f(0f, 0f, 0f);

		// generate a buffer
		matricesUniformBuffer = GL15.glGenBuffers();
		// bind the buffer
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matricesUniformBuffer);
		// setup the buffer's target, size and usage
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, new float[32], GL15.GL_DYNAMIC_DRAW);
		// bind the buffer object to binding the binding point
		GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, ShaderProgram.MATRICES_BUFFER_BINDING, matricesUniformBuffer);

		// create a buffer for the projection matrix
		FloatBuffer projectionMatrix = genProjectionMatrix().getBuffer();
		// write the buffer to the openGL buffer (offset 64 =
		// #floats(mat4)*#bytes(float))
		GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 64, projectionMatrix);

		MemoryUtil.memFree(projectionMatrix);

		// generate a buffer
		dimensionsUniformBuffer = GL15.glGenBuffers();
		// bind the buffer
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, dimensionsUniformBuffer);
		// setup the buffer's target, size and usage
		GL15.glBufferData(GL31.GL_UNIFORM_BUFFER, new float[] { Window.width, Window.height }, GL15.GL_STATIC_DRAW);
		// bind the buffer object to binding the binding point
		GL30.glBindBufferBase(GL31.GL_UNIFORM_BUFFER, ShaderProgram.DIMENSIONS_BUFFER_BINDING, dimensionsUniformBuffer);

		// unbind the buffer
		GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

		changed = true;
		reflected = false;
	}

	/**
	 * If the camera's parameters have changed, the view and projection matrix
	 * will be updated in the uniform buffer. </br>
	 * Shall be called at least every time the camera attributes are changed
	 * (notice: there is no performance loss if it's called every frame).
	 */
	public void updateUniformBlock() {
		if (changed) {
			// create a buffer for the view matrix
			FloatBuffer viewMatrix = genViewMatrix().getBuffer();
			FloatBuffer projectionMatrix = genProjectionMatrix().getBuffer();

			// bind the uniform buffer
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, matricesUniformBuffer);
			// override the viewMatrix data in the buffer
			GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 0, viewMatrix);
			// override the projectionMatrix data in the buffer
			GL15.glBufferSubData(GL31.GL_UNIFORM_BUFFER, 64, projectionMatrix);
			// unbind the buffer
			GL15.glBindBuffer(GL31.GL_UNIFORM_BUFFER, 0);

			MemoryUtil.memFree(viewMatrix);
			MemoryUtil.memFree(projectionMatrix);
		}
		changed = false;
	}

	/**
	 * Calculates the camera's view matrix.
	 * 
	 * @return view matrix
	 */
	protected Matrix4f genViewMatrix() {

		// calculate the camera's coordinate system
		Vector3f[] coordSys = genCoordSystem(position);
		Vector3f right, up, forward;
		forward = coordSys[0];
		right = coordSys[1];
		up = coordSys[2];

		// we move the world, not the camera
		Vector3f position = this.position.negate();

		return genViewMatrix(forward, right, up, position);
	}

	/**
	 * Generates the a view Matrix from the given parameters.
	 * 
	 * @param forward
	 * @param right
	 * @param up
	 * @param position
	 * @return view matrix
	 */
	protected Matrix4f genViewMatrix(Vector3f forward, Vector3f right, Vector3f up, Vector3f position) {

		if (reflected)
			position = new Vector3f(position.x, -position.y, position.z);
		// combining into the view matrix
		Vector4f c1, c2, c3, c4;
		c1 = new Vector4f(right.x, up.x, forward.x, 0f);
		c2 = new Vector4f(right.y, up.y, forward.y, 0f);
		c3 = new Vector4f(right.z, up.z, forward.z, 0f);
		c4 = new Vector4f();
		c4.x = position.x * right.x + position.y * right.y + position.z * right.z;
		c4.y = position.x * up.x + position.y * up.y + position.z * up.z;
		c4.z = position.x * forward.x + position.y * forward.y + position.z * forward.z;
		c4.w = 1f;

		return new Matrix4f(c1, c2, c3, c4);
	}

	/**
	 * Calculates the coordinate system for the camera (forward, right and up
	 * vector).
	 * 
	 * @param forward
	 *            backwards Vector from the view of the camera
	 * @return Vector3f[] { forward, right, up}
	 */
	protected Vector3f[] genCoordSystem(Vector3f forward) {
		if (reflected)
			forward = new Vector3f(forward.x, -forward.y, forward.z);
		Vector3f right, up;
		forward = forward.normalize();
		right = (new Vector3f(0f, reflected ? -1f : 1f, 0f)).cross(forward).normalize();
		up = forward.cross(right);
		return new Vector3f[] { forward, right, up };
	}

	/**
	 * Calculates the camera's projection matrix.
	 * 
	 * @return projection matrix
	 */
	private Matrix4f genProjectionMatrix() {
		return Matrix4f.perspective(fov, aspectRatio, nearClipping, farClipping);
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getRotation() {
		return rotation;
	}

	public float getPitch() {
		return pitch;
	}
	
	public float getFOV() {
		return fov;
	}

	public float getAspectRatio() {
		return aspectRatio;
	}
	
	public Matrix4f getProjectionMatrix() {
		return genProjectionMatrix();
	}

	public Matrix4f getViewMatrix() {
		return genViewMatrix();
	}

	public Matrix4f getViewProjectionMatrix() {
		return getProjectionMatrix().multiply(getViewMatrix());// getViewMatrix().multiply(getProjectionMatrix());
	}

	public boolean isChanged() {
		return changed;
	}

	public void setReflected(boolean reflect) {
		this.reflected = reflect;
		this.changed = true;
		this.updateUniformBlock();
		this.changed = true;
	}
}
