import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.LinkedList;
import java.util.List;


public class TerrainLoader {
	public float [][] getHeights(File file) {
		int rows = 128;
		int cols = 128;
		byte [] buf = new byte[rows * cols];
		float [][] heights = new float[rows][cols];
		int len = Math.min(buf.length, (int) file.length());
		int bytes_received = 0;
		
		try {
			InputStream in = new BufferedInputStream(
								new FileInputStream(file));
			while (bytes_received < len)
				bytes_received += in.read(buf, bytes_received, len - bytes_received);
			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		for (int r = 0; r < rows; r++) 
			for (int c = 0; c < cols; c++)
				heights[r][c] = (float) ((int) buf[r * cols + c] & 0xFF) / 10f;
		
		return heights;
	}
}