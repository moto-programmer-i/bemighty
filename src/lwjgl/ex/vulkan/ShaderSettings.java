package lwjgl.ex.vulkan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.vulkan.VK14.*;

public class ShaderSettings {
	private LogicalDevice logicalDevice;
	private Path spv;
	public ShaderSettings(LogicalDevice logicalDevice, Path spv) {
		this.logicalDevice = logicalDevice;
		this.spv = spv;
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}
	public void setLogicalDevice(LogicalDevice logicalDevice) {
		this.logicalDevice = logicalDevice;
	}
	public Path getSpv() {
		return spv;
	}
	public void setSpv(Path spv) {
		this.spv = spv;
	}
}
