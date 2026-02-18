package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-03/src/main/java/org/vulkanb/eng/graph/vk/PhysDevice.java

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK13.*;

public class PhysicalDevice {
	private final VkPhysicalDevice device;
	
	// 実装するか未定
	// https://github.com/lwjglgamedev/vulkanbook/blob/master/bookcontents/chapter-03/chapter-03.md
	/*
	vkDeviceExtensions:Bufferサポートされている拡張機能 (名前とバージョン) のリストが含まれています。
	vkMemoryProperties: このデバイスがサポートするさまざまなメモリ ヒープに関連する情報が含まれています。
	vkPhysicalDevice: 物理デバイス自体へのハンドル。
	vkPhysicalDeviceFeatures: 深度クランプ、特定のタイプのシェーダーなどをサポートするかどうかなど、このデバイスでサポートされているきめ細かい機能が含まれます。
	vkPhysicalDeviceProperties: デバイス名、ベンダー、制限などの物理デバイスのプロパティが含まれます。
	
	*/
	/**
	 * デバイスの全てのキュー（GPUの実行するコマンドを保持する待ち行列）
	 * https://chaosplant.tech/do/vulkan/2-3/
	 */
	private final List<QueueFamilyProperties> queueFamilyPropertiesList = new ArrayList<QueueFamilyProperties>();
	
	private OptionalInt graphicsQueueIndex = OptionalInt.empty();

	/**
	 * getFirstPhysicalDeviceから初期化
	 * @param vulkan
	 */
	public PhysicalDevice(Vulkan vulkan) {
		this(getFirstVkPhysicalDevice(vulkan));
	}
	
	public PhysicalDevice(VkPhysicalDevice device) {
		this.device = device;
		try (var stack = MemoryStack.stackPush()) {
			// queueFamilyPropertiesListを初期化
			// C++側で初期化された情報をJava側にうつす、パフォーマンスにどの程度影響するかは不明
			IntBuffer queueFamilyPropertiesCountBuffer = stack.mallocInt(1);
	        try(var queueFamilyPropertiesBuffer = getVkQueueFamilyPropertiesBuffer(device, queueFamilyPropertiesCountBuffer)) {
	            int queueFamilyPropertiesCapacity = queueFamilyPropertiesBuffer.capacity();
	            for (int i = 0; i < queueFamilyPropertiesCapacity; ++i) {
	            	var queueFamilyProperties = queueFamilyPropertiesBuffer.get(i);
		        	var thisQueueFamilyProperties = new QueueFamilyProperties();
		        	
		        	// キューの必要な情報をコピー
		        	thisQueueFamilyProperties.setQueueCount(queueFamilyProperties.queueCount());
		        	thisQueueFamilyProperties.setQueueFlags(queueFamilyProperties.queueFlags());
		        	
		        	// グラフィックキューの位置を保存
		        	if (graphicsQueueIndex.isEmpty() && (queueFamilyProperties.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
		        		graphicsQueueIndex = OptionalInt.of(i);
		            }
		        	

		        	queueFamilyPropertiesList.add(thisQueueFamilyProperties);
	            }
	        	
	        }
		}
	}
	
	public VkPhysicalDevice getDevice() {
		return device;
	}

	public List<QueueFamilyProperties> getQueueFamilyPropertiesList() {
		return queueFamilyPropertiesList;
	}
	
	public static VkPhysicalDevice getFirstVkPhysicalDevice(Vulkan vulkan) {
		return getAllVkPhysicalDevice(vulkan).get(0);
	}

	public OptionalInt getGraphicsQueueIndex() {
		return graphicsQueueIndex;
	}
	
	
	public int findMemoryTypeIndex(int typeFilter, int properties) throws IllegalArgumentException {
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/88e4485af4d708d4fd441a9ef80241b1164eefb4/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L511
        try (var stack = MemoryStack.stackPush()) {
            VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.calloc(stack);
            vkGetPhysicalDeviceMemoryProperties(device, memoryProperties);
            for (int i = 0, typeMask = 1; i < memoryProperties.memoryTypeCount(); ++i, typeMask <<= 1) {
            	// デバイスでサポートされているか
                if ((typeFilter & typeMask) != 0) {
                	// プロパティが一致するか
                    if ((memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                        return i;
                    }
                }
            }
        }
        throw new IllegalArgumentException("適合するメモリタイプが存在しません");
    }

	/**
	 * 条件に合う最初のデバイスを返す
	 * @param vulkan
	 * @param filter
	 * @return
	 */
	public static VkPhysicalDevice getFirstVkPhysicalDevice(Vulkan vulkan, PhysicalDeviceFilter filter) {
		return getAllVkPhysicalDevice(vulkan, filter).get(0);
	}

	public static List<VkPhysicalDevice> getAllVkPhysicalDevice(Vulkan vulkan) {
		return getAllVkPhysicalDevice(vulkan, new PhysicalDeviceFilter());
	}

	public static List<VkPhysicalDevice> getAllVkPhysicalDevice(Vulkan vulkan, PhysicalDeviceFilter filter) {
		var devices = new ArrayList<VkPhysicalDevice>();

		try (var stack = MemoryStack.stackPush()) {
			// 物理デバイスの数を取得
			PointerBuffer pPhysicalDevices;
			IntBuffer deviceCountBuffer = stack.mallocInt(1);
			Vulkan.throwExceptionIfFailed(vkEnumeratePhysicalDevices(vulkan.getVkInstance(), deviceCountBuffer, null),
					"物理デバイスの数の取得に失敗しました");
			int deviceCount = deviceCountBuffer.get(0);
			pPhysicalDevices = stack.mallocPointer(deviceCount);
			Vulkan.throwExceptionIfFailed(
					vkEnumeratePhysicalDevices(vulkan.getVkInstance(), deviceCountBuffer, pPhysicalDevices),
					"物理デバイスの取得に失敗しました");

			int capacity = pPhysicalDevices.capacity();
			for (int i = 0; i < capacity; ++i) {
				var device = new VkPhysicalDevice(pPhysicalDevices.get(i), vulkan.getVkInstance());

				// Extensionで絞り込み
				var supportedExtensions = getExtensions(device);
				if (!supportedExtensions.containsAll(filter.getExtensions())) {
					continue;
				}
				
				// Featuresで絞り込み
				if(filter.isSynchronization2()) {
					// 機能をチェックするのに各構造体を作らなければならない
					var queryExtendedDynamicStateFeatures = VkPhysicalDeviceExtendedDynamicStateFeaturesEXT.calloc(stack)
			                .sType$Default();
					var queryVulkan13Features = VkPhysicalDeviceVulkan13Features.calloc(stack)
			                .sType$Default()
			                .pNext(queryExtendedDynamicStateFeatures.address());
					var queryDeviceFeatures2 = VkPhysicalDeviceFeatures2.calloc(stack)
			                .sType$Default()
			                .pNext(queryVulkan13Features.address());
		            vkGetPhysicalDeviceFeatures2(device, queryDeviceFeatures2);

			            // 保留
//			            if (!query_vulkan13_features.dynamicRendering()) {
//			                throw new RuntimeException("Dynamic Rendering feature is missing");
//			            }
			            if (!queryVulkan13Features.synchronization2()) {
			                // GPUに同期機能（Synchronization2）がない
			            	continue;
			            }
				}
				 

				// その他で絞り込み
				if (filter.hasGraphicsQueueFamily()) {
					if(!hasGraphicsQueueFamily(getQueueFamilyProperties(device))) {
						continue;
					}
				}

				devices.add(device);
			}
		}

		return devices;
	}

	public static Set<String> getExtensions(VkPhysicalDevice device) {
		try (var stack = MemoryStack.stackPush()) {
			var vkDeviceExtensionsBuffer = stack.mallocInt(1);
			// Get device extensions
			Vulkan.throwExceptionIfFailed(
					vkEnumerateDeviceExtensionProperties(device, (String) null, vkDeviceExtensionsBuffer, null),
					"デバイスのVulkan拡張機能の数の取得に失敗しました");
			var vkDeviceExtensions = VkExtensionProperties.calloc(vkDeviceExtensionsBuffer.get(0));
			Vulkan.throwExceptionIfFailed(vkEnumerateDeviceExtensionProperties(device, (String) null,
					vkDeviceExtensionsBuffer, vkDeviceExtensions), "デバイスのVulkan拡張機能の取得に失敗しました");

			int extensionCount = vkDeviceExtensionsBuffer.get();
			var extensions = new HashSet<String>(extensionCount);
			for (int i = 0; i < extensionCount; i++) {
				extensions.add(vkDeviceExtensions.get(i).extensionNameString());
			}
			return extensions;
		}
	}

	public static List<VkQueueFamilyProperties> getQueueFamilyProperties(VkPhysicalDevice device) {
		var list = new ArrayList<VkQueueFamilyProperties>();
		try (var stack = MemoryStack.stackPush()) {
			IntBuffer queueFamilyPropertiesBuffer = stack.mallocInt(1);
			vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyPropertiesBuffer, null);
			int queueFamilyProperyCount = queueFamilyPropertiesBuffer.get(0);
			var queueFamilyProperties = VkQueueFamilyProperties.calloc(queueFamilyProperyCount);
			vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyPropertiesBuffer, queueFamilyProperties);
			for (int i = 0; i < queueFamilyProperyCount; ++i) {
				list.add(queueFamilyProperties.get(i));
			}
			return list;
		}
	}
	
