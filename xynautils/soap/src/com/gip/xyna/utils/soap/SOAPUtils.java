/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.soap;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.soap.SOAPException;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.soap.Codes;
import com.gip.xyna.utils.soap.serializer.SoapEnvelopeSerializer;
import com.gip.xyna.utils.xml.XMLDocument;


/**
 * Erzeugen und Versenden einer SOAP-Nachricht.
 * 
 * Kapselt alle SOAP-spezifschen Dinge, die beim Versenden des HTTP Requests zu
 * beachten sind.
 * 
 * <p>
 * Features:
 * <p>Kodierung beliebig über setEncoding(...), zwei vordefinierte Konstanten UTF_8 und ISO_8859_1
 * <p>Protokoll http oder https möglich
 * <p>SOAP-Version 1.1 oder 1.2<br>
 *
 * In den SOAPUtils kann man diverse hauptsächlich die HTTPConnection betreffende
 * Einstellungen vornehmen (SOAPConnector bündelt das). <br>
 * Verwendung in Verknüpfung mit SoapRequest und SoapResponse. Man erstellt zuerst
 * einen SoapRequest, und ruft damit dann das soapUtils.sendSOAPMessage() auf. Man 
 * bekommt eine SoapResponse zurück, auf die man diverse Operationen anwenden kann:
 * <p>Fehler-erkennung
 * <p>XML Parsing in entsprechende Objekte
 *
 * Aufruf-Beispiel:<p>
 * <code>
 * SOAPUtils soapUtils = new SOAPUtils("protocol://hostname:port/service" );<br>
 * soapUtils.setEncoding(Encoding.ISO_8859_1); //default ist UTF_8<br>
 * soapUtils.setUserAgent( "SoapTester" );<br>
 * SoapRequest soapReq = new SoapRequest("<.... . />");<br>
 * soapReq.setSoapAction("dosomething");<br>
 * SoapResponse soapResp = soapUtils.sendSOAPMessage(soapReq);<br>
 * if (soapResp.hasError()) {<br>
 *   throw soapResp.getError();<br>
 * } else {<br>
 *   String xmlPayload = soapResp.getXMLObject().getBody().getXMLPayload();<br>
 * }<br>
 * </code><p>
 * 
 * Für die Benutzung von HTTPS muss das Server-Zertifikat authentifiziert sein.
 * Falls es dies nicht automatisch ist, muss ein keyStore eingerichtet werden
 * (beispielsweise über keytool  -import -v -alias gip -file gip.crt ) und im aufrufenden Programm muss
 * vorher System.setProperty("javax.net.ssl.trustStore", "/afs/gip.local/home/USER/.keystore");
 * aufgerufen werden.
 *
 * verwendet log4j.
 *
 * TODO SOAP 1.2 nicht getestet
 * TODO SOAP-Header nur teilweise unterstützt
 */
public class SOAPUtils extends SOAPConnector {

  private static Logger logger = Logger.getLogger("xyna.utils.soap.soaputils");

  private Encoding encoding = Encoding.UTF_8;
  private String soapAction;

  private SOAPVersion version = SOAPVersion.SOAP11;
  /**
   *
   */
  public SOAPUtils() {
    super();
  }


  /**
   * @param url komplette URL des SoapServers "protocol://hostname:port/service"
   * @throws MalformedURLException
   */
  public SOAPUtils(String url) throws MalformedURLException {
    super(url);
  }


  /**
   * @param protocol
   * @param hostname
   * @param port
   * @param service
   */
  public SOAPUtils(String protocol, String hostname, int port,
                   String service) {
    super(protocol, hostname, port, service);
  }


  /**
   * Erzeugen der SOAP-Nachricht aus einem XML-String, indem SoapEnvelope 
   * drumrum gestülpt wird.<br>
   * macht das gleiche wie <code>new SoapRequest(message).getAsXML();</code>
   * @param message valides xml
   * @return
   * @deprecated use class SoapRequest
   */
  public String generateSOAPMessage(String message) {
    try {
      return new SoapRequest(message).getAsXML();
    } catch (Exception e) {
      //XML Parsing Fehler fangen und nicht werfen, weil sich sonst signatur
      //ändert und abwärtskompatibilität verloren geht. statt dessen null zurück
      //geben
      return null;
    }
  }


