package bemighty;

import java.nio.LongBuffer;

import lwjgl.ex.vulkan.BufferType;
import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.StagingBuffer;
import lwjgl.ex.vulkan.StagingBufferSettings;
import lwjgl.ex.vulkan.VertexBindingBuilder;
import motopgi.utils.FloatVector2;

// https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html
// を試す用。内部は大幅に修正
public class ParticleTest implements AutoCloseable {
	public static final int PARTICLE_COUNT = 1;
	
	private Particle particle = new Particle();
	
	private LogicalDevice logicalDevice;
	
	private final StagingBuffer buffer;
	
	private VertexBindingBuilder binding;

	public ParticleTest(LogicalDevice logicalDevice) throws Exception {
		this.logicalDevice = logicalDevice;
		binding = particle.createBinding();
		var bufferSettings = new StagingBufferSettings(logicalDevice, binding.createCopy());
		
		// 本来は、Partcleのサイズ * 数を設定する
		// ここでは（数が1なのでサイズだけでよい）
		bufferSettings.setSize(binding.getAllBytes());
		bufferSettings.setType(BufferType.STORAGE);
		
		// チュートリアル版
		// bufferSettings.setDestinationMemoryPropertyFlags(StagingBufferSettings.MEMORY_PROPERTY_FLAGS_DESTINATION);
		
		// 遅いらしいが、一旦確認用
		bufferSettings.setDestinationMemoryPropertyFlags(StagingBufferSettings.MEMORY_PROPERTY_FLAGS_VISIBLE);
		
		
		buffer = new StagingBuffer(bufferSettings);
	}

	@Override
	public void close() throws Exception {
		buffer.close();
	}
	
	public LongBuffer getForParticle() {
		return buffer.getForHandler();
	}

	public StagingBuffer getBuffer() {
		return buffer;
	}

	public VertexBindingBuilder getBinding() {
		return binding;
	}

	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
}
