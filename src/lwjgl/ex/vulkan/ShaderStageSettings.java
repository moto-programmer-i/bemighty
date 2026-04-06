package lwjgl.ex.vulkan;

// https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineShaderStageCreateInfo.html

public class ShaderStageSettings {
	private int stage;
	private int format;
	private String entryPointName;
	/**
	 * 
	 * @param shader
	 * @param stage org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BITなど(intなのはLWJGLの設計ミス)
	 * @param entryPointName VkPipelineShaderStageCreateInfoのpName、shader.slangの対象関数名
	 */
	public ShaderStageSettings(int stage, int format, String entryPointName) {
		this.stage = stage;
		this.format = format;
		this.entryPointName = entryPointName;
	}
	public int getStage() {
		return stage;
	}
	public void setStage(int stage) {
		this.stage = stage;
	}
	public String getEntryPointName() {
		return entryPointName;
	}
	public void setEntryPointName(String entryPointName) {
		this.entryPointName = entryPointName;
	}
	public int getFormat() {
		return format;
	}
	public void setFormat(int format) {
		this.format = format;
	}
}
