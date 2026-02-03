package lwjgl.ex.vulkan;


import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;

//参考
//https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java

import static org.lwjgl.vulkan.VK14.*;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandBufferSubmitInfo;
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
    public void record(Command command, MemoryStack stack, SwapChain swapChain) {        
        var info = VkCommandBufferBeginInfo.calloc(stack).sType$Default();
        
        // CommandBufferの使用法を決定
        // （VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BITなど）
        info.flags(settings.getUsageBit());
        
        // todo secondaryの場合の実装
        // https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java
//            if (!primary) {
            
        Vulkan.throwExceptionIfFailed(vkBeginCommandBuffer(buffer, info), "CommandBufferの開始に失敗しました");
        try {
        	// 描画
        	VkRenderingInfo renderingInfo = command.render(stack, swapChain); 
    		vkCmdBeginRendering(buffer, renderingInfo);
        }
        finally {
        	
        	// vkEndCommandBuffer(): It is invalid to issue this call inside an active VkRenderPass 0x0.

        	
        	Vulkan.throwExceptionIfFailed(vkEndCommandBuffer(buffer), "CommandBufferの終了に失敗しました");            	
        }
    }
    
    public VkCommandBufferSubmitInfo.Buffer createSubmitInfoBuffer(MemoryStack stack) {
    	return VkCommandBufferSubmitInfo.calloc(1, stack)
        .sType$Default()
        .commandBuffer(buffer);
    }
	
	@Override
	public void close() throws Exception {
		vkFreeCommandBuffers(settings.getCommandPool().getSettings().getLogicalDevice().getDevice(), settings.getCommandPool().getHandler(), buffer);
	}

}
