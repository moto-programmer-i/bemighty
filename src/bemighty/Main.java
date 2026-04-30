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
import lwjgl.ex.vulkan.GraphicPipelineSettings;
import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.LogicalDeviceSettings;
import lwjgl.ex.vulkan.Model;
import lwjgl.ex.vulkan.PhysicalDevice;
import lwjgl.ex.vulkan.Pipeline;
import lwjgl.ex.vulkan.PipelineSettings;
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
//	public static final Path TEST_MODEL = RESOURCE_PATH.resolve("models/test.gltf");
	public static final Path TEST_MODEL = RESOURCE_PATH.resolve("models/polyMesh.gltf");

	public static void main(String[] args) throws Exception {
		// 処理前の時刻を取得
        long startMilliseconds = System.currentTimeMillis();
		
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
						var surface = new Surface(surfaceSettings);
						var particleTest = new ParticleTest(logicalDevice)
						) {
					
					var swapChainSettings = new SwapChainSettings(window, logicalDevice, surface);
					
					var shaderSettings = new ShaderSettings(logicalDevice, SHADER_SPV);
					
					try(var swapChain = new SwapChain(swapChainSettings);
							var shader = new Shader(shaderSettings);
									) {
						
						// shader.slangと対応させる必要がある
						// ComputePipelineと GraphicPipelineの関係が謎
						var computeSettings = new PipelineSettings(shader, particleTest.getBuffer()); 
						// https://docs.vulkan.org/tutorial/latest/_attachments/17_swap_chain_recreation.cpp
						computeSettings.add(new ShaderStageSettings(VK_SHADER_STAGE_COMPUTE_BIT, "compMain"));
						var graphicShaderSettings = new PipelineSettings(shader, particleTest.getBuffer());
						graphicShaderSettings.add(new ShaderStageSettings(VK_SHADER_STAGE_VERTEX_BIT, "vertMain"));
//						graphicShaderSettings.add(new ShaderStageSettings(VK_SHADER_STAGE_FRAGMENT_BIT, "fragMain"));
						var graphicSettings = new GraphicPipelineSettings(surfaceSettings);
						
						
						var queueSettings = new QueueSettings(logicalDevice);
						Queue queue = new Queue(queueSettings);
						
						var renderSettings = new RenderSettings(logicalDevice, swapChain, queue, shader);
						
						try(var graphic = Pipeline.createGraphics(graphicShaderSettings, graphicSettings, particleTest.getBinding());
								var compute = Pipeline.createCompute(computeSettings);
								var render = new Render(renderSettings)
								) {
							
							// 頂点の重複を削除できてない。なぜ？
//							int importFileFlag = Assimp.aiProcess_JoinIdenticalVertices;
//							try(var testModel = new Model(TEST_MODEL, logicalDevice, render.getCommandPool(), queue, vertexDescriptionHelper, swapChain)) {
								
								
								try (var command = new ComputeTestCommand(BACKGROUND, swapChain, graphic, compute, particleTest)) {
									final int testCount = 1;
									for(int i = 0; i < testCount; ++i) {
										if (window.shouldClose()) {
											break;
										}
										// ウィンドウをイベント待ちへ
										window.pollEvents();
										
										render.render(command);
									}
									
									// 処理後の時刻を取得
							        long endMilliseconds = System.currentTimeMillis();
							        System.out.println("処理時間 " + (endMilliseconds - startMilliseconds) / 1000.0);									
									// ウィンドウが閉じられるまで待つ
									window.waitUntilClose();	
								}
								
//							}
						}
					}
				}
			}
		}
	}

}
