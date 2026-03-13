package lwjgl.ex.vulkan;

import java.nio.LongBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK14.*;


public class StagingBufferSettings implements Cloneable {
//	public static final int USAGE_TRANSFER_VERTEX = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
//	public static final int USAGE_TRANSFER_INDEX = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
	/**
	 * vk::MemoryPropertyFlagBits::eHostVisible | vk::MemoryPropertyFlagBits::eHostCoherent
	 */
	public static final int MEMORY_PROPERTY_FLAGS_SOURCE = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
	/**
	 * vk::MemoryPropertyFlagBits::eDeviceLocal
	 */
	public static final int MEMORY_PROPERTY_FLAGS_DESTINATION = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
	private LogicalDevice logicalDevice;
	private long size;
	private int usage;
	
	/**
	 * 本来はvk::MemoryPropertyFlags
	 * LWJGLの設計ミス
	 */
	private int sourceMemoryPropertyFlags = MEMORY_PROPERTY_FLAGS_SOURCE;
	private int destinationMemoryPropertyFlags = MEMORY_PROPERTY_FLAGS_DESTINATION;
	
	private LongConsumer copy;

//	private int outUsage;
	
	/**
	 * 
	 * @param logicalDevice
	 * @param copy Bufferコピー処理。例：MemoryUtil.memCopy(values, long);
	 */
	public StagingBufferSettings(LogicalDevice logicalDevice, LongConsumer copy) {
		this.logicalDevice = logicalDevice;
		this.copy = copy;
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}
	public long getSize() {
		return size;
	}
	public void setSize(long size) {
		this.size = size;
	}
	public int getUsage() {
		return usage;
	}
	/**
	 * VK_BUFFER_USAGE_VERTEX_BUFFER_BIT
	 * VK_BUFFER_USAGE_INDEX_BUFFER_BITなど
	 * @param usage
	 */
	public void setUsage(int usage) {
		this.usage = usage;
	}
	
	public int getSourceMemoryPropertyFlags() {
		return sourceMemoryPropertyFlags;
	}
	public void setSourceMemoryPropertyFlags(int sourceMemoryPropertyFlags) {
		this.sourceMemoryPropertyFlags = sourceMemoryPropertyFlags;
	}
	public int getDestinationMemoryPropertyFlags() {
		return destinationMemoryPropertyFlags;
	}
	public void setDestinationMemoryPropertyFlags(int destinationMemoryPropertyFlags) {
		this.destinationMemoryPropertyFlags = destinationMemoryPropertyFlags;
	}
	public LongConsumer getCopy() {
		return copy;
	}
}
