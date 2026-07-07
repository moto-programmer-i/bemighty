package lwjgl.ex.vulkan;

import java.nio.FloatBuffer;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.DoubleVector3;
import motopgi.utils.FloatVector3;

import static org.lwjgl.vulkan.VK14.*;

import java.util.Stack;

import static lwjgl.ex.vulkan.StagingBufferSettings.*;

/**
 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
 * のUniformBufferObject
 */
public class UniformBufferObject implements AutoCloseable {
	private double theta = (float)(
//			0
//			Math.PI / 2
			Math.PI * 2 / 12
//			Math.PI / 2
			
			);
	
	// -------------.slang側と対応しなければならない↓------------
	// https://techblog.sega.jp/entry/2021/06/15/100000 pdf 130ページ目
	// ロドリゲスの回転公式 https://w3e.kanazawa-it.ac.jp/math/physics/category/physical_math/linear_algebra/henkan-tex.cgi?target=/math/physics/category/physical_math/linear_algebra/rodrigues_rotation_formula.html
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	// を計算するためにGPUに送る変数
	private FloatVector3 rotateAxis = new FloatVector3(0, 1, 1).normalize();
	// private float cos = (float)Math.cos(theta);
	private float cos = (float)Math.cos(theta);
	private float sin = (float)Math.sin(theta);
	// (1 - cosθ)回転
	private FloatVector3 oneCosRotateAxis = rotateAxis.clone().multiplies(1 - cos);
	private float scale = 0.8f;
	// -------------.slang側と対応しなければならない↑------------
	
	// 上の変数の個数（現状、数えて対応させるしかない）
	private static final int LENGTH = 9 + 1; // なぜかずれるのでダミー分を追加
	private static final int BYTES = Float.BYTES * LENGTH;
	
	
	
	
	
	private StagingBufferSettings settings;

	
	
	// 行列計算の参考
	// https://chaosplant.tech/do/vulkan/5-14/
	
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
	
	// view
	private static DoubleVector3 camera = new DoubleVector3();
	private static DoubleVector3 direction;
	
	// model（モデルごとに異なるためstaticではない）
	
	private DoubleVector3 translation;
	
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
			
			uniformBuffer.put(rotateAxis.getX());
			uniformBuffer.put(rotateAxis.getY());
			uniformBuffer.put(rotateAxis.getZ());
			
			// バグ？なのか知らないが、なぜかoneCosRotateAxisがずれる
			// どうしようもないので非常に嫌だが、ダミーをいれて対処する
			// 本来はLWJGLに報告するべきだが、面倒なので保留
			uniformBuffer.put(0);
			
			uniformBuffer.put(oneCosRotateAxis.getX());
			uniformBuffer.put(oneCosRotateAxis.getY());
			uniformBuffer.put(oneCosRotateAxis.getZ());
			
			uniformBuffer.put(cos);
			uniformBuffer.put(sin);
			
			uniformBuffer.put(scale);
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
