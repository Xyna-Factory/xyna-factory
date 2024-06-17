/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xact.trigger;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLSocket;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.streams.LineBufferedInputStream;
import com.gip.xyna.utils.streams.LineBufferedInputStream.LineMarker;
import com.gip.xyna.xact.trigger.http.HTTPTRIGGER_HTTP_STREAM_ERROR;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;



public class HTTPTriggerConnection extends TriggerConnection {

  private static final long serialVersionUID = -3773548159077128176L;

  private static Logger logger = CentralFactoryLogging.getLogger(HTTPTriggerConnection.class);

  private static final String CRLF = "\r\n";

  private transient Socket socket;
  private transient SocketChannel socketChannel;
  private String uri;
  private String version = "HTTP/1.0";
  @Deprecated //nur noch für Deserialisierung
  private String method; 
  private Method methodEnum;
  private Properties header;
  @Deprecated
  /**
   * Use parameters instead
   */
  private Properties paras;
  private HashMap<String, List<String>> parameters;
  private String payload;
  private String charSet = Charset.defaultCharset().name();

  private AtomicBoolean isRead = new AtomicBoolean();

  private String triggerIp;
  private String triggerHostname;
  //private transient InputStream inputStream;
  private transient LineBufferedInputStream lineBufferedInputStream;
  private transient OutputStream outputStream;
  private final boolean suppressLogging;
  /**
   * Some HTTP response status codes
   */
  public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently",
                  HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found",
                  HTTP_BADREQUEST = "400 Bad Request", HTTP_UNAUTHORIZED = "401 Unauthorized",
                  HTTP_CONFLICT = "409 Conflict", HTTP_INTERNALERROR = "500 Internal Server Error",
                  HTTP_NOTIMPLEMENTED = "501 Not Implemented";


  public static final String PROP_KEY_WWW_AUTHENTICATE = "WWW-Authenticate";
  public static final String PROP_KEY_CONNECTION = "Connection";
  public static final String PROP_KEY_CONTENT_LENGTH = "Content-Length";
  public static final String PROP_KEY_CONTENT_TYPE = "Content-Type";
  public static final String PROP_KEY_AUTHORIZATION = "authorization";
  private static final Set<String> HEADERKEYS_SEPARATE =
      new HashSet<String>(Arrays.asList(new String[] {PROP_KEY_CONTENT_TYPE, PROP_KEY_CONTENT_LENGTH}));


  public static final String MD5_ALGORITHM = "MD5";
  
  //header hat immer ISO-8859-1 encoding
  private static final String ENCODING_HEADER = "ISO-8859-1";
  
  private final static Pattern CHUNK_SIZE_PATTERN = Pattern.compile("^([a-fA-F0-9]+)");

  /**
   * Common mime types for dynamic content
   */
  public static final String MIME_PLAINTEXT = "text/plain", MIME_HTML = "text/html",
                  MIME_DEFAULT_BINARY = "application/octet-stream";

  public static enum Method {
    GET(false),
    POST(true),
    PUT(true),
    DELETE(false),
    HEAD(false),
    OPTIONS(false),
    PATCH(true),
    TRACE(false);

    private boolean hasPayload;

    private Method(boolean hasPayload) {
      this.hasPayload = hasPayload;
    }
    
    public boolean hasPayload() {
      return hasPayload;
    }
  }

  
  public HTTPTriggerConnection(HTTPTrigger trigger, Socket s, boolean suppressLogging) throws XynaException {
    this.suppressLogging = suppressLogging;
    if (!suppressLogging && logger.isDebugEnabled()) {
      logger.debug("HTTP connection established: (local address: " + s.getLocalSocketAddress() + ", remote address: "
          + s.getRemoteSocketAddress() + ")");
      if (logger.isTraceEnabled()) {
        logger.trace("Classloader: " + getClass().getClassLoader());
        try {
          logger.trace("solinger="+s.getSoLinger());
        } catch (SocketException e) {
          logger.trace("solinger=unknown (" + e.getMessage() + ")");
        }
        try {
          logger.trace("oobinline=" + s.getOOBInline());
        } catch (SocketException e) {
          logger.trace("oobinline=unknown (" + e.getMessage() + ")");
        }
        try {
          logger.trace("rcvbuf=" + s.getReceiveBufferSize() + " sndbuf=" + s.getSendBufferSize());
        } catch (SocketException e) {
          logger.trace("rcvbuf=unknown sndbuf=unknown (" + e.getMessage() + ")");
        }
      }
    }

    this.socket = s;
    try {
      lineBufferedInputStream = new LineBufferedInputStream( socket.getInputStream(),
                                                             LineMarker.CRLF, ENCODING_HEADER);
      outputStream = socket.getOutputStream();
    } catch (IOException e) {
      throw new XynaException("could not open streams to socket", e);
    }
    triggerIp = trigger.getOwnIp();
    triggerHostname = trigger.getOwnHostname();

  }


