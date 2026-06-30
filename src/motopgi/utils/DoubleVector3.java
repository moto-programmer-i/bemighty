package motopgi.utils;

public class DoubleVector3 {
	/**
	 * 要素数
	 */
	public static final int LENGTH = 3;
	private double x;
	private double y;
	private double z;
	
	public DoubleVector3() {
	}
	public DoubleVector3(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}
	public double getZ() {
		return z;
	}
	public void setZ(double z) {
		this.z = z;
	}
	public double calcLength2() {
		return x * x + y * y + z * z;
	}
	
	public double calcLength() {
		// 綺麗さならこうだが、速度を気にして汚く書く
//		return Math.sqrt(calcLength2());
		return Math.sqrt(x * x + y * y + z * z);
	}
	
	public DoubleVector3 normalize() {
		// 綺麗さならこうだが、速度を気にして汚く書く
//		var length = calcLength();
		var length = Math.sqrt(x * x + y * y + z * z);
		x /= length;
		y /= length;
		z /= length;
		return this;
	}
}
