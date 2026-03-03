package lwjgl.ex.vulkan;

import java.util.Set;

import org.lwjgl.vulkan.KHRSwapchain;

public class LogicalDeviceSettings {
	public static final float DEFAULT_QUEUE_PRIORITIES = 1.0f;
	public static final Set<String> DEFAULT_REQIRED_EXTENSIONS = Set.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
	
	private PhysicalDevice physicalDevice;
	private Set<String> requiredExtensions = DEFAULT_REQIRED_EXTENSIONS;
	private float queuePriorities = DEFAULT_QUEUE_PRIORITIES;
	
	/**
	 * The Vulkan spec states: The synchronization2 feature must be enabled (https://vulkan.lunarg.com/doc/view/1.4.321.1/linux/antora/spec/latest/chapters/cmdbuffers.html#VUID-vkQueueSubmit2-synchronization2-03866)
vkResetFences(): pFences[0] (VkFence 0xb000000000b) is in use.
	 */
	private boolean synchronization2 = true;
	private boolean shaderDrawParameters = true;
	
	public PhysicalDevice getPhysicalDevice() {
		return physicalDevice;
	}
	public void setPhysicalDevice(PhysicalDevice physicalDevice) {
		this.physicalDevice = physicalDevice;
	}
	/**
	 * 初期値 {@value #DEFAULT_REQIRED_EXTENSIONS}
	 * @return
	 */
	public Set<String> getRequiredExtensions() {
		return requiredExtensions;
	}
	public void setRequiredExtensions(Set<String> requiredExtensions) {
		this.requiredExtensions = requiredExtensions;
	}
	
	/**
	 * 初期値 {@value #DEFAULT_QUEUE_PRIORITIES}
	 * @return
	 */
	public float getQueuePriorities() {
		return queuePriorities;
	}
	public void setQueuePriorities(float queuePriorities) {
		this.queuePriorities = queuePriorities;
	}
	public boolean isSynchronization2() {
		return synchronization2;
	}
	public void setSynchronization2(boolean synchronization2) {
		this.synchronization2 = synchronization2;
	}
	public boolean hasShaderDrawParameters() {
		return shaderDrawParameters;
	}
	public void setShaderDrawParameters(boolean shaderDrawParameters) {
		this.shaderDrawParameters = shaderDrawParameters;
	}
}
