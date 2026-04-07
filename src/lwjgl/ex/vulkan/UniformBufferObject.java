package lwjgl.ex.vulkan;



import org.lwjgl.system.MemoryStack;

import motopgi.utils.FloatVector3;

import static org.lwjgl.vulkan.VK14.*;


import static lwjgl.ex.vulkan.StagingBufferSettings.*;

/**
 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
 * のUniformBufferObject
 */
public class UniformBufferObject implements AutoCloseable {
	// C++のように指定はできないので、ごまかす
//	alignas(16) glm::mat4 model;
//	alignas(16) glm::mat4 view;
//	alignas(16) glm::mat4 proj;
	
	// 3DCGの知識(MVP行列)
	// https://chaosplant.tech/do/vulkan/5-14/
	public static final int MATRIX4_NUMS = 4 * 4;
//	public static final int MATRIX4_BYTES = Float.BYTES * MATRIX4_NUMS;
//	public static final int MODEL_BYTES = MATRIX4_BYTES;
	
	public static final int VIEW_INDEX = MATRIX4_NUMS;
//	public static final int VIEW_BYTES = MATRIX4_BYTES;
	
	public static final int PROJECTION_INDEX = VIEW_INDEX + MATRIX4_NUMS;
//	public static final int PROJECTION_BYTES = MATRIX4_BYTES;

	public static final int LENGTH = PROJECTION_INDEX + MATRIX4_NUMS;
	public static final int BYTES = Float.BYTES * LENGTH;
	
	// 行列は、行が先になる
	public static final int MATRIX4_INDEX_00 = 0;
	public static final int MATRIX4_INDEX_10 = 1;
	public static final int MATRIX4_INDEX_20 = 2;
	public static final int MATRIX4_INDEX_30 = 3;
	public static final int MATRIX4_INDEX_01 = 4;
	public static final int MATRIX4_INDEX_11 = 5;
	public static final int MATRIX4_INDEX_21 = 6;
	public static final int MATRIX4_INDEX_31 = 7;
	public static final int MATRIX4_INDEX_02 = 8;
	public static final int MATRIX4_INDEX_12 = 9;
	public static final int MATRIX4_INDEX_22 = 10;
	public static final int MATRIX4_INDEX_32 = 11;
	public static final int MATRIX4_INDEX_03 = 12;
	public static final int MATRIX4_INDEX_13 = 13;
	public static final int MATRIX4_INDEX_23 = 14;
	public static final int MATRIX4_INDEX_33 = 15;

	private final float[] data = new float[LENGTH];
	
	// scaleだけ別でもっておかないと無理だった
	private double scale = 1.0;
	
	private StagingBuffer buffer;
	
