package motopgi.utils;

public class FloatVector3 {
	private float x;
	private float y;
	private float z;
	
	public FloatVector3() {
	}
	public FloatVector3(float x, float y, float z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	public float getX() {
		return x;
	}
	public void setX(float x) {
		this.x = x;
	}
	public float getY() {
		return y;
	}
	public void setY(float y) {
		this.y = y;
	}
	public float getZ() {
		return z;
	}
	public void setZ(float z) {
		this.z = z;
	}
	
	/**
	 * 単位ベクトルにする
	 */
	public FloatVector3 normalize() {
		var scale = 1 / Math.sqrt(x * x + y * y + z * z);
		x *= scale;
		y *= scale;
		z *= scale;
		return this;
	}
	
	public FloatVector3 clone() {
		return new FloatVector3(x, y, z);
	}
	
	public FloatVector3 multiplies(float a) {
		x *= a;
		y *= a;
		z *= a;
		return this;
	}
	@Override
	public String toString() {
		return "FloatVector3 [x=" + x + ", y=" + y + ", z=" + z + "]";
	}
}