  /**
   * Erzeugen der SOAP-Nachricht aus einem XML-Object
   *
   * @param message
   * @return
   * @deprecated use class SoapRequest
   */
  public String generateSOAPMessage(XMLDocument message) {    
    XMLDocument xml = new XMLDocument(0);
    xml.addDeclaration("1.0", encoding.getEncoding());
    xml.setNamespace("soapenv");
    xml.addStartTag("Envelope", version.getNamespace());
    {
      xml.addStartTag("Body");
      {
        // TODO: remove XML declartion from message
        xml.append(message);
      }
      xml.addEndTag("Body");
    }
    xml.addEndTag("Envelope");
    return xml.toString();
  }


  private String trim(String message) {
    message = message.trim();
    if (message.startsWith("<?xml")) {
      message = message.replaceFirst("<\\?xml.*?\\?>", "");
    }
    return message;
  }


  /**
   * Versenden der SOAP-Nachricht mit leerer soapAction
   *
   * @param message
   * @return
   * @throws IOException
   * @throws SOAPException
   * @deprecated use 
   * @see sendSOAPMessage(SoapRequest)
   */
  public String sendSOAPMessage(String message) throws IOException,
                                                       SOAPException {
    return sendSOAPMessage(message, "");
  }


  /**
   * Versenden der SOAP-Nachricht unter Angabe der SOAPAction. Wirft SOAPException,
   * falls die Response als fehlerhaft erkannt wird.
   *
   * @param messageWithSoapEnv falls nur messagepayload bekannt ist, benutze 
   *    #sendSOAPMessage(SoapRequest)
   * @param soapAction
   * @return
   * @see sendSOAPMessage(SoapRequest)
   * @throws IOException
   * @throws SOAPException
   */
  public String sendSOAPMessage(String messageWithSoapEnv,
                                String soapAction) throws IOException,
                                                          SOAPException {
    SoapRequest req = new SoapRequest();
    SoapResponse resp = null;
    try {
      req.setMessageWithSoapEnv(messageWithSoapEnv);
      setSoapAction(soapAction);
      resp = sendSOAPMessage(req);
    } catch (XynaException xe) {
      throw new SOAPException(xe.getMessage());
    } catch (Exception e) {
      throw new SOAPException(e);
    }
    if (resp.hasError()) {
      throw new SOAPException(resp.getError());
    }
    return resp.getXMLString();
  }


  /**
   * Schneidet den SoapTeil der Message ab, nur der reine Body wird zurückgegeben
   *
   * @param message
   * @return
   */
  public static String stripSOAPMessage(String message) { 
    //TODO eigtl nicht mehr benötigt, da die Funktion in SoapResponse integriert ist
    String result = null;
    try {
      result =
          new SoapEnvelopeSerializer(message).toBean().getBody().getXMLPayload();
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
    }
    return result;
  }


  /**
   * Benutzung von SOAP 1.2
   * @deprecated
   */
  public void setUseSOAP12() {
    version = SOAPVersion.SOAP12;
  }


  /**
   * Benutzung von SOAP 1.1
   * @deprecated
   */
  public void setUseSOAP11() {
    version = SOAPVersion.SOAP11;
  }


  // //////////////////////////////////////////////////////////////////////////
  //
  // Debugging-Ausgaben
  //
  // //////////////////////////////////////////////////////////////////////////

  /**
   * @param httpConn
   */
  private void printRequestProperties(HttpURLConnection httpConn) {
    String rp = "RequestProperties:";
    Map<String, List<String>> requestProperties =
      httpConn.getRequestProperties();
    Iterator<String> iter = requestProperties.keySet().iterator();
    while (iter.hasNext()) {
      Object key = iter.next();
      Object value = requestProperties.get(key);
      rp += "\n" + key + " -> " + value;
    }
    log(rp);
  }


  /**
   * @param object
   */
  private void log(Object object) {
    logger.debug(object);
  }


  /**
   * @return Returns the encoding.
   */
  public Encoding getEncoding() {
    return encoding;
  }


  /**
   * @param encoding The encoding to set.
   */
  public void setEncoding(Encoding encoding) {
    this.encoding = encoding;
  }


  /**
   * @throws SOAPException
   * @deprecated use sendSOAPMessage instead.
   */
  public static String sendMessage(String protocol, String targetHost,
                                   int targetPort, String endPoint,
                                   String soapAction, String message,
                                   HashMap additionalHeaders) throws MalformedURLException,  IOException,
                                                                     SOAPException {
    SOAPUtils soapUtils = new SOAPUtils();
    soapUtils.setProtocol(protocol);
    soapUtils.setHostname(targetHost);
    soapUtils.setPort(targetPort);
    soapUtils.setService(endPoint);
    String soap = soapUtils.generateSOAPMessage(message);
    return soapUtils.sendSOAPMessage(soap, soapAction);
  }


