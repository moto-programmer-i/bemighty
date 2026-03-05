package lwjgl.ex.vulkan;

public class QueueSettings {
	// キューの適切な初期値不明、一旦0にしておく
	public static final int DEFAULT_QUEUE_INDEX = 0;
	
	private LogicalDevice logicalDevice;
	private int queueFamilyIndex;
	private int queueIndex = DEFAULT_QUEUE_INDEX;
	
	
	
	public QueueSettings(LogicalDevice logicalDevice) {
		setLogicalDevice(logicalDevice);
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
	/**
	 * deviceを設定
	 * device.getGraphicsQueueIndex().getAsInt()から、queueFamilyIndexも同時に設定される
	 * @param device
	 */
	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		queueFamilyIndex = logicalDevice.getGraphicsQueueIndex().getAsInt();
	}
	public int getQueueFamilyIndex() {
		return queueFamilyIndex;
	}
	/**
	 * setDeviceによっても設定される（device.getGraphicsQueueIndex().getAsInt()）
	 * @param queueFamilyIndex
	 */
	public void setQueueFamilyIndex(int queueFamilyIndex) {
		this.queueFamilyIndex = queueFamilyIndex;
	}
	public int getQueueIndex() {
		return queueIndex;
	}
	public void setQueueIndex(int queueIndex) {
		this.queueIndex = queueIndex;
	}
}
