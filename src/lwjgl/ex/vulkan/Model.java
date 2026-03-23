package lwjgl.ex.vulkan;


import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;

import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexture;
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
	
	private LogicalDevice logicalDevice;
	private AIScene model;
	private AutoCloseableList<AIMesh> meshes = new AutoCloseableList<>();
	
	// 複数モデルの場合は保留
	
	private float[] vertices;
	private long verticesBytes = 0;
	private StagingBuffer vertexBuffer;
	
	private int[] indices;
	private long indicesBytes = 0;
	private StagingBuffer indexBuffer;
	private AutoCloseableList<Texture> textures = new AutoCloseableList<>();
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper) {
		this(modelPath, logicalDevice, commandPool, queue, descriptionHelper, DEFAULT_IMPORT_FILE_FLAG);
	}
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper, int importFileFlag) {
		this.model = Assimp.aiImportFile(modelPath.toString(), importFileFlag);
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
        	
        	
        	// indexのサイズを追加
        	// 一旦べたがき
        	indices = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        	indicesBytes += Integer.BYTES * indices.length;
        }
        
        // Textureの取得
        int numTextures = model.mNumTextures();
        for(int i = 0; i < numTextures; ++i) {
        	var texture = AITexture.create(model.mTextures().get(i));
        	textures.add(new Texture(texture, logicalDevice, commandPool, queue, descriptionHelper));
//        	System.out.println("capacity " + texture.pcDataCompressed().capacity());
//        	System.out.println("mWidth " + texture.mWidth());
//        	System.out.println("mHeight " + texture.mHeight());
        }
        
        
        // GPUへ送信
        vertexBuffer = new StagingBuffer(createVertexBufferSettings());
        indexBuffer = new StagingBuffer(createIndexBufferSettings());
	}
	
	private StagingBufferSettings createVertexBufferSettings() {
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var vertexBuffer = buffer.getFloatBuffer(0, (int)verticesBytes);
			vertexBuffer.put(vertices);
		});
		settings.setSize(verticesBytes);
//		settings.setUsage(USAGE_VERTEX_DESTINATION);
//		settings.setUsage(USAGE_SOURCE);
		settings.setUsage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT);
//		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_DESTINATION);
//		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_SOURCE);
		
		// これは遅いらしいが、動作確認のため一旦こうする
		settings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		return settings;
	}
	
	private StagingBufferSettings createIndexBufferSettings() {
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var indexBuffer = buffer.getIntBuffer(0, (int)indicesBytes);
			indexBuffer.put(indices);
		});
		settings.setSize(indicesBytes);
//		settings.setUsage(USAGE_INDEX_DESTINATION);
//		settings.setUsage(USAGE_SOURCE);
		settings.setUsage(VK_BUFFER_USAGE_INDEX_BUFFER_BIT);
//		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_DESTINATION);
//		settings.setSourceMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_SOURCE);
		// これは遅いらしいが、動作確認のため一旦こうする
		settings.setDestinationMemoryPropertyFlags(MEMORY_PROPERTY_FLAGS_VISIBLE);
		return settings;
	}
	
	

	@Override
	public void close() throws Exception {
		if(model == null) {
			return;
		}
		ExceptionUtils.close(textures, indexBuffer, vertexBuffer, meshes, model);
		model = null;
	}
	
	public LongBuffer getVertexBufferInGPU() {
		return vertexBuffer.getForHandler();
	}
	
	public long getIndexBufferHandlerInGPU() {
		return indexBuffer.getHandler();
	}

	public float[] getVertices() {
		return vertices;
	}

	public int[] getIndices() {
		return indices;
	}
	
}
