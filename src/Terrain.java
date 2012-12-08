import java.io.BufferedInputStream;
import static org.lwjgl.opengl.GL11.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Vector3f;

public class Terrain {
	private int xxx, zzz;
	private int width, height;
	private float[][] hs; // heights
	private float[][] is; // interpolated points
	private float[][] xds; // derivatives in x direction
	private float[][] zds; // derivatives in z direction
	private Vector3f[][] ns; // normals
	private boolean loadFailed = false;

	private static final float METRES_PER_FLOAT = 2f;
	private static final int SUBDIVISION_LVL = 2;
	private static int SUBDIVISION_FACTOR;
	
	private static FloatBuffer[] strips;
	private static final int STRIDE = (3 + 3) * 4;
	private static float min_x, max_x, center_x;
	private static float min_z, max_z, center_z;

	/**
	 * Vytvori objekt tridy Terrain.
	 * 
	 * @param file
	 *            soubor s mapou
	 * @param w
	 *            sirka mapy, musi byt znama predem
	 * @param h
	 *            vyska mapy, musí byt znama predem
	 */
	public Terrain(File file, int w, int h) {
		xxx = width = w;
		zzz = height = h;
		max_z = (h - 1) / 2;
		min_z = -max_z;
		max_x = (w - 1) / 2;
		min_x = -max_x;
		center_z = (max_z - min_z) / 2;
		center_x = (max_x - min_x) / 2;

		hs = getHeights(file);
		if (loadFailed())
			return;

		// TODO computed with subdivided is
		// computeDerivatives(hs);
		// computeNormals(hs);
		// computeStrips(hs, ns);
		computeDerivatives(hs);
		is = subdivide(hs, SUBDIVISION_LVL);
		computeNormals(is);
		computeStrips(is, ns);
	}

	private float[][] subdivide(float[][] hs, int lvl) {
		SUBDIVISION_FACTOR = 1;
		while (--lvl > 0)
			SUBDIVISION_FACTOR *= 2;
		width *= SUBDIVISION_FACTOR;
		height *= SUBDIVISION_FACTOR;
		
		float[][] is = new float[height][width];

		for (int i = 0; i < zzz; i++) {
			for (int j = 0; j < xxx; j++) {
				int start_z = SUBDIVISION_FACTOR * i;
				int start_x = SUBDIVISION_FACTOR * j;

				for (int z = 0; z < SUBDIVISION_FACTOR; z++)
					for (int x = 0; x < SUBDIVISION_FACTOR; x++) {
						float x_frac = (float) x / (float) SUBDIVISION_FACTOR;
						float z_frac = (float) z / (float) SUBDIVISION_FACTOR;
						is[start_z + z][start_x + x] = getY(j - center_x
								+ x_frac, i - center_z + z_frac);
					}
			}
		}

		return is;
	}

	public float getY(float x, float z) {
		x = Math.max(min_x, Math.min(x, max_x));
		z = Math.max(min_z, Math.min(z, max_z));
		return getYcoons(x, z);
	}

	public float getYcoons(float x, float z) {
		x += center_x;
		z += center_z;
		// TODO border cases

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

		if (ax >= xxx && az >= zzz)
			return getH(xxx - 1, zzz - 1);
		if (ax >= xxx)
			return splineEval(
					hSpline(getH(bx, bz), getH(dx, dz), zds[bz][bx],
							zds[dz][dx]), v);
		if (az >= zzz)
			return splineEval(
					hSpline(getH(cx, cz), getH(dx, dz), xds[cz][cx],
							xds[dz][dx]), u);

		float ay = getH(ax, az);
		float cy = getH(cx, cz);
		float by = getH(bx, bz);
		float dy = getH(dx, dz);

		float e = splineEval(hSpline(ay, by, xds[az][ax], xds[bz][bx]), u);
		float f = splineEval(hSpline(cy, dy, xds[cz][cx], xds[dz][dx]), u);
		float g = splineEval(hSpline(ay, cy, zds[az][ax], zds[cz][cx]), v);
		float h = splineEval(hSpline(by, dy, zds[bz][bx], zds[dz][dx]), v);

		float lin_u = (h - g) * u + g;
		float lin_v = (f - e) * v + e;

		float p = (by - ay) * u + ay;
		float q = (dy - cy) * u + cy;
		float bilin = (q - p) * v + p;

		return lin_u + lin_v - bilin;
	}

	public float getYbilinear(float x, float z) {
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

		if (ax >= xxx && az >= zzz)
			return getH(xxx - 1, zzz - 1);
		if (ax >= xxx)
			return (getH(dx, dz) - getH(bx, bz)) * v + getH(bx, bz);
		if (az >= zzz)
			return (getH(dx, dz) - getH(cx, cz)) * u + getH(cx, cz);

		float ay = getH(ax, az);
		float by = getH(bx, bz);
		float cy = getH(cx, cz);
		float dy = getH(dx, dz);

		float p = (by - ay) * u + ay;
		float q = (dy - cy) * u + cy;

		return (q - p) * v + p;
	}

