package motopgi.utils;

import java.util.List;
import java.util.stream.Stream;

public final class ExceptionUtils {

	private ExceptionUtils() {
	}
	
	public static void close(AutoCloseable[] array, AutoCloseable... flex) throws Exception {
		// 配列と可変長引数をまとめて配列にいれるのが難しそうなので、
		// ちょっと変だがそれぞれcloseする
		// もっと良い書き方がわかれば修正
		try {
			close(array);
		} finally {
			close(flex);
		}
	}
	
	/**
	 * まとめてcloseする（配列はtry-with-resources対象外のため）
	 * @param flex
	 * @throws Exception 各closeで発生した例外をまとめたもの
	 */
	public static void close(AutoCloseable... flex) throws Exception {
		// それぞれcloseして例外をまとめて投げる
		var exception = new Exception("closeに失敗しました");
		for(var e: flex) {
			if (e == null) {
				continue;
			}
			try {
				e.close();
			} catch (Exception ex) {
				exception.addSuppressed(ex);
			}
		}
		if (exception.getSuppressed().length > 0) {
			throw exception;
		}
	}
}
