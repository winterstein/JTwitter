/* Package changed from myjavatools to avoid potential versioning conflicts.
 */
package winterwell.jtwitter.guts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Random;

import winterwell.jtwitter.InternalUtils;

/**
 * <p>Title: MyJavaTools: Client HTTP Request class</p>
 * <p>Description: this class helps to send POST HTTP requests with various form data,
 * including files. Cookies can be added to be included in the request.</p>
 *
  * Licensed under the myjavatools license (Apache license version 2.0)
  * http://www.myjavatools.com/license.txt
  *
  * @author Vlad Patryshev
  * @author James Peltzer
  * @version 6.0
  */
 public class ClientHttpRequest extends Observable
 {
  URLConnection connection;
  OutputStream os = null;
  Map<String, String> cookies = new HashMap<String, String>();
  String rawCookies = "";

  protected void connect() throws IOException {
    if (os == null) os = connection.getOutputStream();
  }

  protected void write(char c) throws IOException {
    connect();
    os.write(c);
  }

  
  private static Charset UTF_8;
  static {
	  try {
		  UTF_8  = Charset.forName("UTF8");
	  } catch(Exception ex) {
		  InternalUtils.log("jtwitter", "No utf8 charset: "+ex);
	  }
  }
  
  
  protected void write(String s) throws IOException {
    connect();
    if (UTF_8!=null) {
    	os.write(s.getBytes(UTF_8));
    	return;
    }
    os.write(s.getBytes());
  }

  protected long newlineNumBytes()
  {
    return 2;
  }

  protected void newline() throws IOException {
    connect();
    write("\r\n");
  }

  protected void writeln(String s) throws IOException {
    connect();
    write(s);
    newline();
  }

  private static Random random = new Random();

  protected static String randomString() {
    return Long.toString(random.nextLong(), 36);
  }

  private long boundaryNumBytes()
  {
    return boundary.length() + 2 /*--*/;
  }

  String boundary = "---------------------------" +
      randomString() + randomString() + randomString();

  private void boundary() throws IOException {
    write("--");
    write(boundary);
  }

  /**
   * Creates a new multipart POST HTTP request on a freshly opened URLConnection
   *
   * @param connection an already open URL connection
   * @throws IOException
   */
  public ClientHttpRequest(URLConnection connection) throws IOException {
    this.connection = connection;
    connection.setDoOutput(true);
    connection.setDoInput(true);
    connection.setRequestProperty("Content-Type",
                                  "multipart/form-data; boundary=" + boundary);
  }

  /**
   * Creates a new multipart POST HTTP request for a specified URL
   *
   * @param url the URL to send request to
   * @throws IOException
   */
  public ClientHttpRequest(URL url) throws IOException {
    this(url.openConnection());
  }

  /**
   * Creates a new multipart POST HTTP request for a specified URL string
   *
   * @param urlString the string representation of the URL to send request to
   * @throws IOException
   */
  public ClientHttpRequest(String urlString) throws IOException {
    this(new URL(urlString));
  }

  private void postCookies() {
    StringBuffer cookieList = new StringBuffer(rawCookies);
    for (Map.Entry<String,String> cookie : cookies.entrySet()) {
      if (cookieList.length() > 0) {
        cookieList.append("; ");
      }
      cookieList.append(cookie.getKey() + "=" + cookie.getValue());
    }
    if (cookieList.length() > 0) {
      connection.setRequestProperty("Cookie", cookieList.toString());
    }
  }

  /**
   * Adds a cookie to the requst
   * @param name cookie name
   * @param value cookie value
   * @throws IOException
   */
  public void setCookies(String rawCookies) throws IOException {
    this.rawCookies = (rawCookies == null) ? "" : rawCookies;
    cookies.clear();
  }

  /**
   * Adds a cookie to the requst
   * @param name cookie name
   * @param value cookie value
   * @throws IOException
   */
  public void setCookie(String name, String value) throws IOException {
        cookies.put(name, value);
  }

  /**
   * Adds cookies to the request
   * @param cookies the cookie "name-to-value" map
   * @throws IOException
   */
  public void setCookies(Map cookies) throws IOException {
    if (cookies != null) {
      this.cookies.putAll(cookies);
    }
  }

  /**
   * Adds cookies to the request
   * @param cookies array of cookie names and values
   * (cookies[2*i] is a name, cookies[2*i + 1] is a value)
   * @throws IOException
   */
  public void setCookies(String[] cookies) throws IOException {
    if (cookies != null) {
      for (int i = 0; i < cookies.length - 1; i+=2) {
        setCookie(cookies[i], cookies[i+1]);
      }
    }
  }

  private long writeNameNumBytes(String name)
  {
    return
        newlineNumBytes() +
       "Content-Disposition: form-data; name=\"".length() +
        name.getBytes().length +
        1 /*'"'*/;
  }

  private void writeName(String name) throws IOException {
    newline();
    write("Content-Disposition: form-data; name=\"");
    write(name);
    write('"');
  }

  private boolean isCanceled = false;
  private int bytesSent = 0;

  public int getBytesSent()
  {
    return bytesSent;
  }

  public void cancel()
  {
    isCanceled = true;
  }

  private synchronized void pipe(InputStream in, OutputStream out)
      throws IOException {
    byte[] buf = new byte[1024];
    int nread;
    bytesSent = 0;
    isCanceled = false;
    synchronized (in) {
      while((nread = in.read(buf, 0, buf.length)) >= 0) {
        out.write(buf, 0, nread);
        bytesSent += nread;
        if (isCanceled) {
          throw new IOException("Canceled");
        }
        out.flush();
        this.setChanged();
        this.notifyObservers(bytesSent);
        this.clearChanged();
      }
    }
    out.flush();
    buf = null;
  }

  /**
   * Adds a string parameter to the request
   * @param name parameter name
   * @param value parameter value
   * @throws IOException
   */
  public void setParameter(String name, String value) throws IOException {
    boundary();
    writeName(name);
    newline(); newline();
    writeln(value);
  }

  /**
   * Adds a file parameter to the request
   * @param name parameter name
   * @param filename the name of the file
   * @param is input stream to read the contents of the file from
   * @throws IOException
   */
  public void setParameter(String name, String filename, InputStream is) throws IOException {
    boundary();
    writeName(name);
    write("; filename=\"");
    write(filename);
    write('"');
    newline();
    write("Content-Type: ");
    String type = URLConnection.guessContentTypeFromName(filename);
    if (type == null) type = "application/octet-stream";
    writeln(type);
    newline();
    pipe(is, os);
    newline();
  }

  public long getFilePostSize(String name, File file)
  {
    String filename = file.getPath();
    String type = URLConnection.guessContentTypeFromName(filename);
    if (type == null) type = "application/octet-stream";

    return
      boundaryNumBytes() +
      writeNameNumBytes(name) +
     "; filename=\"".length() +
      filename.getBytes().length +
      1 +
      newlineNumBytes() +
      "Content-Type: ".length() +
      type.length() +
      newlineNumBytes() +
      newlineNumBytes() +
      file.length() +
      newlineNumBytes();
  }

  /**
   * Adds a file parameter to the request
   * @param name parameter name
   * @param file the file to upload
   * @throws IOException
   */
  public void setParameter(String name, File file) throws IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      setParameter(name, file.getPath(), fis);
    } finally {
      if (fis != null) {
        fis.close();
      }
    }
  }

  /**
   * Adds a parameter to the request; if the parameter is a File, the file is uploaded, otherwise the string value of the parameter is passed in the request
   * @param name parameter name
   * @param object parameter value, a File or anything else that can be stringified
   * @throws IOException
   */
  public void setParameter(Object name, Object object) throws IOException {
    if (object instanceof File) {
      setParameter(name.toString(), (File) object);
    } else {
      setParameter(name.toString(), object.toString());
    }
  }

  /**
   * Adds parameters to the request
   * @param parameters "name-to-value" map of parameters; if a value is a file, the file is uploaded, otherwise it is stringified and sent in the request
   * @throws IOException
   */
  public void setParameters(Map parameters) throws IOException {
    if (parameters != null) {
      for (Iterator i = parameters.entrySet().iterator(); i.hasNext();) {
        Map.Entry entry = (Map.Entry)i.next();
        setParameter(entry.getKey().toString(), entry.getValue());
      }
    }
  }

  /**
   * Adds parameters to the request
   * @param parameters (vararg) parameter names and values (parameters[2*i] is a name, parameters[2*i + 1] is a value); if a value is a file, the file is uploaded, otherwise it is stringified and sent in the request
   * @throws IOException
   */
  public void setParameters(Object... parameters) throws IOException {
    for (int i = 0; i < parameters.length - 1; i += 2) {
      setParameter(parameters[i].toString(), parameters[i + 1]);
    }
  }

  public long getPostFooterSize()
  {
    return boundaryNumBytes() + 2 /*--*/ +
        newlineNumBytes() + newlineNumBytes();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added
   * @return input stream with the server response
   * @throws IOException
   */
  private InputStream doPost() throws IOException {
    boundary();
    writeln("--");
    os.close();

    return connection.getInputStream();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added
   * @return input stream with the server response
   * @throws IOException
   */
  public InputStream post() throws IOException {
          postCookies();
        return doPost();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added before (if any), and with parameters that are passed in the argument
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   */
  public InputStream post(Map parameters) throws IOException {
    postCookies();
    setParameters(parameters);
    return doPost();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added before (if any), and with parameters that are passed in the argument
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   */
  public InputStream post(Object... parameters) throws IOException {
    postCookies();
    setParameters(parameters);
    return doPost();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added before (if any), and with cookies and parameters that are passed in the arguments
   * @param cookies request cookies
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   * @see setCookies
   */
  public InputStream post(Map cookies, Map parameters) throws IOException {
    setCookies(cookies);
    postCookies();
    setParameters(parameters);
    return doPost();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added before (if any), and with cookies and parameters that are passed in the arguments
   * @param cookies request cookies
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   * @see setCookies
   */
  public InputStream post(String raw_cookies, Map parameters) throws IOException {
    setCookies(raw_cookies);
    postCookies();
    setParameters(parameters);
    return doPost();
  }

  /**
   * Posts the requests to the server, with all the cookies and parameters that were added before (if any), and with cookies and parameters that are passed in the arguments
   * @param cookies request cookies
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   * @see setCookies
   */
  public InputStream post(String[] cookies, Object[] parameters) throws IOException {
    setCookies(cookies);
    postCookies();
    setParameters(parameters);
    return doPost();
  }

  /**
   * Posts a new request to specified URL, with parameters that are passed in the argument
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   */
  public static InputStream post(URL url, Map parameters) throws IOException {
    return new ClientHttpRequest(url).post(parameters);
  }

  /**
   * Posts a new request to specified URL, with parameters that are passed in the argument
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setParameters
   */
  public static InputStream post(URL url, Object[] parameters) throws IOException {
    return new ClientHttpRequest(url).post(parameters);
  }

  /**
   * Posts a new request to specified URL, with cookies and parameters that are passed in the argument
   * @param cookies request cookies
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setCookies
   * @see setParameters
   */
  public static InputStream post(URL url, Map cookies, Map parameters) throws IOException {
    return new ClientHttpRequest(url).post(cookies, parameters);
  }

  /**
   * Posts a new request to specified URL, with cookies and parameters that are passed in the argument
   * @param cookies request cookies
   * @param parameters request parameters
   * @return input stream with the server response
   * @throws IOException
   * @see setCookies
   * @see setParameters
   */
  public static InputStream post(URL url, String[] cookies, Object[] parameters) throws IOException {
    return new ClientHttpRequest(url).post(cookies, parameters);
  }
}
