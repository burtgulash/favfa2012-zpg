import static org.lwjgl.opengl.GL11.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
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
	private boolean useTexture;
	private float[][] hs;
	public QuadNode root;
	public int rootNodeSize;

	private FloatBuffer vbuf, nbuf, tbuf;
	private IntBuffer ibuf;
	private int index_count;
	private int vHandle, nHandle, iHandle, tHandle;

	private final int SUBDIVISION_LVL = 4;
	public final boolean CULLING_ENABLED = true;
	public final int MIN_DEPTH = 6;
	public final int MAX_DEPTH = 12;
	
	private final float TEXTURE_MAGNIFICATION = 6f;
	private final float METRES_PER_FLOAT = 2f;
	private final int SIZEOF_FLOAT = 4;
	public final int ROOT_DEPTH = 1;

	public Terrain(File file, int w, int h) throws IOException {
		width = w;
		height = h;

		getHeights(file);

		scaleFactor = nSubdivisions(SUBDIVISION_LVL);
		nVertices = scaleFactor * scaleFactor * (width + 1) * (height + 1);
		createBuffers();
		subdivide(scaleFactor);

		rootNodeSize = scaleFactor * width;
		root = new QuadNode(NodeType.ROOT, ROOT_DEPTH, rootNodeSize, 0, null,
				this);

		vbuf.flip();
		nbuf.flip();
		tbuf.flip();

	}

	public Vector3f getV(int i) {
		i *= 3;
		return new Vector3f(vbuf.get(i), vbuf.get(i + 1), vbuf.get(i + 2));
	}

	public boolean useTexture() {
		return useTexture;
	}

	private void createBuffers() {
		vbuf = BufferUtils.createFloatBuffer(3 * nVertices);
		nbuf = BufferUtils.createFloatBuffer(3 * nVertices);
		tbuf = BufferUtils.createFloatBuffer(2 * nVertices);
		ibuf = BufferUtils.createIntBuffer(12 * nVertices);
	}

	private void subdivide(int n) {
		float delta = 1f / (float) n;

		for (int i = 0; i < width; i++) {
			for (int xi = 0; xi < n; xi++) {
				float x = (float) i + ((float) xi / (float) n);
				for (int j = 0; j < height; j++) {
					for (int zi = 0; zi < n; zi++) {
						float z = (float) j + ((float) zi / (float) n);
						float y = getY(x, z);

						addVertexNormalTexture(x, y, z, delta);
					}
				}

				addVertexNormalTexture(x, getY(x, height), height, delta);
			}
		}

		for (int j = 0; j < height + 1; j++) {
			for (int zi = 0; zi < n; zi++) {
				float z = (float) j + ((float) zi / (float) n);
				addVertexNormalTexture(width, getY(width, z), z, delta);
			}
		}
	}

	private void addVertexNormalTexture(float x, float y, float z, float delta) {
		float dx = (getY(x + delta, z) - getY(x - delta, z)) / (2 * delta);
		float dz = (getY(x, z + delta) - getY(x, z - delta)) / (2 * delta);

//		same
//		Vector3f normal = Vector3f.cross(new Vector3f(0, dz, 1), new Vector3f( 1, dx, 0), null);
		Vector3f normal = new Vector3f(-dx, 1, -dz);
		normal.normalise();

		tbuf.put(x / TEXTURE_MAGNIFICATION).put(z / TEXTURE_MAGNIFICATION);
		vbuf.put(x).put(y).put(z);
		nbuf.put(normal.x).put(normal.y).put(normal.z);
	}

	public float getY(float x, float z) {
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

	private void getHeights(File file) throws IOException {
		byte[] buf = new byte[width * height];
		hs = new float[width][height];

		int len = Math.min(buf.length, (int) file.length());
		int bytes_received = 0;

		InputStream in = new BufferedInputStream(new FileInputStream(file));
		while (bytes_received < len)
			bytes_received += in
					.read(buf, bytes_received, len - bytes_received);

		in.close();

		for (int x = 0; x < width; x++)
			for (int z = 0; z < height; z++) {
				hs[x][z] = (float) ((int) buf[x * height + z] & 0xFF) / 10f;
				hs[x][z] /= METRES_PER_FLOAT;
			}
	}

	public void pushIndex(int i) {
		ibuf.put(i);
		index_count++;
	}

	public void update(float x, float z) {
		System.out.printf("drawing %d vertices%n", index_count);
		
		index_count = 0;
		ibuf.clear();

		root.merge();
		root.setDepth(MIN_DEPTH);

		QuadNode xnode = root.nodeWithPointMaxDepth(0, 0, MIN_DEPTH);
		QuadNode znode;
		while (xnode != null) {
			znode = xnode;
			while (znode != null) {
				Vector3f point = getV(znode.position);
				float dx = x - point.x;
				float dz = z - point.z;
				double distance = (dx * dx + dz * dz) / (width * width / 16);

				int maxDepth = depth(distance);
				znode.setDepth(maxDepth);

				znode = znode.neighborBottom;
			}
			xnode = xnode.neighborRight;
		}

		QuadNode active = root.deepestNodeWithPoint(x, z);
		active.split();

		root.setActiveVertices();
		ibuf.flip();
	}

	private int depth(double x) {
		return (int) (Math.max(MIN_DEPTH, MAX_DEPTH * (1 - x)));
	}

	public void buildVBOs() {
		IntBuffer ib = BufferUtils.createIntBuffer(4);
		ARBBufferObject.glGenBuffersARB(ib);
		vHandle = ib.get(0);
		nHandle = ib.get(1);
		iHandle = ib.get(2);
		tHandle = ib.get(3);

		// Bind vertex buffer
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, vHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, vbuf, GL_STATIC_DRAW_ARB);
		glVertexPointer(3, GL_FLOAT, 3 * SIZEOF_FLOAT, 0l);

		// Bind normal buffer
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, nHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, nbuf, GL_STATIC_DRAW_ARB);
		glNormalPointer(GL_FLOAT, 3 * SIZEOF_FLOAT, 0l);

		// Bind texture buffer
		glBindBufferARB(GL_ARRAY_BUFFER_ARB, tHandle);
		glBufferDataARB(GL_ARRAY_BUFFER_ARB, tbuf, GL_STATIC_DRAW_ARB);
		glTexCoordPointer(2, GL_FLOAT, 2 * SIZEOF_FLOAT, 0l);

		glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
	}

	public void loadTexture(String fileName) throws IOException {
		try {
			TextureLoader.loadTexture(fileName);
			useTexture = true;
		} catch (IOException e) {
			useTexture = false;
		}
	}

	public void draw(float x, float z) {
		update(x, z);

		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, iHandle);
		glBufferDataARB(GL_ELEMENT_ARRAY_BUFFER_ARB, ibuf, GL_STATIC_DRAW_ARB);

		glDrawElements(GL_TRIANGLES, index_count, GL_UNSIGNED_INT, 0L);

	}
}
