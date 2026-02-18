package lwjgl.ex.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBuffer;
import static org.lwjgl.vulkan.VK14.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.Supplier;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/VkBuffer.java

public class Buffer implements AutoCloseable {
	private long handler;
	private long allocationSize;
    private long memory;
    private long mappedMemory = NULL;
    private PointerBuffer forMappedMemory;
    
    private final BufferSettings settings;

	public Buffer(BufferSettings settings) {
		this.settings = settings;
		
        try (var stack = MemoryStack.stackPush()) {
            var device = settings.getLogicalDevice().getDevice();
            var bufferCreateInfo = VkBufferCreateInfo.calloc(stack)
                    .sType$Default()
                    .size(settings.getSize())
                    .usage(settings.getUsage())
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            LongBuffer forHandler = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateBuffer(device, bufferCreateInfo, null, forHandler), "Bufferの作成に失敗しました");
            handler = forHandler.get(0);

            var memoryRequirements = VkMemoryRequirements.calloc(stack);
            vkGetBufferMemoryRequirements(device, handler, memoryRequirements);

            var memoryAllocateInfo = VkMemoryAllocateInfo.calloc(stack)
                    .sType$Default()
                    .allocationSize(memoryRequirements.size())
                    .memoryTypeIndex(settings.getLogicalDevice().getPhysicalDevice().findMemoryTypeIndex(memoryRequirements.memoryTypeBits(), settings.getRequestMask()));

            LongBuffer forMemory = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkAllocateMemory(device, memoryAllocateInfo, null, forMemory), "メモリの割り当てに失敗しました");
            allocationSize = memoryAllocateInfo.allocationSize();
            memory = forMemory.get(0);
            forMappedMemory = memAllocPointer(1);

            Vulkan.throwExceptionIfFailed(vkBindBufferMemory(device, handler, memory, 0), "Bufferメモリの紐づけに失敗しました");
        }
	}
	
	// コンストラクタでやればいいのでは？
    public long map() {
        if (mappedMemory == NULL) {
        	Vulkan.throwExceptionIfFailed(vkMapMemory(settings.getLogicalDevice().getDevice(), memory, 0, allocationSize, 0, forMappedMemory), "Failed to map Buffer");
            mappedMemory = forMappedMemory.get(0);
        }
        return mappedMemory;
    }

    public void unMap() {
        if (mappedMemory != NULL) {
            vkUnmapMemory(settings.getLogicalDevice().getDevice(), memory);
            mappedMemory = NULL;
        }
    }
    
    /**
     * GPUのバッファへコピーする
     * @param stack
     * @param commandBuffer
     * @return 中間バッファ（staging buffer）
     */
    public Buffer recordStagingCommand(MemoryStack stack, CommandBuffer commandBuffer) {
    	// 元のバッファから出力先バッファを作成
		var outSettings = settings.clone();
		outSettings.setUsage(settings.getOutUsage());
		outSettings.setRequestMask(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);
		var out = new Buffer(outSettings);

		// 最初からバッファにデータをロードしておいて、
		// ステージングバッファをそのまま管理するべきなのでは？
		// 正しい方法不明
		
//        map();
//        // long size -> int sizeへの変換が本当に正しいのか不明
//        var stagingBuffer = MemoryUtil.memIntBuffer(mappedMemory, (int) settings.getSize());
//        stagingBuffer.put(data);
//        unMap();
    	
        var copyRegion = VkBufferCopy.calloc(1, stack)
                // .srcOffset(0).dstOffset(0)
        		// allocationSizeではないのか？不明
                .size(settings.getSize());
        vkCmdCopyBuffer(commandBuffer.getBuffer(), handler, out.getHandler(), copyRegion);
        return out;
    }

	@Override
	public void close() throws Exception {
		var device = settings.getLogicalDevice().getDevice();
		if (forMappedMemory != null) {
			memFree(forMappedMemory);
			forMappedMemory = null;
		}
		if (memory != NULL) {
			vkFreeMemory(device, memory, null);
			memory = NULL;
		}
		if (handler != NULL) {
			vkDestroyBuffer(device, handler, null);
	        handler = NULL;
		}
	}

	public BufferSettings getSettings() {
		return settings;
	}

	public long getHandler() {
		return handler;
	}
}
