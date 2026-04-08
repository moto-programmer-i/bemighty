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
import org.lwjgl.assimp.AINode;
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
	
	// 複数モデルの場合は保留
	
	private float[] vertices;
	private StagingBuffer vertexBuffer;
	
	private int[] indices;
	private StagingBuffer indexBuffer;
	private UniformBufferObject uniformObject;
	private AutoCloseableList<Texture> textures;
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, DescriptionHelper descriptionHelper, SwapChain swapChain) throws Exception {
		this(modelPath, logicalDevice, commandPool, queue, descriptionHelper, swapChain, DEFAULT_IMPORT_FILE_FLAG);
	}
	
	public Model(Path modelPath, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, DescriptionHelper descriptionHelper, SwapChain swapChain, int importFileFlag) throws Exception {
		this.model = Assimp.aiImportFile(modelPath.toString(), importFileFlag);
		this.logicalDevice = logicalDevice;
		
		// 各メッシュのtranslation（offset）を取得しなければならない
		var translations = new ArrayList<FloatVector3>();
		var  childNodes = model.mRootNode().mChildren();
		var childLimit = childNodes.limit();
		for(int i = 0; i < childLimit; ++i) {
			try(var child = AINode.create(childNodes.get(i))) {
				// 平行移動を取り出す
				// https://chaosplant.tech/do/vulkan/5-14/#ping-xing-yi-dong
				var transformation = child.mTransformation();
				translations.add(new FloatVector3(transformation.a4(), transformation.b4(), transformation.c4()));
			};
		}
		
		
		//　配列のサイズを確定しなければならないので、先に頂点数を取得しなければならない
		int numMeshes = model.mNumMeshes();
        var mesheBuffers = model.mMeshes();
        var verticesCount = 0;
        var indicesCount = 0;
        try(var meshes = new AutoCloseableList<AIMesh>()) {
        	for (int m = 0; m < numMeshes; ++m) {
        		// createしなければいけないらしい
            	// Assimp自体はそうなっていないので、LWJGLの設計ミス？
            	var mesh = AIMesh.create(mesheBuffers.get(m));
            	meshes.add(mesh);
            	verticesCount += mesh.mNumVertices();
            	
            	// index
            	// faceに分かれてしまっているので、面倒だがまず要素数を取得しなければならない
            	// この理由は不明
            	var numFaces = mesh.mNumFaces();
            	var faces = mesh.mFaces();
            	
            	for(int f = 0; f < numFaces; ++f) {
            		var face = faces.get(f);
            		indicesCount += face.mNumIndices();
            	}
            }
        	vertices = new float[(int) XYZUV_COUNT * verticesCount];
        	indices = new int[indicesCount];
        	
        	
        	var verticesIndex = 0;
        	var indicesIndex = 0;
        	var indexOffset = 0;
        	for (int m = 0; m < numMeshes; ++m) {
        		var mesh = meshes.get(m);
            	var numVertices = mesh.mNumVertices();
            	
            	// translation(offset)が必要
            	var translation = translations.get(m);
            	
            	// テクスチャ複数の場合は保留
            	var textureCoords = mesh.mTextureCoords(0);
            	
            	var verticesBuffer = mesh.mVertices();
            	for(int v = 0; v < numVertices; ++v) {
            		var vertex = verticesBuffer.get(v);
            		vertices[verticesIndex++] = vertex.x() + translation.getX();
            		vertices[verticesIndex++] = vertex.y() + translation.getY();
            		vertices[verticesIndex++] = vertex.z() + translation.getZ();
            		
//            		System.out.println("y " + vertex.y());
            		
            		// テクスチャ座標
            		// 画面の中心が（0, 0）、長さ1まで
                	// https://docs.vulkan.org/tutorial/latest/_images/images/normalized_device_coordinates.svg
            		// https://qiita.com/dpals39/items/1681d9101e58b5aefa27
            		var textureCoord = textureCoords.get(v);
            		vertices[verticesIndex++] = textureCoord.x();
            		
            		// vulkanではy座標が逆なので調整
            		// https://docs.vulkan.org/tutorial/latest/08_Loading_models.html
            		vertices[verticesIndex++] = 1.0f - textureCoord.y();
            		
////            	// 確認用
//            		vertices[verticesIndex++] = 0f;
//            		vertices[verticesIndex++] = 0f;
            	}
            	
            	
            	
            	//index取得
            	var numFaces = mesh.mNumFaces();
            	var faces = mesh.mFaces();
            	for(int f = 0; f < numFaces; ++f) {
            		var face = faces.get(f);
            		var numIndices = face.mNumIndices();
            		var mIndices = face.mIndices();
            		for(int i = 0; i < numIndices; ++i) {
            			indices[indicesIndex++] = indexOffset + mIndices.get(i);
            		}
            	}
            	
            	// indexもメッシュごとにオフセットが必要
            	indexOffset += numVertices;
            }
        }
        
        
    	// デバッグ用
//    	// テクスチャと頂点をマッピング
//    	// https://chaosplant.tech/do/vulkan/6-5/
//    	var texture = new FloatVector2(0.5f, 0f);
//    	vertices = new float[] {
//            	// 頂点				テクスチャ座標
//    			0f, -0.5f, 0f,		texture.getX(),texture.getY(),
//    			0.5f, 0.5f, 0f,		texture.getX(),texture.getY(),
//    			-0.5f, 0.5f, 0f,	texture.getX(),texture.getY()};
////    	indices = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
//    	indices = new int[] {0, 1, 2};
        
        
        
        
        uniformObject = new UniformBufferObject(logicalDevice);
        // 初期化
        swapChain.setView(uniformObject);
        
        // デバッグ用
//        uniformObject.scale(0.4f);
//        uniformObject.move(-0.2f, 0f, 0f);
//        var axis = new FloatVector3(0f, 1f, 0f);
//        var angle = Math.PI / 6;
//        uniformObject.rotate(axis, angle);
//        uniformObject.rotate(axis, angle);
//        uniformObject.rotate(axis, angle);
        var camera = new FloatVector3(0, 0.3f, 0);
        var direction = new FloatVector3(0, 0, 1);
//         // y軸とdirectionが重なるときだけ計算が特殊になる
//        var direction = new FloatVector3(0, 1, 0);
      uniformObject.setView(camera, direction);
//        System.out.println("setProjection");
        uniformObject.perspective(2f, 2f, 0f, 100f);
        
        

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
		ExceptionUtils.close(uniformObject, textures, indexBuffer, vertexBuffer, model);
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
//		swapChain.setProjection(uniformObject);
		uniformObject.update();
	}
	
}
