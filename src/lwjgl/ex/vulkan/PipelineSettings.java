package lwjgl.ex.vulkan;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;

public class PipelineSettings {
	private StagingBuffer[] buffers;
	private Shader shader;
	private List<ShaderStageSettings> shaderStageSettingsList = new ArrayList<>();
	
	// Bufferだけなら簡単だったが、Vulkanのクソ設計によりDescriptorの中にBufferと他が混在する
	private Sampler sampler;
	
	public PipelineSettings(Shader shader, StagingBuffer... buffers) {
		this.shader = shader;
		this.buffers = buffers;
	}

	public StagingBuffer[] getBuffers() {
		return buffers;
	}

	public Sampler getSampler() {
		return sampler;
	}
	
	public void add(ShaderStageSettings shaderStageSettings) {
		shaderStageSettingsList.add(shaderStageSettings);
	}

	public List<ShaderStageSettings> getShaderStageSettingsList() {
		return shaderStageSettingsList;
	}
	
	/**
	 * VkComputePipelineCreateInfo.BufferにshaderStage情報を書き込み
	 * @param compute
	 * @param stack
	 */
	public void write(VkComputePipelineCreateInfo.Buffer compute, MemoryStack stack) {
		// ComputeのときはShaderStageが絶対にComputeのみ？不明
		var  shaderStageSettings = shaderStageSettingsList.get(0);
		var shaderStages = VkPipelineShaderStageCreateInfo.calloc(stack).sType$Default()
				.stage(shaderStageSettings.getStage())
				.module(shader.getHandler())
				.pName(shaderStageSettings.getEntryPointNameAsByteBuffer(stack));
		compute.stage(shaderStages);
	}

	public Shader getShader() {
		return shader;
	}
}