  public HTTPTriggerConnection(HTTPTrigger trigger, SocketChannel s, boolean suppressLogging) throws XynaException {
    this(trigger, s.socket(), suppressLogging);
    this.socketChannel = s;
  }
  
  
  /**
   * 
   * @throws XynaException falls fehler nicht zurück signalisiert wurde
   * @throws InterruptedException falls fehler bereits zurück signalisiert wurde
   */
  public void readHeader() throws XynaException, InterruptedException {
    if (socket == null) {
      if (isRead.get()) {
        if (!suppressLogging) {
          logger.debug("HTTP request reading has already begun, nothing to be done.");
        }
        return;
      } else {
        throw new RuntimeException("The socket is not available. This is propably caused by improper"
            + " implementation of filter. Method read may not be called after server restart.");
      }
    }

    // atomically make sure that the stream is only read once
    if (!isRead.compareAndSet(false, true)) {
      // FIXME eigentlich müsste man in diesem Fall blockieren, bis das andere read fertig ist, damit
      //       der zweite Thread nicht auf leere Felder zugreift
      if (!suppressLogging) {
        logger.debug("HTTP request reading has already begun, nothing to be done.");
      }
      return;
    }

    if (!suppressLogging && logger.isTraceEnabled()) {
      logger.trace("Reading HTTP request ... ");
    }

    if (lineBufferedInputStream == null) {
      sendError(HTTP_BADREQUEST, "BAD REQUEST: No Arguments. Usage: GET /uri");
    }

    try {
      readMethodAndUriAndParameters(); //erste zeile die nicht nur ein zeilenumbruch ist
      readHeaders(); //zeilen in der form <name>=<value> CRLF

    } catch (IOException e) {
      throw new HTTPTRIGGER_HTTP_STREAM_ERROR(e.getMessage(), e);
    }
  }

  /**
   * 
   * @throws XynaException falls fehler nicht zurück signalisiert wurde
   * @throws InterruptedException falls fehler bereits zurück signalisiert wurde
   */
  public void readPayload() throws XynaException, InterruptedException {
    readPayload(0x7FFFFFFFFFFFFFFFl);
  }
  
  
  /**
   * @param contentLengthIfEmpty falls keine content-length im header gefunden wird, werden soviele bytes trotzdem versucht auszulesen
   * @throws XynaException falls fehler nicht zurück signalisiert wurde
   * @throws InterruptedException falls fehler bereits zurück signalisiert wurde
   */
  public String readPayload(long contentLengthIfEmpty) throws XynaException, InterruptedException {
    if (header.containsKey("transfer-encoding") && 
        header.getProperty("transfer-encoding").equals("chunked")) {
      payload = readChunkedPayload();
    } else {
   
    if (methodEnum.hasPayload()) {
      boolean contentLengthSet = false;
      String contentByteLength = header.getProperty("content-length");
      long numberOfBytes;
      if (contentByteLength != null) {
        try {
          numberOfBytes = Long.parseLong(contentByteLength);
          contentLengthSet = true;
        } catch (NumberFormatException ex) {
          numberOfBytes = contentLengthIfEmpty;
        }
      } else {
        numberOfBytes = contentLengthIfEmpty;
      }
      
      payload = readPayloadInternally(numberOfBytes, !contentLengthSet);
      payload =  payload.trim();
      //decodeParas(postLine, paras); //FIXME: braucht man das? führt aber zu fehlern, wenn zb %<keine zahl> im text vorkommt.

    } else {
      payload = "";
    }
    }

    if (!suppressLogging && logger.isTraceEnabled() && payload != null && payload.length() > 0) {
      if (payload.length() > 1000) {
        logger.trace("payload = " + payload.substring(0, 1000) + "... [truncated]");
      } else {
        logger.trace("payload = " + payload);
      }
    }
    
    return payload;
  }
  
