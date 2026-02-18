package lwjgl.ex.vulkan;
//参考

// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/VulkanModel.java

import java.util.ArrayList;
import java.util.List;

import motopgi.utils.ExceptionUtils;

public class Model implements AutoCloseable {
	private List<Mesh> meshes = new ArrayList<>();
	@Override
	public void close() throws Exception {
		if (meshes == null) {
			return;
		}
		ExceptionUtils.close(meshes);
		meshes = null;
	}
	public List<Mesh> getMeshes() {
		return meshes;
	}
	
	public boolean add(Mesh mesh) {
		return meshes.add(mesh);
	}
}
