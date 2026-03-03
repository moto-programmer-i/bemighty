package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

// 参考

import static org.lwjgl.vulkan.VK14.*;
import java.awt.Color;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkViewport;

/**
 * Modelを描画するコマンド
 */
public class DrawModelCommand implements Command {
	private Color clearColor;

	// Pipelineなどをどのクラスに持たせるべきか不明
	private Pipeline pipeline;
	private Shader vertex;
	private Shader fragment;
	private Model model;

	public DrawModelCommand(Color clearColor, Pipeline pipeline, Model model) {
		this.clearColor = clearColor;
		this.pipeline = pipeline;
		this.model = model;
	}

	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain,
			ImageView nextSwapChainImageView) {

		var renderingInfo = ClearColorCommand.createRenderingInfo(clearColor, stack, swapChain, nextSwapChainImageView);

		transitionColor(commandBuffer, stack, swapChain, nextSwapChainImageView, () -> {
			commandBuffer.render(renderingInfo, () -> {
				commandBuffer.bind(pipeline);
				commandBuffer.setViewportFrom(swapChain, stack);
				commandBuffer.setScissorFrom(swapChain, stack);
				commandBuffer.drawModel(model, stack);
			});
		});
	}

	private void transitionColor(CommandBuffer commandBuffer, MemoryStack stack, SwapChain swapChain,
			ImageView nextSwapChainImageView, Runnable clearColor) {
		// IMAGE_USAGE_COLOR_ATTACHMENT_BITが有効なときはこれにしなければならない
		var colorLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;

		// 書き込みを指定
		var writeAccessMask = VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT;

		// 色が出力されるステージへ
		var colorOutputStage = VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT;

		commandBuffer.transitionImageLayout(nextSwapChainImageView,
				// 描画前なのでレイアウトは未定義
				VK_IMAGE_LAYOUT_UNDEFINED,

				colorLayout,

				// 依存関係なし
				VK_ACCESS_2_NONE,
				// 書き込みを行う
				writeAccessMask,

				// 参考が同じステージを指定しているが、これで良いのかは不明
				colorOutputStage, colorOutputStage,

				stack);

		clearColor.run();

		commandBuffer.transitionImageLayout(nextSwapChainImageView,

				colorLayout,
				// 表示状態へ
				KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,

				// アクセスを元に戻す
				// （参考だとREADとORをとっているが、理由不明）
				VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | writeAccessMask,
				// その後、処理をしない場合は何も待たなくてよい
				// （シェーダーの読み込みを待つ場合は、VK_ACCESS_2_SHADER_READ_BITを指定することもあるらしい）
				VK_ACCESS_2_NONE,

				colorOutputStage,
				// 同期のスコープの終了時まで
				VK_PIPELINE_STAGE_2_BOTTOM_OF_PIPE_BIT, stack);

	}

}
