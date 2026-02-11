package lwjgl.ex.vulkan;

import java.awt.Dimension;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.KHRSurface;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;

import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import motopgi.utils.ExceptionUtils;

import static org.lwjgl.vulkan.VK14.*;

public class SwapChain implements AutoCloseable {
	private final SwapChainSettings settings;
	private long handler;
	private ImageView[] imageViews;
	private int width;
	private int height;

	private boolean isRecreating = false;
	private final ExecutorService threadPool = Executors.newCachedThreadPool();

	public SwapChain(SwapChainSettings settings) {
		this.settings = settings;
		settings.getWindow().addResizeCallbacks(this::recreate);
		init();
	}

	/**
	 * 初期化 （再作成の場合があるため、コンストラクタ外のメソッドが必要）
	 */
	private void init() {
		var vkDevice = settings.getLogicalDevice().getDevice();
		// 参考
		// https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java
		try (var stack = MemoryStack.stackPush()) {
			// 再作成時は、Surfaceの再設定も必要
			if (isRecreating) {
				settings.getSurface().initCapabilities(stack);
			}
			
			var surfaceCapabilities = settings.getSurface().getSurfaceCapabilities();

			var framebufferSize = settings.getWindow().getFramebufferSize(stack);
			width = framebufferSize.width();
			height = framebufferSize.height();
			
			var minImageCount = surfaceCapabilities.minImageCount();

			var info = VkSwapchainCreateInfoKHR.calloc(stack).sType$Default()
					.surface(settings.getSurface().getHandler())

					// VkImageViewの作成に使うことを指定
					// imageUsage must not be 0
					// (https://vulkan.lunarg.com/doc/view/1.4.321.1/linux/antora/spec/latest/chapters/VK_KHR_surface/wsi.html#VUID-VkSwapchainCreateInfoKHR-imageUsage-requiredbitmask)
					// https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkImageUsageFlagBits.html
					.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)

					// 現在のTransform (pre = present)
					// .preTransform(surfaceCapabilities.currentTransform())では
					// System.out.println("surfaceCapabilities.currentTransform() " +
					// surfaceCapabilities.currentTransform());
					// 1と出力で確認したのにもかかわらず、pCreateInfo->preTransform is zero.のエラーがでたままだった
					// https://docs.vulkan.org/refpages/latest/refpages/source/VkSurfaceTransformFlagBitsKHR.html
					.preTransform(KHRSurface.VK_SURFACE_TRANSFORM_IDENTITY_BIT_KHR)

					// VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR 透明度を無視
					// 透明度を有効にした場合のパフォーマンスの差などは不明
					// https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkCompositeAlphaFlagBitsKHR.html
					.compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)

					.imageArrayLayers(settings.getImageArrayLayers())

					// 一時変数が解放されてしまうのか、0 0 になってしまう
//							.imageExtent(calcSwapChainExtent(stack))
					// 変数から初期化
					.imageExtent(framebufferSize)

					// なぜか変数を経由しないと1になってしまうらしい
					// vkCreateSwapchainKHR(): pCreateInfo->minImageCount is 1 which is less than
					// VkSurfaceCapabilitiesKHR::minImageCount (3) returned by
					// vkGetPhysicalDeviceSurfaceCapabilitiesKHR().
//							.minImageCount(surfaceCapabilities.minImageCount())
					.minImageCount(minImageCount)

					// Surfaceのformatとcolorspaceを設定
					.imageFormat(settings.getSurface().getFormat())
					.imageColorSpace(settings.getSurface().getColorSpace())

					// https://docs.vulkan.org/refpages/latest/refpages/source/VkSwapchainCreateInfoKHR.html

					// オブジェクトの任意の範囲またはイメージ サブリソースへのアクセスが、一度に 1 つのキュー ファミリに排他的
					// （デフォルト）
					// https://registry.khronos.org/VulkanSC/specs/1.0-extensions/man/html/VkSharingMode.html
					.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)

					// 同期待ちを指定（ティアリングは発生しない）
					// https://docs.vulkan.org/refpages/latest/refpages/source/VkPresentModeKHR.html
					.presentMode(KHRSurface.VK_PRESENT_MODE_FIFO_KHR)

					// 表示されない領域に影響するレンダリング操作を破棄
					.clipped(true);

			// リサイズでSwapchainを作り直す場合は追加の処理が必要？
