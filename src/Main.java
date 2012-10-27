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
	private static final float MOVEMENT_SPEED = 3f;
	private static final float CAM_HEIGHT = 1.85f;
	private static final double DAY_LENGTH = 2 * 60;
	private static final int FPS = 60;

	private static final float SUN_DISTANCE = 500f;
	private static final float METRES_PER_FLOAT = 2f;

	private static long lastFrameTime;

	static Terrain t;
	static FloatBuffer[] strips;
	static float min_x, max_x, center_x;
	static float min_z, max_z, center_z;
	private static final int STRIDE = (3 + 3) * 4;

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
		if (dir.endsWith("bin") || dir.endsWith("bin/")) {
			if ((t = new Terrain(new File("data/mapa128x128.raw"), 128, 128))
					.loadFailed())
				System.exit(1);
			System.setProperty("org.lwjgl.librarypath", dir
					+ "/../lib/natives/");
		} else {
			if ((t = new Terrain(new File("bin/data/mapa128x128.raw"), 128, 128))
					.loadFailed())
				System.exit(1);
			System.setProperty("org.lwjgl.librarypath", dir + "/lib/natives/");
		}

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

	// ziska souradnici Y (tzn. vysku v terenu) na zaklade pozice v terenu (x,
	// z)
	private static float getY(float x, float z) {
		// ziskat indexy ctverce, ve kterem jsou zadane souradnice a ktery se
		// nachazi v terenu, ne mimo nej.
		int cc = (int) ((x + center_x) / METRES_PER_FLOAT);
		int rr = (int) ((z + center_z) / METRES_PER_FLOAT);
		cc = Math.min(Math.max(0, cc), t.getW() - 1 - 1);
		rr = Math.min(Math.max(0, rr), t.getH() - 1 - 1);

		// ziskat pozici v terenu rohu tohoto ctverce
		float rdx = (float) cc * METRES_PER_FLOAT - center_x;
		float rdz = (float) rr * METRES_PER_FLOAT - center_z;

		Vector3f b = new Vector3f(rdx, t.getHeight(cc, rr + 1), rdz
				+ METRES_PER_FLOAT);
		Vector3f c = new Vector3f(rdx + METRES_PER_FLOAT, t.getHeight(cc + 1,
				rr), rdz);
		Vector3f cmb = Vector3f.sub(c, b, null);
		Vector3f v;

		// test na jeden ze dvou trojuhelníku ve ctverci
		if (cmb.x * (z - b.z) > cmb.z * (x - b.x))
			v = new Vector3f(rdx + METRES_PER_FLOAT,
					t.getHeight(cc + 1, rr + 1), rdz + METRES_PER_FLOAT);
		else
			v = new Vector3f(rdx, t.getHeight(cc, rr), rdz);

		// ziskani normaly trojúhelníku
		Vector3f vmb = Vector3f.sub(v, b, null);
		Vector3f n = Vector3f.cross(vmb, cmb, null);
		// reseni rovnice n.x * x + n.y * y + n.z * z == nb
		return (Vector3f.dot(n, b) - n.x * x - n.z * z) / n.y;
	}

	// inicializace
	private static void init() {
		min_z = -(t.getH() - 1) * METRES_PER_FLOAT / 2;
		max_z = (t.getH() - 1) * METRES_PER_FLOAT / 2;
		min_x = -(t.getW() - 1) * METRES_PER_FLOAT / 2;
		max_x = (t.getW() - 1) * METRES_PER_FLOAT / 2;
		center_z = (max_z - min_z) / 2;
		center_x = (max_x - min_x) / 2;

		strips = new FloatBuffer[t.getH() - 1];
		for (int i = 0; i < strips.length; i++) {
			strips[i] = BufferUtils.createFloatBuffer(STRIDE * t.getW());
			for (int j = 0; j < t.getW(); j++) {
				float x = (float) j * METRES_PER_FLOAT - center_x;
				float y = t.getHeight(j, i);
				float z = (float) i * METRES_PER_FLOAT - center_z;
				Vector3f n = t.getNormal(j, i);

				strips[i].put(x).put(y).put(z);
				strips[i].put(n.x).put(n.y).put(n.z);

				y = t.getHeight(j, i + 1);
				z += METRES_PER_FLOAT;
				n = t.getNormal(j, i + 1);

				strips[i].put(x).put(y).put(z);
				strips[i].put(n.x).put(n.y).put(n.z);
			}
		}

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
		cam.y = -(CAM_HEIGHT + getY(-cam.x, -cam.z));
	}

	// renderovani jednoho snimku
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
		for (int r = 0; r < t.getH() - 1; r++) {
			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_NORMAL_ARRAY);
			strips[r].position(0);
			glVertexPointer(3, STRIDE, strips[r]);
			strips[r].position(3);
			glNormalPointer(STRIDE, strips[r]);

			glDrawArrays(GL_TRIANGLE_STRIP, 0, 2 * t.getW());

			glDisableClientState(GL_NORMAL_ARRAY);
			glDisableClientState(GL_VERTEX_ARRAY);
		}

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

		velocity.y -= getY(
				-Math.min(Math.max(min_x, (cam.x + velocity.x)), max_x),
				-Math.min(Math.max(min_z, (cam.z + velocity.z)), max_z))
				- getY(-cam.x, -cam.z);
		if (velocity.length() > .01f) {
			velocity.normalise();
			velocity.scale(speed);

			cam.x = Math.min(Math.max(min_x, cam.x + velocity.x), max_x);
			cam.z = Math.min(Math.max(min_z, cam.z + velocity.z), max_z);
			cam.y = -(CAM_HEIGHT + getY(-cam.x, -cam.z));
		}

		azimuth += Mouse.getDX();
		float dy = Mouse.getDY();
		if (altitude + dy > MAX_LOOK_DOWN && altitude + dy < MAX_LOOK_UP)
			altitude += dy;

		w = Keyboard.isKeyDown(Keyboard.KEY_W);
		s = Keyboard.isKeyDown(Keyboard.KEY_S);
		a = Keyboard.isKeyDown(Keyboard.KEY_A);
		d = Keyboard.isKeyDown(Keyboard.KEY_D);

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
		day_time = ((sun_angle + 180f) % 360f) / 360f;

		Display.update();
		Display.sync(FPS);
	}
}
