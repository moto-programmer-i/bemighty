package bemighty;

import java.nio.LongBuffer;

import static org.lwjgl.vulkan.VK14.*;

import lwjgl.ex.vulkan.BufferType;
import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.PipelineSettings;
import lwjgl.ex.vulkan.StagingBuffer;
import lwjgl.ex.vulkan.StagingBufferSettings;
import lwjgl.ex.vulkan.VertexBindingBuilder;
import motopgi.utils.FloatVector2;

// https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html
// を試す用。内部は大幅に修正
public class ParticleTest implements AutoCloseable {
	public static final int PARTICLE_COUNT = 2;
	
	private Particle[] particles = Particle.createArray(PARTICLE_COUNT);
	
	private LogicalDevice logicalDevice;
	
	private final StagingBuffer buffer;
	
	private VertexBindingBuilder binding;

	public ParticleTest(LogicalDevice logicalDevice) throws Exception {
		this.logicalDevice = logicalDevice;
		binding = particles[0].createBinding();
		var bufferSettings = new StagingBufferSettings(logicalDevice, binding.createCopy());
		
		// Partcleのサイズ * 数
		bufferSettings.setSize(binding.getAllBytes() * particles.length);
		
		// Descriptor設定（チュートリアル）
		// https://docs.vulkan.org/tutorial/latest/_attachments/31_compute_shader.cpp
		bufferSettings.setType(BufferType.STORAGE);
		// storageの場合は絶対にcompute?不明
		bufferSettings.setShaderStage(VK_SHADER_STAGE_COMPUTE_BIT);
		
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
	
	public void addDescriptorsTo(PipelineSettings pipelineSettings) {
		var descripotrList = pipelineSettings.getDescriptorList();
		descripotrList.add(buffer);
		
		// チュートリアル上、bufferと同じデスクリプタがGPU上にもう1つ必要
		descripotrList.add(buffer);
	}
}
