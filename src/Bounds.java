import org.lwjgl.util.vector.Vector3f;

public class Bounds {
	private float xlo, xhi;
	private float ylo, yhi;
	private float zlo, zhi;
	
	public Bounds(Vector3f v) {
		xlo = xhi = v.x;
		ylo = yhi = v.y;
		zlo = zhi = v.z;
	}

	public void update(Vector3f v) {
		if (v.x > xhi)
			xhi = v.x;
		if (v.x < xlo)
			xlo = v.x;

		if (v.y > yhi)
			yhi = v.y;
		if (v.y < ylo)
			ylo = v.y;
		
		if (v.z > zhi)
			zhi = v.z;
		if (v.z < zlo)
			zlo = v.z;
	}
	
	public void update(Bounds b) {
		if (b.xhi > xhi)
			xhi = b.xhi;
		if (b.xlo < xlo)
			xlo = b.xlo;

		if (b.yhi > yhi)
			yhi = b.yhi;
		if (b.ylo < ylo)
			ylo = b.ylo;
		
		if (b.yhi > yhi)
			yhi = b.yhi;
		if (b.ylo < ylo)
			ylo = b.ylo;
	}

	public boolean contains(float x, float z) {
		return (xlo <= x && x <= xhi) && (zlo <= z && z <= zhi);
	}
	
	private float dot(float[] a, float[] b) {
		return a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
	}
	
	public boolean isInView() {
		float w = 1;
		// TODO precompute points
		float[][] points = {{xlo, ylo, zlo, w}, 
							{xlo, ylo, zhi, w}, 
							{xlo, yhi, zlo, w},
							{xlo, yhi, zhi, w},
							{xhi, ylo, zlo, w},
							{xhi, ylo, zhi, w},
							{xhi, yhi, zlo, w},
							{xhi, yhi, zhi, w}};
		
		
			
		float[][] clipPlanes = Main.clipPlanes;
		
		planes:
		for (int i = 0; i < clipPlanes.length; i++) {
			for (int j = 0; j < points.length; j++)
				if (dot(clipPlanes[i], points[j]) > 0)
					continue planes;
			return false;
		}
		
		return true;
	}
}
