// -------------Java側と対応しなければならない↓------------
#define PARTCLE_NUM 2
struct Particle {
	float2 position;
};
struct ParticleSSBO {
    Particle particles;
};
// StructuredBuffer<ParticleSSBO> particlesIn;
RWStructuredBuffer<ParticleSSBO> particlesOut;
// -------------Java側と対応しなければならない↑------------


struct VSOutput
{
    float4 pos : SV_Position;
    float pointSize : SV_PointSize;
};

// static var tempPosition = float4(0.0, 0.0, 0.0, 1.0);

[shader("vertex")]
VSOutput vertMain(Particle particle) {
	VSOutput output;
    output.pos = float4(particle.position, 0.0, 1.0);
    
    // computeで変更したstaticは反映されない。
	// output.pos = float4(offset, 0.0, 0.0, 1.0);
    // output.pos = tempPosition;
        
    output.pointSize = 60.0;
    
    
    
    
    return output;
}

// デバッグ用 RGBA（現状Aが効いてないが、不明）
static const float4 tempColor = float4(0.0, 1.0, 0.0, 1.0);

[shader("fragment")]
float4 fragMain() : SV_TARGET {
    // 色を直接出力したい場合
    return tempColor;
}



static const float velocity = 0.2;


// a "thread-local" global variable
// static int c = 3;

[shader("compute")]
// GPU処理では、マルチスレッドが基本。1点を1スレッドに処理させる
[numthreads(PARTCLE_NUM,1,1)]
void compMain(uint3 threadId : SV_DispatchThreadID)
{     
    var index = threadId.x;
    // 初期化の方法が不明、バッファでbooleanを送るしかないのか？
   
    // 点ごとに適当に動かす
    particlesOut[index].particles.position += index * velocity;
}