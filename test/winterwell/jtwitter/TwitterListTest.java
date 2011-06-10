package winterwell.jtwitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;

/**
 * Partial unit tests for TwitterList
 * @author daniel
 *
 */
public class TwitterListTest 
extends TestCase // Comment out to remove the JUnit dependency
{
	
	public static void main(String[] args) {
		TwitterListTest tt = new TwitterListTest();
		Method[] meths = TwitterListTest.class.getMethods();
		for(Method m : meths) {
			if ( ! m.getName().startsWith("test")
					|| m.getParameterTypes().length != 0) continue;
			try {
				m.invoke(tt);
				System.out.println(m.getName());
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.out.println("TEST FAILED: "+m.getName());
				System.out.println("\t"+e.getCause());
			}
		}
	}
	
	public void testGetLists() {
		Twitter jtwit = TwitterTest.newTestTwitter();		
		List<TwitterList> lists = jtwit.getLists("tweetminster");
		assert ! lists.isEmpty();
	}

	public void testTwitterList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		TwitterList list = new TwitterList("tweetminster", "guardian", jtwit);
		assert list.size() > 0;
	}

	public void testGetInt() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		TwitterList list = new TwitterList("tweetminster", "guardian", jtwit);
		User user0 = list.get(0);
		User user25 = list.get(25);
		assert user25 != null;
	}
	
	/**
	 * WARNING: this deletes all the test users' lists!!
	 */
	public void testDelete() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		List<TwitterList> myLists = jtwit.getLists();
		for (TwitterList twitterList : myLists) {
			twitterList.delete();
		}
	}
	
	public void testMakeList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		int salt = new Random().nextInt(1000);
		TwitterList list = new TwitterList("testlist"+salt, jtwit, 
				true, "This is a test of the JTwitter library");		
		List<Status> ss = list.getStatuses();
		assert ss != null;
		list.delete();
	}
	
	public void testEditList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		List<TwitterList> lists = jtwit.getLists();
		TwitterList list =
			new TwitterList("testlist", jtwit, true, "test list"); // create
//			new TwitterList(TwitterTest.TEST_USER, "testlist", jtwit); // access existing		
		list.add(new User("winterstein"));
		assert list.size() > 0;
	}
	
	public void testSubscribers() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		TwitterList list =
//			new TwitterList("testlist", jtwit, true, "test list");
			new TwitterList(TwitterTest.TEST_USER, "testlist", jtwit);		
		List<User> subs = list.getSubscribers();
		assert subs.size() > 0;
		assert list.size() > 0;
	}

}
