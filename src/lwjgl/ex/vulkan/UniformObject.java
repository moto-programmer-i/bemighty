package lwjgl.ex.vulkan;



import org.lwjgl.system.MemoryStack;

import motopgi.utils.FloatVector3;

import static org.lwjgl.vulkan.VK14.*;


import static lwjgl.ex.vulkan.StagingBufferSettings.*;

/**
 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
 * のUniformBufferObject
 */
public class UniformObject implements AutoCloseable {
	// C++のように指定はできないので、ごまかす
//	alignas(16) glm::mat4 model;
//	alignas(16) glm::mat4 view;
//	alignas(16) glm::mat4 proj;
	
	// 3DCGの知識(MVP行列)
	// https://chaosplant.tech/do/vulkan/5-14/
	public static final int MATRIX4_NUMS = 4 * 4;
	public static final int MATRIX4_BYTES = Float.BYTES * MATRIX4_NUMS;
	public static final int MODEL_BYTES = MATRIX4_BYTES;
	
	public static final int VIEW_INDEX = MATRIX4_NUMS;
	public static final int VIEW_BYTES = MATRIX4_BYTES;
	
	public static final int PROJECTION_INDEX = VIEW_INDEX + MATRIX4_NUMS;
	public static final int PROJECTION_BYTES = MATRIX4_BYTES;
	public static final int BYTES = MODEL_BYTES + VIEW_BYTES + PROJECTION_BYTES;
	
	private final float[] data = new float[BYTES];
	
//	private final AIMatrix4x4.Buffer model = AIMatrix4x4.calloc(1);
//	private final AIMatrix4x4.Buffer view = AIMatrix4x4.calloc(1);
//	private final AIMatrix4x4.Buffer projection = AIMatrix4x4.calloc(1);

	private StagingBuffer buffer;
	
	public UniformObject(LogicalDevice logicalDevice) {
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var uniformBuffer = buffer.getFloatBuffer(0, BYTES);
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
	
	public void setView(FloatVector3 camera, FloatVector3 direction, FloatVector3 up) {
		// https://chaosplant.tech/do/vulkan/5-14/#biyuxing-lie-nozhi
		// 計算が合ってるか現状不明
		int i = VIEW_INDEX;
		float view00 = up.getZ() * direction.getY() - up.getY() * direction.getZ();
		float view01 = up.getX() * direction.getZ() - up.getZ() * direction.getX();
		float view02 = up.getY() * direction.getX() - up.getX() * direction.getY();
//		System.out.println(view00);
//		System.out.println(view01);
//		System.out.println(view02);
		data[i++] = view00;
		data[i++] = -up.getX();
		data[i++] = direction.getX();
		i++;
		
		data[i++] = view01;
		data[i++] = -up.getY();
		data[i++] = direction.getY();
		i++;
		
		data[i++] = view02;
		data[i++] = -up.getZ();
		data[i++] = direction.getZ();
		i++;
		
		data[i++] = view00 * -camera.getX() - view01 * camera.getY() - view02 * camera.getZ();
		
		data[i++] = up.getX() * camera.getX() + up.getY() * camera.getY() + up.getZ() * camera.getZ();
		data[i++] = direction.getX() * -camera.getX() - direction.getY() * camera.getY() - direction.getZ() * camera.getZ();
		data[i++] = 1;
	}

}
