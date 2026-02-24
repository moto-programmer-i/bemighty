#version 450

// https://docs.vulkan.org/tutorial/latest/_attachments/09_shader_base.frag

layout(location = 0) in vec3 fragColor;

layout(location = 0) out vec4 outColor;

void main() {
    outColor = vec4(fragColor, 1.0);
}