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
import org.lwjgl.vulkan.VkRenderingInfo;

import lwjgl.ex.vulkan.ClearColorCommand;
import lwjgl.ex.vulkan.Command;
import lwjgl.ex.vulkan.CommandBuffer;
import lwjgl.ex.vulkan.CommandBufferSettings;
import lwjgl.ex.vulkan.CommandPool;
import lwjgl.ex.vulkan.CommandPoolSettings;
import lwjgl.ex.vulkan.GraphicPipelineSettings;
import lwjgl.ex.vulkan.ImageView;
import lwjgl.ex.vulkan.Pipeline;
import lwjgl.ex.vulkan.RecordInfo;
import lwjgl.ex.vulkan.SwapChain;
import motopgi.utils.ExceptionUtils;

import static org.lwjgl.vulkan.VK14.*;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public class ComputeTestCommand implements Command, AutoCloseable {
	
	// Vulkanの意味不明の設計により、shader.slangの[numthreads(x,y,z)]と対応させなければならない
	// https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html#_compute_shaders
	public static final int NUM_THREADS_X = 2;
	public static final int NUM_THREADS_Y = 1;
	public static final int NUM_THREADS_Z = 1;
	private SwapChain swapChain;
	private Pipeline graphic;
	private Pipeline compute;
	private ParticleTest particleTest;
	
	// Compute表示用
	private final ClearColorCommand clearColor;

	public ComputeTestCommand(Color background, SwapChain swapChain, Pipeline graphic, Pipeline compute, ParticleTest particleTest) {
		this.swapChain = swapChain;
		this.graphic = graphic;
		this.compute = compute;
		this.particleTest = particleTest;
		
		clearColor = new ClearColorCommand(background, swapChain);
		
		var logicalDevice = particleTest.getLogicalDevice();
	}
	
	@Override
	public void run(RecordInfo recordInfo) {
		// Computeを先に実行
		// （同期などは保留）
		recordInfo.getCompute().record(() -> {
			recordInfo.getCompute().bindCompute(compute);
			// 意味不明
			recordInfo.getCompute().dispatch(NUM_THREADS_X, NUM_THREADS_Y, NUM_THREADS_Z);	
		});		
		
		
		// renderingInfoが内部で必要なため流用
		clearColor.run(recordInfo, () -> {
			recordInfo.getGraphic().render(clearColor.getRenderingInfo(), () -> {

				recordInfo.getGraphic().setViewportFrom(swapChain, recordInfo.getStack());
				recordInfo.getGraphic().setScissorFrom(swapChain, recordInfo.getStack());
				
				recordInfo.getGraphic().bindGraphics(graphic);
				recordInfo.getGraphic().bindVertices(particleTest.getForParticle());			
				
				recordInfo.getGraphic().draw(ParticleTest.PARTICLE_COUNT);
			});
		});
	}

	@Override
	public void close() throws Exception {
		ExceptionUtils.close(clearColor);
	}

}
