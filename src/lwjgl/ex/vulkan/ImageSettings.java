package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

public class ImageSettings {
	private LogicalDevice logicalDevice;
	private int width;
	private int  height;
	/**
	 * vk::Format
	 */
	private int format;
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
	public ImageSettings(LogicalDevice logicalDevice, int width, int height, int format, int usage) {
		this.logicalDevice = logicalDevice;
		this.width = width;
		this.height = height;
		this.format = format;
		this.usage = usage;
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
}
