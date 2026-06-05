package lwjgl.ex.vulkan;

public class SamplerSettings {

	private LogicalDevice logicalDevice;
	
	private ImageView textureImageView;
	
	// テクスチャが複数になった場合はどうする？？不明
	public SamplerSettings(LogicalDevice logicalDevice, ImageView textureImageView) {
		this.logicalDevice = logicalDevice;
		this.textureImageView = textureImageView;
	}

	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public ImageView getTextureImageView() {
		return textureImageView;
	}
}