	public static boolean hasGraphicsQueueFamily(List<VkQueueFamilyProperties> properties) {
		for(var property: properties) {
			if ((property.queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                return true;
            }
		}
		return false;
	}
	
	// 
	/**
	 * （vkGetPhysicalDeviceQueueFamilyPropertiesが２回呼ぶことが前提の意味不明の設計のため作成）
	 * LWJGLが修正され次第削除
	 * https://javadoc.lwjgl.org/org/lwjgl/vulkan/VK10.html#vkGetPhysicalDeviceQueueFamilyProperties(org.lwjgl.vulkan.VkPhysicalDevice,java.nio.IntBuffer,org.lwjgl.vulkan.VkQueueFamilyProperties.Buffer)
	 * https://github.com/LWJGL/lwjgl3/blob/50f3b0e6f09012133113251dd11cc44fc2f3913f/modules/samples/src/test/java/org/lwjgl/demo/vulkan/HelloVulkan.java#L533C13-L533C53
	 * @param physicalDevice
	 * @param stack
	 * @param pQueueFamilyPropertyCount
	 * @return
	 */
	public static VkQueueFamilyProperties.Buffer getVkQueueFamilyPropertiesBuffer(VkPhysicalDevice physicalDevice, IntBuffer pQueueFamilyPropertyCount) {
		// 意味不明の設計により、vkGetPhysicalDeviceQueueFamilyPropertiesを2回呼ばなければいけない
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, null);
        var queueFamilyPropertiesBuffer = VkQueueFamilyProperties.malloc(pQueueFamilyPropertyCount.get(0));
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyPropertyCount, queueFamilyPropertiesBuffer);
        return queueFamilyPropertiesBuffer;
	}
}
