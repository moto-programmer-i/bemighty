package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkComputePipelineCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDepthStencilStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineDynamicStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRenderingCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

public class Pipeline implements AutoCloseable {
	private LongBuffer forDescriptorPool;
	private LongBuffer forDescriptorSet;
	private LongBuffer forDescriptorLayouts;
	
	private List<Descriptor> descriptorList;
	
	
	private long handler;
    private long layoutHandler;
    
    private LogicalDevice logicalDevice;
    private PipelineSettings settings;
    

    /**
     * ComputePipelineとして初期化する
     */
    private void initAsCompute() {
    	var device = logicalDevice.getDevice();
    	
    	try(var stack = MemoryStack.stackPush()) {
    		VkPipelineLayoutCreateInfo layout = createLayoutInfo(stack);
    		
    		var forLayout = stack.mallocLong(1);
    		Vulkan.throwExceptionIfFailed(vkCreatePipelineLayout(device, layout, null, forLayout),
                    "ComputePipelineLayoutの作成に失敗しました");
    		layoutHandler = forLayout.get(0);
    		
    		var compute = VkComputePipelineCreateInfo.calloc(1, stack).sType$Default()
    				.layout(layoutHandler);
    		settings.write(compute, stack);
        	
        	var forHandler = stack.mallocLong(1);
        	Vulkan.throwExceptionIfFailed(vkCreateComputePipelines(device,
        			// Vulkanのクソ設計によりcacheが必要
        			// （C++版だと恐らく内部生成される）
        			logicalDevice.getPipelineCache().getHandler(),
		        	compute,
		        	null,
		        	forHandler),
        			"ComputePipelinesの作成に失敗しました");
        	handler = forHandler.get(0);
    	}
    }
    
    
    public static Pipeline createCompute(PipelineSettings settings) {
    	var compute = new Pipeline(settings);
    	
    	// Descriptor周りを初期化
    	// （今までGraphicPipelineでやっていたのに、Computeでこっちに移った。意味不明）
    	compute.initDescriptor(settings);
    	
    	compute.initAsCompute();
    	return compute;
    }
    
    public static Pipeline createGraphics(PipelineSettings settings, GraphicPipelineSettings graphicSettings, VertexBindingBuilder binding) {
    	var grachics = new Pipeline(settings);
    	grachics.initAsGraphics(graphicSettings, binding);
    	return grachics;
    }
    
