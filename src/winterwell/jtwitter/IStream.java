package winterwell.jtwitter;

import java.util.List;

import winterwell.jtwitter.AStream.Outage;
import winterwell.jtwitter.Twitter.ITweet;
import winterwell.utils.containers.AbstractMap2;

public interface IStream {

	boolean isAlive();

	boolean isConnected();

	/**
	 * * @return the recent system events. Calling this will clear the list of system events.
	 */
	List<Object[]> popSystemEvents();
	
	void connect();
	
	void close();

}
