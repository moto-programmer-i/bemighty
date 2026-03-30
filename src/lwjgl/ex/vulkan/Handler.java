package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

import java.nio.LongBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

/**
 * Vulkanがなぜかオブジェクトと vk::raii::DeviceMemoryをわけて持つため用意
 */
public abstract class Handler implements AutoCloseable {
	protected LogicalDevice logicalDevice;
	protected LongBuffer forHandler = BufferUtils.createLongBuffer(1);
	protected LongBuffer forMemory = BufferUtils.createLongBuffer(1);

	public Handler(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}
	
	/**
	 * handlerをcloseする方法
	 * （nullチェックなどは事前に行われる）
	 * @param handler
	 * @throws Exception
	 */
	public abstract void closeHandler(long handler, LogicalDevice logicalDevice) throws Exception;

	@Override
	public void close() throws Exception {
		var device = logicalDevice.getDevice();
		if (forMemory != null) {
			vkFreeMemory(device, forMemory.get(0), null);
			forMemory = null;
		}
		
		if (forHandler != null) {
			closeHandler(forHandler.get(0), logicalDevice);
			forHandler = null;
		}
	}

	public LongBuffer getForHandler() {
		return forHandler;
	}
	
	public long getHandler() {
		return forHandler.get(0);
	}

	public LongBuffer getForMemory() {
		return forMemory;
	}
	
	public long getMemory() {
		return forMemory.get(0);
	}

	public static Handler createImageHandler(LogicalDevice logicalDevice) {
		return new Handler(logicalDevice) {
			@Override
			public void closeHandler(long handler, LogicalDevice logicalDevice) throws Exception {
				vkDestroyImage(logicalDevice.getDevice(), forHandler.get(0), null);
			}
		};
	}
}
