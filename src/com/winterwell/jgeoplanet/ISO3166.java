/**
 * (c) Winterwell Associates Ltd   
 * If this class is included in any deliverable, it is provided not as part of that deliverable,
 * but under an LGPL license with full ownership & rights retained by Winterwell.
 * This copyright notice may not be edited or removed.
 * 
 * This class depends on the file "iso-3166-country-codes.csv" which is covered by the same
 * copyright and licensing terms.
 */
package com.winterwell.jgeoplanet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import winterwell.jtwitter.InternalUtils;

/**
 * ISO 3166 2-letter Uppercase Country Codes, e.g. "GB" for Britain
 * 
 * TODO incorporate http://en.wikipedia.org/wiki/List_of_countries_and_capitals_in_native_languages
 * 
 * @author daniel
 * @testedby  ISO3166Test}
 */
public final class ISO3166 {
	
	
	static Map<String, String> code2name;
	static Map<String, String> code2everydayName;
	static Map<String, String> name2code;
	
	public List<String> getAllNames(String codeOrName) {
		String code = getCountryCode(codeOrName);
		if (code==null) throw new IllegalArgumentException(codeOrName);
		ArrayList<String> names = new ArrayList();
		for (Entry<String, String> e : name2code.entrySet()) {
			if (code.equals(e.getValue()) && ! names.contains(e.getKey())) {
				names.add(e.getKey());
			}
		}
		return names;
	}
	
	/**
	 * 2-letter Country Codes.
	 * Uses a static cache, so creating these objects is cheap.
	 */
	public ISO3166() {		
	}
	
	private static void init() {
		if (code2name!=null) return;		
		try {
			code2name = new HashMap();
			name2code = new HashMap();
			code2everydayName = new HashMap();
			// load from file
			InputStream strm = ISO3166.class
					.getResourceAsStream("iso-3166-country-codes.csv");
			Reader _reader = new InputStreamReader(strm, "UTF8");
			BufferedReader reader = new BufferedReader(_reader);
			String line = reader.readLine(); // discard the header line
			while(true) {
				line = reader.readLine();
				if (line==null) break;
				String[] bits = line.split("\t");
				String code = bits[1].toUpperCase();
				code2name.put(code, bits[0]);
				// be lenient here
				String name = canon(bits[0]);				
				name2code.put(name, code);
				// alternative names?
				if (bits.length > 5 && ! bits[5].isEmpty()) {
					String[] alternateNames = bits[5].split(";");
					// If the official short name is a mouthful, use the 1st alternate
					if (bits[0].contains("(") || bits[0].contains(",") 
							|| name.contains("republic")
							|| name.startsWith("libyan")) {
						assert alternateNames[0] != null;
						code2everydayName.put(code, alternateNames[0]);
					}
					for (String alt : alternateNames) {
						alt = canon(alt);			
						String _code = name2code.get(alt);
						assert _code == null || _code.equals(code) : _code+" vs "+code+" for "+alt;
						name2code.put(alt, code);
					}
				}
			}		
			reader.close();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * @param name
	 * @return mangled but better as a map key
	 */
	private static String canon(String name) {
		return InternalUtils.toCanonical(name).replace(" ", "");
	}

	/**
	 * @param countryCodeOrName
	 * @return "standardised" country name, or null if unrecognised.
	 */
	public String getName(String countryCodeOrName) {
		init();
		// in case it's a name, or wrong fromat
		String ccode = getCountryCode(countryCodeOrName);
		String name = code2name.get(ccode);
		return name;		
	}
	
	/**
	 * @param countryCode
	 * @return All names *in canonical internal format*. See {@link #canon(String)}
	 * @deprecated You probably don't want the names in canonical internal format.
	 */
	@Deprecated
	List<String> getAllNamesForCode(String countryCode) {
		countryCode = getCountryCode(countryCode);
		ArrayList names = new ArrayList();
		for(Map.Entry<String, String> e : name2code.entrySet()) {
			if (e.getValue().equals(countryCode)) {
				names.add(e.getKey());
			}
		}
		assert ! names.isEmpty() : countryCode;
		return names;
	}

	/**
	 * 
	 * @param countryNameOrCode Can be null (returns null). This covers alternative names (English & native-language).
	 * @return uppercase 2 letter code, e.g. "GB", or null if unknown
	 */
	public String getCountryCode(String countryNameOrCode) {
		if (countryNameOrCode == null) {
			return null;
		}
		init();
		// Is it a valid code already?
		if (countryNameOrCode.length()==2) {
			String cc = countryNameOrCode.toUpperCase();
			if (code2name.containsKey(cc)) {
				return cc;
			}
		}
		countryNameOrCode = canon(countryNameOrCode);
		return name2code.get(countryNameOrCode);
	}

	/**
	 * @param code Can be null
	 * @return true if this is a valid ISO 3166-1 2-letter code.
	 * This test is case-INsensitive.
	 */
	public boolean is2LetterCode(String code) {
		init();
		if (code==null) return false;
		return code2name.containsKey(code.toUpperCase());
	}

	public Set<String> getCodes() {
		init();
		return code2name.keySet();
	}

	/**
	 * Mostly the same as {@link #getName(String)}. Some countries have an
	 * everyday name (e.g. "Libya"), but a rather long official short name
	 * (e.g. "Libyan Arab Jamawotsit").
	 * @param code Country code or name
	 * @return the common/everyday English name for country 
	 */
	public String getEverydayName(String code) {
		assert code != null;
		init();
		// in case it's a name, or wrong fromat
		String ccode = getCountryCode(code);
		String en = code2everydayName.get(ccode);
		if (en!=null) return en;
		return getName(ccode);
	}

	/**
	 * @return the backing map of country-name -> country-code.
	 * This is many-to-one: countries can have a few names.
	 */
	public Map getName2Code() {
		return name2code;
	}

}
