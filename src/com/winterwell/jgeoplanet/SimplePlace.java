package com.winterwell.jgeoplanet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Ad-hoc place info. E.g. if you just have a free-form string
 * & don't want to geocode it. 
 * @author daniel
 */
public class SimplePlace implements IPlace, Serializable {
	private static final long serialVersionUID = 1L;

	public static final IPlace NO_SUCH_PLACE = new SimplePlace("X");
	
	private String xid;
	private Collection<String> names;
	private String name;
	/**
	 * null unless {@link #setRaw(String)} is called.
	 */
	private String raw;

	private Class<? extends IGeoCode> geocoder;
	
	public String getRaw() {
		return raw;
	}
	
	@Override
	public Class<? extends IGeoCode> getGeoCoder() {
		return geocoder;
	}
	
	public void setGeocoder(Class<? extends IGeoCode> geocoder) {
		this.geocoder = geocoder;
	}
	
	private BoundingBox bbox;
	private String country;
	Location centroid;
	String type;
	private IPlace parent;
	private String coder;
	
	/**
	 * Preserve the raw text form, and details of the geocoder used to interpret it.
	 * @param raw
	 * @param coder Can be null
	 */
	public void setRawAndCoder(String raw, String coder) {
		this.raw = raw;
		this.coder = coder;
	}
	
	/**
	 * Marker for whether the xid field was generated, or externally set.
	 */
	private transient boolean genUID;
	
	public SimplePlace setParent(IPlace parent) {
		this.parent = parent;
		return this;
	}
	
	public SimplePlace setCountry(String country) {
		if (genUID) xid = null;
		this.country = country;
		return this;
	}
	
	@Override
	public String getUID() {
		if (xid==null) {
			genUID = true;
			StringBuilder sb = new StringBuilder();
			if (name!=null) sb.append(name+"_");
			if (country!=null) sb.append(country+"_");
			if (centroid!=null) sb.append(centroid+"_");
			if (sb.length()!=0) sb.delete(sb.length() - 1, sb.length());
			sb.append("@simple");
			xid = sb.toString();
		}
		return xid;
	}
	
	public SimplePlace setType(String type) {
		if (genUID) xid = null;
		this.type = type;
		return this;
	}
	
	@Override
	public String getType() {
		return type;
	}

	@Override
	public String toString() {
		return name==null? (bbox==null? country : bbox.toString()) : name;
	}
	
	/**
	 * 
	 * @param name Can be null
	 * @param bbox Can be null
	 * @param country Can be null. For preference, use the ISO3166 2-letter code.
	 */
	public SimplePlace(String name, BoundingBox bbox, String country) {
		if (name==null && bbox==null && country==null) throw new NullPointerException("all null place");
		this.name = name;
		this.bbox = bbox;
		this.country = country;
	}
	
	public SimplePlace(String loc) {
		this(loc, null, null);
	}

	/**
	 * Copy / conversion constructor.
	 * @param place
	 */
	public SimplePlace(IPlace place) {
		bbox = place.getBoundingBox();
		centroid = place.getCentroid();
		country = place.getCountryCode();
		name = place.getName();
		IPlace _parent = place.getParent();
		if (_parent != null) parent = new SimplePlace(_parent);
		type = place.getType();
		xid = place.getUID();
		
		if (place instanceof SimplePlace) {
			SimplePlace sp = (SimplePlace) place;
			this.raw = sp.raw;
			this.coder = sp.coder;
			this.genUID = sp.genUID;
			this.names = sp.names;
		}
	}
	
	public SimplePlace setName(String name) {
		this.name = name;
		return this;
	}
	
	public SimplePlace(Double latitude, Double longitude) {
		this(null, new BoundingBox(
				new Location(latitude, longitude), LengthUnit.METRE.dx), null);
	}

	public void setAlternativeNames(Collection<String> names) {
		this.names = new ArrayList(names);
		if ( ! names.contains(getName())) this.names.add(getName());		
	}

	public Collection<String> getNames() {
		return names==null? Collections.singletonList(getName()) : names;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getCountryCode() {
		return new ISO3166().getCountryCode(country);
	}

	@Override
	public Location getCentroid() {
		if (centroid!=null) return centroid;
		if (bbox!=null) return bbox.getCenter();
		return null;
	}

	@Override
	public BoundingBox getBoundingBox() {
		return bbox;
	}

	@Override
	public IPlace getParent() {
		return parent;
	}

	public void setUID(String uid) {
		this.xid = uid;
		genUID = false;
	}

	public String getCoder() {
		return coder;
	}

	
}
