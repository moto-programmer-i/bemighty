package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/Render.java

import java.util.Arrays;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

public class Render implements AutoCloseable {
	private final RenderSettings settings;
	private final CommandPool commandPool;
	// 毎フレーム実行する処理であるため、速度を気にして配列にする
	private final FrameRender[] renders;
	private int currentFrame = 0;
	private boolean isFirst;

	public Render(RenderSettings settings) {
		this.settings = settings;
		commandPool = new CommandPool(settings.getCommandPoolSettings());
		settings.getCommandBufferSettings().setCommandPool(commandPool);
		renders = FrameRender.createArray(settings.getMaxInFlight(), settings);
	}
	
	public void render(Command command) {
		FrameRender waiting = renders[currentFrame];
		if (!isFirst) {
			waiting = null;
			isFirst = true;
		}
		
//        int nextIndex = swapChain.acquireNextImage(vkCtx.getDevice(), presCompleteSemphs[currentFrame]);
//        if (nextIndex < 0) {
//            return;
//        }
		// テスト用にswapChainを経由せず取得
		int nextFrame = (currentFrame + 1) % settings.getMaxInFlight();
		
		// 描画
		FrameRender nextRender = renders[nextFrame];
		nextRender.submit(command, waiting);
		
		// currentを更新
		currentFrame = nextFrame;


//        swapChain.presentImage(presentQueue, renderCompleteSemphs[imageIndex], imageIndex);

        // currentFrame = (currentFrame + 1) % VkUtils.MAX_IN_FLIGHT;
    }



	@Override
	public void close() throws Exception {
		ExceptionUtils.close(renders);
		ExceptionUtils.close(commandPool);
	}

}
