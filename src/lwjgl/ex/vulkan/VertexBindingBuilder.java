package lwjgl.ex.vulkan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.vulkan.VK14.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static lwjgl.ex.vulkan.VulkanConstants.*;

/**
 * ・VertexInputBindingDescription
 * ・VertexInputAttributeDescription
 * ・Buffer
 * の3つがVulkanのクソ設計により統一させなければいけないので、
 * ヘルパークラスとして作成
 */
public class VertexBindingBuilder {
	private List<VertexBinding> bindings = new ArrayList<>();
	private int allBytes = 0;

	private VertexBindingBuilder() {
	}
	
	public static VertexBindingBuilder create(VertexBinding binding) {
		var instance = new VertexBindingBuilder();
		instance.add(binding);
		return instance;
	}
	
	public VertexBindingBuilder add(VertexBinding binding) {
		bindings.add(binding);
		allBytes += binding.getBytes();
		return this;
	}
	
	/**
	 * StagingBufferSettings用のバッファへのコピー方法を作成
	 * @return
	 */
	public Consumer<PointerBuffer> createCopy() {
		return (buffer) -> {
			var vertexBuffer = buffer.getFloatBuffer(0, allBytes);
			for(var binding: bindings) {
				vertexBuffer.put(binding.getValues());	
			}
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
       .stride(allBytes)
       .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
	}
	
	/**
	 * 頂点の送り方（formatなど）
	 * @param stack
	 * @return
	 */
	public VkVertexInputAttributeDescription.Buffer createAttribute(MemoryStack stack) {
		var vertexAttribute = VkVertexInputAttributeDescription.calloc(bindings.size(), stack);
		
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/4ef1eebe4af235b2934a165e82aeefcaf8d9b893/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L754-L771
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		int offset = 0;
		for(int i = 0; i < bindings.size(); ++i) {
			var binding = bindings.get(i);
			vertexAttribute.get(i)
	            .location(i)
	            .format(binding.getFormat())
	            .offset(offset);
			
			// 1bindingのバイト数分offsetが必要
			offset += binding.getBytes();
		}
		return vertexAttribute;
	}

	public int getAllBytes() {
		return allBytes;
	}
}
