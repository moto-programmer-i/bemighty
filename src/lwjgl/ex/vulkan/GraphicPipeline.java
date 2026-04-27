//package lwjgl.ex.vulkan;
//
//// 参考
//// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/Pipeline.java
//
//
//import java.nio.ByteBuffer;
//import java.nio.IntBuffer;
//import java.nio.LongBuffer;
//
//import org.lwjgl.system.MemoryStack;
//import org.lwjgl.system.MemoryUtil;
//import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
//import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
//import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
//import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
//import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
//import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
//import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
//import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
//import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
//import org.lwjgl.vulkan.VkVertexInputBindingDescription;
//
//import static org.lwjgl.vulkan.VK14.*;
//import static lwjgl.ex.vulkan.VulkanConstants.*;
//
//public class GraphicPipeline implements AutoCloseable {
//	private PipelineSettings settings;
//	private PipelineCache cache;
//	private long handler;
//    private long layoutHandler;
//    
//    private long computeHandler = MemoryUtil.NULL;
//
//	public GraphicPipeline(PipelineSettings settings) {
//		this.settings = settings;
//		try (var stack = MemoryStack.stackPush()) {
//			var vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
//					.pVertexBindingDescriptions(settings.getDescriptionHelper().createBinding(stack))
//					.pVertexAttributeDescriptions(settings.getDescriptionHelper().createAttribute(stack))
//					;
//
//            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default()
//                    .topology(settings.getTopology())
//                    // https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineInputAssemblyStateCreateInfo.html
//                    .primitiveRestartEnable(false);
//
//            var viewport = VkPipelineViewportStateCreateInfo.calloc(stack)
//                    .sType$Default()
//                    // 1以外になる場合がでたら対処
//                    .viewportCount(1)
//                    .scissorCount(1);
//
//            var rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default()
//            		.depthClampEnable(false)
//         		    .rasterizerDiscardEnable(false)
//                    .polygonMode(settings.getPolygonMode())
//                    .cullMode(settings.getCullMode())
//                    .frontFace(settings.getFrontFace())
//                    .lineWidth(settings.getLineWidth());
//
//            var multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default()
//                    .rasterizationSamples(settings.getLogicalDevice().getMsaaSamples())
//                    .sampleShadingEnable(false);
//            
//            var depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default()
//    		    .depthTestEnable(true)
//    		    .depthWriteEnable(true)
//    		    .depthCompareOp(VK_COMPARE_OP_LESS)
//    		    .depthBoundsTestEnable(false)
//    		    .stencilTestEnable(false);
//
//            var dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
//                    .pDynamicStates(stack.ints(
//                    		// これ以外になる場合がでたら対処
//                            VK_DYNAMIC_STATE_VIEWPORT,
//                            VK_DYNAMIC_STATE_SCISSOR
//                    ));
//
//            var blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
//                    .colorWriteMask(settings.getColorWriteMask())
//                    .blendEnable(settings.isBlendEnable());
//            var colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
//            		.logicOpEnable(false)
//         		    .logicOp(VK_LOGIC_OP_COPY)
//                    .pAttachments(blendAttachment);
//
//            IntBuffer colorFormats = stack.mallocInt(1);
//            colorFormats.put(0, settings.getColorFormat());
//            var depthFormat = settings.getLogicalDevice().getPhysicalDevice().findDepthFormat();
//            var rendCreateInfo = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
//                    // 1以外になる場合がでたら対処
//                    .colorAttachmentCount(1)
//                    .pColorAttachmentFormats(colorFormats)
//                    .depthAttachmentFormat(depthFormat)
//                    ;
//
//            var layout = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
//            		.pSetLayouts(settings.getDescriptionHelper().getForLayouts());
//
//            LongBuffer forLayout = stack.mallocLong(1);
//            Vulkan.throwExceptionIfFailed(vkCreatePipelineLayout(settings.getLogicalDevice().getDevice(), layout, null, forLayout),
//                    "PipelineLayoutの作成に失敗しました");
//            layoutHandler = forLayout.get(0);
//
//            var shader = settings.getShader();
//            var createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
//                    .sType$Default()
//                    .renderPass(VK_NULL_HANDLE)
//                    
//                    .stageCount(shader.getStageCount())
//                    // shaderが複数になった場合の対処法不明
//                    .pStages(shader.createStageBuffer(stack))
//                    
//                    
//                    .pVertexInputState(vertexInput)
//                    .pInputAssemblyState(inputAssembly)
//                    .pViewportState(viewport)
//                    .pRasterizationState(rasterization)
//                    .pColorBlendState(colorBlend)
//                    .pMultisampleState(multisample)
//                    .pDepthStencilState(depthStencil)
//                    .pDynamicState(dynamic)
//                    .layout(layoutHandler)
//                    .pNext(rendCreateInfo)
//                    ;
//
//    		cache = new PipelineCache(settings.getLogicalDevice());
//            
//            LongBuffer forHandler = stack.mallocLong(1);
//            Vulkan.throwExceptionIfFailed(vkCreateGraphicsPipelines(settings.getLogicalDevice().getDevice(), cache.getHandler(), createInfo, null, forHandler),
//                    "GraphicsPipelineの作成に失敗しました");
//            handler = forHandler.get(0);
//            
//            
//            // Compute用のPipelineは別で必要
//            if(shader.hasCompute()) {
//            	
//        		var computeInfo = VkComputePipelineCreateInfo.calloc(1, stack).sType$Default()
//        				.stage(shader.createComputeStageBuffer(stack).get())
//        				.layout(layoutHandler);
//        		
//        		LongBuffer forComputeHandler = stack.mallocLong(1);
//                Vulkan.throwExceptionIfFailed(vkCreateComputePipelines(settings.getLogicalDevice().getDevice(), cache.getHandler(), computeInfo, null, forComputeHandler),
//                        "Compute Shader用のGraphicsPipelineの作成に失敗しました");
//                computeHandler = forHandler.get(0);
//            }
//        }
//	}
//
//	@Override
//	public void close() throws Exception {
//		if (handler != MemoryUtil.NULL) {
//        	vkDestroyPipeline(settings.getLogicalDevice().getDevice(), handler, null);
//        	handler = MemoryUtil.NULL;
//		}
//		try {
//			if (cache != null) {
//				cache.close();
//				cache = null;
//			}
//		} finally {
//			if (layoutHandler != MemoryUtil.NULL) {
//				vkDestroyPipelineLayout(settings.getLogicalDevice().getDevice(), layoutHandler, null);
//				layoutHandler = MemoryUtil.NULL;
//			}
//		}
//        
//        
//        
//	}
//
//	public long getHandler() {
//		return handler;
//	}
//
//	public PipelineSettings getSettings() {
//		return settings;
//	}
//
//	public long getLayoutHandler() {
//		return layoutHandler;
//	}
//	
//	public DescriptionHelper getDescriptionHelper() {
//		return settings.getDescriptionHelper();
//	}
//
//	public long getComputeHandler() {
//		return computeHandler;
//	}
//	
//}
