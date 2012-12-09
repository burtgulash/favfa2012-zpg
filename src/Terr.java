import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.GL11.*;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.*;
import org.lwjgl.opengl.*;
import static org.lwjgl.opengl.ARBBufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;

public class Terr {
	private static final float METRES_PER_FLOAT = 2f;

	private int width, height;
	private float[][] hs; // heights
	private boolean loadFailed = false;

	private float min_x, max_x, center_x;
	private float min_z, max_z, center_z;
	
	private FloatBuffer vbuf, nbuf;
	private IntBuffer ibuf;
	private int vHandle, nHandle, iHandle;
	private int vs;

	public Terr(File file, int w, int h) {
		width = w;
		height = h;
		max_z = (h - 1) / 2;
		min_z = -max_z;
		max_x = (w - 1) / 2;
		min_x = -max_x;
		center_z = (max_z - min_z) / 2;
		center_x = (max_x - min_x) / 2;

		vs = 3 * 2 * width * height;
		
		hs = getHeights(file);
		if (loadFailed())
			return;
		
		fillBuffers();
	}

	public boolean loadFailed() {
		return loadFailed;
	}
	private float[][] getHeights(File file) {
		int rows = 128;
		int cols = 128;
		byte[] buf = new byte[rows * cols];
		float[][] heights = new float[rows][cols];
		int len = Math.min(buf.length, (int) file.length());
		int bytes_received = 0;

		try {
			InputStream in = new BufferedInputStream(new FileInputStream(file));
			while (bytes_received < len)
				bytes_received += in.read(buf, bytes_received, len
						- bytes_received);

			in.close();
		} catch (FileNotFoundException e) {
			loadFailed = true;
			System.err.println("File " + file.getName() + " not found.");
			return null;
		} catch (IOException e) {
			loadFailed = true;
			return null;
		}

		for (int r = 0; r < rows; r++)
			for (int c = 0; c < cols; c++) {
				heights[r][c] = (float) ((int) buf[r * cols + c] & 0xFF) / 10f;
				heights[r][c] /= METRES_PER_FLOAT;
			}

		return heights;
	}

	public float getH(int nx, int nz) {
		nx = Math.max(0, Math.min(nx, width - 1));
		nz = Math.max(0, Math.min(nz, height - 1));
		
		return hs[nx][nz];
	}
	
	public float getY(float x, float z) {
		x = Math.max(min_x, Math.min(x, max_x));
		z = Math.max(min_z, Math.min(z, max_z));
		
		return getYcoons(x, z);
	}
	
	public float getYstep(float x, float z) {
		return hs[(int) (x + center_x)][(int) (z + center_z)];
	}
	public float getYcoons(float x, float z) {
		x += center_x;
		z += center_z;

		int ax = (int) x;
		int az = (int) z;
		int bx = ax + 1;
		int bz = az;
		int cx = ax;
		int cz = az + 1;
		int dx = ax + 1;
		int dz = az + 1;

		float u = x - (float) ax;
		float v = z - (float) az;

		// TODO borders

		float ay = getH(ax, az);
		float cy = getH(cx, cz);
		float by = getH(bx, bz);
		float dy = getH(dx, dz);
		
		float dax = (by - getH(ax - 1, az)) / 2f;
		float dbx = (getH(bx + 1, bz) - ay) / 2f;
		float dcx = (dy - getH(cx - 1, cz)) / 2f;
		float ddx = (getH(dx + 1, dz) - cy) / 2f;
		
		float daz = (cy - getH(ax, az - 1)) / 2f;
		float dbz = (dy - getH(bx, bz - 1)) / 2f;
		float dcz = (getH(cx, cz + 1) - ay) / 2f;
		float ddz = (getH(dx, dz + 1) - by) / 2f;
		

		float e = hSpline(u, ay, by, dax, dbx);
		float f = hSpline(u, cy, dy, dcx, ddx);
		float g = hSpline(v, ay, cy, daz, dcz);
		float h = hSpline(v, by, dy, dbz, ddz);

		float lin_u = (h - g) * u + g;
		float lin_v = (f - e) * v + e;

		float p = (by - ay) * u + ay;
		float q = (dy - cy) * u + cy;
		float bilin = (q - p) * v + p;

		return lin_u + lin_v - bilin;
	}
	
