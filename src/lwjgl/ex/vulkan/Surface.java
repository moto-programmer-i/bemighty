package lwjgl.ex.vulkan;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.KHRSurface;

public class Surface implements AutoCloseable {
	/**
	 * the current width and height of the surface, or the special value
	 * (0xFFFFFFFF, 0xFFFFFFFF) indicating that the surface size will be determined
	 * by the extent of a swapchain targeting the surface.
	 * https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkSurfaceCapabilitiesKHR.html
	 */
	public static final int EXTENT_UNDEFINED_VALUE = 0xFFFFFFFF;

	private final long handler;
	private SurfaceSettings settings;
	private int format;
	private int colorSpace;

	public Surface(SurfaceSettings settings) {
		this.settings = settings;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			handler = settings.getWindow().createWindowSurfaceHandler(stack, settings.getVulkan().getVkInstance());
			initFormat(stack);
		}
	}
	
	public VkSurfaceCapabilitiesKHR getCapabilities(MemoryStack stack) {
		var surfaceCapabilities = VkSurfaceCapabilitiesKHR.calloc(stack);
		Vulkan.throwExceptionIfFailed(
				KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(settings.getPhysicalDevice().getDevice(),
						handler, surfaceCapabilities),
				"surface capabilitiesの取得に失敗しました");
		return surfaceCapabilities;
	}

	private void initFormat(MemoryStack stack) {
		// 指定されたフォーマットが利用可能なら設定
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/Demo.java#L45
		IntBuffer surfaceFormatCountBuffer = stack.mallocInt(1);

		// なぜか引数をnullにして数を取得し、もう1度呼ばなければいけない謎の設計のため
		Vulkan.throwExceptionIfFailed(
				KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(settings.getPhysicalDevice().getDevice(), handler,
						surfaceFormatCountBuffer, null),
				"SurfaceFormatの数の取得に失敗しました");
		int surfaceFormatCount = surfaceFormatCountBuffer.get(0);
		try (VkSurfaceFormatKHR.Buffer supportedSurfaceFormats = VkSurfaceFormatKHR.calloc(surfaceFormatCount)) {
			Vulkan.throwExceptionIfFailed(
					KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(settings.getPhysicalDevice().getDevice(),
							handler, surfaceFormatCountBuffer, supportedSurfaceFormats),
					"SurfaceFormatの取得に失敗しました");
			
			// 指定されたformatなら設定
			for (int i = 0; i < surfaceFormatCount; ++i) {
				var supportedFormat = supportedSurfaceFormats.get(i);
				if (settings.getFormat() == supportedFormat.format()) {
					format = supportedFormat.format();
					colorSpace = supportedFormat.colorSpace();
					return;
				}
			}
			throw new RuntimeException("指定されたフォーマット（" + settings.getFormat() + "）が利用できません " +
			"\n https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkFormat.html"
			);
		}
			
			// 手元のグラボのフォーマットは以下だった
			// https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkFormat.html
			// https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkColorSpaceKHR.html
//			SurfaceFormat colorSpace
//			44 0 (VK_FORMAT_B8G8R8A8_UNORM VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
//			50 0 (VK_FORMAT_B8G8R8A8_SRGB  VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
//			System.out.println("SurfaceFormat colorSpace");
//			for (int i = 0; i < surfaceFormatCount; ++i) {
//				var format = supportedSurfaceFormats.get(i);
//				System.out.println(format.format() + " " + format.colorSpace());
//			}
	}

	@Override
	public void close() throws Exception {
		KHRSurface.vkDestroySurfaceKHR(settings.getVulkan().getVkInstance(), handler, null);
	}

	public long getHandler() {
		return handler;
	}

	public int getFormat() {
		return format;
	}

	public int getColorSpace() {
		return colorSpace;
	}
}
