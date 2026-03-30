package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;
import java.awt.Color;
import java.util.function.BiConsumer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;

/**
 * 画面を塗りつぶすコマンド
 */
public class ClearColorCommand implements Command, AutoCloseable{
	// IMAGE_USAGE_COLOR_ATTACHMENT_BITが有効なときはこれにしなければならない
	public static final int COLOR_LAYOUT = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
	
	// 書き込みを指定
	public static final long WRITE_ACCESS_MASK = VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT;
	
	// 色が出力されるステージへ
	public static final long COLOR_OUTPUT_STAGE = VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT;
	
	private final VkImageMemoryBarrier2.Buffer startBarrier = ImageViewSettings.createDefaultBarrier()
			// 描画前なのでレイアウトは未定義
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(COLOR_LAYOUT)
            
            // 依存関係なし
            .srcAccessMask(VK_ACCESS_2_NONE)
            // 書き込みを行う
            .dstAccessMask(WRITE_ACCESS_MASK)
            
            // 何も待たない
            .srcStageMask(VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT)
            .dstStageMask(COLOR_OUTPUT_STAGE);
	
	private final VkImageMemoryBarrier2.Buffer endBarrier = ImageViewSettings.createDefaultBarrier()
            .oldLayout(COLOR_LAYOUT)
            // 表示状態へ
            .newLayout(KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR)
            
            // アクセスを元に戻す
            .srcAccessMask(WRITE_ACCESS_MASK)
            // その後、処理をしない場合は何も待たなくてよい
        	// （シェーダーの読み込みを待つ場合は、VK_ACCESS_2_SHADER_READ_BITを指定することもあるらしい）
            .dstAccessMask(VK_ACCESS_2_NONE)
            
            // 何も待たない
            .srcStageMask(COLOR_OUTPUT_STAGE)
            // 同期のスコープの終了時まで
            .dstStageMask(VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT);	
	
	private Color color;
	private final VkClearColorValue clearColorValue;
	private final VkClearValue clearValue;
	private final VkRenderingAttachmentInfo.Buffer colorAttachment;
	private final Rect2D renderArea;
	private final VkRenderingInfo renderingInfo;

	public ClearColorCommand(Color color, SwapChain swapChain) {
		this.color = color;
		clearColorValue = ColorUtils.createClearColorValue(color);
		clearValue = ColorUtils.createClearValue(clearColorValue);
		colorAttachment = VkRenderingAttachmentInfo.calloc(1).sType$Default()
				.imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
	            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
	            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
	            .clearValue(clearValue);
		renderArea = new Rect2D();
		renderingInfo = VkRenderingInfo.create().sType$Default()
				// 未設定Rect2Dを代入する意味なし
				// (内部でインスタンス変更ではなく、値更新が行われている)
//	            .renderArea(renderArea.getRect2D())
	            .layerCount(1)
	            .pColorAttachments(colorAttachment);
		
		// 描画範囲初期化
		updateRenderArea(swapChain);
		swapChain.addRecreateListener(this::updateRenderArea);
	}
	
	public void updateRenderArea(SwapChain swapChain) {
		// extent更新 → renderArea設定までやらなければ値が更新されない
		renderArea.extent(swapChain.getWidth(), swapChain.getHeight());
		renderingInfo.renderArea(renderArea.getRect2D());
	}
	


	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain, ImageView nextSwapChainImageView) {
		run(stack, commandBuffer, swapChain, nextSwapChainImageView, () -> {
			commandBuffer.render(renderingInfo);	
		});
	}
	
	/**
	 * 
	 * @param stack
	 * @param commandBuffer
	 * @param swapChain
	 * @param nextSwapChainImageView
	 * @param render transitionの間の処理
	 */
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain, ImageView nextSwapChainImageView, Runnable render) {
		// SwapChain関係は呼び出しのたびに異なる可能性があるので毎回設定する
		colorAttachment.imageView(nextSwapChainImageView.getHandler());		
		
		// transinsionでrenderを挟まなければならない
        commandBuffer.transitionImageLayout(startBarrier, nextSwapChainImageView);
        render.run();
        commandBuffer.transitionImageLayout(endBarrier, nextSwapChainImageView);
	}

	@Override
	public void close() throws Exception {
		if (color == null) {
			return;
		}
		try(startBarrier;endBarrier;clearColorValue;clearValue;colorAttachment;renderArea;renderingInfo){}
		color = null;
	}

	public VkRenderingInfo getRenderingInfo() {
		return renderingInfo;
	}

	
	
}
