package winterwell.jtwitter;
//package winterwell.jtwitter;
//import java.util.Map;
//
//import javax.jws.soap.SOAPBinding.ParameterStyle;
//
///**
// * This has a lot of dependencies - see http://code.google.com/p/oauth/
// * 
// * @see OAuthSignpostClient which uses an alternative OAuth library.
// * 
// * Winterwell are not actively maintaining this class, though we will
// * merge in updates and improvements if you want to submit them.  
// * 
// * @author John Kristian <jmkristian@gmail.com>
// */
//public class OAuthHttpClient implements Twitter.IHttpClient {
//
//	public static final OAuthServiceProvider TWITTER_SERVICE_PROVIDER = new OAuthServiceProvider(
//	        "http://twitter.com/oauth/request_token",
//	        "http://twitter.com/oauth/authorize",
//	        "http://twitter.com/oauth/access_token");
//
//	private final OAuthAccessor accessor;
//	private final OAuthClient client;
//
//	public OAuthHttpClient(OAuthAccessor accessor, OAuthClient client) {
//		this.accessor = accessor;
//		this.client = client;
//	}
//
//	@Override
//	public boolean canAuthenticate() {
//		return accessor.accessToken != null;
//	}
//
//	@Override
//	public String getPage(String uri, Map<String, String> vars, boolean authenticate) throws TwitterException {
//		return access(OAuthMessage.GET, uri, vars, authenticate);
//	}
//
//	@Override
//	public String post(String uri, Map<String, String> vars, boolean authenticate) throws TwitterException {
//		return access(OAuthMessage.POST, uri, vars, authenticate);
//	}
//
//	private String access(String httpMethod, String url, Map<String, String> parameters, boolean authenticate)
//	        throws TwitterException {
//		try {
//			OAuthMessage request = new OAuthMessage(httpMethod, url,
//					(parameters == null) ? null : parameters.entrySet());
//			request.getHeaders().add(
//			        new OAuth.Parameter("User-Agent",
//			                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)"));
//			if (authenticate) {
//				assert accessor.accessToken != null && accessor.tokenSecret != null
//				: "Need OAuth access token and secret for this method";
//				request.addRequiredParameters(accessor);
//			}
//			OAuthResponseMessage response = client.access(request, getStyle(request));
//			int statusCode = response.getHttpResponse().getStatusCode();
//			if (statusCode != HttpResponseMessage.STATUS_OK) {
//				throw new TwitterException(response.toOAuthProblemException());
//			}
//			return response.readBodyAsString();
//		} catch (TwitterException t) {
//			throw t;
//		} catch (Exception e) {
//			throw new TwitterException(e);
//		}
//	}
//
//	private ParameterStyle getStyle(OAuthMessage request) {
//		Object ps = accessor.consumer.getProperty(OAuthClient.PARAMETER_STYLE);
//		ParameterStyle style = (ps != null) ? Enum.valueOf(ParameterStyle.class, ps.toString())
//				: (OAuthMessage.POST.equals(request.method) ? ParameterStyle.BODY
//						: ParameterStyle.QUERY_STRING);
//		return style;
//	}
//
//	int timeout; // TODO use this!
//	
//	/**
//	 * This does not do anything at present!
//	 */
//	@Deprecated
//	@Override
//	public void setTimeout(int millisecs) {
//		this.timeout = millisecs;
//	}
// }
