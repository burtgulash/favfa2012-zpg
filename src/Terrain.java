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
	private boolean loadFailed = true;
	private float[][] hs;
	public QuadNode root;
	public int rootNodeSize;

	private FloatBuffer vbuf, nbuf;
	private IntBuffer ibuf;
	private int index_count;
	private int vHandle, nHandle, iHandle;

	private final float METRES_PER_FLOAT = 2f;
	private final int SUBDIVISION_LVL = 1;
	private final int SIZEOF_FLOAT = 1 << 2;
	public final int MAX_DEPTH = SUBDIVISION_LVL; // TODO correct or +- 1?

	public Terrain(File file, int w, int h) {
		width = w;
		height = h;

		getHeights(file);
		if (loadFailed())
			return;

		int n = nSubdivisions(SUBDIVISION_LVL);
		createBuffers(n * n * (width + 1) * (height + 1));
		subdivide(n);
		
		rootNodeSize = n * width;
		root = new QuadNode(NodeType.ROOT, 0, rootNodeSize, 0, null, this);

		vbuf.flip();
		nbuf.flip();
	}

	public void buildVBOs() {
		IntBuffer ib = BufferUtils.createIntBuffer(3);
		ARBBufferObject.glGenBuffersARB(ib);
		vHandle = ib.get(0);
		nHandle = ib.get(1);
		iHandle = ib.get(2);
	}

	public float getY(float x, float z) {
		return getH((int) x, (int) z);
	}

	private void createBuffers(int nVertices) {
		vbuf = BufferUtils.createFloatBuffer(3 * SIZEOF_FLOAT * nVertices);
		nbuf = BufferUtils.createFloatBuffer(3 * SIZEOF_FLOAT * nVertices);
		ibuf = BufferUtils.createIntBuffer(nVertices);
	}

	private void subdivide(int n) {
		// + 1 ---> 129 x 129
		for (int i = 0; i < width + 1; i++) {
			for (int j = 0; j < height + 1; j++) {
				for (int xi = 0; xi < n; xi++)
					for (int zi = 0; zi < n; zi++) {
						float x = (float) i + (float) xi / (float) n;
						float z = (float) j + (float) zi / (float) n;
						float y = getY(x, z);

						float dx = (getH(xi + 1, zi) - getH(xi - 1, zi)) / 2f;
						float dz = (getH(xi, zi + 1) - getH(xi, zi - 1)) / 2f;

						Vector3f normal = Vector3f.cross(
								new Vector3f(0, dz, 1), new Vector3f(1, dx, 0),
								null);
						normal.normalise();

						vbuf.put(x).put(y).put(z);
						nbuf.put(normal.x).put(normal.y).put(normal.z);
					}
			}
		}
	}

	private float getH(int xi, int zi) {
		xi = Math.max(0, Math.min(xi, width - 1));
		zi = Math.max(0, Math.min(zi, height - 1));

		return hs[xi][zi];
	}

	private int nSubdivisions(int lvl) {
		int n = 1;
		while (--lvl > 0)
			n += n;
		return n;
	}

	private boolean loadFailed() {
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
	
	public void update() {
		index_count = 0;
		root.setActiveVertices();
		ibuf.flip();
	}

	public void draw() {
		update();

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

		glDrawElements(GL_TRIANGLES, index_count, GL_UNSIGNED_INT, 0L);

//		glBindBufferARB(GL_ARRAY_BUFFER_ARB, 0);
//		glBindBufferARB(GL_ELEMENT_ARRAY_BUFFER_ARB, 0);

		glDisableClientState(GL_NORMAL_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
	}
}
