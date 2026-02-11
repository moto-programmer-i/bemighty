package lwjgl.ex.vulkan;

/**
 * 例外を投げる可能性のあるRunnable
 */
@FunctionalInterface
public interface ThrowableRunnable {
	public void run() throws Exception;
}
