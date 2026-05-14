package lwjgl.ex.vulkan;



import org.lwjgl.system.MemoryStack;


@FunctionalInterface
public interface Command {
	public void run(RecordInfo info);
}
