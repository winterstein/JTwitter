package com.winterwell.jgeoplanet;

/**
 * A mutable float
 * @author Daniel Winterstein
 */
public final class MFloat {

	public MFloat(float value) {
		this.value = value;
	}
	
	public MFloat() {
	}

	public float value;

	public void set(float f) {
		this.value = f;
	}
	
}
