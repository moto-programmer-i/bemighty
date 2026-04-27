package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class Sampler implements Descriptor{
	private long handler;
	public Sampler() {
		// TODO Auto-generated constructor stub
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
		
		var descriptorImageInfo = VkDescriptorImageInfo.calloc(1, stack);
				// あとで
//		        	.sampler(descriptionHelper.getSamplerHandler())
//		        	.imageView(textureImageView.getHandler())
//		        	.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
		
		// Vulkanのクソ設計により、Samplerの場合はpImageInfoに代入
		set.pImageInfo(descriptorImageInfo);
	}
	
}
