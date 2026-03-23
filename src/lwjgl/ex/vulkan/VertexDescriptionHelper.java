package lwjgl.ex.vulkan;



import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
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
public class VertexDescriptionHelper implements AutoCloseable {
	public static final int FLOAT_RGB = Float.BYTES * 3;
	public static final int FLOAT_RG = Float.BYTES * 2;
	
	
	/**
	 * DescriptorSetLayoutBindingのデフォルトの数（VertexとFragment）
	 */
	public static final int DEFAULT_DESCRIPTOR_COUNT = 2;
	
	public static final int INDEX_VERTEX = 0;
	public static final int INDEX_FRAGMENT = 1;
	
	/**
	 * https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/vkCmdBindDescriptorSets.html
	 * のfirstSet。0以外を渡す場合があるのか不明。
	 */
	public static final int FIRST_SET = 0;
	
	public static final int[] DEFAULT_FORMATS = {VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32B32_SFLOAT, VK_FORMAT_R32G32_SFLOAT};
	
	private LogicalDevice logicalDevice;
	
	private int[] formats;
	private int[] offsets;
	private int bytes = 0;
	
	private UniformObject uniformObject = new UniformObject();
	private StagingBuffer uniformBuffer;
	
	private LongBuffer forDescriptorSet = MemoryUtil.memAllocLong(1);
	private LongBuffer forDescriptorPool = MemoryUtil.memAllocLong(1);
	private LongBuffer forLayouts = MemoryUtil.memAllocLong(1);

	public VertexDescriptionHelper(LogicalDevice logicalDevice, int... formats) {
		this.logicalDevice = logicalDevice;
		this.formats = formats;
		offsets = new int[formats.length];
		for(int i = 0; i < formats.length; ++i) {
			// offsetはそれまでのbytesの合計
			offsets[i] = bytes;
			bytes += formatToBytes(formats[i]);
		}
		initDescriptor(logicalDevice);
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
		var vertexAttribute = VkVertexInputAttributeDescription.calloc(formats.length, stack);
		
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/4ef1eebe4af235b2934a165e82aeefcaf8d9b893/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L754-L771
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		for(int i = 0; i < formats.length; ++i) {
			vertexAttribute.get(i)
	            .location(i)
	            .format(formats[i])
	            .offset(offsets[i]);	
		}
		return vertexAttribute;
	}
	
	private void initDescriptor(LogicalDevice logicalDevice) {
		try(var stack = MemoryStack.stackPush()) {
			// 本来絶対にDescriptorPoolなどいらないが、Vulkanの制約上必須になっているので仕方ない
			var poolSize = VkDescriptorPoolSize.calloc(DEFAULT_DESCRIPTOR_COUNT, stack);
			poolSize.get(INDEX_VERTEX)
				.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
				.descriptorCount(1);
			poolSize.get(INDEX_FRAGMENT)
				.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
				.descriptorCount(1);
			
			var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
					.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
					.maxSets(DEFAULT_DESCRIPTOR_COUNT)
					.pPoolSizes(poolSize);
			
//					descriptorPool = vk::raii::DescriptorPool(device, poolInfo);
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorPool(logicalDevice.getDevice(), poolInfo, null, forDescriptorPool), "DescriptorPoolの作成に失敗しました");
			
			var bindings = VkDescriptorSetLayoutBinding.calloc(DEFAULT_DESCRIPTOR_COUNT, stack);
			// vertex側の設定
			//  vk::DescriptorSetLayoutBinding(0, vk::DescriptorType::eUniformBuffer, 1, vk::ShaderStageFlagBits::eVertex, nullptr),
			bindings.get(INDEX_VERTEX)
				.binding(INDEX_VERTEX)
				.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
				.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);
			
			// fragment側の設定
			// vk::DescriptorSetLayoutBinding(1, vk::DescriptorType::eCombinedImageSampler, 1, vk::ShaderStageFlagBits::eFragment, nullptr)};
			bindings.get(INDEX_FRAGMENT)
				.binding(INDEX_FRAGMENT)
				.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
				.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);
			
			var layout = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
					.pBindings(bindings);
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorSetLayout(logicalDevice.getDevice(), layout, null, forLayouts),
	                "DescriptorSetLayoutの作成に失敗しました");
			
			var allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
					.pSetLayouts(forLayouts)
					.descriptorPool(forDescriptorPool.get(0));
			Vulkan.throwExceptionIfFailed(vkAllocateDescriptorSets(logicalDevice.getDevice(), allocate, forDescriptorSet),
	                "DescriptorSetsの割り当てに失敗しました");
			
			uniformBuffer = uniformObject.createBuffer(stack, logicalDevice);
		}
	}

	@Override
	public void close() throws Exception {
		try {
			vkDestroyDescriptorSetLayout(logicalDevice.getDevice(), forLayouts.get(0), null);
			// Poolを削除すればDescriptorSetも消える
			vkDestroyDescriptorPool(logicalDevice.getDevice(), forDescriptorPool.get(0), null);
		} finally {
			ExceptionUtils.close(uniformBuffer);	
		}
		
	}

	public LongBuffer getForDescriptorSet() {
		return forDescriptorSet;
	}

	public StagingBuffer getUniformBuffer() {
		return uniformBuffer;
	}

	public LongBuffer getForLayouts() {
		return forLayouts;
	}
	
}
