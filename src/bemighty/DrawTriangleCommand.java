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
public class DrawTriangleCommand implements Command, AutoCloseable  {
	// チュートリアル三角形の値
	private int vertexCount = 3;
	private int instanceCount = 1;
	private int firstVertex = 1;
	private int firstInstance = 1; 
	
	private final ClearColorCommand clearColor;
	private Pipeline pipeline;
	public DrawTriangleCommand(Color background, SwapChain swapChain, Pipeline pipeline) {
		this.clearColor = new ClearColorCommand(background, swapChain);
		this.pipeline = pipeline;
	}

	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain,
			ImageView nextSwapChainImageView) {
		// transitionまでは、ClearColorと共通
		clearColor.run(stack, commandBuffer, swapChain, nextSwapChainImageView, () -> {
			commandBuffer.render(clearColor.getRenderingInfo(), () -> {
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

	@Override
	public void close() throws Exception {
		try(clearColor){};
	}

}