  private String readPayloadInternally(long numberOfBytes, boolean returnOnTrailingLineBreakInBuffer) throws XynaException, InterruptedException {
    String payload;
    
    try {
        if (numberOfBytes <= 0) {
          // if the size is empty dont try to read from the stream because that would block until timeout (bugz 13244)
          payload = "";
        } else {
          //TODO unterscheiden zwischen content-types: bei binärdaten ist das vielleicht nicht die beste methode...
          ByteArrayOutputStream postLine = new ByteArrayOutputStream();
          byte buf[] = new byte[(int) Math.min(512l, numberOfBytes)];
          int read = lineBufferedInputStream.read(buf);

          int readBytes = 0; //bereits gelesene bytes
          String append = ""; //aktuell gelesener teilstring
          String previousAppend; //zuletzt gelesener teilstring
          while (read >= 0) {
            previousAppend = append;
            append = new String(buf, 0, read, getCharSet());
            readBytes += read;
            postLine.write(buf, 0, read);

            if (readBytes >= numberOfBytes) {
              //falls size durch contentlength ermittelt wurde
              break;
            } else if (returnOnTrailingLineBreakInBuffer) {
              //falls size nicht durch contentlength ermittelt wurde
              if ((append.equals("\n") && previousAppend.endsWith("\r"))
                  || (append.length() > 1 && append.endsWith("\r\n"))) {
                break;
              }
            }
            buf = new byte[(int) Math.min(512l, numberOfBytes - readBytes)];
            read = lineBufferedInputStream.read(buf);
          }

          payload = new String(postLine.toByteArray(), getCharSet());
        }
    } catch (IOException e) {
      throw new HTTPTRIGGER_HTTP_STREAM_ERROR(e.getMessage(), e);
    }

    return payload;
  }
  
  
  public String readChunkedPayload() throws XynaException, InterruptedException {
    StringBuilder payloadBuilder = new StringBuilder();
    try {
      String chunkMeta = lineBufferedInputStream.readLine();
      // TODO read chunk extension data
      Matcher chunkSizeMatcher = CHUNK_SIZE_PATTERN.matcher(chunkMeta);
      while (chunkSizeMatcher.find()) {
        long chunkSize = Long.parseLong(chunkSizeMatcher.group(1), 16);
        if (chunkSize <= 0) {
          break;
        }
        String chunk = readPayloadInternally(chunkSize, false);
        payloadBuilder.append(chunk);
        lineBufferedInputStream.readLine(); // we expect a trailing linebreak per chunk
        chunkMeta = lineBufferedInputStream.readLine();
        if (chunkMeta == null) {
          break;
        }
        chunkSizeMatcher = CHUNK_SIZE_PATTERN.matcher(chunkMeta);
      }
    } catch (IOException e) {
      throw new HTTPTRIGGER_HTTP_STREAM_ERROR(e.getMessage(), e);
    }
    return payloadBuilder.toString();
  }
  
  
  /**
   * based on http://tools.ietf.org/html/rfc2616
   * @throws XynaException falls fehler nicht zurück signalisiert wurde
   * @throws InterruptedException falls fehler bereits zurück signalisiert wurde
   */
  public void read() throws XynaException, InterruptedException {
    readHeader();
    readPayload();
  }
  
  /**
   * @param onlyHeader Soll nur Header gelesen werden und Rest als Stream ausgegeben werden
   * @return falls onlyHeader = true: InputStream zum Lesen der Payload, ansonsten null.
   * @throws XynaException
   * @throws InterruptedException
   */
  public InputStream read(boolean onlyHeader) throws XynaException, InterruptedException {
    readHeader();
    if( onlyHeader ) {
      return lineBufferedInputStream;
    } else {
      readPayload();
      return null;
    }
  }

  public InputStream getInputStream() {
    return lineBufferedInputStream;
  }

  /**
   * ermöglicht, dass der erneute aufruf von read auch tatsächlich erneut aus dem socket liest.
   * für connections gedacht, über die mehrere nachrichten ausgetauscht werden. 
   */
  public void reset() {
    isRead.set(false);
  }

  
  private void readHeaders() throws IOException, InterruptedException, SocketNotAvailableException,
      HTTPTRIGGER_HTTP_STREAM_ERROR {

    header = new Properties();

    String line1 = null;
    while ((line1 = lineBufferedInputStream.readLine()) != null) {
      if (line1.length() == 0) {
        debugProperties("header", header, true);
        return; //headers fertig
      }

      if (!suppressLogging && logger.isTraceEnabled()) {
        logger.trace("headerline :" + line1);
      }
      int p = line1.indexOf(':');
      if (p == -1) {
        // debug as many header fields as have been parsed so far
        debugProperties("header", header, true);
        if (!suppressLogging && logger.isDebugEnabled()) {
          logger.debug("Incomplete header field: " + line1);
        }
        logInvalidHttpRequest();
        sendError(HTTP_BADREQUEST, "BAD REQUEST: Incomplete Header");
      }
      header.put(line1.substring(0, p).trim().toLowerCase(), line1.substring(p + 1).trim());
    }

    debugProperties("header", header, true);

  }


