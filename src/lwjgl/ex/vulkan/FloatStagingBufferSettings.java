package lwjgl.ex.vulkan;

import org.lwjgl.PointerBuffer;

public class FloatStagingBufferSettings extends StagingBufferSettings {
	private float[] data;
	public FloatStagingBufferSettings(LogicalDevice logicalDevice, float... data) {
		super(logicalDevice);
		this.data = data;
		setSize(data.length * Float.BYTES);
	}
	
	
	public void copy(PointerBuffer buffer) {
		var floatBuffer = buffer.getFloatBuffer(0, (int)getSize());
		floatBuffer.put(data);
	}
}