	private float getH(int x, int z) {
		return hs[z][x];
	}

	private float splineEval(float[] s, float t) {
		return s[3] + t * (s[2] + t * (s[1] + t * s[0]));
	}

	private float[] hSpline(float y0, float y1, float yy0, float yy1) {
		float[] spline = new float[4];
		spline[0] = 2 * y0 - 2 * y1 + yy0 + yy1;
		spline[1] = -3 * y0 + 3 * y1 - 2 * yy0 - yy1;
		spline[2] = yy0;
		spline[3] = y0;
		return spline;
	}

	/**
	 * Test jestli se teren nacetl spravne.
	 * 
	 * @return true, pokud ano.
	 */
	public boolean loadFailed() {
		return loadFailed;
	}

	/**
	 * Nacte y-souradnice ze souboru.
	 * 
	 * @param file
	 *            soubor s vyskovou mapou terenu.
	 * @return Parsovanou vyskovou mapu.
	 */
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

	private void computeDerivatives(float[][] hs) {
		xds = new float[zzz][xxx];
		zds = new float[zzz][xxx];

		for (int z = 1; z < zzz - 1; z++)
			for (int x = 1; x < xxx - 1; x++) {
				xds[z][x] = (hs[z][x + 1] - hs[z][x - 1]) / 2f;
				zds[z][x] = (hs[z + 1][x] - hs[z - 1][x]) / 2f;
			}
	}

	private void computeStrips(float[][] hs, Vector3f[][] ns) {
		strips = new FloatBuffer[hs.length - 1];

		for (int i = 0; i < strips.length; i++) {
			strips[i] = BufferUtils.createFloatBuffer(STRIDE * width);
			for (int j = 0; j < width; j++) {
				float x = (float) j / (float) SUBDIVISION_FACTOR - center_x;
				float y = hs[i][j];
				// float y = getH(j, i);
				float z = (float) i / (float) SUBDIVISION_FACTOR - center_z;
				Vector3f n = ns[i][j];

				strips[i].put(x).put(y).put(z);
				strips[i].put(n.x).put(n.y).put(n.z);

				// y = getH(j, i + 1);
				y = hs[i + 1][j];
				z = (float) (i + 1) / (float) SUBDIVISION_FACTOR - center_z;
				n = ns[i + 1][j];

				strips[i].put(x).put(y).put(z);
				strips[i].put(n.x).put(n.y).put(n.z);
			}
		}
	}

	/**
	 * Vypocte normaly pro kazdy bod terenu.
	 */
	private void computeNormals(float[][] hs) {
		ns = new Vector3f[hs.length][];
		for (int i = 0; i < hs.length; i++)
			ns[i] = new Vector3f[hs[0].length];

		for (int z = 0; z < hs.length; z++)
			for (int x = 0; x < hs[0].length; x++) {
				Vector3f sum = new Vector3f(0, 0, 0);
				Vector3f top, bottom, left, right;
				top = bottom = left = right = null;

				if (z > 0)
					top = new Vector3f(0f, hs[z - 1][x] - hs[z][x], -1f);
				if (z < zzz - 1)
					bottom = new Vector3f(0f, hs[z + 1][x] - hs[z][x], 1f);
				if (x > 0)
					left = new Vector3f(-1f, hs[z][x - 1] - hs[z][x], 0f);
				if (x < xxx - 1)
					right = new Vector3f(1f, hs[z][x + 1] - hs[z][x], 0f);

				if (x > 0 && z > 0)
					Vector3f.add(Vector3f.cross(top, left, null), sum, sum);
				if (x > 0 && z < zzz - 1)
					Vector3f.add(Vector3f.cross(left, bottom, null), sum, sum);
				if (x < xxx - 1 && z < zzz - 1)
					Vector3f.add(Vector3f.cross(bottom, right, null), sum, sum);
				if (x < xxx - 1 && z > 0)
					Vector3f.add(Vector3f.cross(right, top, null), sum, sum);

				// add normalisation
				ns[z][x] = sum.normalise(sum);
			}
	}

	public void draw() {
		for (int r = 0; r < strips.length; r++) {
			glEnableClientState(GL_VERTEX_ARRAY);
			glEnableClientState(GL_NORMAL_ARRAY);
			strips[r].position(0);
			glVertexPointer(3, STRIDE, strips[r]);
			strips[r].position(3);
			glNormalPointer(STRIDE, strips[r]);

			glDrawArrays(GL_TRIANGLE_STRIP, 0, 2 * width);

			glDisableClientState(GL_NORMAL_ARRAY);
			glDisableClientState(GL_VERTEX_ARRAY);
		}
	}
}
