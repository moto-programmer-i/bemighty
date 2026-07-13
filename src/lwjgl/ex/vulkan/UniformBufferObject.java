package lwjgl.ex.vulkan;
// 参考
// https://chaosplant.tech/do/vulkan/5-14/


import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.DoubleVector3;
import motopgi.utils.FloatVector3;

import static org.lwjgl.vulkan.VK14.*;


import static lwjgl.ex.vulkan.StagingBufferSettings.*;
import static lwjgl.ex.vulkan.VulkanConstants.DUMMY;

/**
 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
 * のUniformBufferObject
 */
public class UniformBufferObject implements AutoCloseable {	
	// -------------.slang側と対応しなければならない↓------------
	// 平行移動 （yは上がマイナス）
	private FloatVector3 translate = new FloatVector3(0f, -0.4f, 0f);
	
	private FloatVector3 cameraPosition = new FloatVector3(0f, 0f, 0f);
	
	private Rotation local = new Rotation(
//			0f
			(float)(-Math.PI / 6)
			,
			
			new FloatVector3(1, 0, 0));
	
	// カメラ座標系参考
	// https://mem-archive.com/2018/02/17/post-74/
	// 計算上、カメラの向きの逆回転が必要なので
	// ここには最初から逆をいれておく
	private Rotation camera = new Rotation(
			0f
//			(float)(-Math.PI * 1 / 12)
			,
			
			new FloatVector3(0, 1, 0));
	
	private float scale = 1.4f;
	
	// -------------.slang側と対応しなければならない↑------------
	
	// 上の変数の個数（現状、数えて対応させるしかない）
	private static final int LENGTH = Rotation.FLOAT_LENGTH * 2 + 4 + 1 + 10; // なぜかずれるのでダミー分を追加
	private static final int BYTES = Float.BYTES * LENGTH;
	
	
	private StagingBufferSettings settings;

	
	
	
	
	// proj
	// 最適な初期値は不明
	public static final double DEFAULT_KX = 2;
	public static final double DEFAULT_KY = 2;
	public static final double DEFAULT_NEAR = -1;
	public static final double DEFAULT_FAR = 100;
	private static double kx = DEFAULT_KX;
	private static double ky = DEFAULT_KY;
	private static double near = DEFAULT_NEAR;
	private static double far = DEFAULT_FAR;
	
	
	private StagingBuffer buffer;
	
	private FloatBuffer uniformBuffer = null;
	
	public UniformBufferObject(LogicalDevice logicalDevice) {		
		settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			// バッファの初期化
			if (uniformBuffer == null) {
				uniformBuffer = buffer.getFloatBuffer(0, LENGTH);
			} else {
				uniformBuffer.clear();
			}
			
			// 先にfloat3をいれないとなぜか0になる
			
			// モデルの位置とカメラの位置を相殺
			uniformBuffer.put(translate.getX() - cameraPosition.getX());
			uniformBuffer.put(translate.getY() - cameraPosition.getY());
			uniformBuffer.put(translate.getZ() - cameraPosition.getZ());
			// バグ？なのか知らないが、なぜかfloat3がずれる
			// どうしようもないので非常に嫌だが、ダミーをいれて対処する
			// 本来はLWJGLに報告するべきだが、面倒なので保留
			uniformBuffer.put(DUMMY);
			
			local.write(uniformBuffer);
			camera.write(uniformBuffer);
			
			uniformBuffer.put(scale);
			
			/* デバッグ用
			uniformBuffer.put(666);// 
			*/
		});
		settings.setSize(BYTES);
		
		
		settings.setType(BufferType.UNIFORM);
		
		settings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		settings.setUnMap(false);
		
		buffer = new StagingBuffer(settings);
	}
	
	public void update() {
		buffer.update();
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
//		// カメラの上を↑以外にするのは保留
//		// 計算が合ってるか現状不明
//		
//		// 単位ベクトルにしておく
//		direction.normalize();
//		
//		// カメラの向きと上は直交である必要があるので、作る
//		// カメラの上は、画面の↑を0度とする
//		// (x, y, z)を(xとz, y)の2次元として考える
//		double dx = direction.getX();
//		double dy = direction.getY();
//		double dz = direction.getZ();
//		var dxz2 = dx * dx + dz * dz;
//		
//		// (xとz, y)の2次元の直交ベクトル（計算省略のためyは2乗のまま）
//		var uXz = -dy;
//		var uY2 = dxz2;
//		
//		// dx : dz = ux : uz より
//		// ux = (ux * uz) / dz
//		// uz = (ux * uz) / dx
//		// dx = 0, dz = 0のときは、ux = 0になることに注意
//		double ux = dz != 0 ? uXz / dz : (dx != 0 ? uXz : 0);
//		double uy = Math.sqrt(uY2);
//		double uz = dx != 0 ? uXz / dx : uXz;		
//		
//		// https://chaosplant.tech/do/vulkan/5-14/#biyuxing-lie-nozhi
//		int i = VIEW_INDEX;
//		double view00 = uz * dy - uy * dz;
//		double view01 = ux * dz - uz * dx;
//		double view02 = uy * dx - ux * dy;
////		System.out.println(view00);
////		System.out.println(view01);
////		System.out.println(view02);
//		data[i++] = (float)view00;
//		data[i++] = (float)-ux;
//		data[i++] = (float)direction.getX();
//		i++;
//		
//		data[i++] = (float)view01;
//		data[i++] = (float)-uy;
//		data[i++] = (float)dy;
//		i++;
//		
//		data[i++] = (float)view02;
//		data[i++] = (float)-uz;
//		data[i++] = (float)dz;
//		i++;
//		
//		data[i++] = (float)(view00 * -camera.getX() - view01 * camera.getY() - view02 * camera.getZ());
//		data[i++] = (float)(ux * camera.getX() + uy * camera.getY() + uz * camera.getZ());
//		data[i++] = (float)(dx * -camera.getX() - dy * camera.getY() - dz * camera.getZ());
//		data[i++] = 1;
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
//		// Z軸の変換
//		// https://chaosplant.tech/do/vulkan/5-14/#zzhou-nobian-huan
//		// https://chaosplant.tech/do/vulkan/5-14/#shi-zhuang
//		
//		// 計算が合ってるか現状不明
//		
//		var f_n = far - near;
//		
//		// N: near
//		// F: far
//		
//		// kx 0  0        0
//		// 0  ky 0        0
//		// 0  0  F/(F-N)  -NF/(F-N)
//		// 0  0  1        0
//		int i = PROJECTION_INDEX;
//		data[i] = kx;
//		
//		i += 5;
//		data[i] = ky;
//		
//		i += 5;
//		data[i] = far / f_n;
//		
//		data[++i] = 1;
//		
//		i += 3;
//		data[i] = -(near * far / f_n);
	}	
}
