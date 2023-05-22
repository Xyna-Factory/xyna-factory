/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.blackeditionconnection;



import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;



public class BlackEditionConnectionClient {

  private static final Logger logger = CentralFactoryLogging.getLogger(BlackEditionConnectionClient.class);

  private InetAddress address;
  private int port;


  private Socket socket;
  private boolean connected;
  private String sendOrder;
  private Object parameter;

  private boolean useEncryption = false;
  private String keyStorePath;
  private String keyStoreType;
  private String keyStorePassword;

  private ModifiedPushbackInputStream buffer;
  private int isEvaluating = 0;

  private ArrayList<Integer> position = new ArrayList<Integer>(); // int-array (verschachtelungstiefe = index)
  private Object[] list;
  private boolean connectHTTP; // falls zb ein filedownload ueber http geschehen soll, braucht nicht ein separates socket
                               // geoeffnet werden
  private String _endpointHTTP;

  private String _name;


  private boolean responseReceived = false;
  private boolean receivedError = false;
  private boolean finished = false;


  public void setEndpointTCP(InetAddress address, int port) throws XynaException {

    this.address = address;
    this.port = port;

    if (!connectHTTP) {

      try {
        if (useEncryption) {
          try {
            SocketFactory sFactory = getSocketFactory(keyStorePath, keyStoreType, keyStorePassword);
            this.socket = sFactory.createSocket(address, port); //, InetAddress.getByName("10.0.0.127"), 2667);
            ((SSLSocket) this.socket).setUseClientMode(true);
            logger.debug("local port " + socket.getLocalPort());
            String[] ciphersuites = ((SSLSocket) this.socket).getSupportedCipherSuites();
            ((SSLSocket) this.socket).setEnabledCipherSuites(ciphersuites);
//            ((SSLSocket) this.socket).getOutputStream()
//                            .write(new String("M3MAsd").getBytes(Constants.DEFAULT_ENCODING));
//            while (socket.getInputStream().available() < 1) {
//              try {
//                Thread.sleep(100);
//              } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//              }
//            }
//            int response = socket.getInputStream().read();
//            System.out.println("got " + response);
          } catch (KeyManagementException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          } catch (KeyStoreException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          } catch (NoSuchAlgorithmException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          } catch (UnrecoverableKeyException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          } catch (CertificateException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          } catch (NoSuchProviderException e) {
            throw new XynaException("Error creating encrypted socket: " + e.getMessage(), e);
          }
        } else {
          this.socket = new Socket(address, port);
        }
      } catch (UnknownHostException e) {
        throw new XynaException("Error creating socket: " + e.getMessage(), e);
      } catch (IOException e) {
        throw new XynaException("Error creating socket: " + e.getMessage(), e);
      }

      try {
        buffer = new ModifiedPushbackInputStream(socket.getInputStream());
      } catch (IOException e) {
        throw new XynaException("Error creating socket: " + e.getMessage(), e);
      }

      try {
        onSocketConnect();
      } catch (UnsupportedEncodingException e1) {
        logger.error(null, e1);
      } catch (IOException e1) {
        logger.error("Failed ", e1);
      }

    }

    Runnable r = new Runnable() {

      public void run() {

        logger.debug("Waiting for incoming data...");
        try {
          while (socket.getInputStream().available() <= 0) {
            Thread.sleep(100);
          }
        } catch (IOException e) {
          logger.error(null, e);
        } catch (InterruptedException e) {
          logger.error(null, e);
        }

        logger.debug("Receiving data...");

        try {
          onReceiveReply();
        } catch (IOException e) {
          logger.error(null, e);
        } catch (XynaException e) {
          logger.error(null, e);
        } catch (Throwable t) {
          try {
          Department.handleThrowable(t);
          } finally {
          logger.error(null, t);
          }
        }

        logger.debug("Receiver thread finished");

      }

    };

    Thread t = new Thread(r);
    t.setName("Receiver waiting thread");
    t.start();

  }


  public InetAddress getAddress() {
    return address;
  }

  public int getPort() {
    return port;
  }


  public void setEndpointHTTP(String s) {
    _endpointHTTP = s;
  }


  public String getEndpointHTTP() {
    return _endpointHTTP;
  }


