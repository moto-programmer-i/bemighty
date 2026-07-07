// -------------Java側と対応しなければならない↓------------
struct VSInput {
    float3 position;
    // チュートリアルでは含まれるが、今のところ使用していない
    // float3 color;
    float2 texCoord;
};

struct UniformBuffer {
	// 回転軸
	float3 rotateAxis;
	
	// ここの間になぜか不明なfloatが存在する
	
	// (1 - cosθ)回転
	float3 oneCosRotateAxis; 	
	
	float cos;
	float sin;
	
	// cos sinの後だと、なぜか値が入らなかった
	// float3 oneCosRotateAxis; // (1 - cosθ)回転
	
	float scale;
	
	
    // float4x4 model;
    // float4x4 view;今まで使ってた？
    // float4x4 proj;
};
ConstantBuffer<UniformBuffer> ubo;
// -------------Java側と対応しなければならない↑------------



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
    // output.position = float4(input.position, 1.0);
    
    // 移動、拡大、回転のみ
    // output.position = mul(ubo.model, float4(input.position, 1.0));
    
    // UniformBufferObjectを用いた座標の変換 
    // output.position = mul(ubo.proj, mul(ubo.view, mul(ubo.model, float4(input.position, 1.0))));


	// 一旦回転のみ
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	output.position = float4(
		(ubo.cos * input.position
			+ ubo.oneCosRotateAxis * mul(ubo.rotateAxis, input.position) 
			+ ubo.sin * cross(ubo.rotateAxis, input.position))
			
			// 拡大
			* ubo.scale
			
			,
		1.0);
	
	// デバッグ用
	/*
	if (ubo.scale > 0) {
		output.position.x += 1;
	}
	*/
    

    output.texCoord = input.texCoord;
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