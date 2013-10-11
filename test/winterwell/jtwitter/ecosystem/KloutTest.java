package winterwell.jtwitter.ecosystem;

import java.util.Map;

import org.junit.Test;

public class KloutTest {

	String key = "ywn6supmsxyxrk86aexrtg6t";
	String ss = "ZaNMVDFE6m";
	
	@Test
	public void test() {
		// test key
		Klout klout = new Klout(key);
		Object kid = klout.getKloutID("winterstein");
		double score = klout.getScore(kid);
		System.out.println(score);
	}

}
