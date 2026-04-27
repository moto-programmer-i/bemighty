package lwjgl.ex.vulkan;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPipelineCacheCreateInfo;
import static org.lwjgl.vulkan.VK14.*;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/PipelineCache.java

/**
 * Vulkanのクソ設計により必要。
 * https://docs.vulkan.org/guide/latest/pipeline_cache.html
 */
public class PipelineCache implements AutoCloseable {
	private LogicalDevice logicalDevice;
	private long handler;

	/**
	 * インスタンス生成
	 * （LogicalDeviceで自動生成されるため、基本的に作る必要はない）
	 * @param logicalDevice
	 */
	public PipelineCache(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		try (var stack = MemoryStack.stackPush()) {
            var createInfo = VkPipelineCacheCreateInfo.calloc(stack).sType$Default();

            LongBuffer forHandler = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreatePipelineCache(logicalDevice.getDevice(), createInfo, null, forHandler),
                    "PipelineCacheの作成に失敗しました");
            handler = forHandler.get(0);
        }
	}

	public long getHandler() {
		return handler;
	}

	@Override
	public void close() throws Exception {
		if (handler == MemoryUtil.NULL) {
			return;
		}
		vkDestroyPipelineCache(logicalDevice.getDevice(), handler, null);
		handler = MemoryUtil.NULL;
	}

}
