package com.winterwell.json;

import static org.junit.Assert.*;

import org.junit.Test;

import winterwell.jtwitter.InternalUtils;

public class JSONObjectTest {

	@Test
	public void testIsNull() {
		assert JSONObject.NULL != null;
		JSONObject jobj = new JSONObject(InternalUtils.asMap("foo", "bar"));
		assert ! jobj.isNull("foo");
		assert jobj.isNull("rabbits");
	}

}
