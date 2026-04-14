package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkImageSubresourceRange;

/**
 * VkImageMemoryBarrier2が通常2つのインスタンスをセットで使うため用意
 */
public class ImageMemoryBarrier {
	public static final int BEGIN_INDEX = 0;
	public static final int END_INDEX = 1;
	public static final int LENGTH = 2;
	VkImageMemoryBarrier2.Buffer[] barriers = new VkImageMemoryBarrier2.Buffer[LENGTH];

	/**
	 * 以下を初期化
	 * srcQueueFamilyIndex
	 * dstQueueFamilyIndex
	 * subresourceRange
	 * 
	 * @param stack
	 */
	public ImageMemoryBarrier(MemoryStack stack) {
		// 本来mallocで同時に確保したいが、2番目のBuffer型をうまく取る方法が不明
		// barriers = VkImageMemoryBarrier2.malloc(LENGTH, stack).sType$Default();
		 barriers[BEGIN_INDEX] = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();
		 barriers[END_INDEX] = VkImageMemoryBarrier2.calloc(1, stack).sType$Default();
		
		// 初期化
		for(int i = 0; i < LENGTH; ++i) {
			barriers[i]
			// ここは大体IGNOREDのようなので初期化する
			.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			
			// ここもデフォルトで初期化
			.subresourceRange(VkImageSubresourceRange.malloc(stack)
					.aspectMask(ImageViewSettings.DEFAULT_ASPECT_MASK)
					.baseMipLevel(ImageViewSettings.DEFAULT_BASE_MIP_LEVEL)
					.levelCount(ImageViewSettings.DEFAULT_MIP_LEVELS)
					.baseArrayLayer(ImageViewSettings.DEFAULT_BASE_ARRAY_LAYER)
					.layerCount(ImageViewSettings.DEFAULT_LAYER_COUNT)
					);
		}
	}
	
	public VkImageMemoryBarrier2.Buffer getBegin() {
		return barriers[BEGIN_INDEX];
	}
	
	public VkImageMemoryBarrier2.Buffer getEnd() {
		return barriers[END_INDEX];
	}
	
	/**
	 * beginとendの間のaccessMaskを一括設定
	 * （beginの前、endの後は個別に設定が必要）
	 * @param accessMask
	 */
	public ImageMemoryBarrier accessMask(long accessMask) {
		barriers[BEGIN_INDEX].dstAccessMask(accessMask);
		barriers[END_INDEX].srcAccessMask(accessMask);
		return this;
	}
	
	/**
	 * beginとendの間のstageMaskを一括設定
	 * （beginの前、endの後は個別に設定が必要）
	 * @param stageMask
	 */
	public ImageMemoryBarrier stageMask(long stageMask) {
		barriers[BEGIN_INDEX].dstStageMask(stageMask);
		barriers[END_INDEX].srcStageMask(stageMask);
		return this;
	}
	
	/**
	 * beginとendの間のlayoutを一括設定
	 * （beginの前、endの後は個別に設定が必要）
	 * @param layout
	 */
	public ImageMemoryBarrier layout(int layout) {
		barriers[BEGIN_INDEX].newLayout(layout);
		barriers[END_INDEX].oldLayout(layout);
		return this;
	}
	
	
	
	public ImageMemoryBarrier image(long handler) {
		barriers[BEGIN_INDEX].image(handler);
		barriers[END_INDEX].image(handler);
		return this;
	}
	
	public ImageMemoryBarrier baseMipLevel(int baseMipLevel) {
		barriers[BEGIN_INDEX].subresourceRange().baseMipLevel(baseMipLevel);
		barriers[END_INDEX].subresourceRange().baseMipLevel(baseMipLevel);
		return this;
	}
}
