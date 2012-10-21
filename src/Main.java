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
	
	private static final float METRES_PER_FLOAT = 2f;
	private static long lastFrameTime;
	
	static Terrain t;
	
	static float altitude = 0;
	static float azimuth = 0;
	static boolean w, s, a, d;
	static float cam_x = 0, cam_y = 0, cam_z = 0;
	static float speed = 3f;
	
	static final float MAX_LOOK_UP = 90f, MAX_LOOK_DOWN = -90f;
	
	static boolean wire_frame = false;
	static int wire_frame_lock = 0;
	static float v_invert = 1f;
	static long v_invert_lock = 0;
	
	static int rr, cc;
	static int sun_angle = 0;
	
	
	
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
		
		lastFrameTime = getTime();
		
		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(2);
		}
		
		// Render wireframe
		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		// Cursor visible on/off
		Mouse.setGrabbed(true);
		
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_NORMALIZE);
		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		glEnable(GL_LIGHT1);
		glEnable(GL_LIGHT2);
		// glLightModel(GL_LIGHT_MODEL_AMBIENT, asFloatBuffer(new float[]{1.5f, 1.5f, 1.5f, 1f}));
		glLight(GL_LIGHT0, GL_DIFFUSE, asFloatBuffer(new float[]{1f,1f,1f, 1f}));
		glLight(GL_LIGHT1, GL_DIFFUSE, asFloatBuffer(new float[]{.5f,.5f,.5f, .5f}));
		glLight(GL_LIGHT2, GL_DIFFUSE, asFloatBuffer(new float[]{.5f,.5f,.5f, .5f}));
	}
	
	private static void render() {
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		// glClearColor(.85f, .85f, 1f, 1f);
		
		
		rr = (int) ((-cam_x / METRES_PER_FLOAT) + t.getW() / 2);
		cc = (int) ((-cam_z / METRES_PER_FLOAT) + t.getH() / 2);
		try {
			cam_y = -(CAM_HEIGHT + t.getHeight(cc, rr));
		} catch (ArrayIndexOutOfBoundsException e) {
		}
		
		// Perspective transformations
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		gluPerspective(70f, (float) Display.getWidth() / (float) Display.getHeight(), .001f, 500f);
		
		
		
		// Modelview transformations
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(-altitude, v_invert * 1f, 0f, 0f);
		glRotatef(azimuth, 0, 1, 0);
		glTranslatef(cam_x, cam_y, cam_z);
		
		glPushMatrix();
		glRotatef(sun_angle++, 0, 0, 1);
		glLight(GL_LIGHT0, GL_POSITION, 
				asFloatBuffer(new float[]{0f, METRES_PER_FLOAT * t.getW() + 50f, 0f, 1f}));
		glPopMatrix();
		
		glLight(GL_LIGHT1, GL_POSITION, asFloatBuffer(new float[]{300f, 300f, 300f, 1f}));
		glLight(GL_LIGHT2, GL_POSITION, asFloatBuffer(new float[]{-300f, 300f, -300f, 1f}));
		
		glColor3f(1f, 1f, 1f);
		
		// Render terrain
		for (int r = 0; r < t.getH() - 1; r++) {
			glBegin(GL_TRIANGLE_STRIP);
			for (int c = 0; c < t.getW(); c++) {
				float x, y, z;
				Vector3f n;
				
				x = (float) (c - t.getW() / 2) * METRES_PER_FLOAT;
				y = t.getHeight(r, c);
				z = (float) (r - t.getH() / 2) * METRES_PER_FLOAT;
				n = t.getNormal(r, c);
				
				glNormal3f(n.x, n.y, n.z);
				glVertex3f(x, y, z);
				
				y = t.getHeight(r + 1, c);
				z += METRES_PER_FLOAT;
				n = t.getNormal(r + 1, c);
				
				glNormal3f(n.x, n.y, n.z);
				glVertex3f(x, y, z);
			}
			glEnd();
		}
		
		// Buttons, keyboard, ...
		speed = (float) getTimeDelta() * MOVEMENT_SPEED / 1000f;
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