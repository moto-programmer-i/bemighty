// Model.javaのverticesと対応しなければならない。
// 二重定義は避けたいが、現状どうしようもない
struct VSInput {
    float3 inPosition;
    // チュートリアルでは含まれるが、今のところ使用していない
    // float3 inColor;
    float2 inTexCoord;
};

struct UniformBuffer {
    float4x4 model;
    float4x4 view;
    float4x4 proj;
};
ConstantBuffer<UniformBuffer> ubo;

struct VertexOutput {
    float4 position : SV_Position;
    // チュートリアルでは含まれるが、今のところ使用していない
    // float3 color;
    float2 texCoord;
};

// すべての頂点に対して呼び出される
[shader("vertex")]
VertexOutput vertMain(VSInput input) {
    VertexOutput output;
    // 変形なし
    // output.position = float4(input.inPosition, 1.0);
    
    // UniformBufferObjectを用いた座標の変換 
    // output.position = mul(ubo.proj, mul(ubo.view, mul(ubo.model, float4(input.inPosition, 1.0))));
    output.position = mul(ubo.proj, float4(input.inPosition, 1.0));
    
    // output.color = input.inColor;
    output.texCoord = input.inTexCoord;
    return output;
}

// テクスチャが入っているらしい。どうやってバッファから受け取っているかは不明
Sampler2D texture;

// デバッグ用 RGBA（現状Aが効いてないが、不明）
static float4 tempColor = float4(0.0, 1.0, 0.0, 1.0);

[shader("fragment")]
float4 fragMain(VertexOutput inVert) : SV_Target
{
    // テクスチャ座標から色を取得
    return texture.Sample(inVert.texCoord);
    
    // 色を直接出力したい場合
    // return tempColor;
}