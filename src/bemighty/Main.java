package bemighty;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;

import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSwapchain;
import org.lwjgl.vulkan.VkClearColorValue;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;
import org.lwjgl.vulkan.VkRenderingInfo;
import org.lwjgl.vulkan.VkSubmitInfo2;


import static org.lwjgl.vulkan.VK14.*;

import lwjgl.ex.vulkan.StagingBuffer;
import lwjgl.ex.vulkan.StagingBufferSettings;
import lwjgl.ex.vulkan.ClearColorCommand;
import lwjgl.ex.vulkan.ColorUtils;
import lwjgl.ex.vulkan.CommandBuffer;
import lwjgl.ex.vulkan.CommandBufferSettings;
import lwjgl.ex.vulkan.CommandPool;
import lwjgl.ex.vulkan.CommandPoolSettings;
import lwjgl.ex.vulkan.DrawModelCommand;
import lwjgl.ex.vulkan.Fence;
import lwjgl.ex.vulkan.FrameRender;
import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.LogicalDeviceSettings;
import lwjgl.ex.vulkan.Model;
import lwjgl.ex.vulkan.PhysicalDevice;
import lwjgl.ex.vulkan.Pipeline;
import lwjgl.ex.vulkan.PipelineSettings;
import lwjgl.ex.vulkan.Queue;
import lwjgl.ex.vulkan.QueueSettings;
import lwjgl.ex.vulkan.RectUtils;
import lwjgl.ex.vulkan.Render;
import lwjgl.ex.vulkan.RenderSettings;
import lwjgl.ex.vulkan.DrawModelCommand;
import lwjgl.ex.vulkan.Shader;
import lwjgl.ex.vulkan.ShaderSettings;
import lwjgl.ex.vulkan.ShaderStageSettings;
import lwjgl.ex.vulkan.Surface;
import lwjgl.ex.vulkan.SurfaceSettings;
import lwjgl.ex.vulkan.SwapChain;
import lwjgl.ex.vulkan.SwapChainSettings;
import lwjgl.ex.vulkan.DescriptionHelper;
import lwjgl.ex.vulkan.Vulkan;
import lwjgl.ex.vulkan.VulkanSettings;
import lwjgl.ex.vulkan.Window;
import lwjgl.ex.vulkan.WindowSettings;

public class Main {
	public static int WIDTH = 400;
	public static int HEIGHT = 400;
	public static String WINDOW_NAME = "Be Mighty";
	public static Color BACKGROUND = Color.black;
	public static final Path RESOURCE_PATH = FileSystems.getDefault().getPath("resources");
	public static final Path SHADER_SPV = RESOURCE_PATH.resolve("shader/slang.spv");
	public static final Path TEST_MODEL = RESOURCE_PATH.resolve("models/test.gltf");

