// -------------Java側と対応しなければならない↓------------
struct Particle {
	float2 position;
	float velocity;
};
RWStructuredBuffer<Particle> particles;
// -------------Java側と対応しなければならない↑------------






[shader("vertex")]
void vertMain() {
}

// デバッグ用 RGBA（現状Aが効いてないが、不明）
static float4 tempColor = float4(0.0, 1.0, 0.0, 1.0);

[shader("fragment")]
float4 fragMain() : SV_TARGET {
    // 色を直接出力したい場合
    return tempColor;
}


[shader("compute")]
// 意味不明
[numthreads(1,1,1)]
void compMain(uint3 threadId : SV_DispatchThreadID)
{
    uint index = threadId.x;
    
    // 適当。確認ができない
    float velocity = particles[0].velocity;
    
    
    /*
    // 一旦index 0
    particlesOut[0].particles.position = particlesIn.position;
    particlesOut[0].particles.velocity = particlesIn.velocity;

	/*
    particlesOut[index].particles.position = particlesIn[index].particles.position + particlesIn[index].particles.velocity.xy * ubo.deltaTime;
    particlesOut[index].particles.velocity = particlesIn[index].particles.velocity;

    // Flip movement at window border
    if ((particlesOut[index].particles.position.x <= -1.0) || (particlesOut[index].particles.position.x >= 1.0)) {
        particlesOut[index].particles.velocity.x = -particlesOut[index].particles.velocity.x;
    }
    if ((particlesOut[index].particles.position.y <= -1.0) || (particlesOut[index].particles.position.y >= 1.0)) {
        particlesOut[index].particles.velocity.y = -particlesOut[index].particles.velocity.y;
    }
    */

}