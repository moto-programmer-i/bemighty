package bemighty;

import lwjgl.ex.vulkan.VertexBinding;
import lwjgl.ex.vulkan.VertexBindingBuilder;
import motopgi.utils.FloatVector2;

public class Particle {
	// Particle(position + velocity)
	private FloatVector2 position;
	private FloatVector2 velocity;
	
	public Particle() {
		position = new FloatVector2();
		velocity = new FloatVector2();
	}

	public Particle(FloatVector2 position, FloatVector2 velocity) {
		this.position = position;
		this.velocity = velocity;
	}

	public FloatVector2 getPosition() {
		return position;
	}

	public void setPosition(FloatVector2 position) {
		this.position = position;
	}

	public FloatVector2 getVelocity() {
		return velocity;
	}

	public void setVelocity(FloatVector2 velocity) {
		this.velocity = velocity;
	}
	
	public VertexBindingBuilder createBinding() {
		// 変数と対応させなければいけない
		return VertexBindingBuilder.create(new VertexBinding(position))
				.add(new VertexBinding(velocity));
	}

}