  public BlackEditionConnectionClient() {
    this("default", false);
  }


  public BlackEditionConnectionClient(String name, boolean connectHTTP) {
    this(name, connectHTTP, false, null, null, null);
  }


  public BlackEditionConnectionClient(String name, boolean connectHTTP, boolean useEncryption, String keyStorePath,
                                      String keyStoreType, String keyStorePassword) {
    this.connectHTTP = connectHTTP;
    _name = name;
    logger.debug("Creating BlackEditionConnection for " + _name);
    this.useEncryption = useEncryption;
    this.keyStorePassword = keyStorePassword;
    this.keyStoreType = keyStoreType;
    this.keyStorePath = keyStorePath;
  }


  private void addList(int length) {

    // falls eine listendeklaration empfangen wurde
    if (list == null) {
      list = new Object[length];
      position.add(0);
    } else {
      Object[] tl = list;
      for (int i = 0; i < position.size() - 1; i++) {
        tl = (Object[]) tl[position.get(i)];
      }
      if (length == 0) {
        addElement(new Object()); // nicht ins neue array reingehen
      } else {
        tl[position.get(position.size() - 1)] = new Object[length];
        position.add(0);
      }
    }
  }


  private void addElement(Object o) {

    // falls ein normaler parameter empfangen wurde

    if (list == null) {
      return;
    }
    int i;
    Object[] tl = list;
    for (i = 0; i < position.size() - 1; i++) {
      int index = position.get(i);
      Object tmp = tl[index];
      if (tmp instanceof byte[]) {
        tl = castBaseTypeByteArrayToObjectArray((byte[]) tmp);
      } else {
        tl = (Object[]) tmp;
      }
    }

    // objekt in aktueller liste einfuegen
    int subIndex = position.size() - 1;
    int index1 = position.get(subIndex);
    if (o instanceof byte[])
      tl[index1] = castBaseTypeByteArrayToObjectArray((byte[]) o);
    else tl[index1] = o;

    // this.dispatchEvent(new BlackEditionConnectionEvent(BlackEditionConnectionEvent.BEC_DEBUG, "got " + o));
    Integer oldValue = position.remove(position.size() - 1);
    position.add(oldValue + 1); // position hochzaehlen
    while (position.get(position.size() - 1) > tl.length - 1) {
      // array schliessen, weil voll => position unten drunter eins weiter zaehlen
      position.remove(position.size() - 1);
      if (position.size() > 0) {
        // nochmal, weil das naechste array damit auch voll sein koennte
        Integer oldValue2 = position.remove(position.size() - 1);
        position.add(oldValue2 + 1); // position hochzaehlen

        tl = list;
        for (i = 0; i < position.size() - 1; i++) {
          int index = position.get(i);
          Object tmp = tl[index];
          if (tmp instanceof byte[]) {
            tl = castBaseTypeByteArrayToObjectArray((byte[]) tmp);
          } else {
            tl = (Object[]) tmp;
          }
        }
      } else {
        return;
      }
    }
  }

  private Byte[] castBaseTypeByteArrayToObjectArray(byte[] o) {
    Byte[] result = new Byte[o.length];
    for (int j = 0; j < result.length; j++) {
      result[j] = o[j];
    }
    return result;
  }

  private int listFinished() { // 0 = keine liste und fertig. 1 == liste und fertig. 2 == sonst
    if (list == null) {
      return 0;
    }
    if (position.size() == 0) {
      return 1;
    }
    return 2;
  }


  private void onReceiveReply() throws IOException, XynaException {
    if (connected) {
      int len = socket.getInputStream().available();
      logger.debug("Receiving reply (length = " + len + ")");
      if (len > 0) {
        evaluateData();
      }
    }
  }


  private void evaluateData() throws UnsupportedEncodingException, IOException, XynaException {
    if (isEvaluating > 0) {
      isEvaluating = isEvaluating + 1;
      return;
    }
    isEvaluating = 1;
    int len;
    byte[] ba;
    boolean repeatLoop = true;

    while (repeatLoop) {
      int av = 0;
      if (!socket.isClosed())
        av = buffer.available();
      else {
        logger.debug("Found closed socket, done");
        break;
      }
      if (av == 0) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          // ignore
        }
        continue;
      }
      logger.trace("(" + _name + ") more bytes available");
      if (responseReceived) {
        // checken, ob ok empfangen wurde!
        finishCommunication();
        return;
      }
      repeatLoop = false;
      int oldPos = buffer.getPosition();
      char b = (char) buffer.read();

