static float2 positions[3] = float2[](
    float2(0.0, -0.5),
    float2(0.5, 0.5),
    float2(-0.5, 0.5)
);

static float3 tempColor = float3(0.0, 1.0, 0.0);

struct VSInput {
    float3 inPosition;
    float3 inColor;
    float2 inTexCoord;
};

struct VertexOutput {
    float3 color;
    float4 sv_position : SV_Position;
};

[shader("vertex")]
VertexOutput vertMain(VSInput input) {
    VertexOutput output;
    // output.sv_position = float4(positions[vid], 0.0, 1.0);
    output.sv_position = float4(input.inPosition, 1.0);
    output.color = tempColor;
    return output;
}

[shader("fragment")]
float4 fragMain(VertexOutput inVert) : SV_Target
{
    float3 color = inVert.color;
    return float4(color, 1.0);
}