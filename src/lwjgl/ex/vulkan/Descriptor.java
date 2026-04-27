package lwjgl.ex.vulkan;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public interface Descriptor {
	public int getDescriptorType();
	public int getShaderStage();
	
	// VkDescriptorSetLayoutBindingとVkWriteDescriptorSetを
	// 両方設定しなければいけないのが意味不明
	
	default void write(VkDescriptorSetLayoutBinding descriptorSetLayoutBinding, int binding) {
		descriptorSetLayoutBinding.binding(binding)
		// 1以外になる場合があるのか不明
		.descriptorCount(1)
		.descriptorType(getDescriptorType())
		.stageFlags(getShaderStage());
	}
	
	/**
	 * VkWriteDescriptorSetに初期値を設定
	 * @param set
	 * @param dstBinding
	 */
	default public void write(VkWriteDescriptorSet set, int dstBinding, LongBuffer forDescriptorSet, MemoryStack stack) {
		set
			.dstSet(forDescriptorSet.get(0))
	    	.dstBinding(dstBinding)
	    	
	    	// 1以外の場合が不明
	    	.descriptorCount(1)
	    	.descriptorType(getDescriptorType());
	}

}
