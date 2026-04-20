package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

import java.awt.image.BufferedImage;

public class ImageSettings {
	
	private LogicalDevice logicalDevice;
	private int width;
	private int  height;
	/**
	 * vk::Format
	 */
	private int format = ImageViewSettings.DEFAULT_FORMAT;
	/**
	 * vk::ImageTiling
	 */
	private int tiling = VK_IMAGE_TILING_OPTIMAL;
	/**
	 * vk::ImageUsageFlags
	 */
	private int usage;
	/**
	 * vk::MemoryPropertyFlags
	 */
	private int properties = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
	
	private int mipLevels = ImageView.DEFAULT_IMAGE_MIP_LEVEL;
	
	private int samples;
	
	
	public ImageSettings(LogicalDevice logicalDevice, int width, int height, int format, int usage) {
		this.logicalDevice = logicalDevice;
		this.width = width;
		this.height = height;
		this.format = format;
		this.usage = usage;
		
		// デフォルトはマルチサンプルとする
		samples = logicalDevice.getMsaaSamples();
	}
	
	public ImageSettings(LogicalDevice logicalDevice, BufferedImage image, int usage) {
		this(logicalDevice, image.getWidth(), image.getHeight(), ImageViewSettings.DEFAULT_FORMAT, usage);
	}
	
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	public int getFormat() {
		return format;
	}
	public int getTiling() {
		return tiling;
	}
	public int getUsage() {
		return usage;
	}
	public int getProperties() {
		return properties;
	}
	
	public void setTiling(int tiling) {
		this.tiling = tiling;
	}
	
	public void setProperties(int properties) {
		this.properties = properties;
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public int getMipLevels() {
		return mipLevels;
	}

	public void setMipLevels(int mipLevels) {
		this.mipLevels = mipLevels;
	}

	public int getSamples() {
		return samples;
	}

	public void setSamples(int samples) {
		this.samples = samples;
	}
	
}
