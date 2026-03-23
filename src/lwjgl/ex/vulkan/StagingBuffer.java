package lwjgl.ex.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.Supplier;

import static lwjgl.ex.vulkan.VulkanConstants.*;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/VkBuffer.java

public class StagingBuffer implements AutoCloseable {
	private long handler;
	private LongBuffer forHandler = MemoryUtil.memAllocLong(1);
	private long allocationSize;
	private long memory;
//    private long mappedMemory = NULL;
//    private PointerBuffer forMappedMemory;
	private LongBuffer forMemory = MemoryUtil.memAllocLong(1);

	private StagingBufferSettings settings;

	public StagingBuffer(StagingBufferSettings settings) {
		this.settings = settings;
		// 参考
		// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
		// https://github.com/LWJGL/lwjgl3/blob/955801a4bb83c0b2459bc294c1628ed74e2888e6/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L470
//		vk::BufferCreateInfo bufferInfo{
//		    .size        = size,
//		    .usage       = usage,
//		    .sharingMode = vk::SharingMode::eExclusive};
//		buffer                                 = vk::raii::Buffer(device, bufferInfo);
//		vk::MemoryRequirements memRequirements = buffer.getMemoryRequirements();
//		vk::MemoryAllocateInfo allocInfo{
//		    .allocationSize  = memRequirements.size,
//		    .memoryTypeIndex = findMemoryType(memRequirements.memoryTypeBits, properties)};
//		bufferMemory = vk::raii::DeviceMemory(device, allocInfo);
//		buffer.bindMemory(bufferMemory, 0);

		try (var stack = MemoryStack.stackPush()) {
			var device = settings.getLogicalDevice().getDevice();
			var bufferCreateInfo = VkBufferCreateInfo.calloc(stack).sType$Default().size(settings.getSize())
					.usage(settings.getUsage()).sharingMode(VK_SHARING_MODE_EXCLUSIVE);

			bufferCreateInfo.usage(settings.getUsage());
			Vulkan.throwExceptionIfFailed(vkCreateBuffer(device, bufferCreateInfo, null, forHandler),
					"Bufferの作成に失敗しました");
			handler = forHandler.get(0);

			
			memory = bindMemory(handler, settings, stack, forMemory);
			Vulkan.throwExceptionIfFailed(vkBindBufferMemory(device, handler, memory, DEFAULT_LONG_OFFSETS),
					"メモリの紐づけに失敗しました");

			// 参考
			// https://github.com/LWJGL/lwjgl3/blob/4ef1eebe4af235b2934a165e82aeefcaf8d9b893/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L460

			// Mapとコピー
			if (!settings.isMap()) {
				return;
			}
			PointerBuffer forMap = stack.mallocPointer(1);
			Vulkan.throwExceptionIfFailed(vkMapMemory(device, memory, 0, settings.getSize(), 0, forMap),
					"vkMapMemoryエラー");
			settings.getCopy().accept(forMap);
			if (settings.isUnMap()) {
				vkUnmapMemory(device, memory);
			}
		}
	}
	
	public static long bindMemory(long handler, StagingBufferSettings settings, MemoryStack stack) {
		return bindMemory(handler, settings, stack, stack.mallocLong(1));
	}
	
	public static long bindMemory(long handler, StagingBufferSettings settings, MemoryStack stack, LongBuffer forMemory) {
		var device = settings.getLogicalDevice().getDevice();
		var memoryRequirements = VkMemoryRequirements.calloc(stack);
		vkGetBufferMemoryRequirements(device, handler, memoryRequirements);
		var memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack).sType$Default()
				.allocationSize(memoryRequirements.size())
				.memoryTypeIndex(settings.getLogicalDevice().getPhysicalDevice().findMemoryTypeIndex(
						memoryRequirements.memoryTypeBits(), settings.getDestinationMemoryPropertyFlags()));
		Vulkan.throwExceptionIfFailed(vkAllocateMemory(device, memoryAllocateInfo, null, forMemory),
				"メモリの割り当てに失敗しました");
//		allocationSize = memoryAllocateInfo.allocationSize();
		return forMemory.get(0);
	}



	@Override
	public void close() throws Exception {
		var device = settings.getLogicalDevice().getDevice();
//		if (forMappedMemory != null) {
//			memFree(forMappedMemory);
//			forMappedMemory = null;
//		}
		if (memory != NULL) {
			vkFreeMemory(device, memory, null);
			memory = NULL;
		}
		if (handler != NULL) {
			vkDestroyBuffer(device, handler, null);
			handler = NULL;
		}
		if (forHandler != null) {
			forHandler = null;
		}
	}

	public StagingBufferSettings getSettings() {
		return settings;
	}

	public long getHandler() {
		return handler;
	}

	public LongBuffer getForHandler() {
		return forHandler;
	}
}
