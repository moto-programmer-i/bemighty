package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/Render.java

import java.util.Arrays;
import java.util.OptionalInt;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

public class Render implements AutoCloseable {
	private final RenderSettings settings;
	private final CommandPool commandPool;
	// 毎フレーム実行する処理であるため、速度を気にして配列にする
	private final FrameRender[] renders;
	private int currentFrame = 0;

	public Render(RenderSettings settings) {
		this.settings = settings;
		commandPool = new CommandPool(settings.getCommandPoolSettings());
		settings.getCommandBufferSettings().setCommandPool(commandPool);
		renders = FrameRender.createArray(settings.getMaxInFlight(), settings);
	}
	
	public void render(Command command) {
		try(var stack = MemoryStack.stackPush()) {
			renders[currentFrame].submit(stack, command);
			
			// 次のフレームへ
			currentFrame = (currentFrame + 1) % settings.getMaxInFlight();
		}
	}



	@Override
	public void close() throws Exception {
		ExceptionUtils.close(renders, commandPool);
	}

}