  private void debugProperties(String fieldName, Properties properties, boolean onlyIfTraceDisabled) {

    if (suppressLogging || !logger.isDebugEnabled() || (logger.isTraceEnabled() && onlyIfTraceDisabled)) {
      return;
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Parsed ").append(fieldName).append(" fields: ");

    Set<Object> propertyKeys = properties.keySet();
    int count = 0;
    for (Object propertyKey : propertyKeys) {
      sb.append("{").append(propertyKey).append(": ").append(properties.get(propertyKey)).append("}");
      count++;
      if (count < propertyKeys.size()) {
        sb.append(", ");
      }
    }

    logger.debug(sb.toString());

  }

  private static final long timeoutFirstPartOfRequestLine = 5000; //TODO konfigurierbar?

  /**
   * @param in
   * @throws IOException
   * @throws InterruptedException
   * @throws SocketNotAvailableException
   * @throws HTTPTRIGGER_HTTP_STREAM_ERROR
   */
  private void readMethodAndUriAndParameters() throws IOException, InterruptedException,
      SocketNotAvailableException, HTTPTRIGGER_HTTP_STREAM_ERROR {

    int previousTimeout = this.socket.getSoTimeout();
    try {
      // unreasonable large amount of time to read method, uri and parameters to prevent a hanging request
      this.socket.setSoTimeout(60000);
    } catch (SocketException e) {
      if (!suppressLogging) {
        logger.warn("Failed to override SocketTimeout", e);
      }
    }

    long start = System.currentTimeMillis();
    //request-zeile zweiteilig auslesen, damit bereits zu beginn eine validierung durchgeführt werden kann.
    //beispiel: ssh-handshake vs http-(nicht https)-trigger: erste zeile enthält dann irgendwelchen kram, aber nicht notwendigerweise einen zeilenumbruch 
    //-> führt zu clienttimeout, weil der server nie antwortet
    byte[] firstPartOfLine = new byte[4];
    int read;
    int off = 0;
    while (off < 4) {
      read = lineBufferedInputStream.read(firstPartOfLine, off, 4 - off);
      off += read;
      if (off < 4 && (read == -1 || System.currentTimeMillis() - start > timeoutFirstPartOfRequestLine)) {
        if (!suppressLogging) {
          logger.debug("Error while reading method: No data to be read.");
        }
        throw new SocketException("No data found while reading method");
      }
    }
    validateStartOfRequestLine(firstPartOfLine);
    ByteArrayOutputStream baos = lineBufferedInputStream.readLineAsByteArrayOutputStream();
    String firstLine = new String(firstPartOfLine, ENCODING_HEADER) + baos.toString(ENCODING_HEADER);
    
    if (!suppressLogging && logger.isInfoEnabled()) {
      logger.info("firstLine = "+ firstLine);
    }

    StringTokenizer tokenizedFirstLine = new StringTokenizer(firstLine);

    if (tokenizedFirstLine.hasMoreTokens()) {
      try {
        methodEnum = Method.valueOf( (tokenizedFirstLine.nextToken()).toUpperCase() );
      } catch( Exception e ) {
        logInvalidHttpRequest();
        sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /uri");
      }
    } else {
      // throws Exception
      logInvalidHttpRequest();
      sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /uri");
    }

    if (tokenizedFirstLine.hasMoreTokens()) {
      uri = tokenizedFirstLine.nextToken();
    } else {
      // throws Exception
      logInvalidHttpRequest();
      sendError(HTTP_BADREQUEST, "BAD REQUEST: Missing URI. Usage: GET /uri");
    }
    
    if (tokenizedFirstLine.hasMoreTokens()) {
      String v = tokenizedFirstLine.nextToken();
      if (v.startsWith("HTTP")) {
        version = v;
      }
    }

    // Decode parameters from the URI
    paras = new Properties();
    parameters = new HashMap<>();
    int qmi = uri.indexOf('?');
    if (qmi >= 0) {
      decodeParas(uri.substring(qmi + 1), paras);
      decodeParameters(uri.substring(qmi +1), parameters);
      uri = decode(uri.substring(0, qmi));
    } else {
      uri = decode(uri);
    }

    if (!suppressLogging && logger.isDebugEnabled()) {
      logger.debug("Method: " + methodEnum + ", URI: " + uri);
    }

    debugProperties("parameter", paras, false);
    
    try {
      this.socket.setSoTimeout(previousTimeout);
    } catch (SocketException e) {
      if (!suppressLogging) {
        logger.trace("Failed to reset SocketTimeout", e);
      }
    }

  }

  
  private static final int[] FIRSTLINE_HASHES = new int[Method.values().length];
  static {
    byte space = " ".getBytes()[0];
    for (int i = 0; i<Method.values().length; i++) {
      FIRSTLINE_HASHES[i] = 127;
      String name = Method.values()[i].name();
      byte[] nameBytes = name.getBytes();
      for (int j = 0; j<4; j++) {
        if (nameBytes.length > j) {
          FIRSTLINE_HASHES[i] += nameBytes[j];
        } else {
          FIRSTLINE_HASHES[i] += space;
        }
        FIRSTLINE_HASHES[i] *= 31;
      }
    }
  }

  private void validateStartOfRequestLine(byte[] firstPartOfLine) throws SocketNotAvailableException, InterruptedException {
    int hash = 127;
    for (byte b : firstPartOfLine) {
      hash += b;
      hash *= 31;
    }
    for (int i = 0; i<FIRSTLINE_HASHES.length; i++) {
      if (hash == FIRSTLINE_HASHES[i]) {
        return; //gut genug als validierung. man könnte sich natürlich immer noch mehr mühe geben
      }
    }
    logInvalidHttpRequest();
    sendError(HTTP_BADREQUEST, "BAD REQUEST: Syntax error. Usage: GET /uri");
  }


  /**
   * Decodes parameters in percent-encoded URI-format ( e.g. "name=Jack%20Daniels&pass=Single%20Malt" ) and adds them to
   * given Properties.
   */
  private void decodeParas(String paras, Properties p) throws InterruptedException {
    if (paras == null)
      return;

    StringTokenizer st = new StringTokenizer(paras, "&");
    while (st.hasMoreTokens()) {
      String e = st.nextToken();
      int sep = e.indexOf('=');
      if (sep >= 0) {
        p.put(decode(e.substring(0, sep)).trim(), decode(e.substring(sep + 1)));
      } else {
        p.put(decode(e).trim(), "");
      }
    }
  }


  /**
   * Decodes parameters in percent-encoded URI-format ( e.g. "default%20workspace" ) and adds them to given HashMap.
   */
  private void decodeParameters(String paras, HashMap<String, List<String>> p) {
    if(paras == null) {
      return;
    }
    
    StringTokenizer st = new StringTokenizer(paras, "&");
    while (st.hasMoreTokens()) {
      String e = st.nextToken();
      int sep = e.indexOf('=');
      String key = sep >= 0 ? decode(e.substring(0, sep)).trim() : decode(e).trim();
      String value = sep >= 0 ? decode(e.substring(sep + 1)) : "";
      p.putIfAbsent(key, new ArrayList<String>());
      p.get(key).add(value);
    }
  }
  
  private static String decode(String string) {
    if( string == null ) {
      return null;
    } else {
      try {
        string = URLDecoder.decode(string, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        //UTF-8 sollte immer passen
        logger.warn("UnsupportedEncodingException in URLDecoder.decode(string, \"UTF-8\")", e);
      }
      return string;
    }
  }

  /**
   * Decodes the percent encoding scheme. <br/>
   * For example: "an+example%20string" -> "an example string"
   * @deprecated
   */
  private String decodePercentX(String str) throws InterruptedException {
    try {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < str.length(); i++) {
        char c = str.charAt(i);
        switch (c) {
          case '+' :
            sb.append(' ');
            break;
          case '%' :
            sb.append((char) Integer.parseInt(str.substring(i + 1, i + 3), 16));
            i += 2;
            break;
          default :
            sb.append(c);
            break;
        }
      }
      return sb.toString();
    } catch (Exception e) {
      try {
        logInvalidHttpRequest();
        sendError(HTTP_BADREQUEST, "BAD REQUEST: Bad percent-encoding.");
      } catch (SocketNotAvailableException e1) {
        if (!suppressLogging) {
          logger.error("socket was unexpectedly not available when trying to send errormessage to client", e1);
        }
      }
      return null; // wird nicht ausgeführt
    }
  }


  private void logInvalidHttpRequest() {
    if (!suppressLogging && logger.isInfoEnabled() && socket.getInetAddress() != null) {
      logger.info("Got invalid request from " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort() + " @ "
          + socket.getLocalAddress().getHostAddress() + ":" + socket.getLocalPort());
    }
  }


  public void sendResponse(String response) throws SocketNotAvailableException {
    try {
      byte[] msgBytes = response.getBytes(getCharSet());
      sendResponse(HTTP_OK, MIME_PLAINTEXT, null, new ByteArrayInputStream(msgBytes), Long.valueOf(msgBytes.length));
    } catch (UnsupportedEncodingException e) {
      handleUnsupportedEncoding();
    }
  }
    
  public void sendHtmlResponse(String response) throws SocketNotAvailableException {
    try {
      byte[] msgBytes = response.getBytes(getCharSet());
      sendResponse(HTTP_OK, MIME_HTML, null, new ByteArrayInputStream(msgBytes),  Long.valueOf(msgBytes.length));
    } catch (UnsupportedEncodingException e) {
      handleUnsupportedEncoding();
    }
  }
  
  
  public void sendBinaryResponse(InputStream is, String name, Long size) throws SocketNotAvailableException {
    Properties props = new Properties();
    if (name != null) {
      props.put("content-disposition", "attachment; filename=\"" + name+ "\"");
    }
    sendResponse(HTTPTriggerConnection.HTTP_OK, HTTPTriggerConnection.MIME_DEFAULT_BINARY, props, is, size);
  }



  void handleUnsupportedEncoding() {
    //support wird schon bei setCharset() überprüft. eigtl sollte das hier nicht passieren.
    throw new RuntimeException("charset " + charSet + " is unsupported.");
  }


  public void sendError(String s) throws SocketNotAvailableException {
    StringBuffer sb = new StringBuffer();
    sb.append("<html><body><h3>" + HTTP_INTERNALERROR + "</h3>");
    sb.append(s);
    sb.append("</body></html>");
    ByteArrayInputStream bais = null;
    long length;
    try {
      byte[] msgBytes = sb.toString().getBytes(getCharSet());
      length = msgBytes.length;
      bais = new ByteArrayInputStream(msgBytes);
    } catch (UnsupportedEncodingException e1) {
      handleUnsupportedEncoding();
      length = -1; //hier kommt man nicht hin
    }
    sendResponse(HTTP_INTERNALERROR, MIME_HTML, null, bais, length);
  }

  public void sendError(XynaException[] es, boolean logError) throws SocketNotAvailableException {
    StringBuffer sb = new StringBuffer();
    int cnt = 0;
    for (XynaException e : es) {
      if (!suppressLogging && logError) {
        logger.error(null, e);
      }
      if (cnt > 0) {
        sb.append("\n");
      }
      sb.append(e.getMessage());
      cnt++;
    }
    sendError(sb.toString());
  }

  public void sendError(XynaException[] es) throws SocketNotAvailableException {
    sendError(es, true);
  }

  @Deprecated
  public void sendError(String status, String msg) throws InterruptedException, SocketNotAvailableException {
    msg = "<html><body><h3>" + msg + "</h3></body></html>";
    try {
      byte[] msgBytes = msg.getBytes(getCharSet());
      sendResponse(status, MIME_HTML, null, new ByteArrayInputStream(msgBytes), new Long(msgBytes.length));
    } catch (UnsupportedEncodingException e) {
      handleUnsupportedEncoding();
    }
    throw new InterruptedException();
  }


  public String getCharSet() {
    return charSet;
  }


  public void setCharSet(String charsetName) throws XynaException {
    if (Charset.isSupported(charsetName)) {
      charSet = charsetName;
    } else {
      throw new XynaException("CharSet " + charsetName + " not supported");
    }
  }


  private static SimpleDateFormat getDateFormat() {
    SimpleDateFormat sdf = new java.text.SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.ENGLISH);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf;
  }


