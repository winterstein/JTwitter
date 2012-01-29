package winterwell.jtwitter.ecosystem;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

public class KloutTest {

	@Test
	public void test() {
		// test key
		Klout klout = new Klout("azkyjz3ahwywwdhjrzba7ds5");
		Map<String, Double> scores = klout.getScore("winterstein","joehalliwell", "cfctruth");
		System.out.println(scores);
	}

}
