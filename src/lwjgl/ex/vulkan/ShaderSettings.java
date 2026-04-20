package lwjgl.ex.vulkan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ShaderSettings {
	private LogicalDevice logicalDevice;
	private Path spv;
	private List<ShaderStageSettings> stages = new ArrayList<>();
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
	
	public boolean add(ShaderStageSettings stageSettings) {
		return stages.add(stageSettings);
	}
	
	public int stagesSize() {
		return stages.size();
	}
	
	public ShaderStageSettings getStage(int index) {
		return stages.get(index);
	}
	
}
