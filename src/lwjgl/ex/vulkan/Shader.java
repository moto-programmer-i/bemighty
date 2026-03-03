package lwjgl.ex.vulkan;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import static org.lwjgl.vulkan.VK14.*;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-06/src/main/java/org/vulkanb/eng/graph/vk/ShaderModule.java

public class Shader implements AutoCloseable {
	private ShaderSettings settings;
	private long handler;

	public Shader(ShaderSettings settings) throws IOException {
		this.settings = settings;
		try (var stack = MemoryStack.stackPush()) {
			// wrapで型は合うが、実行時エラーになる
//			 var spv = ByteBuffer.wrap(Files.readAllBytes(settings.getSpv()));
			
			// 無駄なコピーが発生するが、正しい方法が不明
			// LWJGLのファイル読み込みがわかり次第修正
			var bytes = Files.readAllBytes(settings.getSpv());
			var spv = stack.malloc(bytes.length).put(0, bytes);
			
			var createInfo = VkShaderModuleCreateInfo.calloc(stack)
                    .sType$Default()
                    .pCode(spv);
			
			LongBuffer forHandler = stack.mallocLong(1);
            Vulkan.throwExceptionIfFailed(vkCreateShaderModule(settings.getLogicalDevice().getDevice(), createInfo, null, forHandler),
                    "Shaderの作成に失敗しました");
            handler = forHandler.get(0);
		}
	}

	@Override
	public void close() throws Exception {
		if (settings == null) {
			return;
		}
		vkDestroyShaderModule(settings.getLogicalDevice().getDevice(), handler, null);
		handler = MemoryUtil.NULL;
		settings = null;
	}

	public ShaderSettings getSettings() {
		return settings;
	}

	public long getHandler() {
		return handler;
	}
	
	public VkPipelineShaderStageCreateInfo.Buffer createStageBuffer(MemoryStack stack) {
		int stagesSize = settings.stagesSize();
		var shaderStages = VkPipelineShaderStageCreateInfo.calloc(stagesSize, stack);
        for (int i = 0; i < stagesSize; ++i) {
        	var stage = settings.getStage(i);
            shaderStages.get(i)
                    .sType$Default()
                    .stage(stage.getStage())
                    .module(handler)
                    // なぜかByteBufferに変換しなければいけない
                    .pName(stack.UTF8(stage.getEntryPointName()));
        }
        return shaderStages;
	}
	
	
}
