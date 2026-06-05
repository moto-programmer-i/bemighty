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
		this(values.length);
	}
	
	public VertexBinding(FloatVector2 vector) {
		this(vector.getX(), vector.getY());
	}
	
	public VertexBinding(int valueCount) {		
		// VertexInputAttributeDescriptionのために、バイト数のoffsetが必要
		bytes = Float.BYTES * valueCount;

		setFormat(valueCount);
	}
	
	private void setFormat(int valueCount) {
		// format設定、Vulkanのクソ設計によりRGBformatが流用されている
		format = switch(valueCount)  {
			// 個数によってformatをわけるしかない
			case 1 -> VK_FORMAT_R32_SFLOAT;
			case 2 -> VK_FORMAT_R32G32_SFLOAT;
			case 3 -> VK_FORMAT_R32G32B32_SFLOAT;
			case 4 -> VK_FORMAT_R32G32B32A32_SFLOAT;
			default -> throw new IllegalArgumentException(values.length + " 個のVertexBindingは定義できません");
		};
	}
	
	/**
	 * 
	 * @return 値を紐づけていない場合は空
	 */
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
