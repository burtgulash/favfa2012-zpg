import java.io.File;
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
	// TODO speed = 3f
	private static final float MOVEMENT_SPEED = 10f / METRES_PER_FLOAT;
	private static final float CAM_HEIGHT = 1.85f / METRES_PER_FLOAT;
	private static final double DAY_LENGTH = 2 * 60;
	private static final int FPS = 60;

	private static final float SUN_DISTANCE = 500f;

	private static long lastFrameTime;

	static Terr t;

	static float altitude = 0;
	static float azimuth = 0;
	static boolean w, s, a, d;
	static Vector3f cam;
	static float speed = 0;
	static Vector3f velocity = new Vector3f(0, 0, 0);

	static final float MAX_LOOK_UP = 90f, MAX_LOOK_DOWN = -90f;

	static boolean wire_frame = false;
	static int wire_frame_lock = 0;
	static float v_invert = 1f;
	static int v_invert_lock = 0;

	static float sun_angle = 0f;
	static float sun_intensity = 0f;
	static double day_time = 0f;
	static final float AMB_INT = .1f;

	// main
	public static void main(String[] args) {
		String dir = System.getProperty("user.dir");
		System.setProperty("org.lwjgl.librarypath", dir + "/lib/natives");
		t = new Terr(new File(dir + "/mapa128x128.raw"), 128, 128);

		init();

		while (!Display.isCloseRequested()
				&& !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
			render();

		Display.destroy();
	}

	// hladka funkce urcující intenzitu svetla na zaklade casu
	private static float getSunIntensity(double t) {
		// -.5f -> center on midday
		// 4 -> magic constant to make sky pretty
		double arg = (t - .5d) * 4d;
		return (float) Math.exp(-arg * arg);
	}

	// ziska systemovy cas
	private static long getTime() {
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}

	// ziska casovy rozdil mezi po sobe jdoucimi snimky
	private static int getTimeDelta() {
		long currentTime = getTime();
		int delta = (int) (currentTime - lastFrameTime);
		lastFrameTime = currentTime;

		return delta;
	}

	// pomocna metoda pro zabaleni pole floatu do floatbufferu
	private static FloatBuffer asFloatBuffer(float[] fs) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(fs.length);
		buf.put(fs);
		buf.flip();
		return buf;
	}

	// inicializace
	private static void init() {
		lastFrameTime = getTime();

		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012");
			Display.create();
		} catch (LWJGLException e) {
			System.exit(2);
		}

		// Cursor visible on/off
		Mouse.setGrabbed(true);

		glEnable(GL_DEPTH_TEST);
		glEnable(GL_NORMALIZE);
		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		glEnable(GL_LIGHT1);
		glEnable(GL_LIGHT2);

		cam = new Vector3f(0, 0, 0);
		cam.y = -(CAM_HEIGHT + t.getY(-cam.x, -cam.z));
		
		t.buildVBOs();
	}

	// renderovani jednoho snimku
	private static void render() {
		float si = getSunIntensity(day_time);

		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		glClearColor(si * .9f, si * .9f, si, 1f);
		// TODO remove
		si = .6f;

		// Perspective transformations
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(80f,
				(float) Display.getWidth() / (float) Display.getHeight(),
				.001f, 700f);

		// Modelview transformations
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(-altitude, v_invert * 1f, 0f, 0f);
		glRotatef(azimuth, 0, 1, 0);
		glTranslatef(cam.x, cam.y, cam.z);

		// Ambient light
		glLight(GL_LIGHT1, GL_DIFFUSE, asFloatBuffer(new float[] { AMB_INT,
				AMB_INT, AMB_INT, 1f }));
		glLight(GL_LIGHT1, GL_POSITION, asFloatBuffer(new float[] { 100f, 100f,
				100f, 1f }));

		glLight(GL_LIGHT2, GL_DIFFUSE, asFloatBuffer(new float[] { AMB_INT,
				AMB_INT, AMB_INT, 1f }));
		glLight(GL_LIGHT2, GL_POSITION, asFloatBuffer(new float[] { -100f,
				100f, -100f, 1f }));

		// Sun
		glPushMatrix();
		glRotatef(sun_angle, 0f, 0f, 1f);

		// TODO POKUS slunce begin
		glEnable(GL_POINT_SMOOTH);
		glDisable(GL_LIGHTING);
		glPointSize(30f);
		glBegin(GL_POINTS);
		glColor3f(1f, 1f, 1f);
		glVertex3f(0f, SUN_DISTANCE, 0f);
		glEnd();
		glEnable(GL_LIGHTING);
		glDisable(GL_POINT_SMOOTH);
		// TODO POKUS slunce end

		glLight(GL_LIGHT0, GL_DIFFUSE, asFloatBuffer(new float[] { si, si,
				.85f * si, 1f }));
		glLight(GL_LIGHT0, GL_POSITION, asFloatBuffer(new float[] { 0f,
				SUN_DISTANCE, 0f, 1f }));
		glPopMatrix();

		glColor3f(.93f, .93f, .93f);

		// Render terrain
		t.draw();

		// Buttons, keyboard, ...
		int delta = getTimeDelta();
		speed = (float) delta * MOVEMENT_SPEED / 1000f;

		float azimuth_rads = (float) Math.toRadians(azimuth);
		velocity.set(0, 0, 0);
		if (w) {
			velocity.x += Math.cos(azimuth_rads + Math.PI / 2d);
			velocity.z += Math.sin(azimuth_rads + Math.PI / 2d);
		}
		if (s) {
			velocity.x -= Math.cos(azimuth_rads + Math.PI / 2d);
			velocity.z -= Math.sin(azimuth_rads + Math.PI / 2d);
		}
		if (a) {
			velocity.x += Math.cos(azimuth_rads);
			velocity.z += Math.sin(azimuth_rads);
		}
		if (d) {
			velocity.x -= Math.cos(azimuth_rads);
			velocity.z -= Math.sin(azimuth_rads);
		}
		if (velocity.length() > 0.1f)
			velocity.normalise();
		velocity.scale(speed);

		cam.x += velocity.x;
		cam.z += velocity.z;
		cam.y = -(CAM_HEIGHT + t.getY(-cam.x, -cam.z));

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

		// WIREFRAME
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

		// VERTICAL INVERT
		if (v_invert_lock <= 0) {
			if (Keyboard.isKeyDown(Keyboard.KEY_U)) {
				v_invert *= -1;
				altitude *= -1;
				v_invert_lock = 15;
			}

		} else
			v_invert_lock--;

		// Update time of day and angle of sun
		sun_angle += (float) delta * 360f / (1000f * DAY_LENGTH);
		sun_angle %= 360f;
		// TODO debug remove 60
		sun_angle = 60;
		day_time = ((sun_angle + 180f) % 360f) / 360f;
		
		// TODO debug FPS begin
		int fps = (int) (1000f / (float) delta);
		if (fps < 50)
			System.out.printf("fps: %d%n", fps);
		// TODO debug end

		Display.update();
		Display.sync(FPS);
	}
}
