package lwjgl.ex.vulkan;

import java.nio.LongBuffer;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;


import static org.lwjgl.vulkan.VK14.*;


public class StagingBufferSettings implements Cloneable {
	
	
	
	/**
	 * vk::MemoryPropertyFlagBits::eHostVisible | vk::MemoryPropertyFlagBits::eHostCoherent
	 */
	public static final int MEMORY_PROPERTY_FLAGS_SOURCE = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
	/**
	 * vk::MemoryPropertyFlagBits::eDeviceLocal
	 */
	public static final int MEMORY_PROPERTY_FLAGS_DESTINATION = VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
	
	/**
	 * CPUからアクセス可能なGPUメモリ（遅いらしい）
	 */
	public static final int MEMORY_PROPERTY_FLAGS_VISIBLE = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
	
	private LogicalDevice logicalDevice;
	private long size;
	private BufferType type;
	
	/**
	 * 本来はvk::MemoryPropertyFlags
	 * LWJGLの設計ミス
	 */
//	private int sourceMemoryPropertyFlags = MEMORY_PROPERTY_FLAGS_SOURCE;
	private int destinationMemoryPropertyFlags = MEMORY_PROPERTY_FLAGS_DESTINATION;
	
	private Consumer<PointerBuffer> copy;

	private boolean map = true;
	private boolean unMap = true;
	private int shaderStage = VK_SHADER_STAGE_VERTEX_BIT;
	
	public StagingBufferSettings(LogicalDevice logicalDevice) {
		this(logicalDevice, null);
	}
	
	/**
	 * 
	 * @param logicalDevice
	 * @param copy Bufferコピー処理。
	 * 例：
	 * var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var vertexBuffer = buffer.getFloatBuffer(0, (int)verticesBytes);
			vertexBuffer.put(vertices);
		});
	 */
	public StagingBufferSettings(LogicalDevice logicalDevice, Consumer<PointerBuffer> copy) {
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
	
	
	
//	public int getSourceMemoryPropertyFlags() {
//		return sourceMemoryPropertyFlags;
//	}
//	public void setSourceMemoryPropertyFlags(int sourceMemoryPropertyFlags) {
//		this.sourceMemoryPropertyFlags = sourceMemoryPropertyFlags;
//	}
	public int getDestinationMemoryPropertyFlags() {
		return destinationMemoryPropertyFlags;
	}
	public void setDestinationMemoryPropertyFlags(int destinationMemoryPropertyFlags) {
		this.destinationMemoryPropertyFlags = destinationMemoryPropertyFlags;
	}
	public Consumer<PointerBuffer> getCopy() {
		return copy;
	}
	public void setCopy(Consumer<PointerBuffer> copy) {
		this.copy = copy;
	}
	
	
	public boolean isMap() {
		return map;
	}
	/**
	 * マッピングを行うか
	 * @param map
	 */
	public void setMap(boolean map) {
		this.map = map;
	}
	public boolean isUnMap() {
		return unMap;
	}
	/**
	 * unMapを行うか
	 * @param unMap
	 */
	public void setUnMap(boolean unMap) {
		this.unMap = unMap;
	}

	public int getShaderStage() {
		return shaderStage;
	}

	public void setShaderStage(int shaderStage) {
		this.shaderStage = shaderStage;
	}

	public BufferType getType() {
		return type;
	}

	public void setType(BufferType type) {
		this.type = type;
	}
	
}
