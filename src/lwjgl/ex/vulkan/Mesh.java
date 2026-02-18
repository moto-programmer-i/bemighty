package lwjgl.ex.vulkan;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/VulkanMesh.java

public class Mesh implements AutoCloseable {
	private Buffer vertices;
	private Buffer indices;
	private int numIndices;

	public Mesh(Buffer vertices, Buffer indices, int numIndices) {
		this.vertices = vertices;
		this.indices = indices;
		this.numIndices = numIndices;
	}

	public Buffer getVertices() {
		return vertices;
	}

	public Buffer getIndices() {
		return indices;
	}

	public int getNumIndices() {
		return numIndices;
	}

	@Override
	public void close() throws Exception {
		ExceptionUtils.close(indices, vertices);
	}
}
