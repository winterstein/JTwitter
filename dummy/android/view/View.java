package android.view;

public class View {

	public interface OnTouchListener {

		boolean onTouch(View v, MotionEvent e);

	}
	public static String VISIBLE = null;
	public static String FOCUS_DOWN = null;
	public boolean hasFocus() {
		return true;
	}
	public void requestFocus() {
	}

}
