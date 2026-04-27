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
    // 適当。確認ができない
    float velocity = particles[0].velocity;
}