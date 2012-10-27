import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.lwjgl.util.vector.Vector3f;

public class Terrain {
	private int w;
	private int h;
	private float[][] hs;
	private Vector3f[][] ns;
	private boolean loadFailed = false;

	/**
	 * Vytvoří objekt třídy Terrain.
	 * 
	 * @param file          soubor s mapou
	 * @param w             šířka mapy, musí být známa předem
	 * @param h             výška mapy, musí být známa předem
	 */
	public Terrain(File file, int w, int h) {
		this.w = w;
		this.h = h;

		hs = getHeights(file);
		if (loadFailed)
			return;

		ns = new Vector3f[w][];
		for (int i = 0; i < w; i++)
			ns[i] = new Vector3f[h];

		computeNormals();
	}

	/**
	 * Test jestli se terén načetl správně.
	 * 
	 * @return true, pokud ano.
	 */
	public boolean loadFailed() {
		return loadFailed;
	}

	/**
	 * Získá počet indexů na šířku.
	 * 
	 * @return w
	 */
	public int getW() {
		return w;
	}

	/**
	 * Získá počet indexů na výšku.
	 * 
	 * @return h
	 */
	public int getH() {
		return h;
	}

	/**
	 * Získá y-souřadnici.
	 * 
	 * @param x             šířkový index
	 * @param z             výškový index
	 * @return Výšku v bodě (x, z).
	 */
	public float getHeight(int x, int z) {
		return hs[z][x];
	}

	/**
	 * Získá normálu.
	 * 
	 * @param x             šířkový index
	 * @param z             výškový index
	 * @return Normálu v bodě (x, z).
	 */
	public Vector3f getNormal(int x, int z) {
		return ns[z][x];
	}

	/**
	 * Načte y-souřadnice ze souboru.
	 * 
	 * @param file             soubor s výškovou mapou terénu.
	 * @return Parsovanou výškovou mapu.
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
			for (int c = 0; c < cols; c++)
				heights[r][c] = (float) ((int) buf[r * cols + c] & 0xFF) / 10f;

		return heights;
	}

	/**
	 * Vypočte normály pro každý bod terénu.
	 */
	private void computeNormals() {
		for (int x = 0; x < w; x++)
			for (int z = 0; z < h; z++) {
				Vector3f sum = new Vector3f(0, 0, 0);
				Vector3f top, bottom, left, right;
				top = bottom = left = right = null;

				if (z > 0)
					top = new Vector3f(0f, hs[z - 1][x] - hs[z][x], -1f);
				if (z < h - 1)
					bottom = new Vector3f(0f, hs[z + 1][x] - hs[z][x], 1f);
				if (x > 0)
					left = new Vector3f(-1f, hs[z][x - 1] - hs[z][x], 0f);
				if (x < w - 1)
					right = new Vector3f(1f, hs[z][x + 1] - hs[z][x], 0f);

				if (x > 0 && z > 0)
					Vector3f.add(Vector3f.cross(top, left, null), sum, sum);
				if (x > 0 && z < h - 1)
					Vector3f.add(Vector3f.cross(left, bottom, null), sum, sum);
				if (x < w - 1 && z < h - 1)
					Vector3f.add(Vector3f.cross(bottom, right, null), sum, sum);
				if (x < w - 1 && z > 0)
					Vector3f.add(Vector3f.cross(right, top, null), sum, sum);

				// add normalisation
				ns[z][x] = sum.normalise(sum);
			}
	}
}