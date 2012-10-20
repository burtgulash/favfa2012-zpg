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
	private static final float MOVEMENT_SPEED = 20f;
	private static long lastFrameTime;
	
	
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
	
	public static void main(String[] args) {
		TerrainLoader tl = new TerrainLoader();
		float [][] heights = tl.getHeights(new File("./terrain.raw"));
		float x, y, z;
		float altitude = 0;
		float azimuth = 0;
		float azimuth_rads = (float) Math.toRadians(azimuth);
		float dy;
		boolean w, s, a, d;
		w = s = a = d = false;
		float pos_x = 0, pos_y = 0, pos_z = 0;
		float speed = 3f;
		
		final float MAX_LOOK_UP = 70f, MAX_LOOK_DOWN = -90f;
		
		
		lastFrameTime = getTime();
		
		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		// Render wireframe
//		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
		// Cursor visible on/off
		Mouse.setGrabbed(true);
		
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_NORMALIZE);
		glEnable(GL_LIGHTING);
		glEnable(GL_LIGHT0);
		// glLightModel(GL_LIGHT_MODEL_AMBIENT, asFloatBuffer(new float[]{.5f, .5f, .5f, 1f}));
		glLight(GL_LIGHT0, GL_DIFFUSE, asFloatBuffer(new float[]{1.5f,1.5f,1.5f, 1f}));

		while (!Display.isCloseRequested() && !Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			// FIXME POKUS begin
			int rr = (int) ((pos_x / METRES_PER_FLOAT) + heights.length / 2);
			int cc = (int) ((pos_z / METRES_PER_FLOAT) + heights[rr].length / 2);
			
			pos_y = -5f - heights[rr][cc];
			// FIXME end POKUS
			
			// Perspective transformations
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			gluPerspective(70f, (float) Display.getWidth() / (float) Display.getHeight(), .001f, 500f);
			
			
			
			// Modelview transformations
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			glRotatef(-altitude, 1, 0, 0);
			glRotatef(azimuth, 0, 1, 0);
			glTranslatef(pos_x, pos_y, pos_z);
			
			glLight(GL_LIGHT0, GL_POSITION, asFloatBuffer(new float[]{0f, -40f, 0f, 1f}));
			
			glColor3f(.4f, .4f, .4f);
			
			// Render terrain
			for (int r = 0; r < heights.length - 1; r++) {
				int center_x = heights.length / 2;
				glBegin(GL_TRIANGLE_STRIP);
				for (int c = 0; c < heights[r].length; c++) {
					int center_z = heights[r].length / 2;
					Vector3f aaa, bbb, ccc, ddd;
					Vector3f n;
					
					if (r < heights.length - 2 && c < heights[r].length - 2) {
						x = (float) (r - center_x) * METRES_PER_FLOAT;
						y = heights[r][c];
						z = (float) (c - center_z) * METRES_PER_FLOAT;
						aaa = new Vector3f(x, y, z);
						
						x += 2f * METRES_PER_FLOAT;
						y = heights[r + 2][c];
						bbb = new Vector3f(x, y, z);
						
						y = heights[r + 2][c + 2];
						z += 2f * METRES_PER_FLOAT;
						ccc = new Vector3f(x, y, z);
						
						x -= 2f * METRES_PER_FLOAT;
						y = heights[r][c + 2];
						ddd = new Vector3f(x, y, z);
						
						n = new Vector3f();
						n = Vector3f.cross(Vector3f.sub(aaa, ccc, null), Vector3f.sub(bbb, ddd, null), n);
						glNormal3f(n.x, n.y, n.z);
					}
					
					x = (float) (r - center_x) * METRES_PER_FLOAT;
					y = heights[r][c];
					z = (float) (c - center_z) * METRES_PER_FLOAT;
					glVertex3f(x, y, z);
					
					x += METRES_PER_FLOAT;
					y = heights[r + 1][c];
					
					/// druhej
					r++;
					if (r < heights.length - 2 && c < heights[r].length - 2) {
						x = (float) (r - center_x) * METRES_PER_FLOAT;
						y = heights[r][c];
						z = (float) (c - center_z) * METRES_PER_FLOAT;
						aaa = new Vector3f(x, y, z);
						
						x += 2f * METRES_PER_FLOAT;
						y = heights[r + 2][c];
						bbb = new Vector3f(x, y, z);
						
						y = heights[r + 2][c + 2];
						z += 2f * METRES_PER_FLOAT;
						ccc = new Vector3f(x, y, z);
						
						x -= 2f * METRES_PER_FLOAT;
						y = heights[r][c + 2];
						ddd = new Vector3f(x, y, z);
						
						n = new Vector3f();
						n = Vector3f.cross(Vector3f.sub(aaa, ccc, null), Vector3f.sub(bbb, ddd, null), n);
						glNormal3f(n.x, n.y, n.z);
					}
						
					x = (float) (r - center_x) * METRES_PER_FLOAT;
					y = heights[r][c];
					z = (float) (c - center_z) * METRES_PER_FLOAT;
					glVertex3f(x, y, z);
						
					r--;
				}
				glEnd();
			}
			
			// Buttons, keyboard, ...
			speed = (float) getTimeDelta() * MOVEMENT_SPEED / 1000f;
			azimuth_rads = (float) Math.toRadians(azimuth);
			if (w) {
				pos_x += speed * Math.cos(azimuth_rads + Math.PI / 2d);
				pos_z += speed * Math.sin(azimuth_rads + Math.PI / 2d);
			} if (s) {
				pos_x -= speed * Math.cos(azimuth_rads + Math.PI / 2d);
				pos_z -= speed * Math.sin(azimuth_rads + Math.PI / 2d);
			} if (a) {
				pos_x += speed * Math.cos(azimuth_rads);
				pos_z += speed * Math.sin(azimuth_rads);
			} if (d) {
				pos_x -= speed * Math.cos(azimuth_rads);
				pos_z -= speed * Math.sin(azimuth_rads);
			}
			
			azimuth += Mouse.getDX();
			dy = Mouse.getDY();
			if (altitude + dy > MAX_LOOK_DOWN && altitude + dy < MAX_LOOK_UP)
				altitude += dy;
			
			w = Keyboard.isKeyDown(Keyboard.KEY_W) || Keyboard.isKeyDown(Keyboard.KEY_UP);
			s = Keyboard.isKeyDown(Keyboard.KEY_S) || Keyboard.isKeyDown(Keyboard.KEY_DOWN);
			a = Keyboard.isKeyDown(Keyboard.KEY_A) || Keyboard.isKeyDown(Keyboard.KEY_LEFT);
			d = Keyboard.isKeyDown(Keyboard.KEY_D) || Keyboard.isKeyDown(Keyboard.KEY_RIGHT);
			
			Display.update();
			Display.sync(60);
		}
		
		Display.destroy();
		System.exit(0);
	}
}