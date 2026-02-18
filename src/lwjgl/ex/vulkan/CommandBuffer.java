package lwjgl.ex.vulkan;



//参考
//https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java

import static org.lwjgl.vulkan.VK14.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
import org.lwjgl.vulkan.VkDependencyInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkRenderingInfo;

public class CommandBuffer implements AutoCloseable {
	private final CommandBufferSettings settings;
	private VkCommandBuffer buffer;
	public CommandBuffer(CommandBufferSettings settings) {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
            var info = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType$Default()
                    .commandPool(settings.getCommandPool().getHandler())
                    .level(settings.isPrimary() ? VK_COMMAND_BUFFER_LEVEL_PRIMARY : VK_COMMAND_BUFFER_LEVEL_SECONDARY)
                    .commandBufferCount(settings.getCount());
            PointerBuffer pb = stack.mallocPointer(1);
            Vulkan.throwExceptionIfFailed(vkAllocateCommandBuffers(settings.getCommandPool().getSettings().getLogicalDevice().getDevice(), info, pb),
                    "CommandBufferの作成に失敗しました");

            buffer = new VkCommandBuffer(pb.get(0), settings.getCommandPool().getSettings().getLogicalDevice().getDevice());
        }
	}
	
	
	/**
	 * 描画の情報をcommandとして記録
	 * @param stack
	 * @param command 描画内容
	 */
    public void record(Command command, MemoryStack stack, SwapChain swapChain, ImageView nextSwapChainImageView) {    	
        var beginInfo = VkCommandBufferBeginInfo.calloc(stack)
        		.sType$Default()
        		// CommandBufferの使用法を決定
                // （VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BITなど）
        		.flags(settings.getUsageBit());
        		;
        
        // todo secondaryの場合の実装
        // https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java
//            if (!primary) {
        
        
        Vulkan.throwExceptionIfFailed(vkBeginCommandBuffer(buffer, beginInfo), "CommandBufferの開始に失敗しました");
    	try {
    		command.run(stack, this, swapChain, nextSwapChainImageView);
        }
        finally {        	
        	Vulkan.throwExceptionIfFailed(vkEndCommandBuffer(buffer), "CommandBufferの終了に失敗しました");            	
        }
    }
    
    /**
     * Renderingを開始して終了する。
     * vkBeginCommandBufferとvkEndCommandBufferで挟まなければ実行不可
     * @param renderingInfo
     */
    // このメソッドがpublicなのはちょっと嫌だが、代案が思いつかない
    public void render(VkRenderingInfo renderingInfo) {
    	vkCmdBeginRendering(buffer, renderingInfo);
		vkCmdEndRendering(buffer);
    }
    
    public VkCommandBufferSubmitInfo.Buffer createSubmitInfoBuffer(MemoryStack stack) {
    	return VkCommandBufferSubmitInfo.calloc(1, stack)
        .sType$Default()
        .commandBuffer(buffer);
    }
    
    public void reset() {
    	Vulkan.throwExceptionIfFailed(vkResetCommandBuffer(buffer, VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT),
    			"CommandBufferのリセットに失敗しました");
    }
	
	@Override
	public void close() throws Exception {
		// commandPoolでcloseされるらしい
//		vkFreeCommandBuffers(settings.getCommandPool().getSettings().getLogicalDevice().getDevice(), settings.getCommandPool().getHandler(), buffer);
		buffer = null;
	}	
	
	/**
	 * レイアウトの変更。描画処理の前後で呼ばなければいけない
	 * 例：
	 * transitionImageLayout(
	 * vkCmdBeginRendering(
	 * vkCmdEndRendering(
	 * transitionImageLayout(
	 * 
	 * https://docs.vulkan.org/refpages/latest/refpages/source/VkImageMemoryBarrier2.html
	 * https://chaosplant.tech/do/vulkan/6-2/
	 * @param swapChainImageView
	 * @param oldLayout
	 * @param newLayout
	 * @param srcAccessMask リソースに行うアクセス処理
	 * @param dstAccessMask
	 * @param srcStage 同期用パイプラインステージ
	 * @param dstStage
	 * @param stack
	 */
	public void transitionImageLayout(
			ImageView swapChainImageView,
	        int oldLayout,
	        int newLayout,
	        long srcAccessMask,
	        long dstAccessMask,
	        long srcStage,
	        long dstStage,
	        MemoryStack stack
			) {
		// 参考
		// https://chaosplant.tech/do/vulkan/6-2/
		// https://github.com/LWJGL/lwjgl3/blob/6c89bd4e861407f243305fc84d60ca8d82fe9dd4/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L1073C5-L1073C49
		var subresourceRange = ImageViewSettings.createDefaultSubresourceRange(stack);

        var imageBarrier = VkImageMemoryBarrier2.calloc(1, stack)
            .sType$Default()
            .srcStageMask(srcStage)
            .srcAccessMask(srcAccessMask)
            .dstStageMask(dstStage)
            .dstAccessMask(dstAccessMask)
            .oldLayout(oldLayout)
            .newLayout(newLayout)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .image(swapChainImageView.getImageHandler())
            .subresourceRange(subresourceRange);

        VkDependencyInfo dependencyInfo = VkDependencyInfo.calloc(stack)
            .sType$Default()
            .pImageMemoryBarriers(imageBarrier);

        vkCmdPipelineBarrier2(buffer, dependencyInfo);
	}


	public VkCommandBuffer getBuffer() {
		return buffer;
	}
}
