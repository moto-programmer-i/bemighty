package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;

@FunctionalInterface
public interface Command {
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain, ImageView nextSwapChainImageView);
}
