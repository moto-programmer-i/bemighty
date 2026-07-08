// -------------Java側と対応しなければならない↓------------
struct VSInput {
    float3 position;
    // チュートリアルでは含まれるが、今のところ使用していない
    // float3 color;
    float2 texCoord;
};

struct Rotation {
	// 回転軸
	float3 axis;
	
	// ここの間になぜか不明なfloatが存在する
	
	// (1 - cosθ)回転
	float3 oneCosAxis;
	
	float cos;
	float sin;
}

struct UniformBuffer {
	float3 translate;
	
	// モデル自体の回転
	Rotation local;
	
	// ここの間になぜか不明なfloat3が存在する
	
	// カメラ用の回転
	Rotation camera;
	
	// floatの後だと、なぜかfloat3の値が入らない （不明な空間かも？）
	
	float scale;
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

	

	// モデルの回転
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	var position = ubo.local.cos * input.position
			+ ubo.local.oneCosAxis * mul(ubo.local.axis, input.position) 
			+ ubo.local.sin * cross(ubo.local.axis, input.position)
			
			// 拡大
			* ubo.scale
			
			// 平行移動
			+ ubo.translate;
	
	// カメラ座標系へ（カメラの向きと逆回転）		
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	position = ubo.camera.cos * position
			+ ubo.camera.oneCosAxis * mul(ubo.camera.axis, position) 
			+ ubo.camera.sin * cross(ubo.camera.axis, position);
	
	
	output.position = float4(position, 1.0);
	
	/*
	// デバッグ用
	if (ubo.translate.x > 0.1) {
	// if (ubo.local.axis.x > 600) {
	// if (ubo.local.oneCosAxis.x > 600) {
	// if (ubo.local.sin > 600) {
	// if (ubo.camera.axis.x > 600) {
	// if (ubo.camera.sin > 600) {
	// if (ubo.scale > 600) {
		output.position.x += 1;
	}
	// */
    

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