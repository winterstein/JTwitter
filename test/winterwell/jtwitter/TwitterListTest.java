package winterwell.jtwitter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import junit.framework.TestCase;
import winterwell.jtwitter.TwitterException.E404;

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
		List<TwitterList> lists = jtwit.lists().getListsAll("tweetminster");
		assert ! lists.isEmpty();
	}

	public void testTwitterList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		TwitterList list = jtwit.lists().show("tweetminster", "guardian");
		assert list.getMemberCount() > 0;
	}

	/**
	 * WARNING: this deletes all the test users' lists!!
	 */
	public void testDelete() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		List<TwitterList> myLists = jtwit.getLists();
		for (TwitterList twitterList : myLists) {
			// AZ: getLists now returns ALL lists the user is subscribed to, including his own, so need to test for ownership here...
			if (twitterList.getOwner().getName().equals(TwitterTest.TEST_USER))
				jtwit.lists().delete(twitterList.getId());
		}
	}
	
	public void testMakeList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		int salt = new Random().nextInt(1000);
		Twitter_Lists lists = jtwit.lists();
		TwitterList list = lists.create("testlist"+salt, true, "This is a test of the JTwitter library");		
		List<Status> ss = lists.getListTimeline(list.getId());
		assert ss != null;
		lists.delete(list.getId());
	}
	
	public void testEditList() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Lists lists = jtwit.lists();
		TwitterList list = lists.create("testlist", true, "test list"); // create
//		TwitterList list = lists.show(TwitterTest.TEST_USER, "testlist"); // access existing
		list = lists.addMember(list.getId(), "winterstein");
		assert list.getMemberCount() > 0;
	}
	
	public void testAdd() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Lists lists = jtwit.lists();
		String sn = jtwit.getScreenName();
		assert sn != null;
		TwitterList twitterList;
		try {
			twitterList = lists.show(sn, "just-added");
		} catch (E404 e) {
			twitterList = lists.create("just-added", true, "list test");
		}
		twitterList = lists.addMember(twitterList.getId(), "apigee");
		twitterList = lists.addMember(twitterList.getId(), "docusign");
		// fetch
		TwitterList list2 = lists.show(sn, "just-added");
		assert list2.getMemberCount() == twitterList.getMemberCount();
	}
	
	public void testSubscribers() {
		Twitter jtwit = TwitterTest.newTestTwitter();
		Twitter_Lists lists = jtwit.lists();
		TwitterList list = lists.show(TwitterTest.TEST_USER, "testlist");
		List<User> subs = lists.getListSubscribers(list.getId());
		assert subs.size() > 0 : subs;
		assert list.getMemberCount() > 0 : list;
	}

}
