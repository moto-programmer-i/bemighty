package lwjgl.ex.vulkan;

import java.awt.Point;
import java.nio.LongBuffer;

public final class VulkanConstants {
	public static final int DEFAULT_VIEWPORT = 0;
	public static final float DEFAULT_MIN_DEPTH = 0.0f;
	public static final float DEFAULT_MAX_DEPTH = 1.0f;
	public static final int DEFAULT_SCISSOR = 0;
	public static final int DEFAULT_INT_OFFSETS = 0;
	public static final long DEFAULT_LONG_OFFSETS = 0L;
	public static final Point DEFAULT_OFFSET_POINT = new Point(DEFAULT_INT_OFFSETS, DEFAULT_INT_OFFSETS);
	public static final LongBuffer DEFAULT_ARRAY_OF_BUFFER_OFFSETS = LongBuffer.allocate(0).put(0, DEFAULT_LONG_OFFSETS);
	

	public static final int DEFAULT_FIRST_INDEX = 0;
	public static final int DEFAULT_FIRST_INSTANCE = 0;
	
	/**
	 * カウント 1 
	 * VulkanはAPIが基本的に配列を想定しており、1つだけ送る場合に要素数1を送る必要がある
	 */
	public static final int DEFAULT_COUNT = 1;
	
	/**
	 * firstBindingに活用。
	 * 例 https://docs.vulkan.org/refpages/latest/refpages/source/vkCmdBindVertexBuffers.html
	 */
	public static final int DEFAULT_FIRST_BINDING = 0;
	

	private VulkanConstants() {
	}

}
