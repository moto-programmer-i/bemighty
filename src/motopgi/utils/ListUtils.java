package motopgi.utils;

import java.util.List;

public final class ListUtils {

	private ListUtils() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 存在しない場合nullを返す
	 * @param <E>
	 * @param list
	 * @param index
	 * @return
	 */
	public static <E> E getOrNull(List<E> list, int index) {
		if (index >= list.size()) {
			return null;
		}
		return list.get(index);
	}

}
