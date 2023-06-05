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
package xact.sip;



import gov.nist.javax.sip.SipStackExt;
import gov.nist.javax.sip.clientauthutils.AccountManager;
import gov.nist.javax.sip.clientauthutils.AuthenticationHelper;
import gov.nist.javax.sip.clientauthutils.UserCredentials;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.TooManyListenersException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sip.ClientTransaction;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.TimeoutEvent;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.TransactionUnavailableException;
import javax.sip.TransportNotSupportedException;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;



/**
 * kapselt den sipstack. hilft requests zu erstellen und die response zuzuordnen und zu verarbeiten. rfcs:
 * http://www.ietf.org/rfc/rfc3581.txt http://www.ietf.org/rfc/rfc3265.txt http://www.ietf.org/rfc/rfc3261.txt
 */
public class SipLayer implements SipListener {

  private final static Logger logger = CentralFactoryLogging.getLogger(SipLayer.class);

  private SipStackExt sipStack;

  private SipFactory sipFactory;

  private AddressFactory addressFactory;

  private HeaderFactory headerFactory;

  private MessageFactory messageFactory;

  private SipProvider sipProvider;

  private int localPort;
  private String localIp;

  private AccountManagerImpl accountManager;

  //key = callid 
  private static HashMap<String, SipResponseListener> responseListeners = new HashMap<String, SipResponseListener>();


  protected SipLayer(int localPort) {
    this.localPort = localPort;
  }


  /**
   * öffnet an localip:localPort ein socket. ausgehende verbindungen gehen über den angegebenen proxy (falls nicht null)
   */
  public SipLayer(String localip, int localPort, String proxy) throws PeerUnavailableException,
      TransportNotSupportedException, InvalidArgumentException, ObjectInUseException, TooManyListenersException {
    if (logger.isDebugEnabled()) {
      logger.debug("creating new sip layer for " + localip + ":" + localPort + " connecting to proxy " + proxy);
    }
    this.localPort = localPort;
    this.localIp = localip;
    sipFactory = SipFactory.getInstance();
    sipFactory.setPathName("gov.nist");
    Properties properties = new Properties();
    properties.setProperty("javax.sip.STACK_NAME", "XynaSipLayer_ " + localip + ":" + localPort); //eindeutiger name pro sipstack
    //    properties.setProperty("javax.sip.IP_ADDRESS", localip); => achtung, dann gibt es nur einen stack pro ip. @deprecated
    if (proxy != null) {
      properties.setProperty("javax.sip.OUTBOUND_PROXY", proxy);
    }

    //welche properties gibts? => siehe SipStackImpl.java und SipStack.java
    properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "LOG4J"); //log4j einstellungen benutzen
    //   properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "sipLayer.txt");
    //   properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "sipLayerdebug.log");
    properties.setProperty("gov.nist.javax.sip.DELIVER_UNSOLICITED_NOTIFY", "true"); //notify geht auch ohne subscription
    //properties.setProperty("javax.sip.USE_ROUTER_FOR_ALL_URIS", "true");

    sipStack = (SipStackExt) sipFactory.createSipStack(properties); //einmal pro local IP, gibt ggfs den bestehenden stack zurück
    headerFactory = sipFactory.createHeaderFactory();
    addressFactory = sipFactory.createAddressFactory();
    messageFactory = sipFactory.createMessageFactory();

    //  ListeningPoint tcp = sipStack.createListeningPoint(localip, localPort, "tcp");
    ListeningPoint udp = sipStack.createListeningPoint(localip, localPort, "udp"); //einmal pro local Port+IP (d.h. einmal pro siplayer instanz, s.o.)
    //   sipProvider = sipStack.createSipProvider(tcp);
    sipProvider = sipStack.createSipProvider(udp); //einmal pro local Port/IP

    //  sipProvider.addSipListener(SipLayerSipListener.getInstance()); //ein listener pro stack!!
    sipProvider.addSipListener(this);

