package lwjgl.ex.vulkan;


import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK14.*;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreTypeCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreWaitInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkTimelineSemaphoreSubmitInfo;
import static lwjgl.ex.vulkan.VulkanConstants.*;

public class TimelineSemaphore implements AutoCloseable {
	private final LogicalDevice logicalDevice;

//	private long handler;
	private LongBuffer forHandler = MemoryUtil.memAllocLong(1);
	
	// TimelineSemaphoreSubmitInfoに使用
	private long timeline = 0;
	private LongBuffer forTimeline = MemoryUtil.memAllocLong(1);
	
	public TimelineSemaphore(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		try (var stack = MemoryStack.stackPush()) {
            var type = VkSemaphoreTypeCreateInfo.calloc(stack).sType$Default()
            		.semaphoreType(VK_SEMAPHORE_TYPE_TIMELINE);
            var info = VkSemaphoreCreateInfo.calloc(stack).sType$Default()
            		.pNext(type);

            Vulkan.throwExceptionIfFailed(vkCreateSemaphore(logicalDevice.getDevice(), info, null, forHandler),
                    "Semaphoreの作成に失敗しました");
//            handler = forHandler.get(0);
        }
	}
	
	// TimelineSemaphore内部の値を0にしなければ行けないが、方法が不明
//	public void clear() {
//		// チュートリアルだと0に戻さず、uintを++し続けるが、大丈夫なのか不明
//		timeline = 0;
//	}
	
	
	public void submit(CommandBuffer commandBuffer, Queue queue, IntBuffer waitDestinationStageMask, MemoryStack stack) {
		// LWJGLの設計ミスによりBufferを用意しなければならない
		var waitSemaphoreValue = stack.mallocLong(1);
		var signalSemaphoreValue = stack.mallocLong(1);
		
		// timelineの順番通りに設定（前のsignal = 次のwait）
		waitSemaphoreValue.put(FIRST_INDEX, timeline);
		signalSemaphoreValue.put(FIRST_INDEX, ++timeline);
		var timelineInfo = VkTimelineSemaphoreSubmitInfo.calloc(stack).sType$Default()
				// 1以外の場合があるのか不明
				.waitSemaphoreValueCount(1)
				.signalSemaphoreValueCount(1)
				
				.pWaitSemaphoreValues(waitSemaphoreValue)
				.pSignalSemaphoreValues(signalSemaphoreValue);
		
		// Vulkanの設計ミスにより、Timeline semaphore用のSubmitInfoがないので、
		// Binary semaphore用の形式で送らなければならない
		var submitInfo = VkSubmitInfo.calloc(stack).sType$Default()
				.pNext(timelineInfo)
				.pWaitSemaphores(forHandler)
				.pWaitDstStageMask(waitDestinationStageMask)
				.pCommandBuffers(commandBuffer.getPointer())
				.pSignalSemaphores(forHandler);
		
		// Vulkanの設計ミスにより、Fenceなしのオーバーロードがない
		Vulkan.throwExceptionIfFailed(vkQueueSubmit(queue.getVkQueue(), submitInfo, MemoryUtil.NULL), "TimelineSemaphoreがQueueへのコマンドの送信に失敗しました");
	}

	@Override
	public void close() throws Exception {
		if (forHandler == null) {
			return;
		}
		vkDestroySemaphore(logicalDevice.getDevice(), forHandler.get(0), null);
		MemoryUtil.memFree(forTimeline);
		forTimeline = null;
		MemoryUtil.memFree(forHandler);
		forHandler = null; 
	}

	/**
	 * 
	 * （waitだとObject.waitと関連があるように見えるため）
	 * @param stack
	 */
	public void waitSemaphore(MemoryStack stack) {
		// 現在のtimelineの値で待つ
		forTimeline.put(FIRST_INDEX, timeline);
		
		var info = VkSemaphoreWaitInfo.calloc(stack).sType$Default()
				// 1以外の場合があるのか不明
				.semaphoreCount(1)
				
				.pSemaphores(forHandler)
				.pValues(forTimeline)
				;
		
		Vulkan.throwExceptionIfFailed(vkWaitSemaphores(logicalDevice.getDevice(), info, Long.MAX_VALUE),
				"vkWaitSemaphoresに失敗しました");
	}
}
