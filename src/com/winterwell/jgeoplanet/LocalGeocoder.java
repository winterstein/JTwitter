package com.winterwell.jgeoplanet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import winterwell.jtwitter.InternalUtils;

/**
 * Use a local dataset to do some geocoding.
 * The data rows are:
 * 
 * country-format, name1;name2;name3
 * 
 * List of cities: see http://www.opengeocode.org/download.php#cities
 * We do not use this -- because it contains (rightly) duplicates.
 * c.f. http://opendata.stackexchange.com/questions/3835/a-list-of-cities-of-each-country
 *  
 * @author daniel
 * @testedby  LocalGeocoderTest}
 */
public class LocalGeocoder implements IGeoCode {
	
	private final Map<String, IPlace> canonDesc2place = new HashMap();

	/**
	 * This loads the data from file, so best-practice is to re-use the object.
	 * @throws RuntimeException
	 */
	public LocalGeocoder() throws RuntimeException {
		// Use the bundled csv of country data
		try {
			Map<String, List<String>> code2names = loadISO3166NameData();
			// cities first, so the countries can take precedence if they share the same name
			loadWikipedia("LocalGeocoder_wikipedia_cities.txt");
			loadCSV("LocalGeocoder_cities.csv", null);
			loadCSV("LocalGeocoder_countries.csv", code2names);
//			// HACKs
//			SimplePlace mog = (SimplePlace) getPlace("Mogadishu");
//			mog.centroid = new Location(2.0469, 45.3182);
//			loadCSV("worldcities.csv", null);
			InternalUtils.log("geo","LocalGeocoder loaded "+places.size());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Data from: https://en.wikipedia.org/wiki/List_of_cities_by_latitude
	 * @param resource
	 * @throws IOException
	 */
	private void loadWikipedia(String resource) throws IOException {
		BufferedReader r = new BufferedReader(
				new InputStreamReader(
						LocalGeocoder.class.getResourceAsStream(resource)
						));
		HashMap c2c = new HashMap();
		while(true) {
			String line = r.readLine();
			if (line==null) break;
			if (line.contains("Gaza")) {
				System.out.println(line);
			}
			// expects: eg | data-sort-value="10.183"| 10°11′N ||data-sort-value="-68"| 68°00′W  || [[Valencia, Venezuela|Valencia]] || [[Carabobo]] || {{VEN}}
			String[] bits = line.split("\\|\\|");
			if (bits.length < 5) continue;
			Pattern pcoord = Pattern.compile("(\\d+)°(\\d+)′(\\d*\"?)([NESW])");
			Matcher mlat = pcoord.matcher(bits[0]);
			Matcher mlng = pcoord.matcher(bits[1]);
			if ( ! mlat.find() || ! mlng.find()) {
				continue;
			}
			double degslat = Double.valueOf(mlat.group(1));
			double minlat = Double.valueOf(mlat.group(2));			
			double degslng = Double.valueOf(mlng.group(1));
			double minlng = Double.valueOf(mlng.group(2));
			double lat = degslat + (minlat/60);
			double lng = degslng + (minlng/60);
			String dirlng = mlng.group(4);
			String dirlat = mlat.group(4);
			if (dirlat.equals("S")) lat = -lat;
			if (dirlng.equals("W")) lng = -lng;
			
			String name = bits[2];
			String[] splitname = name.split("\\|");
			String shortname = splitname[splitname.length-1].trim();
			shortname = InternalUtils.trimPunctuation(shortname);			
			// pick the last bit -- the display name 			
			String country = bits[4];
			country = country.replaceFirst("<!--.+-->", ""); // no comments
			country = InternalUtils.removePunctuation(country).trim();	
			if (country.length()<2) {
				continue;
			}
			ISO3166 iso = new ISO3166();
			// Convert the (slightly ad-hoc) Wikipedia codes into proper country codes.
			String truncateAndHope = country.substring(0, 2);
			switch(country) {
			case "DZA": truncateAndHope = "Algeria"; break;
			case "ARE": truncateAndHope = "United Arab Emirates"; break;
			case "BUR": truncateAndHope = "Burma"; break;
			case "DEN": case "DNK": truncateAndHope = "Denmark"; break;
			case "DJI": truncateAndHope = "Djibouti"; break;
			case "EGY": truncateAndHope = "Egypt"; break;
			case "GER": truncateAndHope = "Germany"; break;
			case "SWE": truncateAndHope = "Sweden"; break;
			case "KAZ": truncateAndHope = "Kazakhstan"; break;
			case "MAS": truncateAndHope = "Malaysia"; break;
			case "MRT": truncateAndHope = "Mauritania"; break;
			case "POL": truncateAndHope = "Poland"; break;
			case "SIN": truncateAndHope = "Singapore"; break;
			case "SAM": truncateAndHope = "Samoa"; break;
			case "SOM": truncateAndHope = "Somalia"; break;
			case "IRQ": truncateAndHope = "Iraq"; break;
			case "ISR": case "ISR Disputed": truncateAndHope = "Israel"; break;
			case "PSE": case "PLE": truncateAndHope = "Palestine"; break;
			case "COM": truncateAndHope = "Comoros"; break;
			case "CRO": truncateAndHope = "Croatia"; break;
			case "PRC": truncateAndHope = "China"; break;
			case "TUN": truncateAndHope = "Tunisia"; break;
			case "TUR": truncateAndHope = "Turkey"; break;
			case "POR": truncateAndHope = "Portugal"; break;
			case "KOR": truncateAndHope = "Korea"; break;
			case "HAI": truncateAndHope = "Haiti"; break;
			case "JAM": truncateAndHope = "Jamaica"; break;
			case "ZIM": truncateAndHope = "Zimbabwe"; break;
			}
			String ccode = iso.getCountryCode(truncateAndHope);
			if (ccode==null) {
				continue;
			}
			String country2 = iso.getEverydayName(ccode);
			c2c.put(country, country2+" from "+name);
			Location posn = new Location(lat, lng);
			SimplePlace sp = new SimplePlace(shortname, null, ccode);
			sp.setGeocoder(getClass());
			sp.type = IPlace.TYPE_CITY;
			sp.centroid = posn;			
			places.add(sp);			
			// name lookup map
			String cn = canonical(shortname);
			if (cn.isEmpty()) continue; // shouldn't happen but best to be safe
			if (canonDesc2place.containsKey(cn)) {
//				System.out.println(canonDesc2place.get(cn));
			} else {
				canonDesc2place.put(cn, sp);
			}
		}				
//		System.out.println(Printer.toString(c2c, "\n", ": "));
	}

	private void loadCSV(String resource, Map<String, List<String>> code2names) throws IOException {
		BufferedReader r = new BufferedReader(
				new InputStreamReader(
						LocalGeocoder.class.getResourceAsStream(resource)
						));
		loadCSV(r, code2names);
	}

	/**
	 * @param location
	 * @return
	 * @throws PlaceNotFoundException
	 * This happens quite a bit due to not-unique from the crude
	 * bounding-box algorithm.
	 */
	public IPlace getPlace(Location location) throws PlaceNotFoundException {
		GeoCodeQuery query = new GeoCodeQuery().setLocation(location);
		return getBestPlace(query);
	}
		
	@Override
	public IPlace getPlace(String locationDescription) {
		Map<IPlace, Double> places = getPlace(new GeoCodeQuery(locationDescription));
		if (places.isEmpty()) return null;
		return InternalUtils.getBest(places);
	}
	
	@Override
	public Map<IPlace, Double> getPlace(GeoCodeQuery query) {
		String locnDesc = query.desc;
		Location locn = query.locn;
		if (locn==null) locn = Location.parse(locnDesc);
		if (locn!=null) {
			Map<IPlace, Double> pmap = getPlace2(locn);
			return pmap;
		}
		// by name?
		String ld = canonical(locnDesc);
		IPlace _place = canonDesc2place.get(ld);
		if (_place!=null) {
			return Collections.singletonMap(_place, 0.8);
		}
		throw new PlaceNotFoundException(locnDesc);
	}
	
	private Map<IPlace, Double> getPlace2(Location locn) {
		// 0? Special case 'cos it almost certainly means unset
		if (locn.latitude==0 && locn.longitude==0) {
			return Collections.emptyMap(); // TODO Should we have a North Pole constant for this?
		}
		// check the bounding boxes
		List<IPlace> possible = new ArrayList();
		// TODO a quad-tree??
		// 30km
		Dx cityRadius = new Dx(40000);
		for(IPlace place : places) {
			BoundingBox bbox = place.getBoundingBox();
			if (bbox==null) {
				if (place.getCentroid()==null) continue;
				// city check
				Dx dist = locn.distance(place.getCentroid());
				if (dist.isShorterThan(cityRadius)) {
					return Collections.singletonMap(place, 0.98);
				}
				continue;
			}
			if (bbox.contains(locn)) {
				possible.add(place);
			}
		}
		if (possible.size()==0) {
			throw new PlaceNotFoundException(locn.toString());
		}
		Map<IPlace, Double> map = new HashMap();
		for(IPlace p : possible) {
			map.put(p, 0.95);
		}
		return map;
	}

	/**
	 * @param locnDesc
	 * @return place or null 
	 */
	public IPlace getPlaceLenient(String locnDesc) {
		// Is it a longitude/latitude pair?
		// NB: this is here to distinguish the lat/long are not-unique exception 
		Location locn = Location.parse(locnDesc);
		if (locn!=null) {			
			return getPlace(locn);
		}
		// OK - try the normal service
		try {
			IPlace place = getPlace(locnDesc);
			if (place!=null) return place;
		} catch (PlaceNotFoundException e) {
			// we're not beat yet
		}
		// The lenient bit: by a part of name?, e.g. London, UK
		IPlace place = getPlaceLenient2(locnDesc);
		return place;
	}
	

	@Override
	public Boolean matches(GeoCodeQuery query, IPlace place) {
		// TODO use GIS to do better! !st develop test cases.
		return InternalUtils.geoMatch(query, place);
	}
	

	/**
	 * The lenient part of {@link #getPlaceLenient(String)}
	 * @param locnDesc
	 * @return place or null
	 */
	public SimplePlace getPlaceLenient2(String locnDesc) {
		String ld = canonical(locnDesc);
		// check each place as a word in locnDesc
		for(SimplePlace p : places) {
			// cn should always be a meaningful string
			for(String name : p.getNames()) {
				String cn = canonical(name);
				if (cn.isEmpty()) continue;
				Pattern regex;
				// We need to be careful with codes, which can accidentally 
				// be a part of a longer word. Oman/Uman is also problematic
				if (cn.length() < 5) {
					regex = Pattern.compile("\\b"+cn+"\\b", Pattern.CASE_INSENSITIVE);					
				} else {
					// Allow within-word matches, e.g. american
					regex = Pattern.compile("\\b"+cn, Pattern.CASE_INSENSITIVE);
				}
				if (regex.matcher(ld).find()) {
					return p;
				}
			}
		}
		return null;
	}

	String canonical(String locnDesc) {
		if (locnDesc==null) return null;
		// mangle it for easier matching
		// NB: stripping out spaces proved too much -- it led to false matches
		// e.g. with Oman/Uman appearing inside strings
		return InternalUtils.toCanonical(locnDesc);
	}

	public LocalGeocoder(BufferedReader csv) throws IOException {
		loadCSV(csv, null);
	}
	
	
	
	/**
	 * 
	 * @param csv
	 * @param code2names Alternate names -- can be null
	 * @throws IOException
	 */
	void loadCSV(BufferedReader csv, Map<String, List<String>> code2names) throws IOException {		
		// expects: country-code, place-name(s) ; separated, n, e, s, w, lat?, lng?
		while(true) {
			String line = csv.readLine();
			if (line==null) break;
			String[] bits = line.split("\\|");
			if (bits.length < 2) continue;
			String[] names = bits[1].trim().split(";");
			String country = bits[0].trim();
			SimplePlace sp;
			if (bits.length < 5) {
				// no geometry :(
				sp = new SimplePlace(names[0], null, country);
				sp.setGeocoder(getClass());
			} else {
				Location northEast = new Location(Double.valueOf(bits[2]),Double.valueOf(bits[3]));
				Location southWest = new Location(Double.valueOf(bits[4]),Double.valueOf(bits[5]));
				BoundingBox bbox = new BoundingBox(northEast, southWest);
				sp = new SimplePlace(names[0], bbox, country);
				sp.setGeocoder(getClass());
			}
			if (bits.length > 6) {
				Location centroid = new Location(Double.valueOf(bits[6]),Double.valueOf(bits[7]));
				sp.centroid = centroid;
			}
			loadCSV2_altNames(names, sp, code2names);
			places.add(sp);			
			// name lookup map
			for(String name : sp.getNames()) {
				String cn = canonical(name);
				if (cn.isEmpty()) continue; // shouldn't happen but best to be safe
				canonDesc2place.put(cn, sp);
			}
		}
		csv.close();
	}

	private void loadCSV2_altNames(String[] names, SimplePlace sp, Map<String,List<String>> countryNames) {
		// alternative names
		if (names.length > 1) {
			sp.setAlternativeNames(Arrays.asList(names));
			return;
		}
		ISO3166 iso3166 = new ISO3166();
		if (countryNames!=null && iso3166.getCountryCode(sp.getName())!=null) 
		{	// it's a country -- give it the known alternative names
			String ccode = iso3166.getCountryCode(sp.getName());
			List<String> cNames = countryNames.get(ccode);
			if (cNames !=null) sp.setAlternativeNames(cNames);
		}
	}
	
	Map<String,List<String>> loadISO3166NameData() throws IOException {
		// load from file
		InputStream strm = ISO3166.class
				.getResourceAsStream("iso-3166-country-codes.csv");
		Reader _reader = new InputStreamReader(strm, "UTF8");
		BufferedReader reader = new BufferedReader(_reader);
		String line = reader.readLine(); // discard the header line
		Map<String, List<String>> code2names = new HashMap();
		while(true) {
			line = reader.readLine();
			if (line==null) break;
			String[] bits = line.split("\t");
			String code = bits[1].toUpperCase();
			List<String> names = code2names.get(code);
			if (names==null) {
				names = new ArrayList();
				code2names.put(code, names);
			}
			names.add(bits[0]);
			// alternative names?
			if (bits.length > 5 && ! bits[5].isEmpty()) {
				String[] alternateNames = bits[5].split(";");
				for (String alt : alternateNames) {
					names.add(alt);
				}
			}
		}		
		return code2names;
	}

	/**
	 * Using a list & scanning with regexes.
	 * Not terribly efficient -- we should build a big word-based index
	 * instead.
	 */
	List<SimplePlace> places = new ArrayList();

	/**
	 * @param gq
	 * @return Best guess for the query.
	 * @throws PlaceNotFoundException
	 */
	public IPlace getBestPlace(GeoCodeQuery gq) {
		Map<IPlace, Double> qplaces = getPlace(gq);
		IPlace best = null;
		double bestScore = Double.NEGATIVE_INFINITY;
		for (IPlace p : qplaces.keySet()) {
			Double s = qplaces.get(p);
			if (s != null && s > bestScore) {
				best = p;
				bestScore = s;
			}
		}
		return best;
	}
}
