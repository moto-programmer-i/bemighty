package lwjgl.ex.vulkan;

import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkOffset2D;
import org.lwjgl.vulkan.VkRect2D;

import motopgi.utils.ExceptionUtils;

public class Rect2D implements AutoCloseable {
	private VkOffset2D offset2D;
	private VkExtent2D extent2D;
	private VkRect2D rect2D;


	public Rect2D() {
		offset2D = VkOffset2D.create();
		extent2D = VkExtent2D.create();
		
		// インスタンスのアドレスを持つわけではなく、offsetなどの呼び出し時に値を変えているっぽい
//		rect2D = VkRect2D.create().offset(offset2D).extent(extent2D);
		rect2D = VkRect2D.create();
	}
	
	public void offset(int x, int y) {
		offset2D.x(x).y(y);
		// これを呼び出さない限り、内部で値が更新されない
		rect2D.offset(offset2D);
	}
	public void extent(int width, int height) {
		extent2D.width(width).height(height);
		// これを呼び出さない限り、内部で値が更新されない
		rect2D.extent(extent2D);
	}

	public VkOffset2D getOffset2D() {
		return offset2D;
	}

	public VkExtent2D getExtent2D() {
		return extent2D;
	}

	public VkRect2D getRect2D() {
		return rect2D;
	}

	@Override
	public void close() throws Exception {
		if (rect2D == null) {
			return;
		}
		ExceptionUtils.close(rect2D, extent2D, offset2D);
		rect2D = null;
		extent2D = null;
		offset2D = null;
	}

}
