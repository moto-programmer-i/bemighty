package lwjgl.ex.vulkan;

import java.awt.Dimension;
import java.awt.Point;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;




public final class RectUtils {
	public static final Point DEFAULT_OFFSET = new Point(0, 0);

	private RectUtils() {
	}

	public static VkRect2D createRect(int width, int height, MemoryStack stack) {
		return createRect(width, height, DEFAULT_OFFSET, stack);
	}
	
	public static VkRect2D createRect(int width, int height, Point offset, MemoryStack stack) {
		VkOffset2D vulkanOffset = VkOffset2D.calloc(stack)
                .set(offset.x, offset.y);
		VkExtent2D extent = VkExtent2D.calloc(stack)
                .width(width)
                .height(height);
		return VkRect2D.calloc(stack)
				.offset(vulkanOffset)
                .extent(extent);
	}
}
