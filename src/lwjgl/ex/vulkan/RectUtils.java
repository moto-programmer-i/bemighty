package lwjgl.ex.vulkan;


import java.awt.Point;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;
import static lwjgl.ex.vulkan.VulkanConstants.*;



public final class RectUtils {

	private RectUtils() {
	}

	public static VkRect2D createRect(int width, int height, MemoryStack stack) {
		return createRect(width, height, DEFAULT_OFFSET_POINT, stack);
	}
	
	public static VkRect2D createRect(int width, int height, Point offset, MemoryStack stack) {
		var vulkanOffset = VkOffset2D.calloc(stack)
                .set(offset.x, offset.y);
		var extent = VkExtent2D.calloc(stack)
                .width(width)
                .height(height);
		return VkRect2D.calloc(stack)
				.offset(vulkanOffset)
                .extent(extent);
	}
	
	public static VkRect2D.Buffer createRectBuffer(int width, int height, MemoryStack stack) {
		return createRectBuffer(width, height, DEFAULT_OFFSET_POINT, stack);
	}
	
	public static VkRect2D.Buffer createRectBuffer(int width, int height, Point offset, MemoryStack stack) {
		var vulkanOffset = VkOffset2D.calloc(stack)
                .set(offset.x, offset.y);
		var extent = VkExtent2D.calloc(stack)
                .width(width)
                .height(height);
		return VkRect2D.calloc(1, stack)
				.offset(vulkanOffset)
				.extent(extent)
				;
	}
}
