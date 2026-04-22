package lwjgl.ex.vulkan;

import static org.lwjgl.vulkan.VK14.*;


public class PipelineSettings {
	public static final float DEFAULT_LINE_WIDTH = 1.0f;
	private LogicalDevice logicalDevice;
	
	// ほぼ複数必要だが、現在は対処法が不明
    private Shader shader;
    
	private float lineWidth = DEFAULT_LINE_WIDTH;
	
	// LWJGLの設計ミスによりint
	// vk::PipelineInputAssemblyStateCreateInfo inputAssembly{.topology = vk::PrimitiveTopology::eTriangleList};
	private int topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
	
	// vk::PipelineRasterizationStateCreateInfo rasterizer{.depthClampEnable = vk::False, .rasterizerDiscardEnable = vk::False, .polygonMode = vk::PolygonMode::eFill, .cullMode = vk::CullModeFlagBits::eBack, .frontFace = vk::FrontFace::eClockwise, .depthBiasEnable = vk::False, .depthBiasSlopeFactor = 1.0f, .lineWidth = 1.0f};
	private boolean depthClampEnable = false;
	private boolean rasterizerDiscardEnable = false;
	private int polygonMode = VK_POLYGON_MODE_FILL;
	private int cullMode = VK_CULL_MODE_BACK_BIT;
	private int frontFace = VK_FRONT_FACE_CLOCKWISE;
	private boolean depthBiasEnable = false;
	private float depthBiasSlopeFactor = 1.0f; 
	private float depthlineWidth = DEFAULT_LINE_WIDTH;
	
	private boolean sampleShadingEnable = false;
	
	// vk::PipelineColorBlendAttachmentState colorBlendAttachment{.blendEnable    = vk::False,
	private boolean blendEnable = false;
	
    // .colorWriteMask = vk::ColorComponentFlagBits::eR | vk::ColorComponentFlagBits::eG | vk::ColorComponentFlagBits::eB | vk::ColorComponentFlagBits::eA};
	private int colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
	
	// vk::PipelineColorBlendStateCreateInfo colorBlending{.logicOpEnable = vk::False, .logicOp = vk::LogicOp::eCopy, .attachmentCount = 1, .pAttachments = &colorBlendAttachment};
	private boolean colorBlendingLogicOpEnable = false;
	private int colorBlendingLogicOp = VK_LOGIC_OP_COPY;
	
	private SurfaceSettings surfaceSettings;
	
	private DescriptionHelper descriptionHelper;
	
	
	public PipelineSettings(LogicalDevice logicalDevice, Shader shader, SurfaceSettings surfaceSettings) {
		this.logicalDevice = logicalDevice;
		this.shader = shader;
		
		// colorFormatはSurfaceと一致させる必要がある
		this.surfaceSettings = surfaceSettings;
	}
	public LogicalDevice getLogicalDevice() {
		return logicalDevice;
	}

	public int getColorFormat() {
		// colorFormatはSurfaceと一致させる必要がある
		return surfaceSettings.getFormat();
	}
	// Surfaceとマッチさせる必要があるため、変更を防ぐ
	// VkRenderingInfo::pColorAttachments[0].imageView format (VK_FORMAT_B8G8R8A8_UNORM) must match the corresponding format in VkPipelineRenderingCreateInfo::pColorAttachmentFormats[0] (VK_FORMAT_UNDEFINED).
//	public void setColorFormat(int colorFormat) {
//		this.colorFormat = colorFormat;
//	}
	
	public Shader getShader() {
		return shader;
	}
	public float getLineWidth() {
		return lineWidth;
	}
	public void setLineWidth(float lineWidth) {
		this.lineWidth = lineWidth;
	}
	public int getTopology() {
		return topology;
	}
	public void setTopology(int topology) {
		this.topology = topology;
	}
	public boolean isDepthClampEnable() {
		return depthClampEnable;
	}
	public void setDepthClampEnable(boolean depthClampEnable) {
		this.depthClampEnable = depthClampEnable;
	}
	public boolean isRasterizerDiscardEnable() {
		return rasterizerDiscardEnable;
	}
	public void setRasterizerDiscardEnable(boolean rasterizerDiscardEnable) {
		this.rasterizerDiscardEnable = rasterizerDiscardEnable;
	}
	public int getPolygonMode() {
		return polygonMode;
	}
	public void setPolygonMode(int polygonMode) {
		this.polygonMode = polygonMode;
	}
	public int getCullMode() {
		return cullMode;
	}
	public void setCullMode(int cullMode) {
		this.cullMode = cullMode;
	}
	public int getFrontFace() {
		return frontFace;
	}
	public void setFrontFace(int frontFace) {
		this.frontFace = frontFace;
	}
	public boolean isDepthBiasEnable() {
		return depthBiasEnable;
	}
	public void setDepthBiasEnable(boolean depthBiasEnable) {
		this.depthBiasEnable = depthBiasEnable;
	}
	public float getDepthBiasSlopeFactor() {
		return depthBiasSlopeFactor;
	}
	public void setDepthBiasSlopeFactor(float depthBiasSlopeFactor) {
		this.depthBiasSlopeFactor = depthBiasSlopeFactor;
	}
	public float getDepthlineWidth() {
		return depthlineWidth;
	}
	public void setDepthlineWidth(float depthlineWidth) {
		this.depthlineWidth = depthlineWidth;
	}
	
	public boolean isSampleShadingEnable() {
		return sampleShadingEnable;
	}
	public void setSampleShadingEnable(boolean sampleShadingEnable) {
		this.sampleShadingEnable = sampleShadingEnable;
	}
	public boolean isBlendEnable() {
		return blendEnable;
	}
	public void setBlendEnable(boolean blendEnable) {
		this.blendEnable = blendEnable;
	}
	public int getColorWriteMask() {
		return colorWriteMask;
	}
	public void setColorWriteMask(int colorWriteMask) {
		this.colorWriteMask = colorWriteMask;
	}
	public boolean isColorBlendingLogicOpEnable() {
		return colorBlendingLogicOpEnable;
	}
	public void setColorBlendingLogicOpEnable(boolean colorBlendingLogicOpEnable) {
		this.colorBlendingLogicOpEnable = colorBlendingLogicOpEnable;
	}
	public int getColorBlendingLogicOp() {
		return colorBlendingLogicOp;
	}
	public void setColorBlendingLogicOp(int colorBlendingLogicOp) {
		this.colorBlendingLogicOp = colorBlendingLogicOp;
	}
	public DescriptionHelper getDescriptionHelper() {
		return descriptionHelper;
	}
	public void setDescriptionHelper(DescriptionHelper vertexDescriptionHelper) {
		this.descriptionHelper = vertexDescriptionHelper;
	}
}