    accountManager = new AccountManagerImpl();
  }


  /* falls nicht siplayer ~ sipstack 1:1 ist:
   *  private static class SipLayerSipListener implements SipListener {
      
      private static SipLayerSipListener instance = new SipLayerSipListener();
      
      private SipLayerSipListener() {
        
      }
      
      public static SipLayerSipListener getInstance() {
        return instance;
      }*/

  /**
   * This method is called by the SIP stack when a dialog (session) ends.
   */
  public void processDialogTerminated(DialogTerminatedEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug("terminated dialog " + evt.toString());
    }
    String id = null;
    if (evt != null && evt.getDialog() != null) {
      id = evt.getDialog().getCallId().getCallId();
      handleResponseListener("the dialog was terminated", id);
    } else {
      logger.debug("not handling event");
    }
  }


  /**
   * This method is called by the SIP stack when a transaction ends.
   */
  public void processTransactionTerminated(TransactionTerminatedEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug("terminated transaction " + evt.toString());
    }
    String id = null;
    if (evt != null && evt.getClientTransaction() != null && evt.getClientTransaction().getDialog() != null) {
      id = evt.getServerTransaction().getDialog().getCallId().getCallId();
      handleResponseListener("the transaction was terminated", id);
    } else {
      logger.debug("not handling event");
    }
  }


  /**
   * This method is called by the SIP stack when there's an asynchronous message transmission error.
   */
  public void processIOException(IOExceptionEvent evt) {
    logger.error("IOException occurred unexpectedly. " + evt.toString() + " " + evt.getSource());
  }


  /**
   * This method is called by the SIP stack when a new request arrives.
   */
  public void processRequest(RequestEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug("not handling this event: got request = " + evt.toString() + " \n" + evt.getRequest().toString());
    }
  }


  /** This method is called by the SIP stack when a response arrives. */
  public void processResponse(ResponseEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug("response = " + evt.toString() + " " + evt.getResponse().getStatusCode() + "\n"
          + evt.getResponse().toString());
    }
    Response response = evt.getResponse();
    handleResponseListener(response);
  }


  /**
   * This method is called by the SIP stack when there's no answer to a message. Note that this is treated differently
   * from an error message.
   */
  public void processTimeout(TimeoutEvent evt) {
    if (logger.isDebugEnabled()) {
      logger.debug("timeout " + evt.toString());
    }
    String id = null;
    if (evt != null && evt.getClientTransaction() != null && evt.getClientTransaction().getDialog() != null) {
      id = evt.getServerTransaction().getDialog().getCallId().getCallId();
      handleResponseListener("a timeout occurred waiting for response", id);
    } else {
      logger.debug("not handling event");
    }
  }


  private void handleResponseListener(String msg, String callId) {
    SipResponseListener rl = null;
    synchronized (responseListeners) {
      rl = responseListeners.remove(callId);
    }
    if (rl != null) {
      logger.debug("responselistener is still available. calling onError ...");
      try {
        rl.onError(new SipNotifySendException(new XynaException(msg)));
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("an uncaught error occured during error handling of request with callid = " + callId, t);
      }
    }
  }


  private void handleResponseListener(Response response) {
    CallIdHeader idHeader = (CallIdHeader) response.getHeader(CallIdHeader.NAME);
    SipResponseListener rl = null;
    synchronized (responseListeners) {
      rl = responseListeners.remove(idHeader.getCallId());
    }
    if (rl != null) {
      try {
        rl.onResponse(response);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("an uncatched error occured during response handling of response with callid = "
                         + idHeader.getCallId(), t);
      }
    }
  }


  private class AccountManagerImpl implements AccountManager {

    private HashMap<String, UserCredentials> credentials = new HashMap<String, UserCredentials>();


    public AccountManagerImpl() {
    }


    public void addUserCredentials(UserCredentials uc, String callIdRef) {
      synchronized (credentials) {
        credentials.put(callIdRef, uc);
      }
    }


    public UserCredentials getCredentials(ClientTransaction challengedTransaction, String realm) {
      CallIdHeader callIdHeader = (CallIdHeader) challengedTransaction.getRequest().getHeader(CallIdHeader.NAME);

      UserCredentials uc;
      synchronized (credentials) {
        uc = credentials.get(callIdHeader.getCallId());
      }
      //TODO nullpointerexception verhindern? accountmanager interface stellt leider keine exception zur verfügung => runtimeexception werfen?
      return uc;
    }


    public void removeUserCredentials(String callIdRef) {
      synchronized (credentials) {
        credentials.remove(callIdRef);
      }
    }

  }

  private class UserCredentialsImpl implements UserCredentials {

    private String userName;
    private String sipDomain;
    private String password;


    public UserCredentialsImpl(String userName, String sipDomain, String password) {
      this.userName = userName;
      this.sipDomain = sipDomain;
      this.password = password;
    }


    public String getPassword() {
      return password;
    }


    public String getSipDomain() {
      return sipDomain;
    }


    public String getUserName() {
      return userName;
    }

  }


  /**
   * erstellt einen notify request mit einigen headern. Beispiel:<br>
   * {@literal 
   * NOTIFY sip:snom870@voip.giplab.local SIP/2.0
     Call-ID: afdc39efe2108907d5aa5858f334e9b0@10.17.0.105
     CSeq: 1 NOTIFY
     From: "xynablack" <sip:xynablack@10.17.0.105:7050>;tag=1266491071489380.9916688535632429
     To: "snom870" <sip:snom870@voip.giplab.local>
     Via: SIP/2.0/UDP 10.17.0.105:7050;branch=z9hG4bK41d29b74217c345858c9533c46a17b9a383631
     Max-Forwards: 70
     Event: check-sync;reboot=false
     Subscription-State: Active;expires=30000
     Contact: "xynablack" <sip:xynablack@10.17.0.105:7050>
     Content-Length: 0
   * }
   * @param user
   * @param to
   * @param toTag
   * @param fromTag
   * @param viaBranch
   * @param eventHeaderInfo
   * @return
   * @throws ParseException
   * @throws InvalidArgumentException
   * @throws SipException
   */
  public Request createNotifyRequest(String user, String to, String toTag, String fromTag, String viaBranch,
                                     SipEventHeader eventHeaderInfo) throws ParseException, InvalidArgumentException,
      SipException {
    //callIdHeader
    CallIdHeader callId = sipProvider.getNewCallId();
    //cseqHeader
    CSeqHeader cSeqHeader = headerFactory.createCSeqHeader(1l, Request.NOTIFY);
    //fromheader
    SipURI from = addressFactory.createSipURI(user, localIp + ":" + localPort);
    Address fromNameAddress = addressFactory.createAddress(from);
    fromNameAddress.setDisplayName(user);
    FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, fromTag);


    //toHeader
    String username = to.substring(0, to.indexOf("@"));
    String address = to.substring(to.indexOf("@") + 1);

    SipURI toAddress = addressFactory.createSipURI(username, address);
    Address toNameAddress = addressFactory.createAddress(toAddress);
    toNameAddress.setDisplayName(username);
    ToHeader toHeader = headerFactory.createToHeader(toNameAddress, toTag);
    //viaHeader
    ArrayList<ViaHeader> viaHeaders = new ArrayList<ViaHeader>();
    ViaHeader viaHeader = headerFactory.createViaHeader(localIp, localPort, "udp", viaBranch);
    //viaHeader.setParameter("rport", null);
    //    viaHeader.setRPort();
    viaHeaders.add(viaHeader);
    //maxForwards
    MaxForwardsHeader maxForwards = headerFactory.createMaxForwardsHeader(70);

    //requestUri
    URI requestUri = (URI) toAddress.clone();


    //request ohne payload bauen, weil notify
    Request request =
        messageFactory.createRequest(requestUri, Request.NOTIFY, callId, cSeqHeader, fromHeader, toHeader, viaHeaders,
                                     maxForwards);


    EventHeader eventHeader = headerFactory.createEventHeader(eventHeaderInfo.getEventType());
    SipEventHeaderParameter headerParam = eventHeaderInfo.getEventParameter();
    if (headerParam != null) {
      eventHeader.setParameter(headerParam.getName(), headerParam.getValue());
    }
    request.addHeader(eventHeader);

    SubscriptionStateHeader substateHeader =
        headerFactory.createSubscriptionStateHeader(SubscriptionStateHeader.ACTIVE);
    //substateHeader.setExpires(30000);
    request.addHeader(substateHeader);


    SipURI contactURI = addressFactory.createSipURI(user, localIp + ":" + localPort);
    //contactURI.setPort(port);
    Address contactAddress = addressFactory.createAddress(contactURI);
    contactAddress.setDisplayName(user);
    ContactHeader contactHeader = headerFactory.createContactHeader(contactAddress);
    request.addHeader(contactHeader);

    //ContentTypeHeader contentType = headerFactory.createContentTypeHeader("application", "snomxml");
    //request.addHeader(contentType);

    //AuthorizationHeader authHeader = headerFactory.createAuthorizationHeader("bla");
    //request.addHeader(authHeader);

    //sipProvider.sendRequest(request);
    return request;
  }


  private interface SipResponseListener {

    public void onResponse(Response r);


    public void onError(SipNotifySendException e);
  }


  private void cleanupResponseListeners(Request r) {
    CallIdHeader idHeader = (CallIdHeader) r.getHeader(CallIdHeader.NAME);
    synchronized (responseListeners) {
      responseListeners.remove(idHeader.getCallId());
    }
  }


  /**
   * responselistener wird entfernt, bevor er aufgerufen wird
   * @param r
   * @param rl
   */
  private void addResponseListener(Request r, SipResponseListener rl) {
    CallIdHeader idHeader = (CallIdHeader) r.getHeader(CallIdHeader.NAME);
    synchronized (responseListeners) {
      responseListeners.put(idHeader.getCallId(), rl);
    }
  }


  public void shutdown() {
    //harter stop. evtl netter machen?
    sipStack.stop();
  }


  public int getLocalPort() {
    return localPort;
  }


  /**
   * wartet bis response zurückgekommen ist. führt einen retry bei 401 fehler durch, bei dem authentifizierungsdetails
   * angehängt werden ({@link #addCredentials})
   * @param r
   * @param timeout millisekunden
   * @throws SipNotifySendException
   * @throws SipNotifyAbortedException
   * @throws SipResponseException
   * @throws SipNotifySendTimeoutException 
   * @throws SipException falls response fehlerhaft ist, oder ein fehler während der responseverarbeitung aufgetreten
   *           ist
   * @throws TimeoutException falls timeout zugeschlagen hat
   * @throws XynaException falls es ein problem beim warten auf die antwort gab
   */
  public void sendRequestSynchronously(String hostName, Request r, long timeout) throws SipNotifySendException,
      SipNotifyAbortedException, SipResponseException, SipNotifySendTimeoutException {

    final long startTime = System.currentTimeMillis();
    final ClientTransaction tx;
    try {
      tx = sipProvider.getNewClientTransaction(r);
    } catch (TransactionUnavailableException e1) {
      throw new SipNotifySendException(e1);
    }
    final CountDownLatch responseLatch = new CountDownLatch(1);
    final List<SipResponseException> responseExceptions = new ArrayList<SipResponseException>();
    final List<SipNotifySendException> sendExceptions = new ArrayList<SipNotifySendException>();
    final List<Response> responses401 = new ArrayList<Response>();
    addResponseListener(r, new SipResponseListener() {

      //durchführung des retries mit authentifizierung
      public void onResponse(Response response) {
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
          //erfolg
        } else if (response.getStatusCode() == 401) {
          responses401.add(response);
        } else {
          SipResponseException e =
              new SipResponseException(response.getStatusCode(), tx.getRequest().toString(), response.toString());
          responseExceptions.add(e);
        }
        responseLatch.countDown();
      }


      public void onError(SipNotifySendException xe) {
        sendExceptions.add(xe);
        responseLatch.countDown();
      }

    });

    try {

      logger.debug("Sending request");
      try {
        tx.sendRequest();
      } catch (SipException e1) {
        throw new SipNotifySendException(e1);
      }
      logger.debug("Waiting for response");

      boolean success;
      try {
        success = responseLatch.await(timeout, TimeUnit.MILLISECONDS);
        if (logger.isDebugEnabled()) {
          if (success) {
            logger.debug("Got response");
          } else {
            logger.debug("Timeout while waiting for response");
          }
        }
      } catch (InterruptedException e) {
        throw new SipNotifyAbortedException(e);
      }

      if (!success) {
        throw new SipNotifySendTimeoutException(hostName, (int) (timeout/1000));
      }

    } finally {
      cleanupResponseListeners(tx.getRequest());
      terminateTransaction(tx);
    }

    if (responseExceptions.size() > 0) {
      throw responseExceptions.get(0);
    }
    if (sendExceptions.size() > 0) {
      throw sendExceptions.get(0);
    }

    if (responses401.size() > 0) {
      timeout = Math.max(timeout - (System.currentTimeMillis() - startTime), 0);
      sendAuthentication(responses401.get(0), tx, timeout);
    }

  }


  private void sendAuthentication(Response response, ClientTransaction tx, long timeout)
      throws SipNotifyAbortedException, SipNotifySendException, SipResponseException {

    logger.debug("handling authentication request");
    //retry
    AuthenticationHelper authHelper = sipStack.getAuthenticationHelper(accountManager, headerFactory);

    final ClientTransaction tx2;
    try {
      tx2 = authHelper.handleChallenge(response, tx, sipProvider, 0);
    } catch (NullPointerException e2) {
      throw new SipNotifySendException(e2);
    } catch (SipException e2) {
      throw new SipNotifySendException(e2);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("new Request:\n" + tx2.getRequest().toString());
    }

    final CountDownLatch responseLatch = new CountDownLatch(1);
    final List<SipResponseException> exceptions = new ArrayList<SipResponseException>();
    final List<SipNotifySendException> sendExceptions = new ArrayList<SipNotifySendException>();
    addResponseListener(tx2.getRequest(), new SipResponseListener() {

      //barrier, damit gewartet wird, bis response des retries mit authentifizierung da ist
      public void onResponse(Response response) {
        if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
          //erfolg
        } else {
          XynaException cause =
              new XynaException("sip layer got response with statuscode " + response.getStatusCode()
                  + " after trying to handle authentication challenge. [Request=" + tx2.getRequest().toString()
                  + ", Response=" + response.toString() + "]");
          SipResponseException e =
              new SipResponseException(response.getStatusCode(), tx2.getRequest().toString(), response.toString(),
                                       cause);
          exceptions.add(e);
        }
        responseLatch.countDown();
      }


      public void onError(SipNotifySendException xe) {
        sendExceptions.add(xe);
        responseLatch.countDown();
      }

    });
    try {
      logger.debug("sending Request");
      try {
        tx2.sendRequest();
      } catch (SipException e1) {
        throw new SipNotifySendException(e1);
      }
      logger.debug("waiting for Response");
      try {
        responseLatch.await(timeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        throw new SipNotifyAbortedException(e);
      }
      logger.debug("got Response");
    } finally {
      cleanupResponseListeners(tx2.getRequest());
      terminateTransaction(tx2);
    }
    if (exceptions.size() > 0) {
      throw exceptions.get(0);
    }
    if (sendExceptions.size() > 0) {
      throw sendExceptions.get(0);
    }
  }


  private void terminateTransaction(ClientTransaction tx) {
    if (tx != null) {
      if (tx.getDialog() != null) {
        tx.getDialog().delete();
      } else {
        try {
          tx.terminate();
        } catch (ObjectInUseException e) {
          logger.debug("transaction termination got error", e);
        }
      }
      //beim notify nicht unbedingt nötig, weil das automatisch passiert
    }
  }


  /**
   * passwort für authentifizierung registrieren. fügt dem internen accountmanager das password hinzu. es ist gebundenen
   * an die callid des requests - d.h. bei anforderung von einer transaktion mit dieser callid wird das passwort
   * benutzt.
   * @param r
   * @param password
   * @throws SipNotifySendException
   * @throws XynaException
   */
  public void addCredentials(Request r, String userName, String password) throws SipNotifySendException {
    CallIdHeader id = (CallIdHeader) r.getHeader(CallIdHeader.NAME);
    ToHeader to = (ToHeader) r.getHeader(ToHeader.NAME);
    URI uri = to.getAddress().getURI();
    if (!(uri instanceof SipURI)) {
      throw new SipNotifySendException(new XynaException("found unsupported uri in to header: "
          + uri.getClass().getName()));
    }
    SipURI sipuri = (SipURI) uri;
    String sipDomain = sipuri.getHost();
    accountManager.addUserCredentials(new UserCredentialsImpl(userName, sipDomain, password), id.getCallId());
  }


  public void removeCredentials(Request r) {
    CallIdHeader id = (CallIdHeader) r.getHeader(CallIdHeader.NAME);
    accountManager.removeUserCredentials(id.getCallId());
  }


}
