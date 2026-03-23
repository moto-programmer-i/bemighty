package lwjgl.ex.vulkan;

import java.awt.Color;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class DrawModelCommand implements Command, AutoCloseable {
		private int instanceCount = 1;
		private int firstIndex = DEFAULT_FIRST_INDEX;
		private int vertexOffset = DEFAULT_INT_OFFSETS;
		private int firstInstance = DEFAULT_FIRST_INSTANCE;
		
		private Model model;
		private final ClearColorCommand clearColor;
		private Pipeline pipeline;
;
		
		public DrawModelCommand(Model model, Color background, SwapChain swapChain, Pipeline pipeline) {
			this.model = model;
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
					commandBuffer.bind(model);
					commandBuffer.bindDescriptorSets(pipeline);
					commandBuffer.drawIndexed(model.getIndices().length, instanceCount, firstIndex,  vertexOffset, firstInstance);

				});
			});
		}

		@Override
		public void close() throws Exception {
			try(clearColor){};
		}


}
