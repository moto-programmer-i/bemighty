package lwjgl.ex.vulkan;

import org.lwjgl.assimp.AITexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import motopgi.utils.ExceptionUtils;

import static lwjgl.ex.vulkan.StagingBufferSettings.MEMORY_PROPERTY_FLAGS_VISIBLE;
import static org.lwjgl.vulkan.VK14.*;

import static lwjgl.ex.vulkan.VulkanConstants.*;
import static lwjgl.ex.vulkan.ImageViewSettings.*;
import static lwjgl.ex.vulkan.VertexDescriptionHelper.*;

public class Texture implements AutoCloseable {
	public static final int DEFAULT_IMAGE_DEPTH = 1;
	public static final int DEFAULT_IMAGE_MIP_LEVEL = 1;
	public static final int DEFAULT_IMAGE_ARRAY_LAYER = 1;
	
	private AITexture texture;
	private StagingBuffer textureBuffer;
	private long imageHandler;
	private long imageMemory;
	private CommandBuffer commandBuffer;
	private ImageView textureImageView;
	
	public Texture(AITexture texture, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper) {
		this.texture = texture;
		commandBuffer = new CommandBuffer(new CommandBufferSettings(commandPool));
		
		var bufferSettings = new StagingBufferSettings(logicalDevice, buffer -> {
			var textureBuffer = buffer.getByteBuffer(0, texture.pcDataCompressed().capacity());
			textureBuffer.put(texture.pcDataCompressed());
		});
		bufferSettings.setUsage(VK_IMAGE_USAGE_SAMPLED_BIT);
		// これは遅いらしいが、動作確認のため一旦こうする
		bufferSettings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		
		textureBuffer = new StagingBuffer(bufferSettings);
		
		
		try(var stack = MemoryStack.stackPush()) {
			// createImage(texWidth, texHeight, vk::Format::eR8G8B8A8Srgb, vk::ImageTiling::eOptimal, vk::ImageUsageFlagBits::eTransferDst | vk::ImageUsageFlagBits::eSampled, vk::MemoryPropertyFlagBits::eDeviceLocal, textureImage, textureImageMemory);
			var imageInfo = VkImageCreateInfo.calloc(stack).sType$Default()
				.imageType(VK_IMAGE_TYPE_2D)
				.format(VK_FORMAT_R8G8_SRGB)
				.extent(VkExtent3D.malloc(stack).width(texture.mWidth()).height(texture.mHeight()).depth(DEFAULT_IMAGE_DEPTH))
				.mipLevels(DEFAULT_IMAGE_MIP_LEVEL)
				.arrayLayers(DEFAULT_IMAGE_ARRAY_LAYER)
				.samples(VK_SAMPLE_COUNT_1_BIT)
				.tiling(VK_IMAGE_TILING_OPTIMAL)
				.usage(VK_IMAGE_USAGE_SAMPLED_BIT)
				.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
			
			var device = logicalDevice.getDevice();
			
			var forImage = stack.mallocLong(1);
			System.out.println("pre vkCreateImage");
			Vulkan.throwExceptionIfFailed(vkCreateImage(device, imageInfo, null, forImage), "Textureの作成に失敗しました");
			System.out.println("after vkCreateImage");
			imageHandler = forImage.get(0);
			imageMemory = StagingBuffer.bindMemory(imageHandler, bufferSettings, stack);
			
			transitionImageLayout(VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, stack);
			copyBufferToImage(stack);
			transitionImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, stack);
			commandBuffer.submit(stack, queue);
			
			
			// createImageView
			var textureImageViewSettings = new ImageViewSettings(logicalDevice);
	        textureImageViewSettings.setFormat(VK_FORMAT_R8G8B8_SRGB);
	        textureImageView = new ImageView(textureImageViewSettings);
	        
	     // https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
	        // createTextureSampler
	        var samplerCreate = VkSamplerCreateInfo.calloc(stack).sType$Default()
	        		.magFilter(VK_FILTER_LINEAR)
	        		.minFilter(VK_FILTER_LINEAR)
	        		.mipmapMode(VK_FILTER_LINEAR)
	        		.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.mipLodBias(DEFAULT_BIAS)
	        		.anisotropyEnable(true)
	        		.maxAnisotropy(logicalDevice.getPhysicalDevice().getMaxSamplerAnisotropy())
	        		.compareEnable(false)
	        		.compareOp(VK_COMPARE_OP_ALWAYS);
	        var forSampler = stack.mallocLong(1);
	        Vulkan.throwExceptionIfFailed(vkCreateSampler(logicalDevice.getDevice(), samplerCreate, null,forSampler), "Samplerの作成に失敗しました");
	        
	        var descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack)
	        	.sampler(forSampler.get(0))
	        	.imageView(textureImageView.getHandler())
	        	.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
	        
	        var descriptorBufferInfo = VkDescriptorBufferInfo.calloc(1, stack)
	        		.buffer(descriptionHelper.getUniformBuffer().getHandler())
	        		.range(UniformObject.BYTES);
	        
	        var descriptorSet = VkWriteDescriptorSet.calloc(DEFAULT_DESCRIPTOR_COUNT, stack).sType$Default();
	        descriptorSet.get(INDEX_VERTEX)
	        	.dstSet(descriptionHelper.getForDescriptorSet().get(INDEX_VERTEX))
	        	.descriptorCount(1)
	        	.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
	        	.pBufferInfo(descriptorBufferInfo);
	        descriptorSet.get(INDEX_VERTEX)
		    	.dstSet(descriptionHelper.getForDescriptorSet().get(INDEX_FRAGMENT))
		    	.descriptorCount(1)
		    	.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
		    	.pImageInfo(descriptorImageInfo);

	        
	        vkUpdateDescriptorSets(logicalDevice.getDevice(), descriptorSet, null);


		}
	}
	
	
	private void transitionImageLayout(int oldLayout, int newLayout, MemoryStack stack) {
		// 参考
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		var barrier = VkImageMemoryBarrier2.calloc(1, stack).sType$Default()
			.oldLayout(oldLayout)
			.newLayout(newLayout)
			.image(imageHandler)
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
	}
	
	private void copyBufferToImage(MemoryStack stack)
	{
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
		    .imageExtent(VkExtent3D.calloc(stack).set(texture.mWidth(), texture.mHeight(), 1))
		    ;
		
		commandBuffer.copyBufferToImage(textureBuffer, imageMemory, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
//		endSingleTimeCommands(*commandBuffer);
	}


	@Override
	public void close() throws Exception {
		ExceptionUtils.close(textureImageView, commandBuffer, textureBuffer);
	}


	public long getImageHandler() {
		return imageHandler;
	}
	
	
	

}
