package bemighty;

import lwjgl.ex.vulkan.VertexBinding;
import lwjgl.ex.vulkan.VertexBindingBuilder;
import motopgi.utils.FloatVector2;

public class Particle {
	private FloatVector2 position;
	
	public Particle() {
		position = new FloatVector2();
	}

	public Particle(FloatVector2 position) {
		this.position = position;
	}

	public FloatVector2 getPosition() {
		return position;
	}

	public void setPosition(FloatVector2 position) {
		this.position = position;
	}

	
	
	public VertexBindingBuilder createBinding() {
		// 変数と対応させなければいけない
		return VertexBindingBuilder.create(new VertexBinding(position));
	}
	
	public static Particle[] createArray(int length) {
		var particles = new Particle[length];
		for(int i = 0; i < length; ++i) {
			particles[i] = new Particle();
		}
		return particles;
	}
}
