package lwjgl.ex.vulkan;


import java.nio.file.Path;

import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;
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
	
	private LogicalDevice logicalDevice;
	private AIScene model;
	private AutoCloseableList<AIMesh> meshes = new AutoCloseableList<>();
	private long verticesSize = 0;
	
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
        	verticesSize += Float.BYTES * XYZ_COUNT * mesh.mNumVertices();
        	
        	
        }
	}
	
	private StagingBufferSettings createVertexBufferSettings(long size) {
		var settings = new StagingBufferSettings(logicalDevice);
		settings.setSize(size);
		settings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_SOURCE);
		return settings;
	}
	
	@Override
	public void close() throws Exception {
		if(model == null) {
			return;
		}
		ExceptionUtils.close(meshes, model);
		model = null;
	}
}
