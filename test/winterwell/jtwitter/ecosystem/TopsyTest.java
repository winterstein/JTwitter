package winterwell.jtwitter.ecosystem;

import org.junit.Test;

import winterwell.jtwitter.ecosystem.Topsy.UrlInfo;

public class TopsyTest {

	@Test
	public void testUrlInfo() {
		Topsy topsy = new Topsy();
		{
		UrlInfo ui = topsy.getUrlInfo("http://www.winterwell.com/software/jtwitter.php");
		System.out.println(ui);
		}
		// Sadly, Topsy doesn't resolve equivalent links very well
		// -- the following all end up at the same page, but only www.soda.sh 
		// seems to score anything :( 
		{
			UrlInfo ui = topsy.getUrlInfo("http://www.sodash.com/");
			System.out.println(ui);
		}
		{
			UrlInfo ui = topsy.getUrlInfo("http://www.soda.sh/");
			System.out.println(ui);
		}
		{
			UrlInfo ui = topsy.getUrlInfo("http://soda.sh/");
			System.out.println(ui);
		}
		{
			UrlInfo ui = topsy.getUrlInfo("http://sodash.com/");
			System.out.println(ui);
		}
		{
			UrlInfo ui = topsy.getUrlInfo("http://bit.ly/8CsKLw");
			System.out.println(ui);
		}
	}

}
