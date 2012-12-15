import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;

public class Main {
	private static final float METRES_PER_FLOAT = 2f;
	private static final float MOVEMENT_SPEED = 30f / METRES_PER_FLOAT;
	private static final float CAM_HEIGHT = 1.85f / METRES_PER_FLOAT;
	private static final double DAY_LENGTH = 2 * 60;

	private static final int V_SYNC = 60;

	private static final float SUN_DISTANCE = 500f;

	private static long lastFrameTime;
	public static float[][] clipPlanes;

	private static Terrain t;
	private static int width, height;

	private static float altitude = 0;
	private static float azimuth = 0;
	private static boolean w, s, a, d;
	private static Vector3f cam;
	private static boolean inAir = false;
	private static Vector3f velocity = new Vector3f(0, 0, 0);

	private static final float MAX_LOOK_UP = 90f, MAX_LOOK_DOWN = -90f;

	private static boolean wire_frame = false;
	private static int wire_frame_lock = 0;
	private static float v_invert = 1f;
	private static int v_invert_lock = 0;

	private static float sun_angle = 0f;
	private static double day_time = 0f;
	private static final float AMB_INT = .1f;
	private static final float GRAVITY = 20f;
	private static final float ATTENUATION = .8f;

	// main
	public static void main(String[] args) {
		init();

		while (!Display.isCloseRequested()
				&& !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
			render();

		Display.destroy();
	}

	private static void init() {
		String dir = System.getProperty("user.dir");
		System.setProperty("org.lwjgl.librarypath", dir + "/lib/natives");

		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012 - Tomas Marsalek (A10B0632P)");
			Display.create();
		} catch (LWJGLException e) {
			System.exit(2);
		}

		width = height = 128;
		try {
			t = new Terrain(new File(dir + "/mapa128x128.raw"), width, height);
		} catch (FileNotFoundException e) {
			System.err.println("File not found");
			System.exit(1);
		} catch (IOException e) {
			System.err.println("Can not load terrain.");
			System.exit(1);
		}

		try {
			// t.loadTexture(dir + "/rough_terrain_texture_24-512x512.png");
			t.loadTexture(dir + "/sand.png");
		} catch (IOException e) {
			System.out.println("texture");
			System.exit(1);
		}

		t.buildVBOs();

		lastFrameTime = getTime();

		// Cursor visible on/off
		Mouse.setGrabbed(true);

		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_NORMAL_ARRAY);
		if (t.useTexture())
			glEnableClientState(GL_TEXTURE_COORD_ARRAY);

