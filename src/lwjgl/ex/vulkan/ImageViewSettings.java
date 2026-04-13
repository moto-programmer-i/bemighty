package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK14.*;

import java.util.Objects;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceRange;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-04/src/main/java/org/vulkanb/eng/graph/vk/ImageView.java

public class ImageViewSettings {
	public static final int DEFAULT_ASPECT_MASK = VK_IMAGE_ASPECT_COLOR_BIT;
	public static final int DEFAULT_BASE_ARRAY_LAYER = 0;
	public static final int DEFAULT_LAYER_COUNT = 1;
	public static final int DEFAULT_BASE_MIP_LEVEL = 0;
	public static final int DEFAULT_LEVEL_COUNT = 1;
	public static final int DEFAULT_VIEW_TYPE = VK_IMAGE_VIEW_TYPE_2D;
	
	/**
	 * これ以外の画像フォーマットにすると、AssimpUtils.writeImageToPointerの対応が難しい
	 */
	public static final int DEFAULT_FORMAT = VK_FORMAT_B8G8R8A8_SRGB;
	
	public static final VkImageSubresourceRange DEFAULT_IMAGE_SUBRESOURCE_RANGE = 
			VkImageSubresourceRange.create()
			.aspectMask(ImageViewSettings.DEFAULT_ASPECT_MASK)
			.baseMipLevel(ImageViewSettings.DEFAULT_BASE_MIP_LEVEL)
			.levelCount(ImageViewSettings.DEFAULT_LAYER_COUNT)
			.baseArrayLayer(ImageViewSettings.DEFAULT_BASE_ARRAY_LAYER)
			.layerCount(ImageViewSettings.DEFAULT_LAYER_COUNT);
	
	public static final VkImageSubresourceRange DEPTH_SUBRESOURCE_RANGE = 
			VkImageSubresourceRange.create()
			.aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT)
			.baseMipLevel(ImageViewSettings.DEFAULT_BASE_MIP_LEVEL)
			.levelCount(ImageViewSettings.DEFAULT_LAYER_COUNT)
			.baseArrayLayer(ImageViewSettings.DEFAULT_BASE_ARRAY_LAYER)
			.layerCount(ImageViewSettings.DEFAULT_LAYER_COUNT);

	private int aspectMask = DEFAULT_ASPECT_MASK;
	private int baseArrayLayer = DEFAULT_BASE_ARRAY_LAYER;
	/**
	 * 本来はenumにするべきだが、LWJGLがこうなってしまっているのでしょうがない
	 */
	private int format = DEFAULT_FORMAT;
	private int layerCount = DEFAULT_LAYER_COUNT;
	private int levelCount = DEFAULT_LEVEL_COUNT;
	private int baseMipLevel = DEFAULT_BASE_MIP_LEVEL;
	private int viewType = DEFAULT_VIEW_TYPE;

	private long imageHandler;

	private LogicalDevice logicalDevice;
	

	public ImageViewSettings(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	public int getAspectMask() {
		return aspectMask;
	}

	public void setAspectMask(int aspectMask) {
		this.aspectMask = aspectMask;
	}

	public int getBaseArrayLayer() {
		return baseArrayLayer;
	}

	public void setBaseArrayLayer(int baseArrayLayer) {
		this.baseArrayLayer = baseArrayLayer;
	}

	public int getFormat() {
		return format;
	}

	public void setFormat(int format) {
		this.format = format;
	}

	public int getLayerCount() {
		return layerCount;
	}

	public void setLayerCount(int layerCount) {
		this.layerCount = layerCount;
	}

	public int getLevelCount() {
		return levelCount;
	}

	// baseMipLevelと対応させなければいけないらしい
	// https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html#_image_creation
//	public void setLevelCount(int levelCount) {
//		this.levelCount = levelCount;
//	}
	public void setBaseMipLevel(int baseMipLevel) {
		this.baseMipLevel = baseMipLevel;
	}
	
	public int getBaseMipLevel() {
		return baseMipLevel;
	}

	public int getViewType() {
		return viewType;
	}

	public void setViewType(int viewType) {
		this.viewType = viewType;
	}

	public long getImageHandler() {
		return imageHandler;
	}

	public void setImageHandler(long imageHandler) {
		this.imageHandler = imageHandler;
	}

	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}

	@Override
	public int hashCode() {
		return Objects.hash(aspectMask, baseArrayLayer, baseMipLevel, format, imageHandler, layerCount, levelCount,
				logicalDevice, viewType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageViewSettings other = (ImageViewSettings) obj;
		return aspectMask == other.aspectMask && baseArrayLayer == other.baseArrayLayer
				&& baseMipLevel == other.baseMipLevel && format == other.format && imageHandler == other.imageHandler
				&& layerCount == other.layerCount && levelCount == other.levelCount
				&& Objects.equals(logicalDevice, other.logicalDevice) && viewType == other.viewType;
	}

	@Override
	protected ImageViewSettings clone() {
		var clone = new ImageViewSettings(logicalDevice);
		clone.aspectMask = aspectMask;
		clone.baseArrayLayer = baseArrayLayer;
		clone.format = format;
		clone.layerCount = layerCount;
		clone.levelCount = levelCount;
		clone.baseMipLevel = baseMipLevel;
		clone.viewType = viewType;
		clone.imageHandler = imageHandler;
		clone.logicalDevice = logicalDevice;
		return clone;
	}

	public static VkImageMemoryBarrier2.Buffer createDefaultBarrier() {
		return VkImageMemoryBarrier2.calloc(1).sType$Default()
		.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .subresourceRange(ImageViewSettings.DEFAULT_IMAGE_SUBRESOURCE_RANGE);
	}
}
