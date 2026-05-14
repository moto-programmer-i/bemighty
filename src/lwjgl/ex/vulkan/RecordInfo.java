package lwjgl.ex.vulkan;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

public class RecordInfo implements AutoCloseable {
	private MemoryStack stack;
	private CommandBuffer compute;
	private CommandBuffer graphic;
	private ImageView nextSwapChainImageView;
	public RecordInfo(CommandBufferSettings settings) {
		this.compute = new CommandBuffer(settings);
		this.graphic = new CommandBuffer(settings);
	}
	public MemoryStack getStack() {
		return stack;
	}
	public CommandBuffer getCompute() {
		return compute;
	}
	public CommandBuffer getGraphic() {
		return graphic;
	}
	public ImageView getNextSwapChainImageView() {
		return nextSwapChainImageView;
	}
	
	/**
	 * Frameによって違うインスタンスを設定
	 * @param stack
	 * @param nextSwapChainImageView
	 */
	public void setFrame(MemoryStack stack, ImageView nextSwapChainImageView) {
		this.stack = stack;
		this.nextSwapChainImageView = nextSwapChainImageView;
	}
	@Override
	public void close() throws Exception {
		ExceptionUtils.close(graphic, compute);
	}
}
