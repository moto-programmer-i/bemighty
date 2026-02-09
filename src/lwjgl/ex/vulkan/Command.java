package lwjgl.ex.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkRenderingInfo;

@FunctionalInterface
public interface Command {
	public VkRenderingInfo render(MemoryStack stack, SwapChain swapChain, ImageView nextSwapChainImageView);
	
	// Render -> FrameRender -> CommandBuffer -> Command -> swapChain
}