//					https://github.com/LWJGL/lwjgl3/blob/a73648fbfcbc0945e9a0ffa2a3dca021c372f3b2/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java#L670
//					info.oldSwapchain(handler);

			LongBuffer buffer = stack.mallocLong(1);
			Vulkan.throwExceptionIfFailed(vkCreateSwapchainKHR(vkDevice, info, null, buffer), "swap chainの作成に失敗しました");
			handler = buffer.get(0);

			// createImageView
			// https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/vulkan/khronos/HelloTriangle_1_3.java
			IntBuffer imageCountBuffer = stack.mallocInt(1);
			Vulkan.throwExceptionIfFailed(vkGetSwapchainImagesKHR(vkDevice, handler, imageCountBuffer, null),
					"Swapchainイメージの数の取得に失敗しました");
			int imageCount = imageCountBuffer.get(0);
			LongBuffer swapchainImagesBuffer = stack.mallocLong(imageCount);
			Vulkan.throwExceptionIfFailed(
					vkGetSwapchainImagesKHR(vkDevice, handler, imageCountBuffer, swapchainImagesBuffer),
					"Swapchainイメージの取得に失敗しました");

			imageViews = ImageView.createArray(imageCount, swapchainImagesBuffer, settings.getImageViewSettings());
		}

	}

	public ImageView acquireNextImageView(MemoryStack stack, Semaphore acquire) {
		return imageViews[acquireNextImageIndex(stack, acquire)];
	}

	/**
	 * ImageIndexを取得する（Vulkanの仕様はランダムらしい） https://stackoverflow.com/a/72799450
	 * 
	 * @param stack
	 * @param acquire
	 * @return
	 */
	private int acquireNextImageIndex(MemoryStack stack, Semaphore acquire) {
		IntBuffer imageIndexBuffer = stack.mallocInt(1);
		int code = vkAcquireNextImageKHR(settings.getLogicalDevice().getDevice(), handler, Long.MAX_VALUE,
				acquire.getHandler(), MemoryUtil.NULL, imageIndexBuffer);

		System.out.println("acquireNextImageIndex " + Vulkan.codeToMessage(code));
		switch (code) {
		// おそらく、OUT_OF_DATEのときだけ別対応が必要だが、保留
		case VK_ERROR_OUT_OF_DATE_KHR:
			System.out.println("VK_ERROR_OUT_OF_DATE_KHR");
		default:
			Vulkan.throwExceptionIfFailed(code, "vkAcquireNextImageKHRに失敗しました");

		}

		return imageIndexBuffer.get(0);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public LongBuffer createLongBuffer(MemoryStack stack) {
		return stack.longs(handler);
	}

	public void recreate() throws Exception {
		// 毎回再作成は避ける
		if (isRecreating) {
			return;
		}
		isRecreating = true;

		// 設定待機時間に1回だけ再作成する
		// 非同期参考 https://qiita.com/koduki/items/086d42b5a3c74ed8b59e
		threadPool.execute(() -> {
			try {
				Thread.sleep(settings.getRecreteDebounceMilliseconds());
			} catch (InterruptedException e) {
				// 失敗時は無視
			}

			// 再作成
			settings.getLogicalDevice().waitIdle();
			try {
				closeSwapChain();
			// ここで例外が発生した場合の適切な対処不明
			} catch (Exception e) {
				e.printStackTrace();
			}
			init();

			isRecreating = false;
		});
	}
	
	/**
	 * SwapChain部分のclose
	 * （再作成時に必要）
	 * @throws Exception
	 */
	private void closeSwapChain() throws Exception {
		if (handler == VK_NULL_HANDLE) {
			return;
		}
		try {
			ExceptionUtils.close(imageViews);
		} finally {
			vkDestroySwapchainKHR(settings.getLogicalDevice().getDevice(), handler, null);
			handler = VK_NULL_HANDLE;
		}
	}

	@Override
	public void close() throws Exception {
		// 全体のclose
		try {
			closeSwapChain();
		} finally {
			ExceptionUtils.close(threadPool);
		}
	}
}