		glShadeModel(GL_SMOOTH);
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_NORMALIZE);
		glEnable(GL_COLOR_MATERIAL);
		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		glEnable(GL_LIGHT1);
		glEnable(GL_LIGHT2);
		glLightModel(GL_LIGHT_MODEL_AMBIENT, asFloatBuffer(new float[] { .02f,
				.02f, .02f, 1f }));

		cam = new Vector3f(-64, 0, -64);
		cam.y = -(CAM_HEIGHT + t.getY(-cam.x, -cam.z));
	}

	private static void render() {
		float si = getSunIntensity(day_time);

		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glClearColor(si * .9f, si * .9f, si, 1f);

		// Perspective transformations
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(80f,
				(float) Display.getWidth() / (float) Display.getHeight(),
				.001f, 700f);

		// Modelview transformations
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(-altitude, v_invert * 1, 0, 0);
		glRotatef(azimuth, 0, 1, 0);
		glTranslatef(cam.x, cam.y, cam.z);

		// Ambient light
		glLight(GL_LIGHT1, GL_DIFFUSE, asFloatBuffer(new float[] { AMB_INT,
				AMB_INT, AMB_INT, 1f }));
		glLight(GL_LIGHT1, GL_POSITION, asFloatBuffer(new float[] { width,
				SUN_DISTANCE, height, 1f }));

		glLight(GL_LIGHT2, GL_DIFFUSE, asFloatBuffer(new float[] { AMB_INT,
				AMB_INT, AMB_INT, 1f }));
		glLight(GL_LIGHT2, GL_POSITION, asFloatBuffer(new float[] { 0,
				SUN_DISTANCE, height, 1f }));

		// Sun
		glPushMatrix();
		glRotatef(sun_angle, 0, 0, 1);

		glEnable(GL_POINT_SMOOTH);
		glDisable(GL_LIGHTING);
		glPointSize(30);
		glBegin(GL_POINTS);
		glColor3f(1, 1, 1);
		glVertex3f(0, SUN_DISTANCE, 0);
		glEnd();
		glEnable(GL_LIGHTING);
		glDisable(GL_POINT_SMOOTH);

		// sun
		float ri = (1 - AMB_INT) * si;
		glLight(GL_LIGHT0, GL_DIFFUSE, asFloatBuffer(new float[] { ri, ri,
				.98f * ri, 1f }));
		glLight(GL_LIGHT0, GL_AMBIENT, asFloatBuffer(new float[] { ri * .05f,
				ri * .05f, ri * .07f, 1f }));
		glLight(GL_LIGHT0, GL_POSITION, asFloatBuffer(new float[] { 0f,
				SUN_DISTANCE, 0f, 1f }));
		glPopMatrix();

		// Render terrain
		getClip();
		glColor3f(1, 1, 1);
		glEnable(GL_TEXTURE_2D);
		t.draw(-cam.x, -cam.z);
		glDisable(GL_TEXTURE_2D);

		// MOVEMENT
		int delta = getTimeDelta();
		float time = (float) delta / 1000f;
		// float speed = (float) delta * MOVEMENT_SPEED / 1000f;

		float azimuth_rads = (float) Math.toRadians(azimuth);

		if (!inAir) {
			cam.y = -(t.getY(-cam.x, -cam.z) + CAM_HEIGHT);
			Vector3f newV = getVelocity(azimuth_rads, MOVEMENT_SPEED);
			if (newV.lengthSquared() > .001f)
				velocity = newV;
			else
				velocity.scale(ATTENUATION); // Friction

			if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
				inAir = true;
				velocity.y += -MOVEMENT_SPEED * 5;
			}
		}

		// GRAVITY
		velocity.y -= -GRAVITY * time;

		// this is distance = v * t
		Vector3f distance = new Vector3f(velocity.x, velocity.y, velocity.z);
		distance.scale(time);

		cam.x += distance.x;
		cam.y += distance.y;
		cam.z += distance.z;

		// enforce terrain bounds
		cam.x = Math.max(-width, Math.min(cam.x, 0));
		cam.z = Math.max(-height, Math.min(cam.z, 0));
		// Collide with terrain

		float collision = t.getY(-cam.x, -cam.z);
		if (collision + CAM_HEIGHT >= -cam.y) {
			inAir = false;
			cam.y = Math.min(cam.y, -(collision + CAM_HEIGHT));
		}

		// height correction
		// cam.y = -(CAM_HEIGHT + t.getY(-cam.x, -cam.z));

		updateSun(delta);

		// Buttons, keyboard, ...
		azimuth += Mouse.getDX();
		float dy = Mouse.getDY();
		if (altitude + dy > MAX_LOOK_DOWN && altitude + dy < MAX_LOOK_UP)
			altitude += dy;

		w = Keyboard.isKeyDown(Keyboard.KEY_W);
		w = w || Keyboard.isKeyDown(Keyboard.KEY_UP);
		s = Keyboard.isKeyDown(Keyboard.KEY_S);
		s = s || Keyboard.isKeyDown(Keyboard.KEY_DOWN);
		a = Keyboard.isKeyDown(Keyboard.KEY_A);
		a = a || Keyboard.isKeyDown(Keyboard.KEY_LEFT);
		d = Keyboard.isKeyDown(Keyboard.KEY_D);
		d = d || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);

		handleWireframeMode();
		handleVerticalInvert();

		printFPS(delta);

		Display.update();
		Display.sync(V_SYNC);
	}

	private static void getClip() {
		float[] modelViewMatrix = new float[16];
		float[] projectionMatrix = new float[16];
		float[][] product = new float[4][4];
		FloatBuffer tmp = BufferUtils.createFloatBuffer(16);

		glGetFloat(GL_MODELVIEW_MATRIX, tmp);
		tmp.get(modelViewMatrix);
		tmp.clear();

		glGetFloat(GL_PROJECTION_MATRIX, tmp);
		tmp.get(projectionMatrix);

		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				for (int k = 0; k < 4; k++)
					product[j][i] += projectionMatrix[k * 4 + j]
							* modelViewMatrix[i * 4 + k];

		clipPlanes = new float[6][];
		float[] p3 = product[3];
		for (int i = 0; i < 3; i++) {
			float[] p = product[i];
			clipPlanes[i * 2] = new float[] {p3[0] + p[0], p3[1] + p[1], p3[2] + p[2], p3[3] + p[3]};
			clipPlanes[i * 2 + 1] = new float[] {p3[0] - p[0], p3[1] - p[1], p3[2] - p[2], p3[3] - p[3]};
		}
	}


	/**
	 * smooth transition function for sun intensity
	 * 
	 * @param t
	 *            time
	 * @return sun intensity.
	 */
	private static float getSunIntensity(double t) {
		// -.5f -> center on midday
		// 4 -> magic constant to make sky pretty
		double arg = (t - .5d) * 4d;
		return (float) Math.exp(-arg * arg);
	}

	/**
	 * Get system time.
	 * 
	 * @return system time in millis
	 */
	private static long getTime() {
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}

	/**
	 * Get time difference between this and last frame.
	 * 
	 * @return time difference in millis.
	 */
	private static int getTimeDelta() {
		long currentTime = getTime();
		int delta = (int) (currentTime - lastFrameTime);
		lastFrameTime = currentTime;

		return delta;
	}

	/**
	 * Helper function to create float buffer from float array.
	 * 
	 * @param fs
	 *            float array
	 * @return float buffer.
	 */
	private static FloatBuffer asFloatBuffer(float[] fs) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(fs.length);
		buf.put(fs);
		buf.flip();
		return buf;
	}

	private static Vector3f getVelocity(float azimuth, float speed) {
		Vector3f v = new Vector3f(0, 0, 0);

		if (w) {
			v.x += Math.cos(azimuth + Math.PI / 2d);
			v.z += Math.sin(azimuth + Math.PI / 2d);
		}
		if (s) {
			v.x -= Math.cos(azimuth + Math.PI / 2d);
			v.z -= Math.sin(azimuth + Math.PI / 2d);
		}
		if (a) {
			v.x += Math.cos(azimuth);
			v.z += Math.sin(azimuth);
		}
		if (d) {
			v.x -= Math.cos(azimuth);
			v.z -= Math.sin(azimuth);
		}

		normaliseVelocity(cam.x, t.getY(-cam.x, -cam.z), cam.z, v);
		v.scale(speed);

		return v;
	}

	private static void normaliseVelocity(float x, float y, float z, Vector3f v) {
		if (v.lengthSquared() < .00001f)
			return;

		for (int i = 0; i < 10; i++) {
			v.normalise();
			v.y = -(t.getY(-(x + v.x), -(z + v.z)) - y);

			if (Math.abs(v.lengthSquared() - 1f) < .001f)
				break;
		}
	}

	private static void handleWireframeMode() {
		if (wire_frame_lock <= 0) {
			if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
				wire_frame = !wire_frame;
				wire_frame_lock = 15;
			}
		} else
			wire_frame_lock--;

		if (wire_frame)
			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		else
			glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
	}

	private static void handleVerticalInvert() {
		if (v_invert_lock <= 0) {
			if (Keyboard.isKeyDown(Keyboard.KEY_U)) {
				v_invert *= -1;
				altitude *= -1;
				v_invert_lock = 15;
			}

		} else
			v_invert_lock--;
	}

	private static void updateSun(int timeDelta) {
		sun_angle += (float) timeDelta * 360f / (1000f * DAY_LENGTH);
		sun_angle %= 360f;
		day_time = ((sun_angle + 180f) % 360f) / 360f;

	}

	private static void printFPS(int timeDelta) {
		int fps = (int) (1000f / (float) timeDelta);
		// if (fps > 50)
		// return;
		System.out.print(fps + " ");
		for (int q = 0; q < fps / 4; q++)
			System.out.print("#");
		System.out.println();
	}
}
