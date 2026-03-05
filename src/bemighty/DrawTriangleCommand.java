package bemighty;

import java.awt.Color;

import org.lwjgl.system.MemoryStack;

import lwjgl.ex.vulkan.ClearColorCommand;
import lwjgl.ex.vulkan.Command;
import lwjgl.ex.vulkan.CommandBuffer;
import lwjgl.ex.vulkan.ImageView;
import lwjgl.ex.vulkan.Pipeline;
import lwjgl.ex.vulkan.SwapChain;

/**
 * チュートリアルの三角形描写。いらなくなったら削除
 */
public class DrawTriangleCommand implements Command  {
	// チュートリアル三角形の値
	private int vertexCount = 3;
	private int instanceCount = 1;
	private int firstVertex = 1;
	private int firstInstance = 1; 
	
	private Color clearColor;
	private Pipeline pipeline;
	public DrawTriangleCommand(Color clearColor, Pipeline pipeline) {
		this.clearColor = clearColor;
		this.pipeline = pipeline;
	}

	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain,
			ImageView nextSwapChainImageView) {
		// renderingInfo作成とtransitionまでは、ClearColorと共通
		var renderingInfo = ClearColorCommand.createRenderingInfo(clearColor, stack, swapChain, nextSwapChainImageView);
		ClearColorCommand.transitionColor(commandBuffer, stack, swapChain, nextSwapChainImageView, () -> {
			commandBuffer.render(renderingInfo, () -> {
				commandBuffer.bind(pipeline);
				commandBuffer.setViewportFrom(swapChain, stack);
				commandBuffer.setScissorFrom(swapChain, stack);
				
				// チュートリアル用描画
				commandBuffer.draw(vertexCount, instanceCount, firstVertex, firstInstance);			
			});			
		});
	}

	public int getVertexCount() {
		return vertexCount;
	}

	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}

	public int getInstanceCount() {
		return instanceCount;
	}

	public void setInstanceCount(int instanceCount) {
		this.instanceCount = instanceCount;
	}

	public int getFirstVertex() {
		return firstVertex;
	}

	public void setFirstVertex(int firstVertex) {
		this.firstVertex = firstVertex;
	}

	public int getFirstInstance() {
		return firstInstance;
	}

	public void setFirstInstance(int firstInstance) {
		this.firstInstance = firstInstance;
	}

}
