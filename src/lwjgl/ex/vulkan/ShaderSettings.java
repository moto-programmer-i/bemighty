package lwjgl.ex.vulkan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.lwjgl.vulkan.VK14.*;

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
	
	public boolean hasStage(int stage) {
		return stages.stream().anyMatch(e -> e.getStage() == stage);
	}
	
	public Optional<ShaderStageSettings> getCompute() {
		for(var stage: stages) {
			if (stage.getStage() == VK_SHADER_STAGE_COMPUTE_BIT) {
				return Optional.of(stage);
			}
		}
		return Optional.empty();
	}
	
}
