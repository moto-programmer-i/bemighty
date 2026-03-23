package lwjgl.ex.vulkan;



import org.lwjgl.system.MemoryStack;
import static org.lwjgl.vulkan.VK14.*;

import static lwjgl.ex.vulkan.StagingBufferSettings.*;

/**
 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
 * のUniformBufferObject
 */
public class UniformObject {
	// C++のように指定はできないので、ごまかす
//	alignas(16) glm::mat4 model;
//	alignas(16) glm::mat4 view;
//	alignas(16) glm::mat4 proj;
	public static final int MODEL_BYTES = Float.BYTES * 4;
	public static final int VIEW_BYTES = Float.BYTES * 4;
	public static final int PROJECTION_BYTES = Float.BYTES * 4;
	public static final int BYTES = MODEL_BYTES + VIEW_BYTES + PROJECTION_BYTES;
	
	private final float[] data = new float[BYTES];

	public UniformObject() {
		
	}

	public float[] getData() {
		return data;
	}
	
	public StagingBuffer createBuffer(MemoryStack stack, LogicalDevice logicalDevice) {
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var uniformBuffer = buffer.getFloatBuffer(0, BYTES);
			uniformBuffer.put(data);
		});
		settings.setSize(BYTES);
		settings.setUsage(VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT);
		settings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		settings.setUnMap(false);
		return new StagingBuffer(settings);
	}

}
