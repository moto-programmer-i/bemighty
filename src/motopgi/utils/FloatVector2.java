package motopgi.utils;

import java.nio.FloatBuffer;

public class FloatVector2 {
	/**
	 * バッファに送る場合のサイズ
	 */
	public static final int SIZE = Float.BYTES * 2;
	private float x;
	private float y;
	
	public FloatVector2() {
	}
	public FloatVector2(float x, float y) {
		this.x = x;
		this.y = y;
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
	@Override
	public String toString() {
		return "FloatVector2 [x=" + x + ", y=" + y + "]";
	}
	
	/**
	 * FloatBuffer.put(this)
	 * @param buffer
	 */
	public void sendTo(FloatBuffer buffer) {
		buffer.put(x);
		buffer.put(y);
	}
}
