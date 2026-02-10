package lwjgl.ex.vulkan;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import static org.lwjgl.vulkan.VK14.*;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/Fence.java

//// Fence仕様確認
//long timeoutNanoseconds = 2000_000_000;
//try(var fence0 = new Fence(logicalDevice)) {
//	Runnable r = () -> {
//		try(var stack = MemoryStack.stackPush()) {
//			Thread.sleep(timeoutNanoseconds / 2000_000);
//			var submitInfo = VkSubmitInfo2.calloc(1, stack)
//    				.sType$Default();
//			Vulkan.throwExceptionIfFailed(vkQueueSubmit2(queue.getVkQueue(), submitInfo, fence0.getHandler()), "Queueへのコマンドの送信に失敗しました");
//			System.out.println("signal");
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}									
//	};
//	r.run();
//	fence0.startWaiting(timeoutNanoseconds);
//	System.out.println(fence0);
//}


/**
 * CPU側の同期のための待機機構
 * https://chaosplant.tech/do/vulkan/ex2/
 */
public class Fence implements AutoCloseable {
	public static final long DEFAULT_TIMEOUT_NANOSECONDS = Long.MAX_VALUE;
	private long handler;
	private final LogicalDevice logicalDevice;
	
	public Fence(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		try (var stack = MemoryStack.stackPush()) {
            var info = VkFenceCreateInfo.calloc(stack)
                    .sType$Default();
            		// 最初からsignaled、待機終了にしたいときもあるらしいが不明なので保留
                    // .flags(signaled ? VK_FENCE_CREATE_SIGNALED_BIT : 0);

            LongBuffer lp = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateFence(logicalDevice.getDevice(), info, null, lp), "Fenceの作成に失敗しました");
            handler = lp.get(0);
        }
	}

	/**
	 * 同期待ちに入る（waitがObjectのメソッドであり、オーバーライドになってしまう）
	 * startWaitingでCPU同期待ちに入り、
	 * 別スレッドでvkQueueSubmit2などでsignal状態になると終了する
	 */
	public void startWaiting() {
		startWaiting(DEFAULT_TIMEOUT_NANOSECONDS);
	}
	public void startWaiting(long timeoutNanoseconds) {
        // 引数waitAllは、LWJGLの設計ミス。本来複数ハンドラを送る場合に、1つでも完了したら待機終了とするときに使う
        Vulkan.throwExceptionIfFailed(vkWaitForFences(logicalDevice.getDevice(), handler, true, timeoutNanoseconds)
        		, "Fenceの待機開始に失敗しました");
	}
	
	public void reset() {
		Vulkan.throwExceptionIfFailed(vkResetFences(logicalDevice.getDevice(), handler)
				, "Fenceのリセットに失敗しました");
	}
	
	public void waitAndReset() {
		// 同期待ちが終了したら、リセットして戻す
		startWaiting();
		reset();
	}

	@Override
	public void close() throws Exception {
		if (handler == VK_NULL_HANDLE) {
			return;
		}
		vkDestroyFence(logicalDevice.getDevice(), handler, null);
		handler = VK_NULL_HANDLE;
	}

	public long getHandler() {
		return handler;
	}
}
