package lwjgl.ex.vulkan;


import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import motopgi.utils.ExceptionUtils;

import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK14.*;

import java.awt.image.BufferedImage;

import static lwjgl.ex.vulkan.VulkanConstants.*;
import static lwjgl.ex.vulkan.ImageViewSettings.*;
import static lwjgl.ex.vulkan.DescriptionHelper.*;
import static lwjgl.ex.vulkan.StagingBufferSettings.*;

public class Texture implements AutoCloseable {
	/**
	 * おそらくARGB分で4バイト
	 */
	public static final int PIXEL_BYTES = 4;
	
	public static final int DEFAULT_FORMAT = VK_FORMAT_R8G8B8A8_SRGB;
	
	private StagingBuffer textureBuffer;
	private CommandBuffer commandBuffer;
	private ImageView textureImageView;
	
	private BufferedImage image;
	private LogicalDevice logicalDevice;
	
	private Handler imageHandler;
	
	
	public Texture(BufferedImage image, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, DescriptionHelper descriptionHelper, UniformObject uniformObject) {
		this.image = image;
		this.logicalDevice = logicalDevice;
		commandBuffer = new CommandBuffer(new CommandBufferSettings(commandPool));
		
		// 画像のピクセルデータと、Imageのインスタンスは別々に送らなければいけないらしい
		var bufferSettings = new StagingBufferSettings(logicalDevice, buffer -> {
			AssimpUtils.writeImageToPointer(image, buffer);
		});
		bufferSettings.setSize(AssimpUtils.calcSize(image));
		
		// 2の方はlongになっているが、不明
		// VK_BUFFER_USAGE_2_TRANSFER_SRC_BIT
		bufferSettings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
		
		// これは遅いらしいが、動作確認のため一旦こうする
		bufferSettings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		
		textureBuffer = new StagingBuffer(bufferSettings);

		// createImage(texWidth, texHeight, vk::Format::eR8G8B8A8Srgb, vk::ImageTiling::eOptimal, vk::ImageUsageFlagBits::eTransferDst | vk::ImageUsageFlagBits::eSampled, vk::MemoryPropertyFlagBits::eDeviceLocal, textureImage, textureImageMemory);
		imageHandler = ImageView.createImage(new ImageSettings(logicalDevice, image.getWidth(), image.getHeight(), DEFAULT_FORMAT, VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT));
		
		
		try(var stack = MemoryStack.stackPush()) {
			// まとめてsubmitできないのか？
			transitionImageLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, queue, stack);
			copyBufferToImage(stack, queue);
			transitionImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, queue, stack);
			
			
			// createImageView
			var textureImageViewSettings = new ImageViewSettings(logicalDevice);
	        textureImageViewSettings.setFormat(DEFAULT_FORMAT);
	        textureImageViewSettings.setImageHandler(imageHandler.getHandler());
	        textureImageView = new ImageView(textureImageViewSettings);
	        
	        
	        // ここでやることでない可能性が高い、現在は不明
	        // テクスチャが複数あるとバグる可能性大
	        
	     // https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
	        var descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack)
	        	.sampler(descriptionHelper.getSamplerHandler())
	        	.imageView(textureImageView.getHandler())
	        	.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
	        
	        var descriptorBufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
	        		.buffer(uniformObject.getBuffer().getHandler())
	        		.range(UniformObject.BYTES);
	        
	        var descriptorSetBuffer = VkWriteDescriptorSet.calloc(descriptionHelper.getDescriptorCount(), stack).sType$Default();
	        var shaderSettings = descriptionHelper.getShaderSettings();
	        for(int i = 0; i < descriptionHelper.getDescriptorCount(); ++i) {
	        	var stage = shaderSettings.getStage(i);
	        	var descriptorSet = descriptorSetBuffer.get(i);
	        	descriptorSet.sType$Default()
		        	.dstSet(descriptionHelper.getDescriptorSetHandler())
		        	.dstBinding(i)
		        	.descriptorCount(1)
		        	.descriptorType(DescriptionHelper.shaderStageToDescriptorType(stage));

	        	// 非常にきもいが、Vulkanの仕様上どうしようもない？
	        	switch(stage.getStage()) {
	    		case VK_SHADER_STAGE_VERTEX_BIT -> descriptorSet.pBufferInfo(descriptorBufferInfo);
	    		case VK_SHADER_STAGE_FRAGMENT_BIT -> descriptorSet.pImageInfo(descriptorImageInfo);
	    		// 不明な場合の正しい挙動不明
	    		}
	        }
	        
	        vkUpdateDescriptorSets(logicalDevice.getDevice(), descriptorSetBuffer, null);
		}
	}
	
	
	private void transitionImageLayout(int oldLayout, int newLayout, Queue queue, MemoryStack stack) {
		commandBuffer.begin();
		// 参考
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		var barrier = VkImageMemoryBarrier2.calloc(1, stack).sType$Default()
			.oldLayout(oldLayout)
			.newLayout(newLayout)
			.image(imageHandler.getHandler())
			.subresourceRange(ImageViewSettings.DEFAULT_IMAGE_SUBRESOURCE_RANGE);
		
		if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
			barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			barrier.srcStageMask(VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT);
			barrier.dstStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT);
		}
		else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
			barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
			barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);
			barrier.srcStageMask(VK_PIPELINE_STAGE_TRANSFER_BIT);
			barrier.dstStageMask(VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);
		}
		else {
			throw new IllegalArgumentException("不明なlayout遷移です");
		}
		commandBuffer.transitionImageLayout(barrier);
		commandBuffer.submit(stack, queue);
	}
	
	private void copyBufferToImage(MemoryStack stack, Queue queue)
	{
		commandBuffer.begin();
		var region = VkBufferImageCopy.calloc(1, stack)
			.bufferOffset(DEFAULT_LONG_OFFSETS)
			// 不明、0で良いのか？
			.bufferRowLength(0)
			.bufferImageHeight(0)
			.imageSubresource(VkImageSubresourceLayers.calloc(stack)
					.aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
					 .mipLevel(DEFAULT_BASE_MIP_LEVEL)
					.baseArrayLayer(DEFAULT_BASE_ARRAY_LAYER)
					.layerCount(DEFAULT_LAYER_COUNT)
					)
			.imageOffset(DEFAULT_OFFSET_3D)
		    .imageExtent(VkExtent3D.calloc(stack).set(image.getWidth(), image.getHeight(), 1))
		    ;
		
		commandBuffer.copyBufferToImage(textureBuffer, imageHandler.getHandler(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
		commandBuffer.submit(stack, queue);
	}


	@Override
	public void close() throws Exception {
		var device = logicalDevice.getDevice();
		ExceptionUtils.close(textureImageView, imageHandler, commandBuffer, textureBuffer);
	}


	public Handler getImageHandler() {
		return imageHandler;
	}

	
	

}
