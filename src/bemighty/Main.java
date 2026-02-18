package bemighty;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Set;

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

import lwjgl.ex.vulkan.Buffer;
import lwjgl.ex.vulkan.BufferSettings;
import lwjgl.ex.vulkan.ClearColorCommand;
import lwjgl.ex.vulkan.ColorUtils;
import lwjgl.ex.vulkan.CommandBuffer;
import lwjgl.ex.vulkan.CommandBufferSettings;
import lwjgl.ex.vulkan.CommandPool;
import lwjgl.ex.vulkan.CommandPoolSettings;
import lwjgl.ex.vulkan.Fence;
import lwjgl.ex.vulkan.FrameRender;
import lwjgl.ex.vulkan.LogicalDevice;
import lwjgl.ex.vulkan.LogicalDeviceSettings;
import lwjgl.ex.vulkan.Mesh;
import lwjgl.ex.vulkan.Model;
import lwjgl.ex.vulkan.PhysicalDevice;
import lwjgl.ex.vulkan.Queue;
import lwjgl.ex.vulkan.QueueSettings;
import lwjgl.ex.vulkan.RectUtils;
import lwjgl.ex.vulkan.Render;
import lwjgl.ex.vulkan.RenderSettings;
import lwjgl.ex.vulkan.Surface;
import lwjgl.ex.vulkan.SurfaceSettings;
import lwjgl.ex.vulkan.SwapChain;
import lwjgl.ex.vulkan.SwapChainSettings;
import lwjgl.ex.vulkan.VertexDescriptionBufferSettings;
import lwjgl.ex.vulkan.Vulkan;
import lwjgl.ex.vulkan.VulkanSettings;
import lwjgl.ex.vulkan.Window;
import lwjgl.ex.vulkan.WindowSettings;

public class Main {
	public static int WIDTH = 400;
	public static int HEIGHT = 400;
	public static String WINDOW_NAME = "Be Mighty";
	public static Color clearColor = Color.black;

	public static void main(String[] args) throws Exception {
		var vulkanSettings = new VulkanSettings();
		vulkanSettings.setName(WINDOW_NAME);

		var windowSettings = new WindowSettings();
		windowSettings.setWidth(WIDTH);
		windowSettings.setHeight(HEIGHT);
		windowSettings.setName(WINDOW_NAME);
		try(var window = new Window(windowSettings)) {
//			window.swapBuffers();
			
//			Requested layer "VK_LAYER_KHRONOS_validation" failed to load!
//			libVkLayer_khronos_validation.so: 共有オブジェクトファイルを開けません: そのようなファイルやディレクトリはありません
			
			try(var vulkan = new Vulkan(vulkanSettings)) {
				var vkPhysicalDevice = PhysicalDevice.getFirstVkPhysicalDevice(vulkan);
				var physicalDevice = new PhysicalDevice(vkPhysicalDevice);
				var logicalDeviceSettings = new LogicalDeviceSettings();
				logicalDeviceSettings.setPhysicalDevice(physicalDevice);
				try(var logicalDevice = new LogicalDevice(logicalDeviceSettings)) {
					
					var surfaceSettings = new SurfaceSettings();
					surfaceSettings.setVulkan(vulkan);
					surfaceSettings.setPhysicalDevice(physicalDevice);
					surfaceSettings.setWindow(window);
					
					// vulkanインスタンスclose時にまとめてcloseしていいか不明、良いならやる
					try(var surface = new Surface(surfaceSettings)) {
						
						var settings = new QueueSettings();
						settings.setLogicalDevice(logicalDevice);
						Queue queue = new Queue(settings);
						
						var swapChainSettings = new SwapChainSettings();
						swapChainSettings.setLogicalDevice(logicalDevice);
						swapChainSettings.setSurface(surface);
						swapChainSettings.setWindow(window);
						try(var swapChain = new SwapChain(swapChainSettings)) {
							
							var renderSettings = new RenderSettings();
							renderSettings.setLogicalDevice(logicalDevice);
							renderSettings.setSwapChain(swapChain);
							renderSettings.setQueue(queue);
							
							
//							 Thread.sleep(2000);
							
							try(var render = new Render(renderSettings)) {
								
								
								var command = new ClearColorCommand(clearColor);
								final int testCount = 3;
								
								
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
								
								System.out.println("width " + swapChain.getWidth());
							}
						}
					}
				}
			}
		}
	}
	
	public static Model createTestModel(LogicalDevice logicalDevice) {
		var model = new Model();
		
		// 参考
		// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/ModelsCache.java
		// createVerticesBuffers
        var vertices = 3;
        var verticesSettings = new BufferSettings(logicalDevice);
        verticesSettings.setSize(vertices * VertexDescriptionBufferSettings.VALUES_XYZ * Float.BYTES);
        verticesSettings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
        verticesSettings.setRequestMask(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        verticesSettings.setOutUsage(BufferSettings.USAGE_TRANSFER_VERTEX);
        var verticesSource = new Buffer(verticesSettings);
        FloatBuffer verticesPosition = MemoryUtil.memFloatBuffer(verticesSource.map(), (int) verticesSettings.getSize());
        // 適当に三角形(xyz)
        int distance = 200;
        verticesPosition.put(0);
        verticesPosition.put(0);
        verticesPosition.put(0);
        verticesPosition.put(distance);
        verticesPosition.put(0);
        verticesPosition.put(0);
        verticesPosition.put(0);
        verticesPosition.put(distance);
        verticesPosition.put(0);
        
        // createIndicesBuffers
        var indices = new int[]{0, 1, 2, 0};
        var indicesSettings = new BufferSettings(logicalDevice);
        indicesSettings.setSize(indices.length * Integer.BYTES);
        indicesSettings.setUsage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT);
        indicesSettings.setRequestMask(VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        indicesSettings.setOutUsage(BufferSettings.USAGE_TRANSFER_INDEX);
        var indicesSource = new Buffer(indicesSettings);
        IntBuffer data = MemoryUtil.memIntBuffer(indicesSource.map(), (int) indicesSettings.getSize());
        data.put(indices);
        
        model.add(new Mesh(verticesSource, indicesSource, indices.length));
        return model;
	}

}
