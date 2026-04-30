package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;

import motopgi.utils.FloatVector2;

/**
 * VertexInputAttributeDescription とバッファ対応するためのヘルパークラス
 * VertexBindingBuilderも参照
 */
public class VertexBinding {
	private float[] values;
	private int bytes;
	private int format;
	
	public VertexBinding(float... values) {
		this.values = values;
		
		// VertexInputAttributeDescriptionのために、バイト数のoffsetが必要
		bytes = Float.BYTES * values.length;
		
		// format設定、Vulkanのクソ設計によりRGBformatが流用されている
		// 他でも必要になる場合はメソッド化
		format = switch(values.length)  {
			// 個数によってformatをわけるしかない
			case 1 -> VK_FORMAT_R32_SFLOAT;
			case 2 -> VK_FORMAT_R32G32_SFLOAT;
			case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
			case 4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
			default -> throw new IllegalArgumentException(values.length + " 個のVertexBindingは定義できません");
		};
	}
	
	public VertexBinding(FloatVector2 vector) {
		this(vector.getX(), vector.getY());
	}
	
	public float[] getValues() {
		return values;
	}
	
	public int getBytes() {
		return bytes;
	}
	public int getFormat() {
		return format;
	}
	
	

}
