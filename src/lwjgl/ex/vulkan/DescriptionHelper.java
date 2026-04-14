package lwjgl.ex.vulkan;



import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_COMPARE_OP_ALWAYS;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_REPEAT;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import motopgi.utils.ExceptionUtils;

import static lwjgl.ex.vulkan.VulkanConstants.*;

/**
 * VertexInputBindingDescription, VertexInputAttributeDescriptionの作成に使う
 */
public class DescriptionHelper implements AutoCloseable {
	public static final int FLOAT_RGB = Float.BYTES * 3;
	public static final int FLOAT_RG = Float.BYTES * 2;
	
	
	/**
	 * https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/vkCmdBindDescriptorSets.html
	 * のfirstSet。0以外を渡す場合があるのか不明。
	 */
	public static final int FIRST_SET = 0;
	
	private LogicalDevice logicalDevice;
	private ShaderSettings shaderSettings;
	
	private int[] offsets;
	private int bytes = 0;
	
	/**
	 * Descriptorの種類
	 */
	private final int descriptorCount;
	
	
	private LongBuffer forDescriptorSet = MemoryUtil.memAllocLong(1);
	private LongBuffer forDescriptorPool = MemoryUtil.memAllocLong(1);
	private LongBuffer forLayouts = MemoryUtil.memAllocLong(1);

	private long samplerHandler;

	public DescriptionHelper(LogicalDevice logicalDevice, ShaderSettings shaderSettings) {
		this.logicalDevice = logicalDevice;
		this.shaderSettings = shaderSettings;
		
		// 可読性を考慮して用意
		descriptorCount = shaderSettings.stagesSize();
		
		offsets = new int[descriptorCount];
		for(int i = 0; i < offsets.length; ++i) {
			// offsetはそれまでのbytesの合計
			offsets[i] = bytes;
			bytes += formatToBytes(shaderSettings.getStage(i).getFormat());
		}
		

		try(var stack = MemoryStack.stackPush()) {
			// 本来絶対にDescriptorPoolなどいらないが、Vulkanの制約上必須になっているので仕方ない
			var poolSize = VkDescriptorPoolSize.calloc(descriptorCount, stack);
			
			for(int i = 0; i < descriptorCount; ++i) {
				poolSize.get(i)
					.type(shaderStageToDescriptorType(shaderSettings.getStage(i)))
					.descriptorCount(1);
			}
			
			
			var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
					.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
					.maxSets(descriptorCount)
					.pPoolSizes(poolSize);
			
	//					descriptorPool = vk::raii::DescriptorPool(device, poolInfo);
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorPool(logicalDevice.getDevice(), poolInfo, null, forDescriptorPool), "DescriptorPoolの作成に失敗しました");
			
			var bindings = VkDescriptorSetLayoutBinding.calloc(descriptorCount, stack);
			
			for(int i = 0; i < descriptorCount; ++i) {
				var stage = shaderSettings.getStage(i);
				bindings.get(i)
				.binding(i)
				.descriptorCount(1)
				.descriptorType(shaderStageToDescriptorType(stage))
				.stageFlags(stage.getStage());
			}
			
			var layout = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
					.pBindings(bindings);
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorSetLayout(logicalDevice.getDevice(), layout, null, forLayouts),
	                "DescriptorSetLayoutの作成に失敗しました");
			
			var allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
					.pSetLayouts(forLayouts)
					.descriptorPool(forDescriptorPool.get(0));
			Vulkan.throwExceptionIfFailed(vkAllocateDescriptorSets(logicalDevice.getDevice(), allocate, forDescriptorSet),
	                "DescriptorSetsの割り当てに失敗しました");
			
			// createTextureSampler
	        var samplerCreate = VkSamplerCreateInfo.calloc(stack).sType$Default()
	        		// mipmapModeを選択
	        		// 設定可能にするべきか不明
	        		// https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html#_sampler
	        		.magFilter(VK_FILTER_LINEAR)
	        		.minFilter(VK_FILTER_LINEAR)
	        		.mipmapMode(VK_FILTER_LINEAR)
	        		.mipLodBias(DEFAULT_BIAS)
	        		.minLod(DEFAULT_MIN_LEVEL_OF_DETAIL)
			        .maxLod(VK_LOD_CLAMP_NONE)
	        		