	private float getYbilinear(float x, float z) {
		x += center_x;
		z += center_z;
		
		int ax = (int) x;
		int az = (int) z;
		int bx = ax + 1;
		int bz = az;
		int cx = ax;
		int cz = az + 1;
		int dx = ax + 1;
		int dz = az + 1;

		float ay = hs[ax][az];
		float by = hs[bx][bz];
		float cy = hs[cx][cz];
		float dy = hs[dx][dz];
		
		float u = x - (float) ax;
		float v = z - (float) az;
		
		float p = (by - ay) * u + ay;
		float q = (dy - cy) * u + cy;

		return (q - p) * v + p;
	}

	private float hSpline(float t, float y0, float y1, float yy0, float yy1) {
		float s = 0;
		
		s += 2 * y0 - 2 * y1 + yy0 + yy1;
		s *= t;
		s += -3 * y0 + 3 * y1 - 2 * yy0 - yy1;
		s *= t;
		s += yy0;
		s *= t;
		s += y0;
		
		return s;
	}
	
	public void buildVBOs() {
		IntBuffer ib = BufferUtils.createIntBuffer(3);
		ARBBufferObject.glGenBuffersARB(ib);
		vHandle = ib.get(0);
		nHandle = ib.get(1);
		iHandle = ib.get(2);
	}
	
	private void bufAdd(FloatBuffer v, FloatBuffer n, int i, int j) {
		float x = i - center_x;
		float z = j - center_z;
		float y = getY(x, z);
		v.put(x).put(y).put(z);
		
		float dx = (getY(x + 1, z) - getY(x - 1, z)) / 2f;
		float dz = (getY(x, z + 1) - getY(x, z - 1)) / 2f;
		Vector3f nx = new Vector3f(1f, dx, 0f);
		Vector3f nz = new Vector3f(0f, dz, 1f);
		Vector3f normal = Vector3f.cross(nz, nx, null);
		normal.normalise();
		n.put(normal.x).put(normal.y).put(normal.z);
	}
	
	private void fillBuffers() {
		vbuf = BufferUtils.createFloatBuffer((3 << 2) * vs);
		nbuf = BufferUtils.createFloatBuffer((3 << 2) * vs);
		ibuf = BufferUtils.createIntBuffer(vs);

		int index = 0;
		for (int i = 0; i < width - 1; i++)
			for (int j = 0; j < height - 1; j++) {
				bufAdd(vbuf, nbuf, i, j);
				bufAdd(vbuf, nbuf, i + 1, j);
				bufAdd(vbuf, nbuf, i, j + 1);
				bufAdd(vbuf, nbuf, i + 1, j + 1);
				
				ibuf.put(index + 0);
				ibuf.put(index + 1);
				ibuf.put(index + 2);
				
				ibuf.put(index + 2);
				ibuf.put(index + 1);
				ibuf.put(index + 3);
				
				index += 4;
			}
		
		vbuf.flip();
		nbuf.flip();
		ibuf.flip();
	}
		

	public void draw() {
		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_NORMAL_ARRAY);
		
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vbuf, GL_STATIC_DRAW_ARB);
		glVertexPointer(3, GL_FLOAT, 3 << 2, 0l);
		
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, nHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, nbuf, GL_STATIC_DRAW_ARB);
		glNormalPointer(GL_FLOAT, 3 << 2, 0l);
		
		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, iHandle);
		glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, ibuf, GL_STATIC_DRAW_ARB);
		
		glDrawElements(GL_TRIANGLES, vs, GL_UNSIGNED_INT, 0L);
		
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

		glDisableClientState(GL_NORMAL_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		
//		ib.put(0, vHandle);
//		ib.put(1, nHandle);
//		glDeleteBuffersARB(ib);
	}
}