  /**
   * @deprecated use {@link #getAuthenticationInformationOrSend401Response(String, String)}
   */
  public DigestAuthentificationInformation getAuthentificationInformationOrSend401Response(String realm, String mime)
                  throws XynaException, InterruptedException {
    return getAuthenticationInformationOrSend401Response(realm, mime);
  }


  /**
   * Checks whether the received header contains authorization information. If not, a '401 Unauthorized' response will
   * be sent. If auth info is present, an object is returned that can later be used to evaluate a given password string.
   * @see DigestAuthentificationInformation.reevaluate(String)
   * @param realm this is passed to the client if a 401 response is sent
   * @param mime used for an eventual 401 response
   * @throws InterruptedException if either a 401 Unauthorized response has been sent due to no auth info or a 400 Bad
   *           Request response has been sent due to missing fields in the auth string.
   */
  public DigestAuthentificationInformation getAuthenticationInformationOrSend401Response(String realm, String mime)
                  throws XynaException, InterruptedException {
    return getAuthenticationInformationOrSend401Response(realm, mime, null);
  }


  /**
   * @deprecated use {@link #getAuthenticationInformationOrSend401Response(String, String, String)}
   */
  public DigestAuthentificationInformation getAuthentificationInformationOrSend401Response(String realm, String mime,
                                                                                           String password)
                  throws InterruptedException, XynaException {
    return getAuthenticationInformationOrSend401Response(realm, mime, password);
  }

