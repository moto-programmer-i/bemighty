package lwjgl.ex.vulkan;


import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;

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
	private final VkCommandBuffer buffer;
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
        var beginInfo = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
        
        // CommandBufferの使用法を決定
        // （VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BITなど）
        beginInfo.flags(settings.getUsageBit());
        
        // todo secondaryの場合の実装
        // https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java
//            if (!primary) {
        
        
        Vulkan.throwExceptionIfFailed(vkBeginCommandBuffer(buffer, beginInfo), "CommandBufferの開始に失敗しました");
    	try {
    		VkRenderingInfo renderingInfo = command.render(stack, swapChain, nextSwapChainImageView);
    		// 暫定でtransitionColor固定。将来的にはインターフェースをはさむ？
    		transitionColor(nextSwapChainImageView, stack, () -> {
    			vkCmdBeginRendering(buffer, renderingInfo);
        		vkCmdEndRendering(buffer);
    		});
        }
        finally {        	
        	Vulkan.throwExceptionIfFailed(vkEndCommandBuffer(buffer), "CommandBufferの終了に失敗しました");            	
        }
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
		vkFreeCommandBuffers(settings.getCommandPool().getSettings().getLogicalDevice().getDevice(), settings.getCommandPool().getHandler(), buffer);
	}
	
	private void transitionColor(ImageView swapChainImageView, MemoryStack stack, Runnable rendering) {
		// IMAGE_USAGE_COLOR_ATTACHMENT_BITが有効なときはこれにしなければならない
		var colorLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
		
		// 書き込みを指定
		var writeAccessMask = VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT;
		
		// 色が出力されるステージへ
		var colorOutputStage = VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT;
		
		// https://github.com/LWJGL/lwjgl3/blob/6c89bd4e861407f243305fc84d60ca8d82fe9dd4/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L943
		transitionImageLayout(swapChainImageView,
				// 描画前なのでレイアウトは未定義
                VK_IMAGE_LAYOUT_UNDEFINED,
                
                colorLayout,
                
                
                // 依存関係なし
                VK_ACCESS_2_NONE,
                // 書き込みを行う
                writeAccessMask,
                
                // 何も待たない
                VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT,
                
                
                colorOutputStage,
                stack);
		
		rendering.run();
		
		transitionImageLayout(
				swapChainImageView,
				
				
				colorLayout,
				// 表示状態へ
				KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
				
				
				// アクセスを元に戻す
				writeAccessMask,
				// その後、処理をしない場合は何も待たなくてよい
				// （シェーダーの読み込みを待つ場合は、VK_ACCESS_2_SHADER_READ_BITを指定することもあるらしい）
				VK_ACCESS_2_NONE,
	            
				
				colorOutputStage,
	            // 同期のスコープの終了時まで
	            VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
	            stack
	        );

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
	private void transitionImageLayout(
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
}
