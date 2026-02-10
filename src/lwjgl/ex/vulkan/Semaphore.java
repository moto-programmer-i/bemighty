package lwjgl.ex.vulkan;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreSubmitInfo;

import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK14.*;

//参考
//https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/Semaphore.java


/**
 * GPU側の同期のための待機機構
 * https://chaosplant.tech/do/vulkan/ex2/
 */
public class Semaphore implements AutoCloseable {
	public static final long DEFAULT_STAGE_MASK = VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT;
	private long handler;
	private final LogicalDevice logicalDevice;
	/**
	 * 本来enumにすべきだが、LWJGLの設計ミスによりint
	 * https://docs.vulkan.org/spec/latest/chapters/synchronization.html#VkPipelineStageFlagBits2 
	 */
	private long stageMask = DEFAULT_STAGE_MASK;
	
	public Semaphore(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		try (var stack = MemoryStack.stackPush()) {
            var info = VkSemaphoreCreateInfo.calloc(stack).sType$Default();

            LongBuffer lp = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateSemaphore(logicalDevice.getDevice(), info, null, lp),
                    "Semaphoreの作成に失敗しました");
            handler = lp.get(0);
        }
	}
	
	public VkSemaphoreSubmitInfo.Buffer createSubmitInfoBuffer(MemoryStack stack) {
		return VkSemaphoreSubmitInfo.calloc(1, stack)
		        .sType$Default()
		        .stageMask(stageMask)
		        .semaphore(handler);
	}
	
	public LongBuffer createLongBuffer(MemoryStack stack) {
		return stack.longs(handler);
	}

	public long getStageMask() {
		return stageMask;
	}

	public void setStageMask(long stageMask) {
		this.stageMask = stageMask;
	}

	@Override
	public void close() throws Exception {
		if (handler == VK_NULL_HANDLE) {
			return;
		}
		vkDestroySemaphore(logicalDevice.getDevice(), handler, null);
		handler = VK_NULL_HANDLE;
	}

	public long getHandler() {
		return handler;
	}

}
