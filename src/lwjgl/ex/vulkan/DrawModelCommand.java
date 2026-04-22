package lwjgl.ex.vulkan;

import java.awt.Color;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK14.*;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class DrawModelCommand implements Command, AutoCloseable {
	public static final long START_BARRIER_STAGE = VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT;
	private int instanceCount = 1;
	private int firstIndex = DEFAULT_FIRST_INDEX;
	private int vertexOffset = DEFAULT_INT_OFFSETS;
	private int firstInstance = DEFAULT_FIRST_INSTANCE;
	
	private float depth = 1.0f;
	
	private final VkImageMemoryBarrier2.Buffer depthBarrier = ImageViewSettings.createDefaultBarrier()
		.newLayout(VK_IMAGE_LAYOUT_DEPTH_ATTACHMENT_OPTIMAL)

		// チュートリアルでは同じだが、なぜこれでいいのかは不明
		.srcAccessMask(VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)
		.dstAccessMask(VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)

		// チュートリアルでは同じだが、なぜこれでいいのかは不明
		.srcStageMask(START_BARRIER_STAGE)
		.dstStageMask(START_BARRIER_STAGE)
		.subresourceRange(ImageViewSettings.DEPTH_SUBRESOURCE_RANGE);


	private Model model;
	private SwapChain swapChain;
	private final ClearColorCommand clearColor;
	private Pipeline pipeline;
	private final VkRenderingAttachmentInfo depthAttachment;

	public DrawModelCommand(Model model, Color background, SwapChain swapChain, Pipeline pipeline) {
		this.model = model;
		this.swapChain = swapChain;
		this.clearColor = new ClearColorCommand(background, swapChain);
		this.pipeline = pipeline;
		
		// SwapChain壊れたときの対応とかは保留
		depthBarrier.image(swapChain.getDepthImageView().getImageHandler());
		
		try(var stack = MemoryStack.stackPush()) {
			depthAttachment = VkRenderingAttachmentInfo.create().sType$Default()
					.imageLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL)
					.imageView(swapChain.getDepthImageView().getHandler())
		            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
		            .storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
		            .clearValue(
		            		VkClearValue.calloc(stack).depthStencil(
		            				VkClearDepthStencilValue.calloc(stack).depth(depth)
		            				));
			clearColor.getRenderingInfo().pDepthAttachment(depthAttachment);
		}
		
	}
	
	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, ImageView nextSwapChainImageView, CommandBuffer computeCommandBuffer) {
		
		// transitionまでは、ClearColorと共通
		clearColor.run(stack, commandBuffer, nextSwapChainImageView, () -> {
			
			commandBuffer.transitionImageLayout(depthBarrier);	
			commandBuffer.render(clearColor.getRenderingInfo(), () -> {
				commandBuffer.bindGraphics(pipeline);
				commandBuffer.setViewportFrom(swapChain, stack);
				commandBuffer.setScissorFrom(swapChain, stack);
				commandBuffer.bind(model);
				commandBuffer.drawIndexed(model.getIndices().length, instanceCount, firstIndex, vertexOffset, firstInstance);

			});
		});
	}

	@Override
	public void close() throws Exception {
		try (depthBarrier;clearColor;depthAttachment) {}
	}

}
