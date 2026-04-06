package lwjgl.ex.vulkan;


import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.vulkan.VK14.*;

import motopgi.utils.AutoCloseableList;
import motopgi.utils.ExceptionUtils;
import motopgi.utils.FloatVector2;
import motopgi.utils.FloatVector3;

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
	private StagingBuffer vertexBuffer;
	
	private int[] indices;
	private StagingBuffer indexBuffer;
	private UniformObject uniformObject;
	private AutoCloseableList<Texture> textures;
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper, SwapChain swapChain) {
		this(modelPath, logicalDevice, commandPool, queue, descriptionHelper, swapChain, DEFAULT_IMPORT_FILE_FLAG);
	}
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper, SwapChain swapChain, int importFileFlag) {
		this.model = Assimp.aiImportFile(modelPath.toString(), importFileFlag);
		this.logicalDevice = logicalDevice;
		
		int numMeshes = model.mNumMeshes();
        var mesheBuffers = model.mMeshes();
        for (int m = 0; m < numMeshes; ++m) {
        	// createしなければいけないらしい
        	// Assimp自体はそうなっていないので、LWJGLの設計ミス？
        	var mesh = AIMesh.create(mesheBuffers.get(m));
        	meshes.add(mesh);
        	
        	// 頂点のサイズを追加
//        	var numVertices = mesh.mNumVertices();
//        	vertices = new float[(int) XYZ_COUNT * numVertices];
//        	verticesBytes += Float.BYTES * vertices.length;
//        	
//        	// mesh複数の場合は保留
//        	var verticesBuffer = mesh.mVertices();
//        	for(int v = 0, index = 0; v < numVertices; ++v) {
//        		var vertex = verticesBuffer.get(v);
//        		vertices[index++] = vertex.x();
//        		vertices[index++] = vertex.y();
//        		vertices[index++] = vertex.z();
//        	}
        	// 画面の中心が（0, 0）、長さ1まで
        	// https://docs.vulkan.org/tutorial/latest/_images/images/normalized_device_coordinates.svg

        	
        	// テクスチャ読み込み
//        	System.out.println("numTextureCoords " + numTextureCoords);
        	List<FloatVector2> textures = new ArrayList<>();
        	var forTextureCoords = mesh.mTextureCoords();
        	var textureCoordAddress = forTextureCoords.get();
        	for(int i = 0; textureCoordAddress != MemoryUtil.NULL; ++i, textureCoordAddress = forTextureCoords.get()) {
        		var textureCoord = mesh.mTextureCoords(i);
        		textures.add(new FloatVector2(textureCoord.x(), textureCoord.y()));
        	}
        	
        	System.out.println("テクスチャ " +  textures.get(0));
        	
        	
        	// テクスチャと頂点をマッピング
        	// https://chaosplant.tech/do/vulkan/6-5/
        	var texture = textures.get(0);
        	
        	
        	vertices = new float[] {
                	// 頂点				テクスチャ座標
        			0f, -0.5f, 0f,		texture.getX(),texture.getY(),
        			0.5f, 0.5f, 0f,		texture.getX(),texture.getY(),
        			-0.5f, 0.5f, 0f,	texture.getX(),texture.getY()};
//        	indices = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
        	indices = new int[] {0, 1, 2};
        	
//        	// index
//        	var numFaces = mesh.mNumFaces();
////        	System.out.println("faces " + numFaces);
//        	var faces = mesh.mFaces();
//        	int indicesLength = 0;
//        	// faceに分かれてしまっているので、面倒だがまず要素数を取得しなければならない
//        	for(int f = 0; f < numFaces; ++f) {
//        		var face = faces.get(f);
//        		indicesLength += face.mNumIndices();
//        	}
//        	indices = new int[indicesLength];
//        	indicesBytes += Integer.BYTES * indices.length;
//        	
//        	//index取得
//        	int allIndicesIndex = 0;
//        	for(int f = 0; f < numFaces; ++f) {
//        		var face = faces.get(f);
//        		var numIndices = face.mNumIndices();
//        		var mIndices = face.mIndices();
//        		for(int i = 0; i < numIndices; ++i) {
//        			indices[allIndicesIndex++] = mIndices.get(i);
//        		}
//        	}
        }
        
        uniformObject = new UniformObject(logicalDevice);
        // 初期化
        uniformObject.modelToUnit();
        swapChain.setView(uniformObject);

     // Textureの取得
        textures = AssimpUtils.readTextures(model, logicalDevice, commandPool, queue, descriptionHelper, uniformObject);
        
        // 描画範囲初期化
        onSwapChainRecreate(swapChain);
     	swapChain.addRecreateListener(this::onSwapChainRecreate);
        
        
        // GPUへ送信
        vertexBuffer = new StagingBuffer(createVertexBufferSettings());
        indexBuffer = new StagingBuffer(createIndexBufferSettings());
	}
	
	private StagingBufferSettings createVertexBufferSettings() {
		var settings = new StagingBufferSettings(logicalDevice, (buffer) -> {
			var vertexBuffer = buffer.getFloatBuffer(0, vertices.length);
			vertexBuffer.put(vertices);
		});
		settings.setSize(Float.BYTES * vertices.length);
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
			var indexBuffer = buffer.getIntBuffer(0, indices.length);
			indexBuffer.put(indices);
		});
		settings.setSize(Integer.BYTES * Float.BYTES * indices.length);
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
		ExceptionUtils.close(uniformObject, textures, indexBuffer, vertexBuffer, meshes, model);
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

	public void onSwapChainRecreate(SwapChain swapChain) {
		swapChain.setProjection(uniformObject);
		uniformObject.update();
	}
	
}
