package lwjgl.ex.vulkan;

public class RenderSettings {
	/**
	 * 同時に処理するフレーム数
	 * GPUの描画待ちがあるので、アクセスされるコマンドバッファ、セマフォ、フェンスを
	 * 事前に複製しておく数
	 * https://docs.vulkan.org/tutorial/latest/03_Drawing_a_triangle/03_Drawing/03_Frames_in_flight.html
	 */
	public static final int DEFAULT_MAX_IN_FLIGHT = 2;


	private int maxInFlight = DEFAULT_MAX_IN_FLIGHT;
	
	private LogicalDevice logicalDevice;
	private SwapChain swapChain;
	private Queue queue;
	private final CommandPoolSettings commandPoolSettings;
	private final CommandBufferSettings commandBufferSettings = new CommandBufferSettings();
	private final QueueSettings queueSettings;
	
	

	public RenderSettings(LogicalDevice logicalDevice, SwapChain swapChain, Queue queue) {
		queueSettings = new QueueSettings(logicalDevice);
		commandPoolSettings = new CommandPoolSettings(logicalDevice);
		setLogicalDevice(logicalDevice);
		this.swapChain = swapChain;
		this.queue = queue;
	}

	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		commandPoolSettings.setLogicalDevice(logicalDevice);
		queueSettings.setLogicalDevice(logicalDevice);
	}
	
	public SwapChain getSwapChain() {
		return swapChain;
	}

	public void setSwapChain(SwapChain swapChain) {
		this.swapChain = swapChain;
	}

	public Queue getQueue() {
		return queue;
	}

	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	public CommandBufferSettings getCommandBufferSettings() {
		return commandBufferSettings;
	}

	public CommandPoolSettings getCommandPoolSettings() {
		return commandPoolSettings;
	}

	public QueueSettings getQueueSettings() {
		return queueSettings;
	}
	

	
	public int getMaxInFlight() {
		return maxInFlight;
	}
	public void setMaxInFlight(int maxInFlight) {
		this.maxInFlight = maxInFlight;
	}
}
