package motopgi.utils;

import java.util.ArrayList;

public class AutoCloseableList<E extends AutoCloseable> extends ArrayList<E> implements AutoCloseable {

	private static final long serialVersionUID = -6883412886536410057L;

	@Override
	public void close() throws Exception {
		if (isEmpty()) {
			return;
		}
		ExceptionUtils.close(this);
		clear();
	}

}
