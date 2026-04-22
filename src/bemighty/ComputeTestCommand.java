package bemighty;

import java.awt.Color;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkClearDepthStencilValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import lwjgl.ex.vulkan.ClearColorCommand;
import lwjgl.ex.vulkan.Command;
import lwjgl.ex.vulkan.CommandBuffer;
import lwjgl.ex.vulkan.ImageView;
import lwjgl.ex.vulkan.ImageViewSettings;
import lwjgl.ex.vulkan.Model;
import lwjgl.ex.vulkan.Pipeline;
import lwjgl.ex.vulkan.SwapChain;
import motopgi.utils.ExceptionUtils;

import static org.lwjgl.vulkan.VK14.*;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class ComputeTestCommand implements Command, AutoCloseable {
	
	// Vulkanの意味不明の設計により、shader.slangの[numthreads(x,y,z)]と対応させなければならない
	// https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html#_compute_shaders
	public static final int NUM_THREADS_X = 1;
	public static final int NUM_THREADS_Y = 1;
	public static final int NUM_THREADS_Z = 1;
	private final ClearColorCommand clearColor;
	private SwapChain swapChain;
	private Pipeline pipeline;
	private ParticleTest particleTest;

	public ComputeTestCommand(Color background, SwapChain swapChain, Pipeline pipeline, ParticleTest particleTest) {
		this.clearColor = new ClearColorCommand(background, swapChain);
		this.swapChain = swapChain;
		this.pipeline = pipeline;
		this.particleTest = particleTest;
	}
	
	@Override
	public void run(MemoryStack stack, CommandBuffer commandBuffer, ImageView nextSwapChainImageView, CommandBuffer computeCommandBuffer) {
		// Computeを先に実行
		// （同期などは保留）
		computeCommandBuffer.record(() -> {
			computeCommandBuffer.bindCompute(pipeline);
			// 意味不明
			computeCommandBuffer.dispatch(NUM_THREADS_X, NUM_THREADS_Y, NUM_THREADS_Z);
		});
		
		// transitionまでは、ClearColorと共通
		clearColor.run(stack, commandBuffer, nextSwapChainImageView, () -> {
			
			
			commandBuffer.render(clearColor.getRenderingInfo(), () -> {
				commandBuffer.bindGraphics(pipeline);
				commandBuffer.setViewportFrom(swapChain, stack);
				commandBuffer.setScissorFrom(swapChain, stack);
				commandBuffer.bindVertices(particleTest.getForParticle());
				commandBuffer.draw(ParticleTest.PARTICLE_COUNT);
			});
		});
	}

	@Override
	public void close() throws Exception {
		ExceptionUtils.close(clearColor);
	}

}
