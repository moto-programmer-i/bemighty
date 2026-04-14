package lwjgl.ex.vulkan;

import java.awt.image.BufferedImage;
import java.nio.LongBuffer;
import java.util.Arrays;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageBlit;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import motopgi.utils.ExceptionUtils;

import static lwjgl.ex.vulkan.StagingBufferSettings.MEMORY_PROPERTY_FLAGS_DESTINATION;
import static lwjgl.ex.vulkan.VulkanConstants.DEFAULT_LONG_OFFSETS;
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
	
	// VkImageBlit.offsets[2]は名前が不適切、実際にはboundsなので用意
	// https://docs.vulkan.org/refpages/latest/refpages/source/VkImageBlit.html
	public static final int BOUNDS_INDEX_LEFT_TOP = 0;
	public static final int BOUNDS_INDEX_WIDTH_HEIGHT = 1;
	
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
                            // levelCountという名前が不適切なので変更した 
                            // https://docs.vulkan.org/refpages/latest/refpages/source/VkImageSubresourceRange.html
                            .levelCount(settings.getMipLevels())
                            
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
	
	public static void generateMipmaps(Handler imageHandler, BufferedImage image, int imageFormat, int mipLevels, LogicalDevice logicalDevice, CommandBuffer commandBuffer, Queue queue)
	{
		// https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html#_generating_mipmaps
		try(var stack = MemoryStack.stackPush()) {
			// Check if image format supports linear blit-ing
			var formatProperties = logicalDevice.getPhysicalDevice().getFormatProperties(imageFormat, stack).formatProperties();
			
			
			if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_2_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0)
			{
				throw new IllegalArgumentException("テクスチャの画像フォーマットがlinear blittingをサポートしていません\n フォーマット：" + imageFormat);
			}

			commandBuffer.begin();
			
			var barrier = new ImageMemoryBarrier(stack)
					.accessMask(VK_ACCESS_TRANSFER_READ_BIT)
					.stageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT)
					.layout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
					.image(imageHandler.getHandler())
					;

			var barrierBegin = barrier.getBegin()
				.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
				.srcStageMask(VK_PIPELINE_STAGE_2_TRANSFER_BIT)
				.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			
			var barrierEnd = barrier.getEnd()
					.dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
					.dstStageMask(VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT)
					.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
			

			int sourceMipWidth  = image.getWidth();
			int sourceMipHeight = image.getHeight();
			int destinationMipWidth, destinationMipHeight;
			
			// https://docs.vulkan.org/refpages/latest/refpages/source/VkImageBlit.html
			var imageBlit = VkImageBlit.malloc(1, stack);
			// 変数名はoffsets[2]となっているが、実際はbounds
			// 左上は当然0
			// サイズはzが1らしい
			// https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html#_generating_mipmaps
			imageBlit.srcOffsets().get(BOUNDS_INDEX_LEFT_TOP).x(0).y(0).z(0);
			imageBlit.srcOffsets().get(BOUNDS_INDEX_WIDTH_HEIGHT).z(1);
			imageBlit.dstOffsets().get(BOUNDS_INDEX_LEFT_TOP).x(0).y(0).z(0);
			imageBlit.dstOffsets().get(BOUNDS_INDEX_WIDTH_HEIGHT).z(1);
			
			imageBlit.srcSubresource()
				.aspectMask(ImageViewSettings.DEFAULT_ASPECT_MASK)
				.baseArrayLayer(ImageViewSettings.DEFAULT_BASE_ARRAY_LAYER)
				.layerCount(ImageViewSettings.DEFAULT_LAYER_COUNT);
			imageBlit.dstSubresource()
				.aspectMask(ImageViewSettings.DEFAULT_ASPECT_MASK)
				.baseArrayLayer(ImageViewSettings.DEFAULT_BASE_ARRAY_LAYER)
				.layerCount(ImageViewSettings.DEFAULT_LAYER_COUNT);

			// baseMipLevel 0 から、次のbaseMipLevelへコピーしていく
			for(int sourceMipLevel = 0, destinationMipLevel = 1; destinationMipLevel < mipLevels; ++sourceMipLevel, ++destinationMipLevel) {
				barrier.baseMipLevel(sourceMipLevel);
				commandBuffer.transitionImageLayout(barrierBegin);

				// mipWidthはチュートリアルに合わせて半分にしていく
				imageBlit.srcOffsets().get(BOUNDS_INDEX_WIDTH_HEIGHT).x(sourceMipWidth).y(sourceMipHeight);
				destinationMipWidth = sourceMipWidth > 1 ? sourceMipWidth / 2 : 1;
				destinationMipHeight = sourceMipHeight > 1 ? sourceMipHeight / 2 : 1;
				imageBlit.dstOffsets().get(BOUNDS_INDEX_WIDTH_HEIGHT).x(destinationMipWidth).y(destinationMipHeight);
				
				// mipLevel更新
				imageBlit.srcSubresource().mipLevel(sourceMipLevel);
				imageBlit.dstSubresource().mipLevel(destinationMipLevel);

				commandBuffer.blitImage(imageHandler, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, imageHandler, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, imageBlit, VK_FILTER_LINEAR);

				commandBuffer.transitionImageLayout(barrierEnd);
				

				// destinationは次のループでsourceになる
				sourceMipWidth = destinationMipWidth;
				sourceMipHeight = destinationMipHeight;
			}

			
			// 最後のmipLevelを送信
			// 事前のnewLayoutとoldLayoutが異なるが、エラーにならない。不明。
			barrierEnd.subresourceRange().baseMipLevel(mipLevels - 1);
			barrierEnd.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
			barrierEnd.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			commandBuffer.transitionImageLayout(barrierEnd);

			commandBuffer.submit(stack, queue);
		}
	}
}
