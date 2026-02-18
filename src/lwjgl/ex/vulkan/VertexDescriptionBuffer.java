package lwjgl.ex.vulkan;

import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK14.*;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/scn/VtxBuffStruct.java

/**
 * 頂点の形式を保存するバッファ
 * 把握次第設計変更
 */
public class VertexDescriptionBuffer implements AutoCloseable {
	private VertexDescriptionBufferSettings settings;
	
	private final VkVertexInputAttributeDescription.Buffer attribute;
	private final VkVertexInputBindingDescription.Buffer binding;
	private final VkPipelineVertexInputStateCreateInfo inputState;

	public VertexDescriptionBuffer(VertexDescriptionBufferSettings settings) {
		this.settings = settings;
		attribute = VkVertexInputAttributeDescription.calloc(settings.getAttributeCapacity());
		binding = VkVertexInputBindingDescription.calloc(1);
		inputState = VkPipelineVertexInputStateCreateInfo.calloc();

		int bytesOfVertex = settings.getNumberOfValues() * settings.getBytesOfValue();
		
		for(int i = 0; i < settings.getAttributeCapacity(); ++i) {
			// 参考
			// https://github.com/LWJGL/lwjgl3/blob/8d12523d40890a78eb11673ce26732a9125971a4/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L761
			// getとlocation両方必要なのはVulkanの設計ミス？
			attribute.get(i).location(i).binding(0)
			// フォーマットをそれぞれ用意する必要があれば設計変更
			.format(settings.getFormat())
			.offset(i * bytesOfVertex)
			;
		}

		// 参考
		// https://docs.vulkan.org/guide/latest/vertex_input_data_processing.html
		// bindingが複数必要なのかは不明
		binding.get(0).binding(0)
			.stride(bytesOfVertex)
			.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

		inputState.sType$Default()
			.pVertexBindingDescriptions(binding)
			.pVertexAttributeDescriptions(attribute);
	}

	@Override
	public void close() throws Exception {
		if (settings == null) {
			return;
		}
		try(attribute;binding;inputState){}
		settings = null;
	}

    public VkPipelineVertexInputStateCreateInfo getInputState() {
        return inputState;
    }

}
