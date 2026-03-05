package lwjgl.ex.vulkan;

import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import static org.lwjgl.vulkan.VK14.*;

public class SurfaceSettings {
	/**
	 * フォーマット。元々はちゃんとenumだが、LWJGLがintにしてしまっている
	 * https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkFormat.html
	 */
	// SRGBだと自動でガンマ補正が入ってしまうらしい。標準はSRGBの可能性があるが、一度試す
	public static final int DEFAULT_FORMAT = VK_FORMAT_B8G8R8A8_UNORM;
	private Vulkan vulkan;
	private PhysicalDevice physicalDevice;
	private Window window;
	private int format = DEFAULT_FORMAT;
	
	public SurfaceSettings(Vulkan vulkan, PhysicalDevice physicalDevice, Window window) {
		this.vulkan = vulkan;
		this.physicalDevice = physicalDevice;
		this.window = window;
	}
	public Vulkan getVulkan() {
		return vulkan;
	}
	public void setVulkan(Vulkan vulkan) {
		this.vulkan = vulkan;
	}
	
	public PhysicalDevice getPhysicalDevice() {
		return physicalDevice;
	}
	public void setPhysicalDevice(PhysicalDevice physicalDevice) {
		this.physicalDevice = physicalDevice;
	}
	public Window getWindow() {
		return window;
	}
	public void setWindow(Window window) {
		this.window = window;
	}
	public int getFormat() {
		return format;
	}
	public void setFormat(int format) {
		this.format = format;
	}
}
