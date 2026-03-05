package lwjgl.ex.vulkan;

public class WindowSettings {
	private int width;
	private int height;
	private String name;
	private int swapInterval = 1;
	
	public WindowSettings(int width, int height, String name) {
		this.width = width;
		this.height = height;
		this.name = name;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getSwapInterval() {
		return swapInterval;
	}
	
	/**
	 * 画面スワップまで待機する更新の数（初期値1）
	 * https://www.glfw.org/docs/3.3/group__context.html#ga6d4e0cdf151b5e579bd67f13202994ed
	 */
	public void setSwapInterval(int swapInterval) {
		this.swapInterval = swapInterval;
	}
	
}