    private void initAsGraphics(GraphicPipelineSettings graphicSettings, VertexBindingBuilder binding) {
    	var device = logicalDevice.getDevice();
    	
    	try (var stack = MemoryStack.stackPush()) {
    		var vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType$Default()
					.pVertexBindingDescriptions(binding.createBinding(stack))
					.pVertexAttributeDescriptions(binding.createAttribute(stack));

            var inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType$Default()
                    .topology(graphicSettings.getTopology())
                    // https://docs.vulkan.org/refpages/latest/refpages/source/VkPipelineInputAssemblyStateCreateInfo.html
                    .primitiveRestartEnable(false);

            var viewport = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType$Default()
                    // 1以外になる場合がでたら対処
                    .viewportCount(1)
                    .scissorCount(1);

            var rasterization = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType$Default()
            		.depthClampEnable(false)
         		    .rasterizerDiscardEnable(false)
                    .polygonMode(graphicSettings.getPolygonMode())
                    .cullMode(graphicSettings.getCullMode())
                    .frontFace(graphicSettings.getFrontFace())
                    .lineWidth(graphicSettings.getLineWidth());

            var multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType$Default()
                    .rasterizationSamples(logicalDevice.getMsaaSamples())
                    .sampleShadingEnable(false);
            
            var depthStencil = VkPipelineDepthStencilStateCreateInfo.calloc(stack).sType$Default()
    		    .depthTestEnable(true)
    		    .depthWriteEnable(true)
    		    .depthCompareOp(VK_COMPARE_OP_LESS)
    		    .depthBoundsTestEnable(false)
    		    .stencilTestEnable(false);

            var dynamic = VkPipelineDynamicStateCreateInfo.calloc(stack).sType$Default()
                    .pDynamicStates(stack.ints(
                    		// これ以外になる場合がでたら対処
                            VK_DYNAMIC_STATE_VIEWPORT,
                            VK_DYNAMIC_STATE_SCISSOR
                    ));

            var blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
                    .colorWriteMask(graphicSettings.getColorWriteMask())
                    .blendEnable(graphicSettings.isBlendEnable());
            var colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType$Default()
            		.logicOpEnable(false)
         		    .logicOp(VK_LOGIC_OP_COPY)
                    .pAttachments(blendAttachment);

            var colorFormats = stack.mallocInt(1);
            colorFormats.put(0, graphicSettings.getColorFormat());
            var depthFormat = logicalDevice.getPhysicalDevice().findDepthFormat();
            var rendCreateInfo = VkPipelineRenderingCreateInfo.calloc(stack).sType$Default()
                    // 1以外になる場合がでたら対処
                    .colorAttachmentCount(1)
                    .pColorAttachmentFormats(colorFormats)
                    .depthAttachmentFormat(depthFormat)
                    ;

            
            var layout = VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
            		;
            		// ComputePipelineがでてきたら、GraphicPipelineDescriptorLayout設定がなくなった
            		// 意味不明
            		// https://docs.vulkan.org/tutorial/latest/_attachments/31_compute_shader.cpp
            		// .pSetLayouts(forDescriptorLayouts);
            var forLayout = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreatePipelineLayout(device, layout, null, forLayout),
                    "PipelineLayoutの作成に失敗しました");
            layoutHandler = forLayout.get(0);

            var shader = settings.getShader();
            var createInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType$Default()
                    .renderPass(VK_NULL_HANDLE)
                    .pVertexInputState(vertexInput)
                    .pInputAssemblyState(inputAssembly)
                    .pViewportState(viewport)
                    .pRasterizationState(rasterization)
                    .pColorBlendState(colorBlend)
                    .pMultisampleState(multisample)
                    .pDepthStencilState(depthStencil)
                    .pDynamicState(dynamic)
                    .layout(layoutHandler)
                    .pNext(rendCreateInfo)
                    
                    // 統一のため、下のsettings.writeで設定
//                  .stageCount(settings.getShaderStageSettingsList().size())
//                  .pStages(shader.createStageBuffer(stack))
                    ;
            
            settings.write(createInfo, stack);
            
            LongBuffer forHandler = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateGraphicsPipelines(device, logicalDevice.getPipelineCache().getHandler(), createInfo, null, forHandler),
                    "GraphicsPipelineの作成に失敗しました");
            handler = forHandler.get(0);
        }
    }
    
    private VkPipelineLayoutCreateInfo createLayoutInfo(MemoryStack stack) {
		return VkPipelineLayoutCreateInfo.calloc(stack).sType$Default()
				.pSetLayouts(forDescriptorLayouts);
	}
  
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    /**
	 * 共通の前処理
	 * （Descriptor周りを初期化）
	 * @param settings
	 */
    private Pipeline(PipelineSettings settings) {
    	this.settings = settings;
    	// バッファがlogicalDeviceを持っているので、先頭のものから取得
    	logicalDevice = settings.getBuffers()[0].getSettings().getLogicalDevice();
    }
    
    
    
	/**
	 * Descriptor周りを初期化
	 * @param settings
	 */
    private void initDescriptor(PipelineSettings settings) {
    	// 各バッファの初期化
    	forDescriptorPool = MemoryUtil.memAllocLong(1);
    	forDescriptorSet = MemoryUtil.memAllocLong(1);
    	forDescriptorLayouts = MemoryUtil.memAllocLong(1);
    	
		var device = logicalDevice.getDevice();
		
		// bufferをdescriptorのリストとして扱う
		descriptorList = Arrays.asList(settings.getBuffers());
		
		// Bufferだけなら簡単だったが、
		// Vulkanのクソ設計によりDescriptorの中にBufferと他が混在する
		if (settings.getSampler() != null) {
			descriptorList.add(settings.getSampler());
		}
				
		var descriptorTypeMap = new HashMap<Integer, Integer>();
		
		
		
		try(var stack = MemoryStack.stackPush()) {
			// 本来絶対にDescriptorPoolなどいらないが、Vulkanの制約上必須になっているので仕方ない
			// 事前にdescriptorTypeの種類ごとに数える必要がある
			// Integerのオートボクシングによってやや遅いが、問題がでたら考える
			for(var descriptor: descriptorList) {
				var descriptorType = descriptor.getDescriptorType();
				var descriptorTypeCount = descriptorTypeMap.get(descriptorType);
				// なければ1、あれば ++
				if(descriptorTypeCount == null) {
					descriptorTypeMap.put(descriptorType, 1);
				}
				else {
					descriptorTypeMap.put(descriptorType, ++descriptorTypeCount);
				}
			}
			
			var descriptorTypeEntrySet = descriptorTypeMap.entrySet();
			var poolSize = VkDescriptorPoolSize.calloc(descriptorTypeEntrySet.size(), stack);
			
			// Setにget(index)がないため仕方なく
			{
				int i = 0;
				for(var e: descriptorTypeEntrySet) {
					poolSize.get(i)
					.type(e.getKey())
					.descriptorCount(e.getValue());
					++i;
				};
			}
			
			var poolInfo = VkDescriptorPoolCreateInfo.calloc(stack).sType$Default()
					.flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
					// 不明。問題がでたら変更
					// https://docs.vulkan.org/refpages/latest/refpages/source/VkDescriptorPoolCreateInfo.html
					.maxSets(1)
					.pPoolSizes(poolSize);
			
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorPool(device, poolInfo, null, forDescriptorPool), "DescriptorPoolの作成に失敗しました");
			
			var bindings = VkDescriptorSetLayoutBinding.calloc(descriptorList.size(), stack);
			var descriptorSetBuffer = VkWriteDescriptorSet.calloc(descriptorList.size(), stack).sType$Default();
			for(int d = 0; d < descriptorList.size(); ++d) {
				var descriptor = descriptorList.get(d);
				descriptor.write(bindings.get(d), d);
			}
			
			var layout = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType$Default()
					.pBindings(bindings);
			Vulkan.throwExceptionIfFailed(vkCreateDescriptorSetLayout(device, layout, null, forDescriptorLayouts),
	                "DescriptorSetLayoutの作成に失敗しました");
			
			var allocate = VkDescriptorSetAllocateInfo.calloc(stack).sType$Default()
					.pSetLayouts(forDescriptorLayouts)
					.descriptorPool(forDescriptorPool.get(0));
			Vulkan.throwExceptionIfFailed(vkAllocateDescriptorSets(device, allocate, forDescriptorSet),
	                "DescriptorSetsの割り当てに失敗しました");
	        
	        // さらにDescriptorSetを設定しなければならない。意味不明。
	        for(int d = 0; d < descriptorList.size(); ++d) {
	        	descriptorList.get(d).write(descriptorSetBuffer.get(d), d, forDescriptorSet, stack);
	        }
	        
	        vkUpdateDescriptorSets(device, descriptorSetBuffer, null);
		}
    }
	
    @Override
	public void close() throws Exception {
    	var device = logicalDevice.getDevice();
		
		if (layoutHandler != MemoryUtil.NULL) {
			vkDestroyPipelineLayout(device, layoutHandler, null);
			layoutHandler = MemoryUtil.NULL;
		}
		
		if (forDescriptorLayouts != null) {
			vkDestroyDescriptorSetLayout(device, forDescriptorLayouts.get(0), null);
			forDescriptorLayouts.clear();
			forDescriptorLayouts = null;
		}
		
		
		if (forDescriptorPool != null) {
			// Poolを削除すればDescriptorSetも消える
			vkDestroyDescriptorPool(device, forDescriptorPool.get(0), null);
			forDescriptorPool.clear();
			forDescriptorPool = null;
		}
		
		if (handler != MemoryUtil.NULL) {
        	vkDestroyPipeline(device, handler, null);
        	handler = MemoryUtil.NULL;
		}
	}

	public long getHandler() {
		return handler;
	}

	public long getLayoutHandler() {
		return layoutHandler;
	}

	public LongBuffer getForDescriptorSet() {
		return forDescriptorSet;
	}

	public LongBuffer getForDescriptorLayouts() {
		return forDescriptorLayouts;
	}	
}
