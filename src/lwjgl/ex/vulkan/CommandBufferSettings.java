package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/vk/CmdBuffer.java

import static org.lwjgl.vulkan.VK14.*;

import java.util.Objects;

public class CommandBufferSettings implements Cloneable {
	/**
	 * CommandBufferを作成するデフォルトの数
	 */
	public static final int DEFAULT_COUNT = 1;
	private CommandPool commandPool;
	private boolean primary = true;
	private int usageBit = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
	private int count = DEFAULT_COUNT;
	
	public CommandBufferSettings() {
	}
	public CommandBufferSettings(CommandPool commandPool) {
		this.commandPool = commandPool;
	}
	public CommandPool getCommandPool() {
		return commandPool;
	}
	
	public void setCommandPool(CommandPool commandPool) {
		this.commandPool = commandPool;
	}
	public boolean isPrimary() {
		return primary;
	}
	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
	public int getUsageBit() {
		return usageBit;
	}
	/**
	 * https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkCommandBufferUsageFlagBits.html
	 * @param usageBit
	 */
	public void setUsageBit(int usageBit) {
		this.usageBit = usageBit;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	@Override
	public int hashCode() {
		return Objects.hash(commandPool, count, primary, usageBit);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CommandBufferSettings other = (CommandBufferSettings) obj;
		return Objects.equals(commandPool, other.commandPool) && count == other.count && primary == other.primary
				&& usageBit == other.usageBit;
	}
	@Override
	protected CommandBufferSettings clone() {
		// cloneの厳密な実装法不明、実用上は問題ないはず
		var clone = new CommandBufferSettings(commandPool);
		clone.primary = primary;
		clone.usageBit = usageBit;
		clone.count = count;
		
		return clone;
	}
	
	
}