	public UniformBufferObject(LogicalDevice logicalDevice) {
		modelToUnit();
		viewToUnit();
		projectionToUnit();
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var uniformBuffer = buffer.getFloatBuffer(0, data.length);
			uniformBuffer.put(data);
		});
		settings.setSize(BYTES);
		settings.setUsage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
		settings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		settings.setUnMap(false);
		buffer = new StagingBuffer(settings);
	}
	

	public float[] getData() {
		return data;
	}
	
	public void update() {
		buffer.update();
	}
	
	public void modelToUnit() {
		// 単位ベクトルにする
		for(int i = 0; i < MATRIX4_NUMS; ++i) {
			// 1 0 0 0
			// 0 1 0 0
			// 0 0 1 0
			// 0 0 0 1
			data[i] = i % 5 == 0 ? 1f : 0f;
		}
		scale = 1.0;
	}
	
	public void viewToUnit() {
		// 単位ベクトルにする
		for(int i = VIEW_INDEX; i < PROJECTION_INDEX; ++i) {
			// 1 0 0 0
			// 0 1 0 0
			// 0 0 1 0
			// 0 0 0 1
			data[i] = i % 5 == 0 ? 1f : 0f;
		}
	}
	
	public void projectionToUnit() {
		// 単位ベクトルにする
		for(int i = PROJECTION_INDEX; i < data.length; ++i) {
			// 1 0 0 0
			// 0 1 0 0
			// 0 0 1 0
			// 0 0 0 1
			data[i] = i % 5 == 0 ? 1f : 0f;
		}
	}
	
	public void scale(float s) {
		// 過去のscaleを捨てて拡大率を設定
		// （例：scale(2); scale(3); は6倍ではなく、3倍にする）
		var toNewScale = s / scale;
		
		// 拡大用の行列に設定
		// https://chaosplant.tech/do/vulkan/5-14/#kuo-da-suo-xiao
		// s 0 0 0
		// 0 s 0 0
		// 0 0 s 0
		// 0 0 0 1
		data[MATRIX4_INDEX_00] *= toNewScale;
		data[MATRIX4_INDEX_11] *= toNewScale;
		data[MATRIX4_INDEX_22] *= toNewScale;
		
		// 新しい拡大率を設定
		scale = s;
	}
	
	/**
	 * 回転を設定（過去の角度は捨てる）
	 * https://chaosplant.tech/do/vulkan/5-14/#hui-zhuan
	 * @param axis 回転軸のベクトル
	 * @param angle 回転（ラジアン）
	 */
	public void rotate(FloatVector3 axis, double angle) {
		// 回転軸を単位ベクトルに
		axis.normalize();
		
		// 暗黙の変換が何度も起きないようにdoubleにしておく
		double x = axis.getX();
		double y = axis.getY();
		double z = axis.getZ();
		
		// 回転の行列に設定
		// https://chaosplant.tech/do/vulkan/5-14/#hui-zhuan
		double sin = Math.sin(angle);
		double cos = Math.cos(angle);
		// 1 - cosθ
		double one_cos = 1 - cos; 
		// x(1 - cosθ)
		double xOne_cos = x * one_cos;
		// y(1 - cosθ)
		double yOne_cos = y * one_cos;
		// xy(1 - cosθ)
		double xyOne_cos = x * yOne_cos;
		// zx(1 - cosθ)
		double zxOne_cos = z * xOne_cos;
		// zy(1 - cosθ)
		double zyOne_cos = z * yOne_cos;
		// xsinθ
		double xsin = x * sin;
		// ysinθ
		double ysin = y * sin;
		// zsinθ
		double zsin = z * sin;
		
		// 回転にかかる部分だけは掛ける必要がある
		data[MATRIX4_INDEX_00] = (float) (scale * (x * xOne_cos + cos));
		data[MATRIX4_INDEX_10] = (float)(y * xOne_cos + zsin);
		data[MATRIX4_INDEX_20] = (float)(zxOne_cos - ysin);
		data[MATRIX4_INDEX_01] = (float)(xyOne_cos - zsin);
		data[MATRIX4_INDEX_11] = (float)(scale * (y * yOne_cos + cos));
		data[MATRIX4_INDEX_21] = (float)(zyOne_cos + xsin);
		data[MATRIX4_INDEX_02] = (float)(zxOne_cos + ysin);
		data[MATRIX4_INDEX_12] = (float)(zyOne_cos - xsin);
		data[MATRIX4_INDEX_22] = (float)(scale * (z * z * one_cos + cos));
	}
	
	public void move(float x, float y, float z) {
		// 平行移動の行列に設定
		// https://chaosplant.tech/do/vulkan/5-14/#ping-xing-yi-dong
		// 1 0 0 x
		// 0 1 0 y
		// 0 0 1 z
		// 0 0 0 1
		data[MATRIX4_INDEX_03] = x;
		data[MATRIX4_INDEX_13] = y;
		data[MATRIX4_INDEX_23] = z;
	}
	
	

	@Override
	public void close() throws Exception {
		if (buffer != null) {
			buffer.close();
			buffer = null;
		}
	}

	public StagingBuffer getBuffer() {
		return buffer;
	}
	
	public void setView(FloatVector3 camera, FloatVector3 direction) {
		// カメラの上を↑以外にするのは保留
		// 計算が合ってるか現状不明
		
		// 単位ベクトルにしておく
		direction.normalize();
		
		// カメラの向きと上は直交である必要があるので、作る
		// カメラの上は、画面の↑を0度とする
		// (x, y, z)を(xとz, y)の2次元として考える
		double dx = direction.getX();
		double dy = direction.getY();
		double dz = direction.getZ();
		var dxz2 = dx * dx + dz * dz;
		
		// (xとz, y)の2次元の直交ベクトル（計算省略のためyは2乗のまま）
		var uXz = -dy;
		var uY2 = dxz2;
		
		// dx : dz = ux : uz より
		// ux = (ux * uz) / dz
		// uz = (ux * uz) / dx
		// dx = 0, dz = 0のときは、ux = 0になることに注意
		double ux = dz != 0 ? uXz / dz : (dx != 0 ? uXz : 0);
		double uy = Math.sqrt(uY2);
		double uz = dx != 0 ? uXz / dx : uXz;
		
		System.out.println("up " + ux + " " + uy + " " + uz);
		
		
		
		// https://chaosplant.tech/do/vulkan/5-14/#biyuxing-lie-nozhi
		int i = VIEW_INDEX;
		double view00 = uz * dy - uy * dz;
		double view01 = ux * dz - uz * dx;
		double view02 = uy * dx - ux * dy;
//		System.out.println(view00);
//		System.out.println(view01);
//		System.out.println(view02);
		data[i++] = (float)view00;
		data[i++] = (float)-ux;
		data[i++] = (float)direction.getX();
		i++;
		
		data[i++] = (float)view01;
		data[i++] = (float)-uy;
		data[i++] = (float)dy;
		i++;
		
		data[i++] = (float)view02;
		data[i++] = (float)-uz;
		data[i++] = (float)dz;
		i++;
		
		data[i++] = (float)(view00 * -camera.getX() - view01 * camera.getY() - view02 * camera.getZ());
		data[i++] = (float)(ux * camera.getX() + uy * camera.getY() + uz * camera.getZ());
		data[i++] = (float)(dx * -camera.getX() - dy * camera.getY() - dz * camera.getZ());
		data[i++] = 1;
	}
	
	/**
	 * プロジェクション行列を設定
	 * kx:ky = ウィンドウ横 : 縦　でなければ歪む
	 * @param kx x方向の視野角の傾き
	 * @param ky y方向の視野角の傾き
	 * @param near 前方クリッピング面
	 * @param far 後方クリッピング面
	 */
	public void perspective(float kx, float ky, float near, float far) {
		// Z軸の変換
		// https://chaosplant.tech/do/vulkan/5-14/#zzhou-nobian-huan
		// https://chaosplant.tech/do/vulkan/5-14/#shi-zhuang
		
		// 計算が合ってるか現状不明
		
		var f_n = far - near;
		
		// N: near
		// F: far
		
		// kx 0  0        0
		// 0  ky 0        0
		// 0  0  F/(F-N)  -NF/(F-N)
		// 0  0  1        0
		int i = PROJECTION_INDEX;
		data[i] = kx;
		
		i += 5;
		data[i] = ky;
		
		i += 5;
		data[i] = far / f_n;
		
		data[++i] = 1;
		
		i += 3;
		data[i] = -(near * far / f_n);
	}

}
