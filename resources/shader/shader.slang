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


// 画面の縦横比はどうせ16:9なので、合わせてパースをつける
// https://chaosplant.tech/do/vulkan/5-14/#pasuwotukeru-1
static const float persX = 0.2;
static const float persY = persX * 9 / 16;
static const float minPersX = 0.8;
static const float minPersY = 0.9;

// デフォルトだと手前が+、1〜0を描画してしまうため、
// 原点が1になってしまうので調整用
static const float cameraZTranslate = 1;

// すべての頂点に対して呼び出される
[shader("vertex")]
VertexOutput vertMain(VSInput input) {
    VertexOutput output;
    // 変形なし
    // var position = input.position;

	// モデルの回転
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	var position = (ubo.local.cos * input.position
			+ ubo.local.oneCosAxis * mul(ubo.local.axis, input.position) 
			+ ubo.local.sin * cross(ubo.local.axis, input.position)
			)
			
			// 拡大
			* ubo.scale
			
			// 平行移動
			+ ubo.translate;
	
	// カメラ座標系へ（カメラの向きと逆回転）		
	// cosθ位置 + (1 - cosθ)(回転・位置)回転 + sinθ(回転×位置)
	position = ubo.camera.cos * position
			+ ubo.camera.oneCosAxis * mul(ubo.camera.axis, position) 
			+ ubo.camera.sin * cross(ubo.camera.axis, position);
			
	
	// パース
	// 確認してみたら、z軸がなぜか手前が+なので参考から逆数に
	// zが0以上1未満以外の範囲は描画されないことに注意
	// 範囲変換 https://scrapbox.io/rn-works/%E6%95%B0%E5%AD%A6_:_%E7%AF%84%E5%9B%B2%E5%A4%89%E6%8F%9B
	position.x *= position.z * persX + minPersX;
	position.y *= position.z * persY + minPersY;
	
	
	// 1〜0の範囲を 0〜-1 に変更
	position.z += cameraZTranslate;
	
	
	output.position = float4(position, 1.0);	
	
	
	/*
	// デバッグ用
	// if (ubo.translate.x > 0.1) {
	// if (ubo.local.axis.x > 0) {
	// if (ubo.local.oneCosAxis.x > 0) {
	// if (ubo.local.sin > 0) {
	// if (ubo.camera.axis.x > 600) {
	// if (ubo.camera.sin > 600) {
	if (ubo.scale > 600) {
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