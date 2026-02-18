package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

public class VertexDescriptionBufferSettings {
	public static final int DEFAULT_CAPACITY = 1;
	public static final int DEFAULT_FORMAT = VK_FORMAT_R32G32B32_SFLOAT;
	public static final int DEFAULT_BYTES_OF_VALUE = Float.BYTES;
	/**
	 * XYZ頂点データの場合の要素数
	 */
	public static final int VALUES_XYZ = 3;
	/**
	 * XYZUV頂点データの場合の要素数
	 */
	public static final int VALUES_XYZUV = 5;
	/**
	 * 頂点データの要素数
	 */
	public static final int DEFAULT_NUMBERS_OF_VALUES = VALUES_XYZ;
	
	private int attributeCapacity = DEFAULT_CAPACITY;
	private int format = DEFAULT_FORMAT;
	private int bytesOfValue = DEFAULT_BYTES_OF_VALUE;
	private int numberOfValues = DEFAULT_NUMBERS_OF_VALUES;
	public int getAttributeCapacity() {
		return attributeCapacity;
	}
	public void setAttributeCapacity(int attributeCapacity) {
		this.attributeCapacity = attributeCapacity;
	}
	public int getFormat() {
		return format;
	}
	public void setFormat(int format) {
		this.format = format;
	}
	public int getBytesOfValue() {
		return bytesOfValue;
	}
	public void setBytesOfValue(int bytesOfValue) {
		this.bytesOfValue = bytesOfValue;
	}
	public int getNumberOfValues() {
		return numberOfValues;
	}
	public void setNumberOfValues(int numberOfValues) {
		this.numberOfValues = numberOfValues;
	}
}
