// -------------Java側と対応しなければならない↓------------
#define PARTCLE_NUM 2
struct Particle {
	float2 position;
	float velocity;
};
RWStructuredBuffer<Particle> particles;
// -------------Java側と対応しなければならない↑------------


struct VSOutput
{
    float4 pos : SV_Position;
    float pointSize : SV_PointSize;
};



[shader("vertex")]
VSOutput vertMain(Particle particle) {
	VSOutput output;
    output.pos = float4(particle.position, 0.0, 1.0);
        
    output.pointSize = 100.0;
    
    
    
    
    return output;
}

// デバッグ用 RGBA（現状Aが効いてないが、不明）
static float4 tempColor = float4(0.0, 1.0, 0.0, 1.0);

[shader("fragment")]
float4 fragMain() : SV_TARGET {
    // 色を直接出力したい場合
    return tempColor;
}



[shader("compute")]
// GPU処理では、マルチスレッドが基本。1点を1スレッドに処理させる
[numthreads(PARTCLE_NUM,1,1)]
void compMain(uint3 threadId : SV_DispatchThreadID)
{     
    // 適当。確認ができない
    particles[0].position.x = -0.5;
}