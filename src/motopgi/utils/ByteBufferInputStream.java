package motopgi.utils;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;

/**
 * ByteBuffer -> InputStream
 */
public class ByteBufferInputStream extends ByteArrayInputStream {
	public ByteBufferInputStream(ByteBuffer buffer) {
		super(new byte[buffer.capacity()]);
		// 最初にバイト配列に変換してしまう
    	buffer.get(super.buf);
	}
}
