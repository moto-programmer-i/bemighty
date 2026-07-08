package lwjgl.ex.vulkan;

import java.nio.FloatBuffer;

import motopgi.utils.FloatVector3;
import static lwjgl.ex.vulkan.VulkanConstants.DUMMY;

/**
 * 回転のパラメータをまとめたクラス
 */
public class Rotation {
	// 下の変数の個数（現状、数えて対応させるしかない）
	public static final int FLOAT_LENGTH = 8 + 1;// なぜかずれるのでダミー分を追加
	
	// https://techblog.sega.jp/entry/2021/06/15/100000 pdf 130ページ目
	// ロドリゲスの回転公式 https://w3e.kanazawa-it.ac.jp/math/physics/category/physical_math/linear_algebra/henkan-tex.cgi?target=/math/physics/category/physical_math/linear_algebra/rodrigues_rotation_formula.html
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	// を計算するためにGPUに送る変数
	
	// private float theta;
	private FloatVector3 axis;
	private float cos;
	private float sin;
	// (1 - cosθ)回転
	private FloatVector3 oneCosAxis;

	public Rotation(float theta, FloatVector3 axis) {
//		this.theta = theta;
		this.axis = axis.normalize();
		cos = (float)Math.cos(theta);
		sin = (float)Math.sin(theta);
		oneCosAxis = axis.clone().multiplies(1 - cos);
	}

	public void write(FloatBuffer buffer) {
		buffer.put(axis.getX());
		buffer.put(axis.getY());
		buffer.put(axis.getZ());
		
		// バグ？なのか知らないが、なぜかfloat3がずれる
		// どうしようもないので非常に嫌だが、ダミーをいれて対処する
		// 本来はLWJGLに報告するべきだが、面倒なので保留
		buffer.put(DUMMY);
		
		buffer.put(oneCosAxis.getX());
		buffer.put(oneCosAxis.getY());
		buffer.put(oneCosAxis.getZ());
		
		buffer.put(cos);
		buffer.put(sin);
		
		/* デバッグ用
		buffer.put(666);//
		*/ 
		
		// 謎の空間が発生する。こう対処するべきではないが、他の方法がない
		buffer.put(DUMMY);
		buffer.put(DUMMY);
		buffer.put(DUMMY);
	}
}