	public static void main(String[] args) throws Exception {
		// 頂点の重複を削除できてない。なぜ？
		 int importFileFlag = Assimp.aiProcess_JoinIdenticalVertices;
		 
		 
		
//		try(var testModel = Assimp.aiImportFile(TEST_MODEL.toString(), importFileFlag)) {
////			System.out.println(testModel.mNumMeshes());
//			int numMeshes = testModel.mNumMeshes();
//	        var meshes = testModel.mMeshes();
//	        for (int i = 0; i < numMeshes; i++) {
//	        	// create?????
//	            try(var mesh = AIMesh.create(meshes.get(i))) {
//	            	var vertices = mesh.mVertices();
////	            	System.out.println("頂点数 " + mesh.mNumVertices());
//		            for(int v = 0; v < mesh.mNumVertices(); ++v) {
//		            	var vertex = vertices.get(v);
//			            System.out.println("(" + vertex.x() + ", " + vertex.y() + ", " + vertex.z() + ")");
//		            }
//	            }
//	            System.out.println("------------------------------------");
//	        }
//		}
//		if(true)return;
		
		var vulkanSettings = new VulkanSettings();
		vulkanSettings.setName(WINDOW_NAME);

		var windowSettings = new WindowSettings(WIDTH, HEIGHT, WINDOW_NAME);
		try(var window = new Window(windowSettings)) {

			
//			Requested layer "VK_LAYER_KHRONOS_validation" failed to load!
//			libVkLayer_khronos_validation.so: 共有オブジェクトファイルを開けません: そのようなファイルやディレクトリはありません
			// が出た場合の対処はLWJGLメモ.txt参照
			
			try(var vulkan = new Vulkan(vulkanSettings)) {
				var vkPhysicalDevice = PhysicalDevice.getFirstVkPhysicalDevice(vulkan);
				var physicalDevice = new PhysicalDevice(vkPhysicalDevice);
				var logicalDeviceSettings = new LogicalDeviceSettings(physicalDevice);
				var surfaceSettings = new SurfaceSettings(vulkan, physicalDevice, window);
				
				// 並列にインスタンスを作成するべきだが、今はこのまま
				try(var logicalDevice = new LogicalDevice(logicalDeviceSettings);
						var surface = new Surface(surfaceSettings)
						) {
					
					var swapChainSettings = new SwapChainSettings(window, logicalDevice, surface);
					
					var shaderSettings = new ShaderSettings(logicalDevice, SHADER_SPV);
					// shader.slangのVSInputと対応させる必要がある
					// https://docs.vulkan.org/tutorial/latest/_attachments/17_swap_chain_recreation.cpp
					shaderSettings.add(new ShaderStageSettings(VK_SHADER_STAGE_VERTEX_BIT, VK_FORMAT_R32G32B32_SFLOAT, "vertMain"));
					shaderSettings.add(new ShaderStageSettings(VK_SHADER_STAGE_FRAGMENT_BIT, VK_FORMAT_R32G32_SFLOAT, "fragMain"));
					
					try(var swapChain = new SwapChain(swapChainSettings);
							var shader = new Shader(shaderSettings);
							var vertexDescriptionHelper = new DescriptionHelper(logicalDevice, shaderSettings);
									) {
						var pipelineSettings = new PipelineSettings(logicalDevice, shader, surfaceSettings);
						pipelineSettings.setVertexDescriptionHelper(vertexDescriptionHelper);
						
						var queueSettings = new QueueSettings(logicalDevice);
						Queue queue = new Queue(queueSettings);
						
						var renderSettings = new RenderSettings(logicalDevice, swapChain, queue);
						
						try(var pipeline = new Pipeline(pipelineSettings);
								var render = new Render(renderSettings)
								) {
							
							// 頂点の重複を削除できてない。なぜ？
//							int importFileFlag = Assimp.aiProcess_JoinIdenticalVertices;
							try(var testModel = new Model(TEST_MODEL, logicalDevice, render.getCommandPool(), queue, vertexDescriptionHelper, swapChain)) {
								
								
								try (var command = new DrawModelCommand(testModel, BACKGROUND, swapChain, pipeline)) {
									final int testCount = 1;
									for(int i = 0; i < testCount; ++i) {
										if (window.shouldClose()) {
											break;
										}
										// ウィンドウをイベント待ちへ
										window.pollEvents();
										
										render.render(command);
									}
									
									// ウィンドウが閉じられるまで待つ
									window.waitUntilClose();	
								}
								
							}
						}
					}
				}
			}
		}
	}
	
//	public static Model createTestModel(LogicalDevice logicalDevice) {
//		var model = new Model();
//		
//		// 参考
//		// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/ModelsCache.java
//		// createVerticesBuffers
//        var vertices = 3;
//        var verticesSettings = new BufferSettings(logicalDevice);
//        verticesSettings.setSize(vertices * VertexDescriptionBufferSettings.VALUES_XYZ * Float.BYTES);
//        verticesSettings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
//        verticesSettings.setRequestMask(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
//        verticesSettings.setOutUsage(BufferSettings.USAGE_TRANSFER_VERTEX);
//        var verticesSource = new Buffer(verticesSettings);
//        FloatBuffer verticesPosition = MemoryUtil.memFloatBuffer(verticesSource.map(), (int) verticesSettings.getSize());
//        // 適当に三角形(xyz)
//        int distance = 200;
//        verticesPosition.put(0);
//        verticesPosition.put(0);
//        verticesPosition.put(0);
//        verticesPosition.put(distance);
//        verticesPosition.put(0);
//        verticesPosition.put(0);
//        verticesPosition.put(0);
//        verticesPosition.put(distance);
//        verticesPosition.put(0);
//        
//        // createIndicesBuffers
//        var indices = new int[]{0, 1, 2, 0};
//        var indicesSettings = new BufferSettings(logicalDevice);
//        indicesSettings.setSize(indices.length * Integer.BYTES);
//        indicesSettings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
//        indicesSettings.setRequestMask(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
//        indicesSettings.setOutUsage(BufferSettings.USAGE_TRANSFER_INDEX);
//        var indicesSource = new Buffer(indicesSettings);
//        IntBuffer data = MemoryUtil.memIntBuffer(indicesSource.map(), (int) indicesSettings.getSize());
//        data.put(indices);
//        
//        model.add(new Mesh(verticesSource, indicesSource, indices.length));
//        return model;
//	}

}
