package lwjgl.ex.vulkan;

import java.awt.image.BufferedImage;
import java.nio.LongBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import motopgi.utils.ExceptionUtils;

import static lwjgl.ex.vulkan.StagingBufferSettings.MEMORY_PROPERTY_FLAGS_DESTINATION;
import static lwjgl.ex.vulkan.VulkanConstants.DEFAULT_LONG_OFFSETS;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK14.*;

/**
 * VkImageはこのクラスを通して使う想定。
 * VkImageは名前が不適切なので、クラスとしては用意しない。
 */

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-04/src/main/java/org/vulkanb/eng/graph/vk/ImageView.java
public class ImageView implements AutoCloseable {
	public static final int DEFAULT_IMAGE_DEPTH = 1;
	public static final int DEFAULT_IMAGE_MIP_LEVEL = 1;
	public static final int DEFAULT_IMAGE_ARRAY_LAYER = 1;

	/**
	 * 最大mipLevel（適当に3にした）
	 */
	public static final int MAX_MIP_LEVEL = 3;
	
	/**
	 * この幅を超えたとき、mipLevelを使用（適当に1000にした）
	 */
	public static final int WIDTH_DELIMITER_FOR_MIP = 1000;
	
	private long handler;
	private final ImageViewSettings settings;
	private final int index;
	
	/**
	 * vk::Imageを持つ場合はここにいれる
	 * （ImageViewがimageを持つ場合と持たない場合があるので変になっている）
	 */
	private Handler imageHandler;
	
	public ImageView(ImageViewSettings settings) {
		this(settings, 0);
	}
	public ImageView(ImageViewSettings settings, int index) {
		this.settings = settings;
		this.index = index;
		try (var stack = MemoryStack.stackPush()) {
            LongBuffer lp = stack.mallocLong(1);
            var viewCreateInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType$Default()
                    .image(settings.getImageHandler())
                    .viewType(settings.getViewType())
                    // formatがimageに紐づくなら、imageクラスのほうがいい
                    .format(settings.getFormat())
                    .subresourceRange(it -> it
                            .aspectMask(settings.getAspectMask())
                            .baseMipLevel(settings.getBaseMipLevel())
                            .levelCount(settings.getLevelCount())
                            .baseArrayLayer(settings.getBaseArrayLayer())
                            .layerCount(settings.getLayerCount()));

            Vulkan.throwExceptionIfFailed(vkCreateImageView(settings.getLogicalDevice().getDevice(), viewCreateInfo, null, lp),
                    "ImageViewの作成に失敗しました");
            handler = lp.get(0);
		}
	}
	
	public int getIndex() {
		return index;
	}



	@Override
	public void close() throws Exception {
		if (handler != VK_NULL_HANDLE) {
			vkDestroyImageView(settings.getLogicalDevice().getDevice(), handler, null);
			handler = VK_NULL_HANDLE;	
		}
		ExceptionUtils.close(imageHandler);
		
	}
	
	public static ImageView[] createArray(int length, ImageViewSettings settings) {
		var array = new ImageView[length];
		// 参考
		// https://qiita.com/payaneco/items/ea5db7b62d092927aed8
		Arrays.setAll(array, i -> new ImageView(settings, i));
		return array;
	}
	
	public static ImageView[] createArray(int length, LongBuffer imageBuffer, ImageViewSettings commonSettings) {
		var array = new ImageView[length];
		// 参考
		// https://qiita.com/payaneco/items/ea5db7b62d092927aed8
		Arrays.setAll(array, i -> {
			// imageは別のため、settingsをcloneする必要がある
			var settings = commonSettings.clone();
			settings.setImageHandler(imageBuffer.get(i));
			return new ImageView(settings, i);	
		});
		return array;
	}
	public static Handler createImage(ImageSettings imageSettings) {
		var handler = Handler.createImageHandler(imageSettings.getLogicalDevice());
		
		try(var stack = MemoryStack.stackPush()) {
			// createImage(texWidth, texHeight, vk::Format::eR8G8B8A8Srgb, vk::ImageTiling::eOptimal, vk::ImageUsageFlagBits::eTransferDst | vk::ImageUsageFlagBits::eSampled, vk::MemoryPropertyFlagBits::eDeviceLocal, textureImage, textureImageMemory);
			var imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
				.imageType(VK_IMAGE_TYPE_2D)
				.format(imageSettings.getFormat())
				.extent(VkExtent3D.malloc(stack).width(imageSettings.getWidth()).height(imageSettings.getHeight()).depth(DEFAULT_IMAGE_DEPTH))
				.mipLevels(imageSettings.getMipLevels())
				.arrayLayers(DEFAULT_IMAGE_ARRAY_LAYER)
				.samples(VK_SAMPLE_COUNT_1_BIT)
				.tiling(imageSettings.getTiling())
				.usage(imageSettings.getUsage())
				.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			
			var device = imageSettings.getLogicalDevice().getDevice();
			
			Vulkan.throwExceptionIfFailed(vkCreateImage(device, imageInfo, null, handler.getForHandler()), "Textureの作成に失敗しました");
			var imageHandler = handler.getHandler();
			var memoryRequirements = VkMemoryRequirements.calloc(stack);
			vkGetImageMemoryRequirements(device, imageHandler, memoryRequirements);
			
			
			
			// Imageの方ではMEMORY_PROPERTY_FLAGS_VISIBLEが対応していなかった
//			bufferSettings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
			
			// 恐らくImageのインスタンスを送っている？？？
			// 本来、Imageのインスタンスを送るときに画像データも送るべき
			var imageMemory = StagingBuffer.allocateMemory(imageHandler, imageSettings.getLogicalDevice(), imageSettings.getProperties(), memoryRequirements, stack, handler.getForMemory());
			Vulkan.throwExceptionIfFailed(vkBindImageMemory(device, imageHandler, imageMemory, DEFAULT_LONG_OFFSETS), "Imageインスタンスのメモリへの紐づけに失敗しました");
		}
		
		return handler;
	}
	
	// vk::raii::ImageView createImageView(vk::raii::Image &image, vk::Format format, vk::ImageAspectFlags aspectFlags)
	public static ImageView from(ImageSettings imageSettings, int format, int aspectMask) {
		var image = createImage(imageSettings);
		var viewSettings = new ImageViewSettings(imageSettings.getLogicalDevice());
		viewSettings.setImageHandler(image.getHandler());
		viewSettings.setFormat(format);
		viewSettings.setAspectMask(aspectMask);
		var view = new ImageView(viewSettings);
		
		// 設計が変だが、ImageViewerがimageを解放するときとしないときがあるので、現状仕方ない？
		view.imageHandler = image;
		
		return view;
	}


	public long getHandler() {
		return handler;
	}
	public long getImageHandler() {
		return settings.getImageHandler();
	}
	/**
	 * ミップレベルの計算
	 * （ミップマップは、事前に計算された縮小版の画像）
	 * @param image
	 * @return
	 */
	public static int calcMipLevel(BufferedImage image) {
		// https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html
		// mipLevelを適当に設定
//		if(image.getWidth() >= WIDTH_DELIMITER_FOR_MIP) {
			return MAX_MIP_LEVEL;
//		}
//		return ImageView.DEFAULT_IMAGE_MIP_LEVEL;
	}
}