  /**
   * @throws SOAPException
   * @deprecated use sendSOAPMessage instead.
   */
  public static String sendMessage(String protocol, String targetHost,
                                   int targetPort, String endPoint,
                                   String soapAction,
                                   String message) throws MalformedURLException,
                                                          IOException,
                                                          SOAPException {
    return sendMessage(protocol, targetHost, targetPort, endPoint, soapAction,
                       message, null);
  }

  /**
   * öffnet httpconnection, schliesst sie aber nicht
   * setzt mime content-type auf denjenigen der eingestellten SOAPVersion, falls
   * nicht schon andersweitig gesetzt.
   * setzt soapaction nicht, falls soapversion = 1.2
   * @param req
   * @return
   * @throws IOException
   * @throws XynaException
   */
  public SoapResponse sendSOAPMessage(SoapRequest req) throws IOException,
                                                              XynaException{
    String message = null;
    try {
      message = req.getAsXML();
    } catch (Exception e) {
      throw new XynaException(Codes.CODE_REQUEST_INVALID_XML).initCause(e);
    }

    open();
    logger.debug("sending to " + getHttpCon().getURL().toString() + ": " + message);

    // Message in ByteArray schreiben, um die Anzahl der Bytes zählen zu können
    ByteArrayOutputStream messageBaos = new ByteArrayOutputStream();
    OutputStreamWriter osw =
      new OutputStreamWriter(messageBaos, encoding.getEncoding());
    osw.write(message);
    osw.close();

    if (getRequestProperty(PROPERTYKEY_CONTENT_TYPE) == null) {
      setRequestProperty(PROPERTYKEY_CONTENT_TYPE, version.getMimeType());
    }
    setRequestProperty(PROPERTYKEY_CONTENT_LENGTH, "" + messageBaos.size());
    if (!version.equals(SOAPVersion.SOAP12)) {
      setRequestProperty(PROPERTYKEY_SOAP_ACTION, "\"" + getSoapAction() + "\""); // ohne Anfuehrungszeichen um die soapAction
      // wird bei gleicher Signatur die Methode
      // nicht erkannt
    }
    if (logger.isDebugEnabled()) {
      printRequestProperties(getHttpCon());
    }

    // Message-Body schreiben
    messageBaos.writeTo(getOutputStream());

    SoapResponse r = new SoapResponse(getHttpCon(), encoding);
    return r;
  }

  public String getSoapAction() {
    return soapAction;
  }

  public void setSoapAction(String soapAction) {
    this.soapAction = soapAction;
  }

  /**
   * schickt eine Nachricht an einen Webservice
   *
   * @param args
   */
  public static void main(String[] args) {
//    args = new String[]{"http://gipsun215:7777/CapacityManager/CapacityManager", "http://www.gip.com/xyna/1.5/wsdl/factorymanager/capacitymanager/service/1.1/modifyCapacity", "example.xml"};
    if (args.length != 3) {
      System.out.println("Usage: SOAPUtils <url> <operation> <Request XML File>");
      System.out.println("Example: SOAPUtils http://gipsun185:7777/DispatcherManager/DispatcherManager http://www.gip.com/xyna/1.5.10/dispatchermanager/service/1.0/reload myRequest.xml");
      return;
    }
    try {
      String url = args[0];
      String operation = args[1];
      String filename = args[2];
      System.out.println("Sending content of " + filename + " to " + url +
                         " as operation " + operation + ":");
      SOAPUtils soapUtils = new SOAPUtils();
      soapUtils.setURL(url);
      System.out.println("Reading file ...");
      FileInputStream filestream = new FileInputStream(filename);
      byte[] b = new byte[filestream.available()];
      filestream.read(b);
      filestream.close();
      String xmlMessage = new String(b, "UTF-8");
      System.out.println("Content of file is:");
      System.out.println(xmlMessage);
      String soapMessage = soapUtils.generateSOAPMessage(xmlMessage);
      String response = soapUtils.sendSOAPMessage(soapMessage, operation);
      System.out.println("Operation " + operation +
                         " has been invoked successfully.");
      System.out.println("Response: ");
      System.out.println(response);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
