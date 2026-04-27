// https://docs.vulkan.org/tutorial/latest/_attachments/31_shader_compute.slang
// にて今までのものとは完全に別物にされた

struct VSInput {
    float2 inPosition;
    // float4 inColor;
};

static float pointSize = 50;

struct VSOutput
{
    float4 pos : SV_Position;
    // float pointSize : SV_PointSize;
    // float3 fragColor : COLOR0;
};

struct PSInput
{
    float4 pos : SV_POSITION;
    // float3 fragColor : COLOR0;
    // float2 pointCoord : SV_PointCoord;
};

[shader("vertex")]
VSOutput vertMain(VSInput input) {
    VSOutput output;
    // output.pointSize = 14.0;
    output.pos = float4(input.inPosition, 1.0, 1.0);
    // output.fragColor = input.inColor.rgb;
    //output.pointCoord = (input.inPosition * float2(1.0, -1.0) + float2(1.0)) / 2.0;
    return output;
}

// デバッグ用 RGBA（現状Aが効いてないが、不明）
static float4 tempColor = float4(0.0, 1.0, 0.0, 1.0);

[shader("fragment")]
float4 fragMain(PSInput input) : SV_TARGET {
    // float2 coord = input.pointCoord - float2(0.5);
    // return float4(input.fragColor, 0.5 - length(coord));
    
    // 色を直接出力したい場合
    return tempColor;
}

struct Particle {
	float2 position;
	float velocity;
	// float2 velocity;
    // float4 color;
};

// ConstantBuffer<Particle> particlesIn;
// RWStructuredBuffer<ParticleArray> particlesOut;
RWStructuredBuffer<Particle> particles;



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