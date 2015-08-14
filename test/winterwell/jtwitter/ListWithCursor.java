package winterwell.jtwitter;

import java.util.ArrayList;


/**
 * A List which also carries cursor information for fetching the next/previous page.
 * @author daniel
 *
 * @param <E>
 */
public class ListWithCursor<E> extends ArrayList<E> {
	private static final long serialVersionUID = 1L;
	public static final String LOST = "lost";
	public static final String END = "end";
	private String cursor;

	/**
	 * @return the next-page cursor, or null. Can use the special constants {@link #LOST} or {@link #END}
	 */
	public String getCursor() {
		return cursor;
	}
	
	/**
	 * For internal use really.
	 * @param cursor
	 */
	public void setCursor(String cursor) {
		this.cursor = cursor;
	}

	/**
	 * @return true if the list has a valid next-page cursor
	 */
	public boolean hasCursor() {
		return cursor != null && ! LOST.equals(cursor) && ! END.equals(cursor);
	}

	/**
	 * @return true if this list has the special {@link #END} cursor, to mark it as having
	 * reached the end.
	 */
	public boolean isEnd() {
		return END.equals(cursor);
	}

}