      // Resultparameter
      if (b == 'R') {
        logger.trace("(" + _name + ") received resultparameter");
        len = getLength("R");
        if (buffer.available() >= len && len > -1) {
          logger.trace("(" + _name + ") received length: " + len);
          ba = readFromSocket(len);
          if (listFinished() == 0) {
            logger.trace("(" + _name + ") finished normal element, calling finishConnection");
            finishCommunication(ba);
            logger.debug("(" + _name + ") finishConnection complete after finishing normal element");
            responseReceived = true;
            responseEvent(new BlackEditionConnectionResponse(ba));
          } else {
            logger.trace("(" + _name + ") le");
            addElement(ba);
            if (listFinished() == 1) {
              logger.trace("(" + _name + ") finished list, calling finishConnection");
              finishCommunication();
              logger.debug("(" + _name + ") finishConnection complete after finishing List");
              responseReceived = true;
              responseEvent(new BlackEditionConnectionResponse(list));
            }
          }
          repeatLoop = true;
        } else {
          buffer.unread(buffer.getPosition() - oldPos);
          logger.trace("(" + _name + ") rp");
        }
      }
      // Error
      else if (b == 'E') {
        receivedError  = true;
        len = getLength("E");

        if (buffer.available() >= len && len > -1) {
          ba = readFromSocket(len);
          if (listFinished() == 0) {
            finishCommunication(ba);
            responseReceived = true;
            logger.debug("Finished receiving single error String");
            logger.trace("error string: " + new String(ba));
            responseEvent(new BlackEditionConnectionResponse(ba));
            // throw new XynaException("BlackEditionConnectionClient error: " + ba);
          } else {
            addElement(ba);
            if (listFinished() == 1) {
              finishCommunication();
              responseReceived = true;
              logger.debug("Finished receiving error information");
              responseEvent(new BlackEditionConnectionResponse(list));
              // throw new XynaException("BlackEditionConnectionClient error: " + list);
            }
          }
          repeatLoop = true;
        } else {
          // buffer.getPosition() = oldPos;
          buffer.unread(buffer.getPosition() - oldPos);
        }

      }
      // List
      else if (b == 'L') {
        int tempLe = getLength("L");
        if (tempLe > -1) {
          logger.trace("(" + _name + ") Receiving list (length = " + tempLe + ")");
          addList(tempLe);
          repeatLoop = true;
        } else {
          // buffer.position = oldPos;
          buffer.unread(buffer.getPosition() - oldPos);
        }
      } else {
        try {
          socket.close();
        } finally {
          finished = true;
        }
        throw new XynaException("(" + _name + ") got invalid Response " + b + " = " + String.valueOf(b));
      }
    }

    if (isEvaluating > 1) {
      // daten dazugekommen. kann nicht passieren, da singlethreaded
      isEvaluating = 0;
      evaluateData();
    } else {
      isEvaluating = 0;
    }

  }


  private void finishCommunication() throws UnsupportedEncodingException, IOException, XynaException {
    finishCommunication(null);
  }


  /**
   * out sind optional die empfangenen responsedaten
   */
  private void finishCommunication(byte[] out) throws UnsupportedEncodingException, IOException, XynaException {

    if (socket.isConnected()) {
      boolean needToWaitForOK = true;
      while (needToWaitForOK) {
        needToWaitForOK = false;
        if (buffer.available() >= 5) { // probably received "OK"
          byte[] ba = readFromSocket(5);
          if (new String(ba).equals("R2ROK")) { // really received "OK"
            logger.trace("(" + _name + ") Got 'OK', closing socket (" + _name + ")");
            try {
              socket.getOutputStream().write("M3MBye".getBytes(Constants.DEFAULT_ENCODING));
              socket.getOutputStream().flush();
              socket.close();
            } finally {
              finished = true;
            }
          } else { // received something unexpected
            throw new XynaException("(" + _name + ") expected 'OK' at end of stream");
          }
        } else if (out != null && new String(out).equals("OK")) {
          // void methode aufgerufen, nur OK empfangen
          try {
            socket.getOutputStream().write("M3MBye".getBytes(Constants.DEFAULT_ENCODING));
            socket.getOutputStream().flush();
            socket.close();
          } finally {
            finished = true;
          }
        } else {
          // ok ist noch nicht angekommen => warten, bis es da ist.
          needToWaitForOK = true;
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            // ignore
          }
        }
      }
    } else {
      logger.info("(" + _name + ") Found closed socket while trying to close connection (" + _name + ")");
    }
  }


  private int getLength(String separator) throws IOException {
    String s = "";
    boolean t = true;
    while (t) {
      if (buffer.available() == 0) {
        t = false; // laenge kann nicht vollstaendig ermittelt werden. -1 zurueckgeben und spaeter wieder probieren
        s = "";
      } else {
        int b = buffer.read();
        if (b == separator.toCharArray()[0]) {
          t = false;
        } else {
          s += String.valueOf((char) b);
        }
      }
    }

    logger.trace("(" + _name + ") parsed length " + s);
    try {
      return Integer.valueOf(s);
    } catch (NumberFormatException e) {
      return -1;
    }

  }


  private byte[] readFromSocket(int length) throws IOException {
    byte[] ba = new byte[length];
    int start = 0;
    while (length > 0) {
       int x = buffer.read(ba, start, length);
       if (x == -1) {
         throw new EOFException("Unexpected end of input stream after " + start + " bytes.");
       }
       length -= x;
       start += x;
    }
    return ba;
  }


  private void onError(IOException e) throws XynaException {
    logger.debug("onError: " + e.toString());
    throw new XynaException("Got error: " + e.getMessage(), e);
  }


  private void onSecError(SecurityException e) throws XynaException {
    throw new XynaException("Got initialization error: " + e.getMessage(), e);
  }


  private void onSocketConnect() throws UnsupportedEncodingException, IOException {
    connected = true;
    if (sendOrder != null) {
      sendInternally(sendOrder, parameter);
    }
  }


  private void onSocketClose() {
    connected = false;
  }


  private void onSocketActivate() {

  }


  private void sendStringParameter(String s) throws UnsupportedEncodingException, IOException {
    byte[] bytes = s.getBytes(Constants.DEFAULT_ENCODING);
    String message = "P" + bytes.length + "P" + s;
    socket.getOutputStream().write(message.getBytes(Constants.DEFAULT_ENCODING));
    socket.getOutputStream().flush();
  }


  private void sendByteArrayParameter(byte[] ba) throws IOException {
    String message = "P" + ba.length + "P";
    socket.getOutputStream().write(message.getBytes(Constants.DEFAULT_ENCODING));
    socket.getOutputStream().flush();
    socket.getOutputStream().write(ba, 0, ba.length);
    socket.getOutputStream().flush();
  }


  private void sendArrayParameter(Object[] a) throws UnsupportedEncodingException, IOException {
    String message = "L" + a.length + "L";
    socket.getOutputStream().write(message.getBytes(Constants.DEFAULT_ENCODING));
    socket.getOutputStream().flush();
    for (int i = 0; i < a.length; i++) {
      Object parameter = a[i];
      if (parameter instanceof String) {
        sendStringParameter((String) parameter);
      } else if (parameter instanceof byte[]) {
        sendByteArrayParameter((byte[]) parameter);
      } else if (parameter instanceof Object[]) {
        sendArrayParameter((Object[]) parameter);
      }
    }
  }


  private void sendInternally(String method, Object parameter) throws UnsupportedEncodingException, IOException {
    byte[] bytes = method.getBytes(Constants.DEFAULT_ENCODING);
    String message = "M" + bytes.length + "M" + method;
    socket.getOutputStream().write(message.getBytes(Constants.DEFAULT_ENCODING));
    socket.getOutputStream().flush();
//    try {
//      Thread.sleep(1000);
//    } catch (InterruptedException e) {
//      // ignore
//    } // FIXME is this required?
    if (parameter != null) {
      if (parameter instanceof String) {
        sendStringParameter((String) parameter);
      } else if (parameter instanceof byte[]) {
        sendByteArrayParameter((byte[]) parameter);
      } else if (parameter instanceof Object[]) {
        sendArrayParameter((Object[]) parameter);
      }
    }
  }


  /**
   * kommunikation direkt ueber socket
   */
  public void sendRequest(String method, Object parameter) throws XynaException {
    if (connected) {
      try {
        logger.debug("Sending request method '" + method + "' with parameter " + parameter);
        sendInternally(method, parameter);
      } catch (UnsupportedEncodingException e) {
        throw new XynaException("Failed to send request: " + e.getMessage(), e);
      } catch (IOException e) {
        throw new XynaException("Failed to send request: " + e.getMessage(), e);
      }
    } else {
      sendOrder = method;
      this.parameter = parameter;
    }
  }


  /**
   * http get auf url "&lt;http-endpoint&gt;/&lt;method&gt;?p=&lt;parameter&gt;" ausfuehren falls parameter ein string-array ist, werden
   * die parameter in der form ?p1=&lt;parameter[0]&gt;&amp;p2=&lt;parameter[1]&gt; usw uebergeben vorteil gegenueber reiner
   * socketverbindung: fuer download braucht man nicht zweimal klicken (einmal zum starten der server-connection, zweites
   * mal zum angeben der download-location)
   * 
   * @throws XynaException
   */
  public void saveFile(String method, Object parameter) throws XynaException {
    // var downloadURL:URLRequest = new URLRequest();
    // downloadURL.url = "http://" + _endpointHTTP + "/" + method;
    // if (parameter != null) {
    // if (parameter is String) {
    // var s:String = parameter as String;
    // downloadURL.url += "?p=" + s;
    // } else if (parameter is Array) {
    // var a:Array = parameter as Array;
    // for (var i:int = 0; i<a.length; i++) {
    // if (i==0) {
    // downloadURL.url += "?";
    // } else {
    // downloadURL.url += "&";
    // }
    // downloadURL.url += "p" + i + "=" + parameter[i];
    // }
    // }
    // }
    // var file:FileReference = new FileReference();
    // file.addEventListener(Event.COMPLETE, fileDownloadFinished);
    // file.download(downloadURL, method + ".zip");
    throw new XynaException("Currently unsupported: save file");
  }


  private void fileDownloadFinished() {
    // TODO
    logger.info("Download complete");
  }


  private List<BlackEditionConnectionResponse> responses = new ArrayList<BlackEditionConnectionResponse>();


  // TODO propper synchronization
  private synchronized void responseEvent(BlackEditionConnectionResponse response) {
    responses.add(response);
  }


  public List<BlackEditionConnectionResponse> getResponses() {
    return Collections.unmodifiableList(responses);
  }


  public boolean receivedError() {
    return this.receivedError;
  }


  public boolean isFinished() {
    return this.finished;
  }


  private SocketFactory getSocketFactory(String keyStorePath, String keyStoreType, String keyStorePassword)
                  throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException, CertificateException,
                  FileNotFoundException, IOException, KeyManagementException, NoSuchProviderException {

    if (keyStoreType.equals("default")) {
      return SSLSocketFactory.getDefault();
    }
    SSLSocketFactory ssf = null;
    // set up key manager to do server authentication
    char[] passphrase = keyStorePassword.toCharArray();

    SSLContext ctx = SSLContext.getInstance("TLS", "SunJSSE");

    KeyStore ks = KeyStore.getInstance(keyStoreType);
    FileInputStream fis = new FileInputStream(keyStorePath);
    try {
      ks.load(fis, passphrase);
    } finally {
      fis.close();
    }

    KeyStore ks2 = KeyStore.getInstance(keyStoreType);
    fis = new FileInputStream(keyStorePath);
    try {
      ks2.load(fis, passphrase);
    } finally {
      fis.close();
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
    kmf.init(ks, passphrase);

    TrustManagerFactory trf = TrustManagerFactory.getInstance("SunX509", "SunJSSE");
    trf.init(ks2);

    ctx.init(kmf.getKeyManagers(), trf.getTrustManagers(), null);

    ssf = ctx.getSocketFactory();
    return ssf;
  
  }

}
