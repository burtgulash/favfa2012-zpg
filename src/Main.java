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
	private static final double DAY_LENGTH = 120d;
	private static final float SUN_DISTANCE = 500f;
	
	private static final float METRES_PER_FLOAT = 2f;
	private static long lastFrameTime;
	
	static Terrain t;
	static float min_x, max_x;
	static float min_z, max_z;
	
	static float altitude = 0;
	static float azimuth = 0;
	static boolean w, s, a, d;
	static float cam_x = 0, cam_y = 0, cam_z = 0;
	static float speed = 0;
	
	static final float MAX_LOOK_UP = 90f, MAX_LOOK_DOWN = -90f;
	
	static boolean wire_frame = false;
	static int wire_frame_lock = 0;
	static float v_invert = 1f;
	static long v_invert_lock = 0;
	
	static float sun_angle = 0f;
	static float sun_intensity = 0f;
	static double day_time = 0f;
	
	
	private static float getSunIntensity() {
		// -.5f -> center on midday
		double sin = Math.sin(day_time * Math.PI - .5d);
		return (float) Math.max(0d, sin * sin * sin);
	}
	
	
	private static long getTime() {
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}
	
	private static int getTimeDelta() {
		long currentTime = getTime();
		int delta = (int) (currentTime - lastFrameTime);
		lastFrameTime = currentTime;
		
		return delta;
	}
	
	private static FloatBuffer asFloatBuffer(float[] fs) {
		FloatBuffer buf = BufferUtils.createFloatBuffer(fs.length);
		buf.put(fs);
		buf.flip();
		return buf;
	}
	
	private static void init() {
		t = new Terrain(new File("./terrain.raw"), 128, 128);
		min_z = -(t.getH() - 2) * METRES_PER_FLOAT / 2;
		max_z = t.getH() * METRES_PER_FLOAT / 2;
		min_x = -(t.getW() - 2) * METRES_PER_FLOAT / 2;
		max_x = t.getW() * METRES_PER_FLOAT / 2;
		
		lastFrameTime = getTime();
		
		
		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
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
		
	}
	
	private static float getY(float x, float z) {
		int cc = (int) ((x / METRES_PER_FLOAT) + t.getW() / 2);
		int rr = (int) ((z / METRES_PER_FLOAT) + t.getH() / 2);
		float rdx = (float) (cc - t.getW() / 2) * METRES_PER_FLOAT;
		float rdz = (float) (rr - t.getW() / 2) * METRES_PER_FLOAT;
		
		Vector3f b = new Vector3f(rdx, t.getHeight(cc, rr + 1), rdz + METRES_PER_FLOAT);
		Vector3f c = new Vector3f(rdx + METRES_PER_FLOAT, t.getHeight(cc + 1, rr), rdz);
		Vector3f cmb = Vector3f.sub(c, b, null);
		Vector3f v;
		
		// if (cmb.x * (z - b.z) > cmb.z * (x - b.x))
		if (x - b.x > z - b.z)
			v = new Vector3f(rdx + METRES_PER_FLOAT, t.getHeight(cc + 1, rr + 1), rdz + METRES_PER_FLOAT);
		else
			v = new Vector3f(rdx, t.getHeight(cc, rr), rdz);
		
		Vector3f vmb = Vector3f.sub(v, b, null);
		Vector3f n = Vector3f.cross(vmb, cmb, null);
		return (Vector3f.dot(n, b) - n.x * x - n.z * z) / n.y;
	}
	
	static Vector3f p = new Vector3f(0,0,0);
	static boolean g = false;
	
	private static void render() {
		float si = getSunIntensity();
		
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		// glClearColor(.85f, .85f, 1f, 1f);
		
		
		// Perspective transformations
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(80f, (float) Display.getWidth() / (float) Display.getHeight(), .001f, 700f);
		
		try {
			cam_y = -(CAM_HEIGHT + getY(-cam_x, -cam_z));
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		
		// Modelview transformations
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(-altitude, v_invert * 1f, 0f, 0f);
		glRotatef(azimuth, 0, 1, 0);
		glTranslatef(cam_x, cam_y, cam_z);
		
		
		
		// Ambient light
		// float ambInt = Math.max(.1f, si * .7f);
		float ambInt = .1f;
		glLight(GL_LIGHT1, GL_DIFFUSE, asFloatBuffer(new float[]{ambInt, ambInt, ambInt, 1f}));
		glLight(GL_LIGHT1, GL_POSITION, asFloatBuffer(new float[]{100f, 100f, 100f, 1f}));
		
		glLight(GL_LIGHT2, GL_DIFFUSE, asFloatBuffer(new float[]{ambInt, ambInt, ambInt, 1f}));
		glLight(GL_LIGHT2, GL_POSITION, asFloatBuffer(new float[]{-100f, 100f, -100f, 1f}));
		
		// Sun
		glPushMatrix();
		glRotatef(sun_angle, 0f, 0f, 1f);
		
		// TODO POKUS begin
		glPointSize(20f);
		glDisable(GL_LIGHTING);
		glColor3f(1f, 1f, 0f);
		glBegin(GL_POINTS);
			glVertex3f(0f, SUN_DISTANCE, 0f);
		glEnd();
		glEnable(GL_LIGHTING);
		// TODO POKUS end
		
		glLight(GL_LIGHT0, GL_DIFFUSE, asFloatBuffer(new float[]{si, si, .85f * si, 1f}));
		glLight(GL_LIGHT0, GL_POSITION, asFloatBuffer(new float[]{0f, SUN_DISTANCE, 0f, 1f}));
		glPopMatrix();
		
		
		
		glColor3f(1f, 1f, 1f);
		
		// Render terrain
		for (int r = 0; r < t.getH() - 1; r++) {
			glBegin(GL_TRIANGLE_STRIP);
			for (int c = 0; c < t.getW(); c++) {
				float x, y, z;
				Vector3f n;
				
				x = (float) (c - t.getW() / 2) * METRES_PER_FLOAT;
				y = t.getHeight(c, r);
				z = (float) (r - t.getH() / 2) * METRES_PER_FLOAT;
				n = t.getNormal(c, r);
				
				glNormal3f(n.x, n.y, n.z);
				glVertex3f(x, y, z);
				
				y = t.getHeight(c, r + 1);
				z += METRES_PER_FLOAT;
				n = t.getNormal(c, r + 1);
				
				glNormal3f(n.x, n.y, n.z);
				glVertex3f(x, y, z);
			}
			glEnd();
		}
		
		// Buttons, keyboard, ...
		int delta = getTimeDelta();
		speed = (float) delta * MOVEMENT_SPEED / 1000f;
		if ((w || s) && (a || d)
				&& !(((w || s) && a && d) || ((a || d) && w && s)))
			speed *= 1 / Math.sqrt(2);
		
		float azimuth_rads = (float) Math.toRadians(azimuth);
		if (w) {
			cam_x += speed * Math.cos(azimuth_rads + Math.PI / 2d);
			cam_z += speed * Math.sin(azimuth_rads + Math.PI / 2d);
		} if (s) {
			cam_x -= speed * Math.cos(azimuth_rads + Math.PI / 2d);
			cam_z -= speed * Math.sin(azimuth_rads + Math.PI / 2d);
		} if (a) {
			cam_x += speed * Math.cos(azimuth_rads);
			cam_z += speed * Math.sin(azimuth_rads);
		} if (d) {
			cam_x -= speed * Math.cos(azimuth_rads);
			cam_z -= speed * Math.sin(azimuth_rads);
		}
		
		// Keep camera in bounds
		cam_x = Math.min(Math.max(min_x, cam_x), max_x);
		cam_z = Math.min(Math.max(min_z, cam_z), max_z);
		
		azimuth += Mouse.getDX();
		float dy = Mouse.getDY();
		if (altitude + dy > MAX_LOOK_DOWN && altitude + dy < MAX_LOOK_UP)
			altitude += dy;
		
		w = Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP);
		s = Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN);
		a = Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT);
		d = Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
		
		// WIREFRAME
		if (wire_frame_lock == 0) {
			if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
				wire_frame = !wire_frame;
				wire_frame_lock = 15;
			}
		} else
			wire_frame_lock --;
		
		if (wire_frame)
			glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		else
			glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		
		
		// VERTICAL INVERT
		if (v_invert_lock < getTime() && Keyboard.isKeyDown(Keyboard.KEY_U)) {
			v_invert *= -1f;
			altitude *= -1f;
			v_invert_lock = getTime() + 100;
		}
		
		
		sun_angle += (float) delta * 360f / (1000f * DAY_LENGTH);
		sun_angle %= 360f;
		day_time = ((sun_angle + 180f) % 360f) / 360f;
		
		
		Display.update();
		Display.sync(60);
	}
	
	public static void main(String[] args) {
		init();

		while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE))
			render();
		
		Display.destroy();
	}
}