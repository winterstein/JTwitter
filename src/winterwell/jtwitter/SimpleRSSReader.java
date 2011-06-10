package winterwell.jtwitter;

import java.io.StringReader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import winterwell.jtwitter.Twitter.Status;
import winterwell.jtwitter.Twitter.User;

/**
 * A crude RSS parser - since we need to handle a bit of RSS for a 
 * proper user timeline that includes retweets.
 * <p>
 * Note: this contains various utility methods copied from the 
 * winterwell.utils library.
 * 
 * @author Daniel
 */
class SimpleRSSReader {

	/**
	 * Note: XPaths are not thread safe, so best to create new ones as
	 * needed
	 */
	static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
	
	private final String rss;
	private final User user;

	SimpleRSSReader(User user, String rss) {
		this.user = user;
		this.rss = rss;
	}

	List<Status> getStatuses() {
		try {
			List<Node> items = xpathQuery("//item", rss, false);
			List<Status> texts = new ArrayList<Status>();
			for(Node node : items) {
				String title = xpathExtractString("title", node);
				String tweet = title.substring(user.screenName.length()+2);
				// lose those html entities
				tweet = Twitter.unencode(tweet);
				String pubDate = xpathExtractString("pubDate", node);
				String twid = xpathExtractString("guid", node);
				int i = twid.lastIndexOf('/');
				String id = twid.substring(i+1);
				Date date = new Date(pubDate);
				Status s = new Status(user, tweet, Long.valueOf(id), date);
				texts.add(s);
			}
			return texts;
		} catch (Exception e) {			
			throw new TwitterException(e);
		}
	}
	
	List<Node> asList(final NodeList scripts) {
		return new AbstractList<Node>() {
			@Override
			public Node get(int index) {
				return scripts.item(index);
			}

			@Override
			public int size() {
				return scripts.getLength();
			}
		};
	}

	/**
	 * @see #xpathQuery(String, String, boolean)
	 * @param xpathQuery
	 * @param node
	 * @return
	 * @throws XPathExpressionException 
	 */
	List<Node> xpathQuery(String xpathQuery, Node node) throws XPathExpressionException {
		XPathExpression expr = XPATH_FACTORY.newXPath().compile(xpathQuery);
		NodeList nodeList = (NodeList) expr.evaluate(node, XPathConstants.NODESET);		
		List<Node> nodes = asList(nodeList);
		return nodes;
	}
	
	
	/**
	 * Run an XPath query over an xml document.
	 * <p>
	 * <h3>XPath syntax</h3>
	 * E.g. given the document
	 * <pre>
	 * &lt;shelf>
	 * &lt;book year='1960'>&lt;title>Catch 22&lt;/title>&lt;author>Joseph Heller&lt;/author>&lt;/book>
	 * &lt;book year='2007'>&lt;title>The English Swordsman&lt;/title>&lt;author>Daniel Winterstein&lt;/author>&lt;/book>
	 * &lt;/shelf>
	 * </pre>
	 * You could have the queries:
	 * <pre>
	 * "//book[author='Joseph Heller']/title"
	 * "//book[@year='2007']"
	 * "/shelf/book/title"
	 * </pre>
	 * See http://www.zvon.org/xxl/XPathTutorial/General/examples.html for more info.
	 * <p> 
	 * Note: This method is not optimally efficient if the same query is repeated, or the same document queried multiple times.
	 * @param xpathQuery E.g. "//book[author="Joseph Heller"]/title"
	 * @param xml
	 * @param namespaceAware
	 * @return
	 * @throws Exception 
	 */
	List<Node> xpathQuery(String xpathQuery, String xml, boolean namespaceAware) throws Exception {
		// Parse XML
		Document doc = parseXml(xml, namespaceAware);
		// Build an XPath query
		XPath xp = XPATH_FACTORY.newXPath();
		XPathExpression expr = xp.compile(xpathQuery);
		NodeList nodeList = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);		
		List<Node> nodes = asList(nodeList);
		return nodes;
	}
	
	/**
	 * 
	 * @param xml
	 * @param namespaceAware
	 * @return
	 * @throws ParserConfigurationException 
	 * @testedby {@link WebUtilsTest#testParseXml()}
	 */
	Document parseXml(String xml, boolean namespaceAware) throws Exception {
//		// Pop the first line if its a DTD spec
//		// This is to prevent the baked in xerces behaviour of making a web call, 
//		// then throwing an exception unless that web call succeeds
//		if (xml.startsWith("<!DOCTYPE")) {
//			int i = xml.indexOf('<', 1);
//			if (i != -1) {
//				xml = xml.substring(i);
//			}
//		}
		// but then we get other exceptions - with undeclared entities :(
		// TODO find a lighter/faster xml parser
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(namespaceAware);	
		factory.setValidating(false); // What bit of non-validating doesn't Xerces understand?!
//		factory.setXIncludeAware(false); unnecessary and causes errors
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputSource input = new InputSource(new StringReader(xml));
		Document doc = builder.parse(input);
		return doc;		
	}
	
	
	/**
	 * Use an xpath query to extract what is expected to be a single
	 * string valued node. This is a convenience method for a common case.
	 * @param XPATH
	 * @param node
	 * @return the resulting node's text content, or null if there is
	 * no such node.
	 * @throws XPathExpressionException 
	 * @throws NotUniqueException if the query returns multiple nodes.
	 */
	String xpathExtractString(String xpathQuery, Node node) throws XPathExpressionException {
		List<Node> titles = xpathQuery(xpathQuery, node);
		if (titles.isEmpty()) return null;
		return titles.get(0).getTextContent();		
	}
}
