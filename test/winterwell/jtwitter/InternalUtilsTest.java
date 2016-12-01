package com.winterwell.jtwitter;

import org.junit.Test;

public class InternalUtilsTest {


	@Test
	public void testStr() {
		{
			String s = InternalUtils.str(new Object[]{"a", "b"});
			assert s.equals("a, b");
		}
		{
			String s = InternalUtils.str(new Object[0]);
			assert s.equals("") : s;
		}
		{
			RuntimeException e = new RuntimeException("foo");
			String s = InternalUtils.str(e);
			assert s.contains("testStr");
			assert s.contains("RuntimeException");
		}
		{
			String s = InternalUtils.str(new RuntimeException("foo").getStackTrace());
			assert s.contains("testStr");
		}
	}
	
	@Test
	public void testEncode() {
		String enc = InternalUtils.encode("+Justin Bieber");
		assert enc.equals("%2BJustin%20Bieber") : enc;
	}
	

	@Test
	public void testStripUrls() throws Exception {
		String stripped = InternalUtils.stripUrls("hello foo.com http://www.whatever/blah?a=b&c=1-2 yeah");
		assert stripped.equals("hello foo.com  yeah") : stripped;
	}
	
	@Test
	public void testLog() throws Exception {
		InternalUtils.log("foo", "bar");
		InternalUtils.log("foo", "bar");
	}

}