	        		.addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
	        		.anisotropyEnable(true)
	        		.maxAnisotropy(logicalDevice.getPhysicalDevice().getMaxSamplerAnisotropy())
	        		.compareEnable(false)
	        		.compareOp(VK_COMPARE_OP_ALWAYS);
	        var forSampler = stack.mallocLong(1);
	        Vulkan.throwExceptionIfFailed(vkCreateSampler(logicalDevice.getDevice(), samplerCreate, null,forSampler), "Samplerの作成に失敗しました");
	        samplerHandler = forSampler.get(0);
		}
	}
	
	/**
	 * フォーマットに対応したバイト数を返す
	 * （VertexInputAttributeDescriptionのため）
	 * https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
	 * @param format
	 * @return
	 */
	public static int formatToBytes(int format) {
		return switch (format) {
		// 足りなければ随時追加する
		case VK_FORMAT_R32G32B32_SFLOAT -> FLOAT_RGB;
		case VK_FORMAT_R32G32_SFLOAT -> FLOAT_RG;
		default -> throw new IllegalArgumentException("不明なフォーマット " + format);
		};
	}
	
	/**
	 * 頂点の送り方（合計サイズ）
	 * @param stack
	 * @return
	 */
	public VkVertexInputBindingDescription.Buffer createBinding(MemoryStack stack) {
		return VkVertexInputBindingDescription.calloc(1, stack)
        .binding(DEFAULT_FIRST_BINDING)
        .stride(bytes)
        .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
	}
	
	/**
	 * 頂点の送り方（formatなど）
	 * @param stack
	 * @return
	 */
	public VkVertexInputAttributeDescription.Buffer createAttribute(MemoryStack stack) {
		var vertexAttribute = VkVertexInputAttributeDescription.calloc(descriptorCount, stack);
		
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/4ef1eebe4af235b2934a165e82aeefcaf8d9b893/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L754-L771
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		for(int i = 0; i < descriptorCount; ++i) {
			vertexAttribute.get(i)
	            .location(i)
	            .format(shaderSettings.getStage(i).getFormat())
	            .offset(offsets[i]);	
		}
		return vertexAttribute;
	}
	
	
	/**
	 * vk::ShaderStageFlagBitsからvk::DescriptorTypeへ
	 * @param stageSettings vk::ShaderStageFlagBits
	 * @return vk::DescriptorType
	 */
	public static int shaderStageToDescriptorType(ShaderStageSettings stageSettings) {
		return switch(stageSettings.getStage()) {
		case VK_SHADER_STAGE_VERTEX_BIT -> VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER;
		case VK_SHADER_STAGE_FRAGMENT_BIT -> VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
		default -> throw new IllegalArgumentException("不明なShaderStage");
		};
	}

	@Override
	public void close() throws Exception {
		var device = logicalDevice.getDevice();
		vkDestroyDescriptorSetLayout(device, forLayouts.get(0), null);
		// Poolを削除すればDescriptorSetも消える
		vkDestroyDescriptorPool(device, forDescriptorPool.get(0), null);
		
		if (samplerHandler != NULL) {
			vkDestroySampler(device, samplerHandler, null);
			samplerHandler = NULL;
		}
	}
	
	
	// 今のところDescriptorSetは1つなのでBufferを返す意味がないが、vkCmdBindDescriptorSetsの要求がBufferなので仕方なく
	public LongBuffer getForDescriptorSet() {
		return forDescriptorSet;
	}
	public long getDescriptorSetHandler() {
		return forDescriptorSet.get(0);
	}

	public LongBuffer getForLayouts() {
		return forLayouts;
	}

	public ShaderSettings getShaderSettings() {
		return shaderSettings;
	}

	public int getDescriptorCount() {
		return descriptorCount;
	}

	public long getSamplerHandler() {
		return samplerHandler;
	}
	
	
	
	
}
