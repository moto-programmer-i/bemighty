package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;
import java.awt.Color;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;

/**
 * 画面を塗りつぶすコマンド
 */
public class ClearColorCommand implements Command {
	private Color clearColor;

	public ClearColorCommand(Color clearColor) {
		this.clearColor = clearColor;
	}

	

	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain, ImageView nextSwapChainImageView) {
		VkClearValue clearValue = ColorUtils.createClear(clearColor, stack);
        VkRenderingAttachmentInfo.Buffer colorAttachment = VkRenderingAttachmentInfo.calloc(1, stack)
            .sType$Default()
            .imageView(nextSwapChainImageView.getHandler())
            .imageLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
            .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
            .clearValue(clearValue);

        var renderingRect = RectUtils.createRect(swapChain.getWidth(), swapChain.getHeight(), stack);

        var renderingInfo = VkRenderingInfo.calloc(stack)
            .sType$Default()
            .renderArea(renderingRect)
            .layerCount(1)
            .pColorAttachments(colorAttachment);
        
        transitionColor(commandBuffer, stack, swapChain, nextSwapChainImageView, () -> {
        	commandBuffer.render(renderingInfo);
        });
	}
	
	private void transitionColor(CommandBuffer commandBuffer, MemoryStack stack, SwapChain swapChain, ImageView nextSwapChainImageView, Runnable clearColor) {
		// IMAGE_USAGE_COLOR_ATTACHMENT_BITが有効なときはこれにしなければならない
		var colorLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
		
		// 書き込みを指定
		var writeAccessMask = VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT;
		
		// 色が出力されるステージへ
		var colorOutputStage = VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT;
		
		// https://github.com/LWJGL/lwjgl3/blob/6c89bd4e861407f243305fc84d60ca8d82fe9dd4/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L943
		commandBuffer.transitionImageLayout(nextSwapChainImageView,
				// 描画前なのでレイアウトは未定義
                VK_IMAGE_LAYOUT_UNDEFINED,
                
                colorLayout,
                
                
                // 依存関係なし
                VK_ACCESS_2_NONE,
                // 書き込みを行う
                writeAccessMask,
                
                // 何も待たない
                VK_PIPELINE_STAGE_2_TOP_OF_PIPE_BIT,
                
                
                colorOutputStage,
                stack);
		
		clearColor.run();
		
		commandBuffer.transitionImageLayout(
				nextSwapChainImageView,
				
				
				colorLayout,
				// 表示状態へ
				KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
				
				
				// アクセスを元に戻す
				writeAccessMask,
				// その後、処理をしない場合は何も待たなくてよい
				// （シェーダーの読み込みを待つ場合は、VK_ACCESS_2_SHADER_READ_BITを指定することもあるらしい）
				VK_ACCESS_2_NONE,
	            
				
				colorOutputStage,
	            // 同期のスコープの終了時まで
	            VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT,
	            stack
	        );

	}

}
