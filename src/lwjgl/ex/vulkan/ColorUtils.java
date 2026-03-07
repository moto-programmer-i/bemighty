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
	
	/**
	 * 
	 * @param color（外部でcloseされる必要がある、VkClearValue.closeはVkClearColorValue.closeを呼び出さない）
	 * @return closeが必要
	 */
	public static VkClearValue createClearValue(VkClearColorValue color) {
		return VkClearValue.create().color(color);
	}
	
	/**
	 * 
	 * @param color
	 * @return closeが必要
	 */
	public static VkClearColorValue createClearColorValue(Color color) {
		int index = 0;
		VkClearColorValue clearColor = VkClearColorValue.create()
				// フォーマットによって違う場合あり、必要になったときはフォーマットつき関数をつくる
                .float32(index++, color.getRed())
                .float32(index++, color.getGreen())
                .float32(index++, color.getBlue())
                .float32(index, color.getAlpha());
		 return clearColor;
	}

}
