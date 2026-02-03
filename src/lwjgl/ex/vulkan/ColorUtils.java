package lwjgl.ex.vulkan;

import java.awt.Color;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearValue;

public final class ColorUtils {

	private ColorUtils() {
	}
	
	public static VkClearValue createClear(Color color, MemoryStack stack) {
		int index = 0;
		VkClearColorValue clearColor = VkClearColorValue.calloc(stack)
				// フォーマットによって違う場合あり、必要になったときはフォーマットつき関数をつくる
                .float32(index++, color.getRed())
                .float32(index++, color.getGreen())
                .float32(index++, color.getBlue())
                .float32(index, color.getAlpha());
		
		return VkClearValue.calloc(stack).color(clearColor);
		
	}

}
