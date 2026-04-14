package lwjgl.ex.vulkan;

import java.awt.Point;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkOffset3D;

public final class VulkanConstants {
	public static final int DEFAULT_VIEWPORT = 0;
	public static final float DEFAULT_MIN_DEPTH = 0.0f;
	public static final float DEFAULT_MAX_DEPTH = 1.0f;
	public static final int DEFAULT_SCISSOR = 0;
	public static final int DEFAULT_INT_OFFSETS = 0;
	public static final long DEFAULT_LONG_OFFSETS = 0L;
	public static final Point DEFAULT_OFFSET_POINT = new Point(DEFAULT_INT_OFFSETS, DEFAULT_INT_OFFSETS);
	public static final VkOffset3D DEFAULT_OFFSET_3D = VkOffset3D.create();
	public static final LongBuffer DEFAULT_ARRAY_OF_BUFFER_OFFSETS = MemoryUtil.memAllocLong(1).put(0, DEFAULT_LONG_OFFSETS);
	

	public static final int DEFAULT_FIRST_INDEX = 0;
	public static final int DEFAULT_FIRST_INSTANCE = 0;
	
	/**
	 * 頂点など、x, y, zの変数の数
	 */
	public static final int XYZ_COUNT = 3;
	
	/**
	 * 頂点など、x, y, z, u, vの変数の数
	 */
	public static final int XYZUV_COUNT = 5;
	
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
	
	/**
	 * vkMapMemoryの引数flagsだが、詳細不明。多分なにもしない？
	 * https://docs.vulkan.org/refpages/latest/refpages/source/vkMapMemory.html
	 * https://docs.vulkan.org/refpages/latest/refpages/source/VkMemoryMapFlagBits.html
	 */
	public static final int DEFAULT_MEMORY_MAP_FLAG_BITS = 0;
	
	/**
	 * mipLodBias is the bias to be added to mipmap LOD(Level of Detail) calculation
	 * 
	 * https://docs.vulkan.org/refpages/latest/refpages/source/VkSamplerCreateInfo.html
	 * https://qiita.com/dgtanaka/items/2ec0fd88236daa5c3cc7
	 */
	public static final float DEFAULT_BIAS = 0.0f;
	
	/**
	 * https://docs.vulkan.org/tutorial/latest/09_Generating_Mipmaps.html#_sampler
	 */
	public static final float DEFAULT_MIN_LEVEL_OF_DETAIL = 0.0f;

	/**
	 * 1ピクセルのバイト数
	 */
	public static final int ARGB_BYTES = 4;

	private VulkanConstants() {
	}

}
