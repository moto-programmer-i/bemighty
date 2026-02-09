package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK13.vkCmdBeginRendering;
import static org.lwjgl.vulkan.VK13.vkQueueSubmit2;

import java.util.Arrays;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSubmitInfo2;


/**
 * 1フレームを描画するのに必要なクラス群
 */
public class FrameRender implements AutoCloseable {
	private final RenderSettings settings;
	private final Fence cpuSync;
	private final Semaphore forSwapChain;
	private final Semaphore complete;
	private final CommandBuffer commandBuffer;

	public FrameRender(RenderSettings settings) {
		this.settings = settings;
		// AutoCloseableを変数として持つのでtry-with-resourcesができない
		cpuSync = new Fence(settings.getLogicalDevice());
		forSwapChain = new Semaphore(settings.getLogicalDevice());
		complete = new Semaphore(settings.getLogicalDevice());		
		commandBuffer = new CommandBuffer(settings.getCommandBufferSettings());

//		try(var pool = new CommandPool(settings.getCommandPoolSettings());
//				var poolB = new CommandPool(settings.getCommandPoolSettings())
//				){
//			
//		}
//		catch(Exception e) {
//			
//		}
	}
	
	public void submit(MemoryStack stack, ImageView nextSwapChainImageView, Command command, FrameRender waiting) {
		/*
		https://github.com/lwjglgamedev/vulkanbook/blob/master/bookcontents/chapter-05/chapter-05.md#render-loop
		描画の主な手順は次のとおりです。

		フェンスを待つ：CPUから現在のフレームに関連付けられたリソースにアクセスできるようにするには、それらのリソースがGPUによってまだ使用されていないことを確認する必要があります。フェンスはGPUとCPU間の同期手段であることを覚えておいてください。現在のフレームに関連付けられた作業を送信する際、関連するフェンスを通過します。
		コマンドAの記録: フェンスを通過すると、現在のフレームに関連付けられたコマンドバッファにコマンドの記録を開始できます。しかし、なぜコマンド「A」と「B」の2つのセットが必要なのでしょうか？これは、取得する必要がある特定のスワップチェーンイメージに依存しないコマンド（「Aコマンド」）と、特定のイメージビューに対して操作を実行するコマンド（「Bコマンド」）があるためです。スワップチェーンイメージを取得する前の最初のステップの記録を開始できます。
		画像の取得：レンダリングに使用する次のスワップチェーン画像を取得する必要があります。ただし、この章ではまだ「Aコマンド」は使用しません。
		記録コマンド B : すでに説明しました。
		コマンドの送信: コマンドをグラフィカル キューに送信するだけです。
		現在の画像。
			 */

    
    	// 前の処理を待機
    	if (waiting != null) {
    		waiting.reset();
    	} 
    	
    	
    	// SwapChainとSwapChainImageView両方渡すのはちょっと変だが、現状しょうがない
    	commandBuffer.record(command, stack, settings.getSwapChain(), nextSwapChainImageView);
        var commandBufferInfoBuffers = commandBuffer.createSubmitInfoBuffer(stack);
        var swapChainInfo = forSwapChain.createSubmitInfoBuffer(stack);
        var completeInfo = complete.createSubmitInfoBuffer(stack);
		var submitInfo = VkSubmitInfo2.calloc(1, stack)
				.sType$Default()
				.pWaitSemaphoreInfos(swapChainInfo)
				.pCommandBufferInfos(commandBufferInfoBuffers)
				.pSignalSemaphoreInfos(completeInfo);
		
		// キューへ送信とともに、Fence（CPU処理待ち）開始
		Vulkan.throwExceptionIfFailed(vkQueueSubmit2(settings.getQueue().getVkQueue(), submitInfo, cpuSync.getHandler()), "Queueへのコマンドの送信に失敗しました");
    }
	
	public void reset() {
		cpuSync.waitAndReset();
		commandBuffer.reset();
	}
	
	public Semaphore getForSwapChain() {
		return forSwapChain;
	}

	@Override
	public void close() throws Exception {
//		// Java側も逆順に解放するので、これだと解放順が逆になってしまう
//		try(gpuCompleted;cpuSync;commandBuffer;commandPool) {};
		
		// 生成した順に書けば、Java側が逆順に解放してくれる
		try(cpuSync;forSwapChain;complete;commandBuffer) {}
	}

	public static FrameRender[] createArray(int length, RenderSettings settings) {
		var array = new FrameRender[length];
		// 参考
		// https://qiita.com/payaneco/items/ea5db7b62d092927aed8
		Arrays.setAll(array, i -> new FrameRender(settings));
		return array;
	}
}
