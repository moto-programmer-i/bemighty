package lwjgl.ex.vulkan;

import java.util.ArrayList;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/ModelsCache.java

import java.util.List;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

public class TransferModelsCommand implements Command, AutoCloseable {
	private List<Model> models;
	private List<Buffer> stagingBufferes = new ArrayList<>();
	private List<Buffer> outs = new ArrayList<>();

	public TransferModelsCommand(List<Model> models) {
		this.models = models;
	}

	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, SwapChain swapChain,
			ImageView nextSwapChainImageView) {

		for (var model : models) {
			// GPUバッファへ
			for (Mesh mesh : model.getMeshes()) {
				outs.add(recordTransferCommand(stack, commandBuffer, mesh.getVertices()));
				outs.add(recordTransferCommand(stack, commandBuffer, mesh.getIndices()));
			}
		}
	}
	
	private Buffer recordTransferCommand(MemoryStack stack, CommandBuffer commandBuffer, Buffer buffer) {
		stagingBufferes.add(buffer);
        return buffer.recordStagingCommand(stack, commandBuffer);
    }

	@Override
	public void close() throws Exception {
		ExceptionUtils.close(outs);
	}

}
