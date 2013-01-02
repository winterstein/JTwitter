package winterwell.jtwitter.ecosystem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import winterwell.json.JSONException;
import winterwell.json.JSONObject;

public class TwitterCounterStats {

	
	
	@Override
	public String toString() {
		if (data.isEmpty()) {
			return "TwitterCounterStats[@"+screenName+" no data]";
		}
		Date s = data.get(0).date;
		Date e = data.get(data.size()-1).date;
		return "TwitterCounterStats[@"+screenName+" "+data.size()+" pts from "+s+" to "+e+"]";
	}

	public final String screenName;
	public final Date dateUpdated;
	public final int followDays;
	/**
	 * Often zero due to rounding!
	 */
	public final double avgGrowth;
	public final long rank;
	/**
	 * Follower-counts. Sorted chronologically, with the earliest data first.
	 */
	public final ArrayList<DateValue> data;
	
	public static final class DateValue implements Comparable<DateValue> {

		public final int value;
		public final Date date;

		DateValue(Date date, int v) {
			this.date = date;
			this.value = v;
		}
		
		@Override
		public String toString() {
			return date+": "+value;
		}

		@Override
		public int compareTo(DateValue o) {
			return date.compareTo(o.date);
		}
	}
	
	static final SimpleDateFormat format = new SimpleDateFormat("'date'yyyy-MM-dd");
	static final SimpleDateFormat duformat = new SimpleDateFormat("yyyy-MM-dd");
	
	
	TwitterCounterStats(JSONObject jo) throws JSONException, ParseException {
		screenName = jo.getString("username");
		dateUpdated = duformat.parse(jo.getString("date_updated"));
		followDays = jo.getInt("follow_days");		
		avgGrowth = jo.getDouble("average_growth");
		website = jo.optString("url");
		rank = jo.getLong("rank");
		Map<String, ?> perdate = jo.getJSONObject("followersperdate").getMap();
		data = new ArrayList(perdate.size());
		for(String key : perdate.keySet()) {			
//			try {
			Date date = format.parse(key);
			int v = (Integer) perdate.get(key);
			data.add(new DateValue(date, v));
//			} catch (ParseException e) {
//				System.out.println(key);
//			}
		}
		// earliest first
		Collections.sort(data);
	}
	
	public final String website; 

}
