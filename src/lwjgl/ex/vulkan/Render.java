package lwjgl.ex.vulkan;

// 参考
// https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-05/src/main/java/org/vulkanb/eng/graph/Render.java

import java.util.Arrays;
import java.util.OptionalInt;

import org.lwjgl.system.MemoryStack;

import motopgi.utils.ExceptionUtils;

public class Render implements AutoCloseable {
	private final RenderSettings settings;
	private final CommandPool commandPool;
	// 毎フレーム実行する処理であるため、速度を気にして配列にする
	private final FrameRender[] renders;
	private int currentFrame = 0;
	private FrameRender past = null;

	public Render(RenderSettings settings) {
		this.settings = settings;
		commandPool = new CommandPool(settings.getCommandPoolSettings());
		settings.getCommandBufferSettings().setCommandPool(commandPool);
		renders = FrameRender.createArray(settings.getMaxInFlight(), settings);
	}
	
	public void render(Command command) {
		try(var stack = MemoryStack.stackPush()) {
			/*
			https://github.com/lwjglgamedev/vulkanbook/blob/master/bookcontents/chapter-05/chapter-05.md#render-loop
			描画の主な手順は次のとおりです。

			フェンスを待つ：CPUから現在のフレームに関連付けられたリソースにアクセスできるようにするには、それらのリソースがGPUによってまだ使用されていないことを確認する必要があります。フェンスはGPUとCPU間の同期手段であることを覚えておいてください。現在のフレームに関連付けられた作業を送信する際、関連するフェンスを通過します。
			コマンドAの記録: フェンスを通過すると、現在のフレームに関連付けられたコマンドバッファにコマンドの記録を開始できます。しかし、なぜコマンド「A」と「B」の2つのセットが必要なのでしょうか？これは、取得する必要がある特定のスワップチェーンイメージに依存しないコマンド（「Aコマンド」）と、特定のイメージビューに対して操作を実行するコマンド（「Bコマンド」）があるためです。スワップチェーンイメージを取得する前の最初のステップの記録を開始できます。
			画像の取得：レンダリングに使用する次のスワップチェーン画像を取得する必要があります。ただし、この章ではまだ「Aコマンド」は使用しません。
			記録コマンド B : すでに説明しました。
			コマンドの送信: コマンドをグラフィカル キューに送信するだけです。
			現在の画像。
				 */
			renders[currentFrame].waitAndResetForFence();
			var nextSwapChainImageView = settings.getSwapChain().acquireNextImageView(stack, renders[currentFrame].getForSwapChain());
			renders[currentFrame].submit(stack, nextSwapChainImageView, command, past);
			
			// 次のフレームへ
			past = renders[currentFrame];
			currentFrame = (currentFrame + 1) % settings.getMaxInFlight();
		}
	}



	@Override
	public void close() throws Exception {
		ExceptionUtils.close(renders, commandPool);
	}

}
