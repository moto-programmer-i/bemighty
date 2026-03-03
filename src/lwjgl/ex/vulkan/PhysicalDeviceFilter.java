package lwjgl.ex.vulkan;

import java.util.HashSet;
import java.util.Set;

import org.lwjgl.vulkan.KHRSwapchain;

public class PhysicalDeviceFilter {
	private final Set<String> extensions;
	/**
	 * グラフィック機能があるか
	 */
	private boolean hasGraphicsQueueFamily = true;

// ライブラリとしては追加すべきだが、そこまで気を回せる時間がないため保留
//	/**
//	 * commandBuffer.beginRenderingができるか
//	 * https://zenn.dev/nishiki/articles/cbff357553ae0c
//	 */
//	private boolean dynamicRendering = true;
	private boolean synchronization2 = true;
	
	private boolean shaderDrawParameters = true;

	public static final Set<String> DEFAULT_EXTENSIONS;

	static {
		DEFAULT_EXTENSIONS = new HashSet<>();
		// デバイスが画面に画像を表示できるかどうか
		DEFAULT_EXTENSIONS.add(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME);
	}

	public PhysicalDeviceFilter() {
		this(DEFAULT_EXTENSIONS);
	}

	public PhysicalDeviceFilter(Set<String> extensions) {
		this.extensions = extensions;
	}

	/**
	 * グラフィック機能があるか
	 */
	public boolean hasGraphicsQueueFamily() {
		return hasGraphicsQueueFamily;
	}

	public void setHasGraphicsQueueFamily(boolean hasGraphicsQueueFamily) {
		this.hasGraphicsQueueFamily = hasGraphicsQueueFamily;
	}

	public Set<String> getExtensions() {
		return extensions;
	}

	public boolean isSynchronization2() {
		return synchronization2;
	}
	
	/**
	 * Shaderが使用可能か
	 * @return
	 */
	public boolean hasShaderDrawParameters() {
		return shaderDrawParameters;
	}

	public void setShaderDrawParameters(boolean shaderDrawParameters) {
		this.shaderDrawParameters = shaderDrawParameters;
	}

	/**
	 * Fenceを使うのに必要な同期機能があるか
	 * The Vulkan spec states: The synchronization2 feature must be enabled (https://vulkan.lunarg.com/doc/view/1.4.321.1/linux/antora/spec/latest/chapters/cmdbuffers.html#VUID-vkQueueSubmit2-synchronization2-03866)
vkResetFences(): pFences[0] (VkFence 0xb000000000b) is in use.
	 */
	public void setSynchronization2(boolean synchronization2) {
		this.synchronization2 = synchronization2;
	}
	
}