  /**
   * Checks whether the received header contains authorization information. If valid auth information is present and the
   * provided password is not null, the resulting auth information is checked.<br><br>
   *
   * Extract from RFC 2617:<br><br>
   *
   * ...<br><br>
   * The client is expected to retry the request, passing an Authorization
   *   header line, which is defined according to the framework above,
   *   utilized as follows.
   *
   *  <li> credentials      = "Digest" digest-response
   *  <li> digest-response  = 1#( username | realm | nonce | digest-uri | response | [ algorithm ] |
   *                           [cnonce] | [opaque] | [message-qop] |
   *                           [nonce-count]  | [auth-param] )
   *
   *  <li> username         = "username" "=" username-value
   *  <li> username-value   = quoted-string
   *  <li> digest-uri       = "uri" "=" digest-uri-value
   *  <li> digest-uri-value = request-uri   ; As specified by HTTP/1.1
   *  <li> message-qop      = "qop" "=" qop-value
   *  <li> cnonce           = "cnonce" "=" cnonce-value
   *  <li> cnonce-value     = nonce-value
   *  <li> nonce-count      = "nc" "=" nc-value
   *  <li> nc-value         = 8LHEX
   *  <li> response         = "response" "=" request-digest
   *  <li> request-digest   = <"> 32LHEX <">
   *  <li> LHEX             =  "0" | "1" | "2" | "3" |
   *                           "4" | "5" | "6" | "7" |
   *                           "8" | "9" | "a" | "b" |
   *                           "c" | "d" | "e" | "f"
   *
   *  <br>
   *
   *   The values of the opaque and algorithm fields must be those supplied
   *   in the WWW-Authenticate response header for the entity being
   *   requested.<br><br>
   *
   * ...<br><br>
   *
   * @param realm this is passed to the client if a 401 response is sent
   * @param mime used for an eventual 401 response
   * @param password
   * @return
   * @throws InterruptedException if either a 401 Unauthorized response has been sent due to no auth info or a 400 Bad
   *           Request response has been sent due to missing fields in the auth string.
   * @throws XynaException
   */
  public DigestAuthentificationInformation getAuthenticationInformationOrSend401Response(String realm, String mime,
                                                                                           String password)
                  throws InterruptedException, XynaException {
    if (getHeader() == null) {
      throw new XynaException("Header must be present and may not be null when sending 401 response");
    }

    String completeAuthString = getHeader().getProperty(PROP_KEY_AUTHORIZATION);
    if (completeAuthString == null || completeAuthString.equals("")) {
      // algorithm configurable?
      sendResponse(HTTP_UNAUTHORIZED, mime,
                   DigestAuthentificationUtilities.createDigestAuthenticationHeader(MD5_ALGORITHM, realm, getSocket()
                                   .getInetAddress()), null);

      String msg = new StringBuilder("Sent '" + HTTP_UNAUTHORIZED + "' to ").append(socket.getInetAddress()).toString();
      if (!suppressLogging && logger.isTraceEnabled()) {
        logger.trace(msg);
      }
      throw new InterruptedException(msg);
    }

    boolean digest = false;
    if (completeAuthString.startsWith("Digest ")) {
      completeAuthString = completeAuthString.substring("Digest ".length());
      digest = true;
    }

    if (!digest) {
      sendError(HTTP_BADREQUEST, "Only 'Digest' authentication is supported.");
    }

    DigestAuthentificationInformation authInfo = new DigestAuthentificationInformation(completeAuthString, getMethod());
    if (password != null) {
      try {
        authInfo.reevaluate(password);
      } catch (IllegalArgumentException e) {
        sendError(HTTP_BADREQUEST, e.getMessage());
        throw new InterruptedException("Sent " + HTTP_BADREQUEST + ": " + e.getMessage());
      }
    }
    return authInfo;

  }


