package lwjgl.ex.vulkan;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;

// https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineShaderStageCreateInfo.html

public class ShaderStageSettings {
	private int stage;
	private String entryPointName;
	/**
	 * 
	 * @param shader
	 * @param stage org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BITなど(intなのはLWJGLの設計ミス)
	 * @param entryPointName VkPipelineShaderStageCreateInfoのpName、shader.slangの対象関数名
	 */
	public ShaderStageSettings(int stage, String entryPointName) {
		this.stage = stage;
		this.entryPointName = entryPointName;
	}
	public int getStage() {
		return stage;
	}
	public void setStage(int stage) {
		this.stage = stage;
	}
	
	/**
	 * LWJGLのクソ設計によりpName(entryPointName)ができず、
	 * ByteBufferにして渡さないといけないため
	 * @param stack
	 * @return
	 */
	public ByteBuffer getEntryPointNameAsByteBuffer(MemoryStack stack) {
		return stack.UTF8(entryPointName);
	}
	public String getEntryPointName() {
		return entryPointName;
	}
	public void setEntryPointName(String entryPointName) {
		this.entryPointName = entryPointName;
	}
}
