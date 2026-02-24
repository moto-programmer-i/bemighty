#!/bin/sh -ex

# https://docs.shader-slang.org/en/latest/external/slang/docs/user-guide/00-introduction.html

# vulkan tutorial
slangc shader.slang -target spirv -profile spirv_1_4 -emit-spirv-directly -fvk-use-entrypoint-name -entry vertMain -entry fragMain -o slang.spv