  /**
   * Schreibt Header und Daten in den OutputStream, schließt das Socket aber nicht.
   * Daher kann sendResponse mehrfach aufgerufen werden.
   * Schließen des Sockets wird über {@link #close()} vorgenommen.
   */
  public void sendResponse(String status, String mime, Properties responseHeader, InputStream data)
                  throws SocketNotAvailableException {

    sendResponse(status, mime, responseHeader, data, null);
  }
  
  
  public void sendResponse(String status, String mime, Properties responseHeader, InputStream data, Long dataSize)
                  throws SocketNotAvailableException {
    if (socket == null) {
      throw new SocketNotAvailableException("Socket is not available.");
    }  
    try {
      writeHeader(status, mime, data, dataSize, responseHeader);
      if (data != null) {
        writeData(data);
      }
      outputStream.flush();
    } catch (IOException ioe) {
      // FIXME exception handling
      if (!suppressLogging) {
        logger.debug("error sending response", ioe);
      }
    } finally {
      if (data != null) {
        try {
          data.close();
        } catch (IOException e) {
          if (!suppressLogging) {
            logger.debug("error closing data", e);
          }
        }
      }
    }
  }
  

  /**
   * @param status
   * @param mime
   * @param data
   * @throws IOException 
   */
  private void writeHeader(String status, String mime, InputStream data, Long dataSize, Properties responseHeader) throws IOException {
      PrintWriter pw = new PrintWriter(outputStream);
      pw.print(version + " " + status + CRLF);

      if (mime != null && mime.length() > 0) {
        pw.print(PROP_KEY_CONTENT_TYPE + ": " + mime + CRLF);
        if (dataSize != null) {
          pw.print(PROP_KEY_CONTENT_LENGTH + ": " + dataSize + CRLF);
        }
      }
      if (!suppressLogging && logger.isDebugEnabled()) {
        logger.debug("sending (status=" + status + ") " + (data != null ? data.available() : 0) + " bytes");
      }

      if (responseHeader == null || responseHeader.getProperty("Date") == null) {
        pw.print("Date: " + getDateFormat().format(new Date()) + CRLF);
      }

      if (responseHeader != null) {
        Enumeration<?> e = responseHeader.keys();
        while (e.hasMoreElements()) {
          String key = (String) e.nextElement();
          if (HEADERKEYS_SEPARATE.contains(key)) {
            continue;
          }
          String value = responseHeader.getProperty(key);
          if (value == null) {
            // workaround for multiple response header entries with the same name
            Object complexValue = responseHeader.get(key);
            if (complexValue instanceof List) {
              for (Object singleValue : (List)complexValue) {
                pw.print(key+": "+String.valueOf(singleValue)+CRLF);
              }
            }
          } else {
            pw.print(key+": "+value+CRLF);
          }
        }
      }

      pw.print(CRLF);
      pw.flush(); //ansonsten gibt es "java.net.SocketException Unexpected end of file from server." 
      // beim Empfänger in java.net.HttpURLConnection.getResponseCode()
  }

