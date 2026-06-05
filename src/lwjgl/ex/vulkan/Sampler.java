package lwjgl.ex.vulkan;

// テクスチャが複数になった場合の対処法不明


import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class Sampler implements Descriptor, AutoCloseable{
	private SamplerSettings settings;
	private long handler;
	public Sampler(SamplerSettings settings) {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
			// createTextureSampler
	        var samplerCreate = VkSamplerCreateInfo.calloc(stack).sType$Default()
	        		// このあたりの設定値をどうするかは保留
	        		.magFilter(VK_FILTER_LINEAR)
	        		.minFilter(VK_FILTER_LINEAR)
	        		.mipmapMode(VK_FILTER_LINEAR)
	        		.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.mipLodBias(DEFAULT_BIAS)
	        		.anisotropyEnable(true)
	        		.maxAnisotropy(settings.getLogicalDevice().getPhysicalDevice().getMaxSamplerAnisotropy())
	        		.compareEnable(false)
	        		.compareOp(VK_COMPARE_OP_ALWAYS);
	        var forSampler = stack.mallocLong(1);
	        Vulkan.throwExceptionIfFailed(vkCreateSampler(settings.getLogicalDevice().getDevice(), samplerCreate, null,forSampler), "Samplerの作成に失敗しました");
	        handler = forSampler.get(0);
		}
		
	}
	public long getHandler() {
		return handler;
	}
	@Override
	public int getDescriptorType() {
		return VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
	}
	
	@Override
	public int getShaderStage() {
		return VK_SHADER_STAGE_FRAGMENT_BIT;
	}
	
	@Override
	public void write(VkWriteDescriptorSet set, int dstBinding, LongBuffer forDescriptorSet, MemoryStack stack) {
		// 初期値設定
		Descriptor.super.write(set, dstBinding, forDescriptorSet, stack);
		
		var descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack)
        	.sampler(handler)
        	
        	// テクスチャが複数になった場合は？？
        	.imageView(settings.getTextureImageView().getHandler())
        	
        	.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
		
		// Vulkanのクソ設計により、Samplerの場合はpImageInfoに代入
		set.pImageInfo(descriptorImageInfo);
	}
	@Override
	public void close() throws Exception {
		var device = settings.getLogicalDevice().getDevice();
		
		if (handler != MemoryUtil.NULL) {
			vkDestroySampler(device, handler, null);
			handler = MemoryUtil.NULL;
		}
	}
	
}
