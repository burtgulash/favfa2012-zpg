public class Bounds {
	private float xlo, xhi;
	private float zlo, zhi;
	
	public Bounds(float[] topLeft, float[] bottomRight) {
		xlo = bottomRight[0];
		xhi = topLeft[0];
		zlo = topLeft[2];
		zhi = bottomRight[2];
		if (xlo > xhi) {
			float tmp = xlo;
			xlo = xhi;
			xhi = tmp;
		}
		if (zlo > zhi) {
			float tmp = zlo;
			zlo = zhi;
			zhi = tmp;
		}
	}
	
	public boolean contains(float x, float z) {
		return (xlo <= x && x <= xhi) && (zlo <= z && z <= zhi);
	}
}