  /**
   * @param data
   * @throws IOException 
   */
  private void writeData(InputStream data) throws IOException {
        if (socketChannel != null) {
          // channel mode
          if (!suppressLogging && logger.isTraceEnabled()) {
            logger.trace("Transferring in channel mode...");
          }
          ReadableByteChannel inputChannel = Channels.newChannel(data);
          final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
          while (inputChannel.read(buffer) != -1) {
            buffer.flip();
            socketChannel.write(buffer);
            buffer.compact();
          }
          buffer.flip();
          while (buffer.hasRemaining()) {
            socketChannel.write(buffer);
          }
        } else {
          int socketSendBufferSize = socket.getSendBufferSize();
          byte[] buff = new byte[socketSendBufferSize];
          while (true) {
            int read = data.read(buff, 0, socketSendBufferSize);
            if (read <= 0) {
              break;
            }
            outputStream.write(buff, 0, read);
          }
        }
  }


  public Socket getSocket() {
    return socket;
  }


  public String getUri() {
    return uri;
  }


  public String getMethod() {
    return methodEnum.name();
  }
  
  public Method getMethodEnum() {
    return methodEnum;
  }

  public Properties getHeader() {
    return header;
  }


  public Properties getParas() {
    return paras;
  }

  public String getPayload() {
    return payload;
  }

  public String getTriggerIp() {
    return triggerIp;
  }

  public String getTriggerHostname() {
    return triggerHostname;
  }


  public void closeSocket() {
    //see http://java.sun.com/j2se/1.5.0/docs/guide/net/articles/connection_release.html
    if (socket == null || socket.isClosed() ) {
      return;
    }
    try {
      shutdownSocket();
      socket.close(); //schließt auch socketChannel
    } catch (IOException e) {
      if (!suppressLogging) {
        logger.error("error closing socket", e);
      }
    } finally {
      if (!socket.isClosed()) { //kann das passieren?
        try {
          socket.close();
        } catch(Exception e) {
          if (!suppressLogging) {
            logger.error("error closing socket", e);
          }
        }
      }
    }
    if (!suppressLogging) {
      logger.debug("socket closed");
    }
  }

  
  /**
   * 
   */
  private void shutdownSocket() {
    if (socket instanceof SSLSocket) {
      //vgl http://stackoverflow.com/questions/6424998/properly-closing-sslsocket, o.ä.
      return;
    }
    if (!socket.isOutputShutdown()) {
      try {
        socket.shutdownOutput();
      } catch (IOException e) {
        if (!suppressLogging) {
          logger.error("error shutdownOutput", e);
        }
      }
    }
    if (!socket.isInputShutdown()) {
      try {
        socket.shutdownInput();
      } catch (Exception e1) {
        if (!suppressLogging) {
          logger.debug("Unexpected error while shutdownInput.", e1);
        }
        try {
          //Trotzdem versuchen zu leeren
          if (!suppressLogging && logger.isTraceEnabled()) {
            long t = System.currentTimeMillis();
            int available = lineBufferedInputStream.available();
            lineBufferedInputStream.skip(available);
            logger.trace("reading all available data (" + available + " bytes) from stream before closing socket took "
                + (System.currentTimeMillis() - t) + "ms.");
          } else {
            lineBufferedInputStream.skip(lineBufferedInputStream.available());
          }
        } catch (IOException e) {
          if (!suppressLogging) {
            logger.error("error shutdownOutput", e);
          }
        }
      }
    }
    //unter starker last kann es trotzdem noch vorkommen, dass daten im puffer liegen
  }

  private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    ois.defaultReadObject();
    if( method != null ) {
      try {
        methodEnum = Method.valueOf(method.toUpperCase());
      } catch( IllegalArgumentException e ) {
      }
    }
  }

  @Override
  public synchronized void close() {
    if (isClosed()) {
      return;
    }
    super.close();
    closeSocket();
  }

}
