package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

import java.nio.IntBuffer;
import java.util.Arrays;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo2;

import motopgi.utils.ExceptionUtils;


/**
 * 1フレームを描画するのに必要なクラス群
 */
public class FrameRender implements AutoCloseable {
	private final RenderSettings settings;
	private final Fence cpuSync;
	private final TimelineSemaphore semaphore;
	private final RecordInfo recordInfo;
	
	private IntBuffer computeWaitMask = MemoryUtil.memAllocInt(1);
	private IntBuffer graphicWaitMask = MemoryUtil.memAllocInt(1);

	public FrameRender(RenderSettings settings) {
		this.settings = settings;
		// AutoCloseableを変数として持つのでtry-with-resourcesができない
		cpuSync = new Fence(settings.getLogicalDevice());
		semaphore = new TimelineSemaphore(settings.getLogicalDevice());
		recordInfo = new RecordInfo(settings.getCommandBufferSettings());
		
		computeWaitMask.put(VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT);
		graphicWaitMask.put(VK_PIPELINE_STAGE_VERTEX_INPUT_BIT);
	}
	
	public void submit(MemoryStack stack, Command command) {
		// SwapChain再作成中は描画ができないので、何もしない
		if (settings.getSwapChain().isRecreating()) {
			return;
		}
		
		// 今までswapChainの取得をsemaphoreで同期していたが、fenceに変わっている
		// https://docs.vulkan.org/tutorial/latest/_attachments/31_compute_shader.cpp
		var nextSwapChainImageView = settings.getSwapChain().acquireNextImageView(stack, cpuSync);
		cpuSync.waitAndReset();
		
		// 1フレームの描画に必要な情報を設定
		recordInfo.setFrame(stack, nextSwapChainImageView);
		
		command.run(recordInfo);
		
		// computeをrecordしていればsubmit
		var compute = recordInfo.getCompute();
		// 毎フレーム絶対にcomputeするならチェックは不要だが、
		// 現状不明なのでこうしておく
		if (compute.isRecorded()) {
			semaphore.submit(compute, settings.getQueue(), computeWaitMask, stack);
			compute.setRecorded(false);
		}
		
		// computeを待つようにgraphicをsubmit
		var graphic = recordInfo.getGraphic();
		semaphore.submit(graphic, settings.getQueue(), graphicWaitMask, stack);		

		semaphore.waitSemaphore(stack);
		
		// 結果を取得
		VkPresentInfoKHR present = VkPresentInfoKHR.calloc(stack)
                .sType$Default()
                // 1以外あり得るのか不明
                .swapchainCount(1)
                .pSwapchains(settings.getSwapChain().createLongBuffer(stack))
                .pImageIndices(stack.ints(nextSwapChainImageView.getIndex()));
			Vulkan.throwExceptionIfFailed(KHRSwapchain.vkQueuePresentKHR(settings.getQueue().getVkQueue(), present),
					"KHRSwapchain.vkQueuePresentKHRに失敗しました");
		
		// deviceが待機状態になるのを待つ
		// これがないとVkQueueが使用中から復帰しない
		// vkDestroySemaphore(): can't be called on VkSemaphore 0xd000000000d that is currently in use by VkQueue 0x7f7518a340c0.
		settings.getLogicalDevice().waitIdle();
    }

	@Override
	public void close() throws Exception {
		ExceptionUtils.close(recordInfo, semaphore, cpuSync);
	}

	public static FrameRender[] createArray(int length, RenderSettings settings) {
		var array = new FrameRender[length];
		// 参考
		// https://qiita.com/payaneco/items/ea5db7b62d092927aed8
		Arrays.setAll(array, i -> new FrameRender(settings));
		return array;
	}
}
