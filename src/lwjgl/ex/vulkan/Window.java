package lwjgl.ex.vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkInstance;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Window implements AutoCloseable {

	// The window handle
	private final long window;
	
//	private List<Surface> surfaces = new ArrayList<>();

	public Window(WindowSettings settings) {

		// https://www.lwjgl.org/guide
		// から大体コピペ

		// System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize GLFW");
		}

		// https://github.com/lwjglgamedev/vulkanbook/blob/master/bookcontents/chapter-01/chapter-01.md
		if (!GLFWVulkan.glfwVulkanSupported()) {
			throw new IllegalStateException("Cannot find a compatible Vulkan installable client driver (ICD)");
		}

		// Configure GLFW
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(settings.getWidth(), settings.getHeight(), settings.getName(), NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated
		// or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
		});

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(window, (vidmode.width() - pWidth.get(0)) / 2, (vidmode.height() - pHeight.get(0)) / 2);
		} // the stack frame is popped automatically


		// Make the window visible
		glfwShowWindow(window);
	}

	public void waitUntilClose() {
		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while (!glfwWindowShouldClose(window)) {
			// glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			// フレームレート制限方法が不明
			

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}
	
	public boolean shouldClose() {
		return glfwWindowShouldClose(window);
	}
	
	/**
	 * イベントを待つ
	 * https://www.glfw.org/docs/3.3/group__window.html#ga37bd57223967b4211d60ca1a0bf3c832
	 */
	public void pollEvents() {
		glfwPollEvents();
	}

	@Override
	public void close() throws Exception {
//		// Surfaceを削除
//		for(var surface: surfaces) {
//			surface.close();
//		}
//		surfaces.clear();
//		surfaces = null;
		
		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}
	
	/**
	 * GLFWVulkan.glfwCreateWindowSurfaceを内部で呼び出し
	 * @param stack
	 * @param instance
	 * @return Surfaceのハンドラ
	 */
	public long createWindowSurfaceHandler(MemoryStack stack, VkInstance instance) {
		LongBuffer SurfaceBuffer = stack.mallocLong(1);
		Vulkan.throwExceptionIfFailed(
				GLFWVulkan.glfwCreateWindowSurface(instance, window, null, SurfaceBuffer),
				"GLFWVulkan.glfwCreateWindowSurfaceに失敗しました");
        return SurfaceBuffer.get(0);
	}
	
//	public Surface getSurface() {
//		return surfaces.getFirst();
//	}
	
//	/**
//	 * 
//	 * @return
//	 * @throws NullPointerException GLFWVulkan.glfwGetRequiredInstanceExtensionsがnullを返した場合
//	 */
//	public PointerBuffer getRequiredExtensions() throws NullPointerException {
//        PointerBuffer requiredExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
//        if (requiredExtensions == null) {
//            throw new NullPointerException("glfwGetRequiredInstanceExtensionsがnull");
//        }
//        return requiredExtensions;
//    }
	
	public VkExtent2D getSize(MemoryStack stack) {
			IntBuffer widthBuffer = stack.mallocInt(1);
			IntBuffer heightBuffer = stack.mallocInt(1);
			glfwGetWindowSize(window, widthBuffer, heightBuffer);
			var size = VkExtent2D.malloc(stack);
			size.width(widthBuffer.get(0));
			size.height(heightBuffer.get(0));
			return size;
	}
}
