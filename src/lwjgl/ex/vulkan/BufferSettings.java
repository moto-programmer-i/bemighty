package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

import java.util.Objects;

public class BufferSettings implements Cloneable {
	public static final int USAGE_TRANSFER_VERTEX = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
	public static final int USAGE_TRANSFER_INDEX = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
	private LogicalDevice logicalDevice;
	private long size;
	private int usage;
	private int requestMask;
	private int outUsage;
	
	public BufferSettings(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
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
	public void setUsage(int usage) {
		this.usage = usage;
	}
	public int getRequestMask() {
		return requestMask;
	}
	public void setRequestMask(int requestMask) {
		this.requestMask = requestMask;
	}
	@Override
	public int hashCode() {
		return Objects.hash(logicalDevice, requestMask, size, usage, outUsage);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BufferSettings other = (BufferSettings) obj;
		return Objects.equals(logicalDevice, other.logicalDevice) && requestMask == other.requestMask
				&& size == other.size && usage == other.usage && outUsage == other.outUsage;
	}
	
	@Override
	public BufferSettings clone() {
		var clone = new BufferSettings(logicalDevice);
		clone.requestMask = requestMask;
		clone.size = size;
		clone.usage = usage;
		clone.outUsage = outUsage;
		return clone;
	}
	public int getOutUsage() {
		return outUsage;
	}
	public void setOutUsage(int outUsage) {
		this.outUsage = outUsage;
	}
}
