package bemighty;

import java.nio.LongBuffer;

import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.StagingBuffer;
import lwjgl.ex.vulkan.StagingBufferSettings;
import motopgi.utils.FloatVector2;

// https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html
// を試す用。内部は大幅に修正
public class ParticleTest implements AutoCloseable {
	public static final int PARTICLE_COUNT = 1;
	// Particle(position + velocity)
	private FloatVector2 position = new FloatVector2();
	private float velocity = 1.0f;
	
	/**
	 *  Particleのサイズ
	 */
	private static final int BUFFER_SIZE = FloatVector2.SIZE + Float.BYTES;
	
	private LogicalDevice logicalDevice;
	
	private final StagingBuffer buffer;

	public ParticleTest(LogicalDevice logicalDevice) throws Exception {
		this.logicalDevice = logicalDevice;
		var bufferSettings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			// Particleの内容を送信
			var particleBuffer = buffer.getFloatBuffer(0, BUFFER_SIZE);
			position.sendTo(particleBuffer);
			particleBuffer.put(velocity);
		});
		bufferSettings.setSize(BUFFER_SIZE);
		bufferSettings.setUsage(StagingBufferSettings.USAGE_SHADER_STORAGE);
		bufferSettings.setDestinationMemoryPropertyFlags(StagingBufferSettings.MEMORY_PROPERTY_FLAGS_DESTINATION);
		
		
		buffer = new StagingBuffer(bufferSettings);
	}

	@Override
	public void close() throws Exception {
		buffer.close();
	}
	
	public LongBuffer getForParticle() {
		return buffer.getForHandler();
	}
}
