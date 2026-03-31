package lwjgl.ex.vulkan;

public class SwapChainSettings {
	public static final int DEFAULT_IMAGE_ARRAY_LAYERS = 1;
	public static final int DEFAULT_RECREATE_DEBOUNCE_MILLIISECONDS = 500;
	// ここにいれるべきでない可能性があるが、一旦ここで
	public static final float DEFAULT_CAMERA_NEAR = 0.1f;
	public static final float DEFAULT_CAMERA_FAR = 100f;
	public static final float DEFAULT_CAMERA_ANGLE = 0.5f;
	
	private Window window;
	private LogicalDevice logicalDevice;
	private Surface surface;
	// int requestedImages
	private int imageArrayLayers = DEFAULT_IMAGE_ARRAY_LAYERS;
	
	private boolean vsync;
	private int recreteDebounceMilliseconds = DEFAULT_RECREATE_DEBOUNCE_MILLIISECONDS;
	
	private ImageViewSettings imageViewSettings;
	
	// カメラのクリッピングをどこに設定するべきか不明
	private float cameraAngle = DEFAULT_CAMERA_ANGLE;
	private float cameraNear = DEFAULT_CAMERA_NEAR;
	private float cameraFar = DEFAULT_CAMERA_NEAR;
	
	public SwapChainSettings(Window window, LogicalDevice logicalDevice, Surface surface) {
		this.window = window;
		imageViewSettings = new ImageViewSettings(logicalDevice);
		setLogicalDevice(logicalDevice);
		setSurface(surface);
	}
	public Window getWindow() {
		return window;
	}
	public void setWindow(Window window) {
		this.window = window;
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
		imageViewSettings.setLogicalDevice(logicalDevice);
	}
	public Surface getSurface() {
		return surface;
	}
	/**
	 * Surfaceの設定
	 * （imageViewSettings.setFormatも行われる）
	 * @param surface
	 */
	public void setSurface(Surface surface) {
		this.surface = surface;
		imageViewSettings.setFormat(surface.getFormat());
	}
	
	public int getImageArrayLayers() {
		return imageArrayLayers;
	}
	public void setImageArrayLayers(int imageArrayLayers) {
		this.imageArrayLayers = imageArrayLayers;
	}
	public boolean isVsync() {
		return vsync;
	}
	public void setVsync(boolean vsync) {
		this.vsync = vsync;
	}
	public ImageViewSettings getImageViewSettings() {
		return imageViewSettings;
	}
	public void setImageViewSettings(ImageViewSettings imageViewSettings) {
		this.imageViewSettings = imageViewSettings;
	}
	
	/**
	 * 再作成のディレイ
	 * （リサイズの度に作成していると無駄が多くなりそうなため）
	 * @return
	 */
	public int getRecreteDebounceMilliseconds() {
		return recreteDebounceMilliseconds;
	}
	public void setRecreteDebounceMilliseconds(int recreteDebounceMilliseconds) {
		this.recreteDebounceMilliseconds = recreteDebounceMilliseconds;
	}
	public float getCameraNear() {
		return cameraNear;
	}
	public void setCameraNear(float cameraNear) {
		this.cameraNear = cameraNear;
	}
	public float getCameraFar() {
		return cameraFar;
	}
	public void setCameraFar(float cameraFar) {
		this.cameraFar = cameraFar;
	}
	public float getCameraAngle() {
		return cameraAngle;
	}
	public void setCameraAngle(float cameraAngle) {
		this.cameraAngle = cameraAngle;
	}
}
