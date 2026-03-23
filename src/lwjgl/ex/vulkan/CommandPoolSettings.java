package lwjgl.ex.vulkan;

public class CommandPoolSettings {
	private LogicalDevice logicalDevice;
	private int queueFamilyIndex;
	
	public CommandPoolSettings(LogicalDevice logicalDevice) {
		setLogicalDevice(logicalDevice);
	}
	
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
	
	/**
	 * 論理デバイスの設定
	 * （queueFamilyIndexも同時に設定される）
	 * @param logicalDevice
	 */
	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		queueFamilyIndex = logicalDevice.getGraphicsQueueIndex().getAsInt();
	}
	public int getQueueFamilyIndex() {
		return queueFamilyIndex;
	}
	public void setQueueFamilyIndex(int queueFamilyIndex) {
		this.queueFamilyIndex = queueFamilyIndex;
	}
}
