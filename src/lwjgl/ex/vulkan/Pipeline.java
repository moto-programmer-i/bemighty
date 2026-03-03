package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/Pipeline.java


import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;

import static org.lwjgl.vulkan.VK14.*;

public class Pipeline implements AutoCloseable {
	private PipelineSettings settings;
	private PipelineCache cache;
	private long handler;
    private long layoutHandler;

	public Pipeline(PipelineSettings settings) {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
			var vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default();

            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default()
                    .topology(settings.getTopology());

            var viewport = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType$Default()
                    // 1以外になる場合がでたら対処
                    .viewportCount(1)
                    .scissorCount(1);

            var rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default()
                    .polygonMode(settings.getPolygonMode())
                    .cullMode(settings.getCullMode())
                    .frontFace(settings.getFrontFace())
                    .lineWidth(settings.getLineWidth());

            var multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default()
                    .rasterizationSamples(settings.getRasterizationSamples());

            var dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                    .pDynamicStates(stack.ints(
                    		// これ以外になる場合がでたら対処
                            VK_DYNAMIC_STATE_VIEWPORT,
                            VK_DYNAMIC_STATE_SCISSOR
                    ));

            var blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(settings.getColorWriteMask())
                    .blendEnable(settings.isBlendEnable());
            var colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
                    .pAttachments(blendAttachment);

            IntBuffer colorFormats = stack.mallocInt(1);
            colorFormats.put(0, settings.getColorFormat());
            var rendCreateInfo = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
                    // 1以外になる場合がでたら対処
                    .colorAttachmentCount(1)
                    .pColorAttachmentFormats(colorFormats);

            var layout = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default();

            LongBuffer forLayout = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreatePipelineLayout(settings.getLogicalDevice().getDevice(), layout, null, forLayout),
                    "PipelineLayoutの作成に失敗しました");
            layoutHandler = forLayout.get(0);

            var createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType$Default()
                    .renderPass(VK_NULL_HANDLE)
                    
                    // shaderが複数になった場合の対処法不明
                    .pStages(settings.getShader().createStageBuffer(stack))
                    
                    
                    .pVertexInputState(vertexInput)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewport)
                    .pRasterizationState(rasterization)
                    .pColorBlendState(colorBlend)
                    .pMultisampleState(multisample)
                    .pDynamicState(dynamic)
                    .layout(layoutHandler)
                    .pNext(rendCreateInfo);

    		cache = new PipelineCache(settings.getLogicalDevice());
            
            LongBuffer forHandler = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateGraphicsPipelines(settings.getLogicalDevice().getDevice(), cache.getHandler(), createInfo, null, forHandler),
                    "GraphicsPipelineの作成に失敗しました");
            handler = forHandler.get(0);
        }
	}

	@Override
	public void close() throws Exception {
		if (handler != MemoryUtil.NULL) {
        	vkDestroyPipeline(settings.getLogicalDevice().getDevice(), handler, null);
        	handler = MemoryUtil.NULL;
		}
		try {
			if (cache != null) {
				cache.close();
				cache = null;
			}
		} finally {
			if (layoutHandler != MemoryUtil.NULL) {
				vkDestroyPipelineLayout(settings.getLogicalDevice().getDevice(), layoutHandler, null);
				layoutHandler = MemoryUtil.NULL;
			}
		}
        
        
        
	}

	public long getHandler() {
		return handler;
	}

	

	public PipelineSettings getSettings() {
		return settings;
	}
	
}
