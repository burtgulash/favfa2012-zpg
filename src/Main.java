import java.io.File;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.util.glu.GLU.gluPerspective;


public class Main {
	private static final float METRES_PER_FLOAT = 2f;
	
	public static void main(String[] args) {
		TerrainLoader tl = new TerrainLoader();
		float [][] heights = tl.getHeights(new File("./terrain.raw"));
		float x, y, z;
		
		
		try {
			Display.setDisplayMode(new DisplayMode(800, 600));
			Display.setTitle("ZPG 2012");
			Display.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

		while (!Display.isCloseRequested()) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			// Perspective transformations
			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();
			gluPerspective(40f, (float) Display.getWidth() / (float) Display.getHeight(), .1f, 500f);
			
			// Modelview transformations
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			glTranslatef(0f, -30f, -350f);
			glRotatef(35, 1f, 1, 0);
			
			// Render terrain
			for (int r = 0; r < heights.length - 1; r++) {
				glBegin(GL_TRIANGLE_STRIP);
					for (int c = 0; c < heights[r].length; c++) {
						x = (float) (r - heights.length / 2) * METRES_PER_FLOAT;
						y = heights[r][c];
						z = (float) (c - heights[r].length / 2) * METRES_PER_FLOAT;
						glVertex3f(x, y, z);
						
						x += METRES_PER_FLOAT;
						y = heights[r + 1][c];
						glVertex3f(x, y, z);
					}
				glEnd();
			}
			
			Display.update();
			Display.sync(60);
		}
		
		Display.destroy();
		System.exit(0);
	}
}
