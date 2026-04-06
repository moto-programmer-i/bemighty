package lwjgl.ex.vulkan;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexture;

import motopgi.utils.AutoCloseableList;
import motopgi.utils.ByteBufferInputStream;

import static lwjgl.ex.vulkan.VulkanConstants.*;

public final class AssimpUtils {
//	public static final String IMAGE_FORMAT_BMP = "BMP";

	private AssimpUtils() {
	}
	// https://the-asset-importer-lib-documentation.readthedocs.io/en/latest/usage/use_the_lib.html#textures
	// This applies if aiTexture::mHeight == 0 is fulfilled. Then, the texture is stored in a compressed format such as DDS or PNG. The term “compressed” does not mean that the texture data must actually be compressed, however, the texture was found in the model file as if it was stored in a separate file on the hard disk. Appropriate decoders (such as libjpeg, libpng, D3DX, DevIL) are required to load these textures. aiTexture::mWidth specifies the size of the texture data in bytes, aiTexture::pcData is a pointer to the raw image data and aiTexture::achFormatHint is either zeroed or contains the most common file extension of the embedded texture’s format. This value is only set if Assimp is able to determine the file format.
	
	public static AutoCloseableList<Texture> readTextures(AIScene scene, LogicalDevice logicalDevice, CommandPool commandPool, Queue queue, VertexDescriptionHelper descriptionHelper, UniformObject uniformObject) {
		var list = new AutoCloseableList<Texture>();
		int numTextures = scene.mNumTextures();
        for(int i = 0; i < numTextures; ++i) {
        	// Assimpの設計ミスにより、画像が圧縮されている場合は
        	// 幅にサイズ、高さが0になる
        	var texture = AITexture.create(scene.mTextures().get(i));
        	if (texture.mHeight() != 0) {
        		// 圧縮されていない場合は未実装
        		throw new RuntimeException("Textureが圧縮されたいない場合は未実装");
        	}
        	
        	// 圧縮された画像を展開
        	try {
        		var image = ImageIO.read(new ByteBufferInputStream(texture.pcDataCompressed()));        		
        		list.add(new Texture(image, logicalDevice, commandPool, queue, descriptionHelper, uniformObject));
        	}
        	// ByteBufferからの読み込みなので、基本的には例外でないはず
        	catch (IOException e) {
        		throw new RuntimeException("Texture読み込みエラー", e);
        	}
        	
        }
		return list;
	}
	
//	public static byte[] toBMPArray(BufferedImage image) {
//		System.out.println("toBMPArray width " + image.getWidth() + " height " + image.getHeight());
////		var size = image.getWidth() * image.getHeight() * RGBA_BYTES;
////		var output = new ByteArrayOutputStream(size);
//		var output = new ByteArrayOutputStream();
//		// サイズが0になり、うまくいかなかった
////			ImageIO.write(image, IMAGE_FORMAT_BMP, byteArrayOutputStream);
//
//		for (int y = 0; y < image.getHeight(); ++y) {
//			for(int x = 0; x < image.getWidth(); ++x) {
//				// これでフォーマットが合うのかは不明
////				image.getR
////				output.write(image.getRGB(x, y));
//				output.write(Integer.MAX_VALUE);
//			}
//		}
////		return output.toByteArray();
//		byte[] array = output.toByteArray();
//		System.out.println("array size " + array.length);
//		System.out.println("array calc size " + image.getWidth() * image.getHeight() * RGBA_BYTES);
//		return array;
//	}
	
	public static int calcSize(BufferedImage image) {
		return ARGB_BYTES * image.getWidth() * image.getHeight();
	}
	
	public static void writeImageToPointer(BufferedImage image, PointerBuffer destination) {
		// フォーマットごとへの対応が必要
		// VK_FORMAT_R8G8B8_SRGB
		
		var size = image.getWidth() * image.getHeight();
		var destinationBytes = destination.getIntBuffer(size);
		for (int y = 0; y < image.getHeight(); ++y) {
			for(int x = 0; x < image.getWidth(); ++x) {
				// getRGBはTYPE_INT_ARGBフォーマット
				// https://docs.oracle.com/javase/jp/24/docs/api/java.desktop/java/awt/image/BufferedImage.html#getRGB(int,int)
				// STBI_rgb_alphaもARGBの順番っぽい
				// https://github.com/quentinplessis/STBI/blob/cfeea57e2e9f3980376d735ae81e4b80450fcb82/stb_image.c#L2738
				destinationBytes.put(image.getRGB(x, y));
				
				// これで正しいのか不明
			}
		}
		
		
//		var size = calcSize(image);
//		var output = new ByteArrayOutputStream(size);
//		for (int y = 0; y < image.getHeight(); ++y) {
//			for(int x = 0; x < image.getWidth(); ++x) {
//				var argb = image.getRGB(x, y);
//				// a r g b の各バイトを書く
//				for(int bytes = 0; bytes < ARGB_BYTES; ++bytes) {
//					// write(int)は下位8バイトだけ書かれる
//					output.write(argb);	
//					argb >>= Byte.BYTES;
//				
//					// これで正しいのか不明、順番が逆の可能性も大
//				}
//			}
//		}
		
		
		
	}

}
