package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK14.*;

import java.util.OptionalInt;

/**
 * VulkanがusageとdescriptorTypeをわけているが、
 * まとまっていた方が型チェックができるため統合
 */
public enum BufferType {
	// trasfer sourceの方は別でどうにかしたい
	
	VERTEX(BufferType.USAGE_VERTEX_DESTINATION, OptionalInt.of(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)),
	/**
	 * INDEX
	 * （Descriptorでは送らない）
	 */
	INDEX(BufferType.USAGE_INDEX_DESTINATION, OptionalInt.empty()),
	STORAGE(BufferType.USAGE_SHADER_STORAGE, OptionalInt.of(VK_DESCRIPTOR_TYPE_STORAGE_BUFFER));
	private final int usage;
	private OptionalInt descriptorType;
	
	private BufferType(int usage, OptionalInt descriptorType) {
		this.usage = usage;
		this.descriptorType = descriptorType;
	}
	
	public int getUsage() {
		return usage;
	}


	public int getDescriptorType() {
		return descriptorType.getAsInt();
	}



	public static final int USAGE_SOURCE = VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
	
	
	/**
	 * vk::BufferUsageFlagBits::eTransferDst | vk::BufferUsageFlagBits::eVertexBuffer
	 */
	public static final int USAGE_VERTEX_DESTINATION = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
	
	public static final int USAGE_INDEX_DESTINATION = VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;
	
	
	/**
	 * Compute Shaderに使われるストレージ用のバッファ
	 * vk::BufferUsageFlagBits::eStorageBuffer | vk::BufferUsageFlagBits::eVertexBuffer | vk::BufferUsageFlagBits::eTransferDst
	 * https://docs.vulkan.org/tutorial/latest/11_Compute_Shader.html#_shader_storage_buffer_objects_ssbo
	 */
	public static final int USAGE_SHADER_STORAGE = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | USAGE_VERTEX_DESTINATION;
	
	
}
