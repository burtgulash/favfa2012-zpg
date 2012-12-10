import static org.lwjgl.opengl.GL11.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBBufferObject;
import org.lwjgl.util.vector.Vector3f;

import static org.lwjgl.opengl.ARBBufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;

public class Terrain {
	private int width, height;
	private int nVertices;
	private int scaleFactor;
	private boolean loadFailed = true;
	private float[][] hs;
	public QuadNode root;
	public int rootNodeSize;

	private FloatBuffer vbuf, nbuf;
	private IntBuffer ibuf;
	private int index_count;
	private int vHandle, nHandle, iHandle;

	private final float METRES_PER_FLOAT = 2f;
	private final int SUBDIVISION_LVL = 3;
	private final int SIZEOF_FLOAT = 4;
	public final int ROOT_DEPTH = 1;
	public final int MIN_DEPTH = 6;
	public final int MAX_DEPTH = 11;
	public boolean CULLING_ENABLED = true;

	public Terrain(File file, int w, int h) {
		width = w;
		height = h;

		getHeights(file);
		if (loadFailed)
			return;

		scaleFactor = nSubdivisions(SUBDIVISION_LVL);
		nVertices = scaleFactor * scaleFactor * (width + 1) * (height + 1);
		createBuffers();
		subdivide(scaleFactor);

		rootNodeSize = scaleFactor * width;
		root = new QuadNode(NodeType.ROOT, ROOT_DEPTH, rootNodeSize, 0, null,
				this);

		vbuf.flip();
		nbuf.flip();
	}

	public float[] getV(int i) {
		float[] v = new float[3];

		i *= 3;
		v[0] = vbuf.get(i);
		v[1] = vbuf.get(i + 1);
		v[2] = vbuf.get(i + 2);

		return v;
	}

	private void createBuffers() {
		vbuf = BufferUtils.createFloatBuffer(3 * nVertices);
		nbuf = BufferUtils.createFloatBuffer(3 * nVertices);
		ibuf = BufferUtils.createIntBuffer(12 * nVertices);
	}

	private void subdivide(int n) {
		// + 1 ---> 129 x 129
		float delta = 1f / (float) n;

		for (int i = 0; i < width; i++) {
			for (int xi = 0; xi < n; xi++) {
				float x = (float) i + ((float) xi / (float) n);
				for (int j = 0; j < height; j++) {
					for (int zi = 0; zi < n; zi++) {
						float z = (float) j + ((float) zi / (float) n);
						float y = getY(x, z);

						addVertexNormal(x, y, z, delta);
					}
				}

				addVertexNormal(x, getY(x, height), height, delta);
			}
		}

		for (int j = 0; j < height + 1; j++) {
			for (int zi = 0; zi < n; zi++) {
				float z = (float) j + ((float) zi / (float) n);
				addVertexNormal(width, getY(width, z), z, delta);
			}
		}
	}

	private void addVertexNormal(float x, float y, float z, float delta) {
		float dx = (getY(x + delta, z) - getY(x - delta, z)) / (2 * delta);
		float dz = (getY(x, z + delta) - getY(x, z - delta)) / (2 * delta);

		Vector3f normal = Vector3f.cross(new Vector3f(0, dz, 1), new Vector3f(
				1, dx, 0), null);
		normal.normalise();

		vbuf.put(x).put(y).put(z);
		nbuf.put(normal.x).put(normal.y).put(normal.z);
	}

	public float getY(float x, float z) {
		// return getH((int) x, (int) z);
		return getYcoons(x, z);
	}

	public float getYcoons(float x, float z) {
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

	private float getH(int xi, int zi) {
		xi = Math.max(0, Math.min(xi, width - 1));
		zi = Math.max(0, Math.min(zi, height - 1));

		return hs[xi][zi];
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

	private int nSubdivisions(int lvl) {
		int n = 1;
		while (--lvl > 0)
			n += n;
		return n;
	}

	public boolean loadFailed() {
		return loadFailed;
	}

	private void getHeights(File file) {
		byte[] buf = new byte[width * height];
		hs = new float[width][height];

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
			return;
		} catch (IOException e) {
			System.err.println("Can not load terrain.");
			loadFailed = true;
			return;
		}

		for (int x = 0; x < width; x++)
			for (int z = 0; z < height; z++) {
				hs[x][z] = (float) ((int) buf[x * height + z] & 0xFF) / 10f;
				hs[x][z] /= METRES_PER_FLOAT;
			}

		loadFailed = false;
	}

	public void pushIndex(int i) {
		ibuf.put(i);
		index_count++;
	}

	public void update(float x, float z) {
		index_count = 0;
		ibuf.clear();

		root.merge();
		// root.enforceMinimumDepth();

		// QuadNode active = root.deepestNodeWithPoint(x, z);
		// if (active != null)
		// active.split();

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				float dx = x - (float) i;
				float dz = z - (float) j;
				double distance = (dx * dx + dz * dz) / (width * width / 16);

				int maxDepth = depth(distance);
				// System.out.println(maxDepth);
				// System.out.println(distance);
				QuadNode nodeAtPoint = root.nodeWithPointMaxDepth(i, j,
						MIN_DEPTH);
				// QuadNode nodeAtPoint = root.deepestNodeWithPoint(i, j);
				if (nodeAtPoint != null)
					nodeAtPoint.setDepth(maxDepth);
				// nodeAtPoint.split();
			}
		}
		// QuadNode active = root.deepestNodeWithPoint(x, z);
		// if (active != null)
		// active.split();

		root.setActiveVertices();
		ibuf.flip();
	}

	private int depth(double x) {
		return (int) (Math.max(MIN_DEPTH, MAX_DEPTH * (1 - x)));
	}

	public void buildVBOs() {
		IntBuffer ib = BufferUtils.createIntBuffer(3);
		ARBBufferObject.glGenBuffersARB(ib);
		vHandle = ib.get(0);
		nHandle = ib.get(1);
		iHandle = ib.get(2);
	}

	public void draw(float x, float z) {
		update(x, z);

		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vbuf, GL_STATIC_DRAW_ARB);
		glVertexPointer(3, GL_FLOAT, 3 * SIZEOF_FLOAT, 0l);

		glBindBufferARB(GL_ARRAY_BUFFER_ARB, nHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, nbuf, GL_STATIC_DRAW_ARB);
		glNormalPointer(GL_FLOAT, 3 * SIZEOF_FLOAT, 0l);

		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, iHandle);
		glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, ibuf, GL_STATIC_DRAW_ARB);

		glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
		// glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

		glEnableClientState(GL_VERTEX_ARRAY);
		glEnableClientState(GL_NORMAL_ARRAY);

		glDrawElements(GL_TRIANGLES, index_count, GL_UNSIGNED_INT, 0L);

		glDisableClientState(GL_NORMAL_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
	}
}
