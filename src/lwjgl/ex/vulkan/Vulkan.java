package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-02/src/main/java/org/vulkanb/eng/graph/vk/Instance.java

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.*;
import org.lwjgl.vulkan.*;

import java.nio.*;
import java.util.*;

import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.VK13.*;

public class Vulkan implements AutoCloseable {
	private static final String VALIDATION_LAYER = "VK_LAYER_KHRONOS_validation";
	private VulkanSettings settings;
	private VkInstance vkInstance;
	private VkDebugUtilsMessengerCreateInfoEXT debugUtils;
	private long vkDebugHandle;

	public Vulkan(VulkanSettings settings) {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
			// Create application information
			ByteBuffer appShortName = stack.UTF8(settings.getName());
			var appInfo = VkApplicationInfo.calloc(stack).sType$Default().pApplicationName(appShortName)
					.applicationVersion(settings.getApplicationVersion()).pEngineName(appShortName)
					.engineVersion(settings.getEngineVersion()).apiVersion(VK_API_VERSION_1_3);

			// Validation layers
			List<String> validationLayers = getSupportedValidationLayers();
			int numValidationLayers = validationLayers.size();
			boolean supportsValidation = settings.isValidate();
			if (supportsValidation && numValidationLayers == 0) {
				supportsValidation = false;
			}

			// Set required layers
			PointerBuffer requiredLayers = null;
			if (supportsValidation) {
				requiredLayers = stack.mallocPointer(numValidationLayers);
				for (int i = 0; i < numValidationLayers; ++i) {
					requiredLayers.put(i, stack.ASCII(validationLayers.get(i)));
				}
			}

//			Set<String> instanceExtensions = getInstanceExtensions();
			// 不明
//            boolean usePortability = instanceExtensions.contains(PORTABILITY_EXTENSION) &&
//                    VkUtils.getOS() == VkUtils.OSType.MACOS;

			// GLFW Extension
			PointerBuffer glfwExtensions = GLFWVulkan.glfwGetRequiredInstanceExtensions();
			if (glfwExtensions == null) {
				throw new RuntimeException("Failed to find the GLFW platform surface extensions");
			}
			var additionalExtensions = new ArrayList<ByteBuffer>();
			if (supportsValidation) {
				additionalExtensions.add(stack.UTF8(EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME));
			}
			// 不明
//            if (usePortability) {
//                additionalExtensions.add(stack.UTF8(PORTABILITY_EXTENSION));
//            }
			int numAdditionalExtensions = additionalExtensions.size();

			PointerBuffer requiredExtensions = stack
					.mallocPointer(glfwExtensions.remaining() + numAdditionalExtensions);
			requiredExtensions.put(glfwExtensions);
			for (int i = 0; i < numAdditionalExtensions; i++) {
				requiredExtensions.put(additionalExtensions.get(i));
			}
			requiredExtensions.flip();

			long extension = MemoryUtil.NULL;
			if (supportsValidation) {
				debugUtils = createDebugCallBack();
				extension = debugUtils.address();
			}

			// Create instance info
			var instanceInfo = VkInstanceCreateInfo.calloc(stack).sType$Default().pNext(extension)
					.pApplicationInfo(appInfo).ppEnabledLayerNames(requiredLayers)
					.ppEnabledExtensionNames(requiredExtensions);
			// 不明
//            if (usePortability) {
//                instanceInfo.flags(0x00000001); // VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR
//            }

			PointerBuffer pInstance = stack.mallocPointer(1);
			throwExceptionIfFailed(
					vkCreateInstance(instanceInfo, null, pInstance),
					"vkCreateInstanceに失敗しました");
			
			vkInstance = new VkInstance(pInstance.get(0), instanceInfo);

			vkDebugHandle = VK_NULL_HANDLE;
			if (supportsValidation) {
				LongBuffer longBuff = stack.mallocLong(1);
				throwExceptionIfFailed(
						vkCreateDebugUtilsMessengerEXT(vkInstance, debugUtils, null, longBuff),
						"vkCreateInstanceに失敗しました");
				vkDebugHandle = longBuff.get(0);
			}
		}
	}

	@Override
	public void close() throws Exception {
		if (vkDebugHandle != VK_NULL_HANDLE) {
			vkDestroyDebugUtilsMessengerEXT(vkInstance, vkDebugHandle, null);
			vkDebugHandle = VK_NULL_HANDLE;
		}
		if (vkInstance != null) {
			vkDestroyInstance(vkInstance, null);
			vkInstance = null;
		}
		
		if (debugUtils != null) {
			debugUtils.pfnUserCallback().free();
			debugUtils.free();
			debugUtils = null;
		}
	}

	/**
	 * vkCreateInstanceなどのコード変換
	 * https://github.com/lwjglgamedev/vulkanbook/blob/ebe25ab57930b80b62e915fed8de5f97169ecd72/booksamples/chapter-02/src/main/java/org/vulkanb/eng/graph/vk/VkUtils.java#L29
	 * https://javadoc.lwjgl.org/org/lwjgl/vulkan/VK10.html#vkCreateInstance(org.lwjgl.vulkan.VkInstanceCreateInfo,org.lwjgl.vulkan.VkAllocationCallbacks,org.lwjgl.PointerBuffer)
	 * https://docs.vulkan.org/refpages/latest/refpages/source/vkCreateInstance.html
	 * 
	 * @param code
	 * @return
	 */
	public static String codeToMessage(int code) {
		return switch (code) {
		case VK_NOT_READY -> "VK_NOT_READY";
		case VK_TIMEOUT -> "VK_TIMEOUT";
		case VK_EVENT_SET -> "VK_EVENT_SET";
		case VK_EVENT_RESET -> "VK_EVENT_RESET";
		case VK_INCOMPLETE -> "VK_INCOMPLETE";
		case VK_ERROR_OUT_OF_HOST_MEMORY -> "VK_ERROR_OUT_OF_HOST_MEMORY";
		case VK_ERROR_OUT_OF_DEVICE_MEMORY -> "VK_ERROR_OUT_OF_DEVICE_MEMORY";
		case VK_ERROR_INITIALIZATION_FAILED -> "VK_ERROR_INITIALIZATION_FAILED";
		case VK_ERROR_DEVICE_LOST -> "VK_ERROR_DEVICE_LOST";
		case VK_ERROR_MEMORY_MAP_FAILED -> "VK_ERROR_MEMORY_MAP_FAILED";
		case VK_ERROR_LAYER_NOT_PRESENT -> "VK_ERROR_LAYER_NOT_PRESENT";
		case VK_ERROR_EXTENSION_NOT_PRESENT -> "VK_ERROR_EXTENSION_NOT_PRESENT";
		case VK_ERROR_FEATURE_NOT_PRESENT -> "VK_ERROR_FEATURE_NOT_PRESENT";
		case VK_ERROR_INCOMPATIBLE_DRIVER -> "VK_ERROR_INCOMPATIBLE_DRIVER";
		case VK_ERROR_TOO_MANY_OBJECTS -> "VK_ERROR_TOO_MANY_OBJECTS";
		case VK_ERROR_FORMAT_NOT_SUPPORTED -> "VK_ERROR_FORMAT_NOT_SUPPORTED";
		case VK_ERROR_FRAGMENTED_POOL -> "VK_ERROR_FRAGMENTED_POOL";
		case VK_ERROR_UNKNOWN -> "VK_ERROR_UNKNOWN";
		case VK_SUCCESS -> "VK_SUCCESS";
		case KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR -> "VK_ERROR_OUT_OF_DATE_KHR";
		case KHRSwapchain.VK_SUBOPTIMAL_KHR -> "VK_SUBOPTIMAL_KHR";
		default -> "Not mapped";
		};
	}
	
	/**
	 * 
	 * @param code
	 * @param message
	 * @throws IllegalArgumentException codeがVK_SUCCESS以外
	 */
	public static void throwExceptionIfFailed(int code, String message) throws IllegalArgumentException {
		// VulkanがCのコードのため、失敗時に例外を投げるメソッドが必要
		if (code == VK_SUCCESS) {
			return;
		}
		
		System.err.println(codeToMessage(code));
		throw new IllegalArgumentException(message);
	}

	private static Set<String> getInstanceExtensions() {
		Set<String> instanceExtensions = new HashSet<>();
		try (var stack = MemoryStack.stackPush()) {
			IntBuffer numExtensionsBuf = stack.callocInt(1);
			vkEnumerateInstanceExtensionProperties((String) null, numExtensionsBuf, null);
			int numExtensions = numExtensionsBuf.get(0);

			var instanceExtensionsProps = VkExtensionProperties.calloc(numExtensions, stack);
			vkEnumerateInstanceExtensionProperties((String) null, numExtensionsBuf, instanceExtensionsProps);
			for (int i = 0; i < numExtensions; ++i) {
				VkExtensionProperties props = instanceExtensionsProps.get(i);
				String extensionName = props.extensionNameString();
				instanceExtensions.add(extensionName);
			}
		}
		return instanceExtensions;
	}

	private static List<String> getSupportedValidationLayers() {
		try (var stack = MemoryStack.stackPush()) {
			IntBuffer numLayersArr = stack.callocInt(1);
			vkEnumerateInstanceLayerProperties(numLayersArr, null);
			int numLayers = numLayersArr.get(0);

			var propsBuf = VkLayerProperties.calloc(numLayers, stack);
			vkEnumerateInstanceLayerProperties(numLayersArr, propsBuf);
			List<String> supportedLayers = new ArrayList<>();
			for (int i = 0; i < numLayers; ++i) {
				VkLayerProperties props = propsBuf.get(i);
				String layerName = props.layerNameString();
				supportedLayers.add(layerName);
			}

			// Main validation layer
			List<String> layersToUse = new ArrayList<>();
			if (supportedLayers.contains(VALIDATION_LAYER)) {
				layersToUse.add(VALIDATION_LAYER);
			}

			return layersToUse;
		}
	}

	private static VkDebugUtilsMessengerCreateInfoEXT createDebugCallBack() {
		return VkDebugUtilsMessengerCreateInfoEXT.calloc().sType$Default()
				// 設定に組み込むべきか？
				.messageSeverity(
						VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT)
				.messageType(
						VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
								| VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
				.pfnUserCallback((messageSeverity, messageTypes, pCallbackData, pUserData) -> {
					VkDebugUtilsMessengerCallbackDataEXT callbackData = VkDebugUtilsMessengerCallbackDataEXT
							.create(pCallbackData);
//                    if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT) != 0) {
//                        Logger.info(DBG_CALL_BACK_PREF, callbackData.pMessageString());
//                    } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT) != 0) {
//                        Logger.warn(DBG_CALL_BACK_PREF, callbackData.pMessageString());
//                    } else if ((messageSeverity & VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT) != 0) {
//                        Logger.error(DBG_CALL_BACK_PREF, callbackData.pMessageString());
//                    } else {
//                        Logger.debug(DBG_CALL_BACK_PREF, callbackData.pMessageString());
//                    }

					// どう書くべきか不明
					System.err.println(callbackData.pMessageString());
					return VK_FALSE;
				});
	}
	
	

	public VkInstance getVkInstance() {
		return vkInstance;
	}

	public VulkanSettings getSettings() {
		return settings;
	}
}
