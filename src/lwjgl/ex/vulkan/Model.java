package lwjgl.ex.vulkan;


import java.nio.file.Path;

import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK14.*;

import motopgi.utils.AutoCloseableList;
import motopgi.utils.ExceptionUtils;
import static lwjgl.ex.vulkan.VulkanConstants.*;
import static lwjgl.ex.vulkan.StagingBufferSettings.*;


/**
 * AISceneがあまりにも使いづらいので用意
 */
public class Model implements AutoCloseable {
	// 頂点の重複を削除できてない。なぜ？
	public static final int DEFAULT_IMPORT_FILE_FLAG = Assimp.aiProcess_JoinIdenticalVertices;
	
	/**
	 * vk::BufferUsageFlagBits::eTransferDst | vk::BufferUsageFlagBits::eVertexBuffer
	 */
	public static final int USAGE_VERTEX_DESTINATION = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
	
	private LogicalDevice logicalDevice;
	private AIScene model;
	private AutoCloseableList<AIMesh> meshes = new AutoCloseableList<>();
	private long verticesBytes = 0;
	// 複数モデルの場合は保留
	private float[] vertices;
	
	private StagingBuffer vertexBuffer;
	
	public Model(Path modelPath, LogicalDevice logicalDevice) {
		this(modelPath, logicalDevice, DEFAULT_IMPORT_FILE_FLAG);
	}
	
	public Model(Path modelPath, LogicalDevice logicalDevice, int importFileFlag) {
		this.model = Assimp.aiImportFile(modelPath.toString(), DEFAULT_IMPORT_FILE_FLAG);
		this.logicalDevice = logicalDevice;
		
		int numMeshes = model.mNumMeshes();
        var mesheBuffers = model.mMeshes();
        for (int i = 0; i < numMeshes; ++i) {
        	// createしなければいけないらしい
        	// Assimp自体はそうなっていないので、LWJGLの設計ミス？
        	var mesh = AIMesh.create(mesheBuffers.get(i));
        	meshes.add(mesh);
        	
        	// 頂点のサイズを追加
        	var numVertices = mesh.mNumVertices();
        	vertices = new float[(int) XYZ_COUNT * numVertices];
        	verticesBytes += Float.BYTES * vertices.length;
        	
        	// mesh複数の場合は保留
        	var verticesBuffer = mesh.mVertices();
        	for(int v = 0, index = 0; v < numVertices; ++v) {
        		var vertex = verticesBuffer.get(v);
        		vertices[index++] = vertex.x();
        		vertices[index++] = vertex.y();
        		vertices[index++] = vertex.z();
        	}
        }
        
        // GPUへ送信
        vertexBuffer = new StagingBuffer(createVertexBufferSettings());
	}
	
	private StagingBufferSettings createVertexBufferSettings() {
		var settings = new StagingBufferSettings(logicalDevice, (destination) -> {
			MemoryUtil.memCopy(vertices, destination);
		});
		settings.setSize(verticesBytes);
		settings.setUsage(USAGE_VERTEX_DESTINATION);
		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_SOURCE);
		return settings;
	}
	
	
	
	public long getVerticesBytes() {
		return verticesBytes;
	}

	@Override
	public void close() throws Exception {
		if(model == null) {
			return;
		}
		ExceptionUtils.close(vertexBuffer, meshes, model);
		model = null;
	}
}
