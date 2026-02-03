package lwjgl.ex.vulkan;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.LongBuffer;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdPool.java

public class CommandPool implements AutoCloseable {
	private final long handler;
	private final CommandPoolSettings settings;

	public CommandPool(CommandPoolSettings settings) {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
            var info = VkCommandPoolCreateInfo.calloc(stack)
                    .sType$Default()
                    .queueFamilyIndex(settings.getQueueFamilyIndex());
            
            // 公式サンプル：resetを使っている
            // https://docs.vulkan.org/tutorial/latest/_attachments/17_swap_chain_recreation.cpp
            // 非公式情報：パフォーマンス上、resetは使わないほうがいい
            // https://github.com/lwjglgamedev/vulkanbook/blob/master/bookcontents/chapter-05/chapter-05.md#command-buffers
            // さらに非公式情報の方のパフォーマンス比較実験の詳細が不明なので、resetを使う
            info.flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT);

            LongBuffer lp = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateCommandPool(settings.getLogicalDevice().getDevice(), info, null, lp),
                    "CommandPoolの作成に失敗しました");

            handler = lp.get(0);
        }
	}

	@Override
	public void close() throws Exception {
		vkDestroyCommandPool(settings.getLogicalDevice().getDevice(), handler, null);
	}

	public long getHandler() {
		return handler;
	}

	public CommandPoolSettings getSettings() {
		return settings;
	}
}
