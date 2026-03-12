package lwjgl.ex.vulkan;

import java.awt.Color;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class SceneCommand implements Command, AutoCloseable {
		private int instanceCount = 1;
		private int firstIndex = DEFAULT_FIRST_INDEX;
		private int vertexOffset = DEFAULT_INT_OFFSETS;
		private int firstInstance = DEFAULT_FIRST_INSTANCE;
		
		private final ClearColorCommand clearColor;
		private Pipeline pipeline;
		
		private LongBuffer vertices;
		private PointerBuffer index;
;
		
		public SceneCommand(AIScene model, Color background, SwapChain swapChain, Pipeline pipeline) {
			this.clearColor = new ClearColorCommand(background, swapChain);
			this.pipeline = pipeline;
			
			// かなり効率悪そうなので、いずれ中間ファイルで対処する
			// 一旦最初のメッシュ
			// https://docs.vulkan.org/tutorial/latest/_attachments/28_model_loading.cpp
			var mesh = AIMesh.create(model.mMeshes().get(0));
			vertices = LongBuffer.allocate(mesh.mNumVertices());
			for(int i = 0; i < vertices.capacity(); ++i) {
				vertices.put(mesh.mVertices().get(i).address());
			}
			
			// 一旦 indexは適当にいれる
			var indexCount = 8;
			index = MemoryUtil.memAllocPointer(indexCount);
			for(int i = 0; i < indexCount; ++i) {
				index.put(mesh.mVertices().get(i).address());
			}
			
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
					commandBuffer.bindVertices(vertices);
					commandBuffer.bindIndex(index);
//					commandBuffer.bindDescriptorSets(vk::PipelineBindPoint::eGraphics, pipelineLayout, 0, *descriptorSets[frameIndex], nullptr);
					commandBuffer.drawIndexed(index.capacity(), instanceCount, firstIndex,  vertexOffset, firstInstance);
				});
			});
		}

		@Override
		public void close() throws Exception {
			try(clearColor){};
		}


}
