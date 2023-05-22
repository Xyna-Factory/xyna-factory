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
package xact.sip;



import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

import javax.sip.message.Request;

import org.apache.log4j.Logger;

import base.Credentials;
import base.Port;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;



public class SipUserAgentImpl implements ExtendedDeploymentTask {

  private final static XynaPropertyString PROPERTY_LOCALIP_FOR_SIP = new XynaPropertyString("xact.sip.localip",
                                                                                            "127.0.0.1");
  private final static XynaPropertyString PROPERTY_LOCALPORTRANGE_FOR_SIP =
      new XynaPropertyString("xact.sip.localportrange", "4250-4260");
  private final static XynaPropertyString PROPERTY_SIPUSERNAME = new XynaPropertyString("xact.sip.username.black",
                                                                                        "xynablack");
  private final static XynaPropertyInt PROPERTY_SIPNOTIFY_TIMEOUT =
      new XynaPropertyInt("xact.sip.notify.responsetimeout", 10000);
  private final static XynaPropertyString PROPERTY_SECUREKEY = new XynaPropertyString("xact.sip.secure.key",
                                                                                      "sip.notify");


  private static Logger logger = CentralFactoryLogging.getLogger(SipUserAgentImpl.class);
  private static HashMap<String, SipLayer> sipLayerCache = new HashMap<String, SipLayer>();


  protected SipUserAgentImpl() {
  }


  private static AtomicLong cnt = new AtomicLong(0);


  public void onDeployment() throws XynaException {
    String user = "SipUserAgent";
    PROPERTY_LOCALIP_FOR_SIP.registerDependency(user);
    PROPERTY_LOCALPORTRANGE_FOR_SIP.registerDependency(user);
    PROPERTY_SIPUSERNAME.registerDependency(user);
    PROPERTY_SIPNOTIFY_TIMEOUT.registerDependency(user);
    PROPERTY_SECUREKEY.registerDependency(user);
  }


  public void onUndeployment() throws XynaException {
    logger.debug("shutting down cached sipLayers...");
    synchronized (sipLayerCache) {
      Iterator<String> it = sipLayerCache.keySet().iterator();
      while (it.hasNext()) {
        String key = it.next();
        SipLayer layer = sipLayerCache.get(key);
        try {
          layer.shutdown();
        } catch (RuntimeException e) {
          logger.warn("Failed to shutdown sip layer instance " + key);
        } catch (Error e) {
          Department.handleThrowable(e);
          logger.warn("Failed to shutdown sip layer instance " + key);
        }
      }
      sipLayerCache.clear();
    }
    logger.debug("shutting down completed");
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public static void notify(SipEventHeader sipEventHeader, SipUserURI sipUserURI, SipProxyAddress proxyAddress,
                            Credentials credentials) throws SipNotifyCreationException, SipNotifySendException,
      SipNotifyAbortedException, SipResponseException, SipNotifySendTimeoutException {

    String toTag = null;//layer.randomizeString();
    String fromTag = randomizeString();
    String viaBranch = randomizeString();
    Request request = null;

    SipLayer layer = getLayer(proxyAddress.getHostName(), proxyAddress.getPort());
    try {
      try {
        request =
            layer.createNotifyRequest(PROPERTY_SIPUSERNAME.get(),
                                      sipUserURI.getUserName() + "@" + sipUserURI.getHostName(), toTag, fromTag,
                                      viaBranch, sipEventHeader);
        if (logger.isDebugEnabled()) {
          logger.debug("created request:\n" + request.toString());
        }
      } catch (Exception e) { //ParseException, InvalidArgumentException, SipException
        throw new SipNotifyCreationException(e);
      }
      layer.addCredentials(request, credentials.getUsername(), credentials.getPassword());
      layer.sendRequestSynchronously(proxyAddress.getHostName(), request, PROPERTY_SIPNOTIFY_TIMEOUT.get());
    } finally {
      if (request != null) {
        try {
          layer.removeCredentials(request);
        } catch (RuntimeException e) {
          logger.error("Failed to remove request from SIP layer", e);
        }
      }
    }

  }


  public static void secureNotify(SipEventHeader sipEventHeader, SipUserURI sipUserURI, SipProxyAddress proxyAddress,
                                  Credentials credentials) throws SipNotifyCreationException, SipNotifySendException,
      SipNotifyAbortedException, SipResponseException, SipNotifySendTimeoutException {
    try {
      notify(sipEventHeader, sipUserURI, proxyAddress, decryptCredentials(credentials));
    } catch (XNWH_EncryptionException e) {
      throw new SipNotifyCreationException(e);
    }
  }


  private static Credentials decryptCredentials(Credentials encrypted) throws XNWH_EncryptionException {
    Credentials decrypted = new Credentials();
    decrypted.setUsername(encrypted.getUsername());
    decrypted.setPassword(XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage()
        .decrypt(PROPERTY_SECUREKEY.get(), encrypted.getPassword()));
    return decrypted;
  }


  public static String randomizeString() {
    return "" + System.currentTimeMillis() + cnt.getAndAdd(1) + Math.random();
  }


  private static SipLayer getLayer(String name, Port port) throws SipNotifyCreationException {
    String key = name + ":" + port.getValue() + "/UDP";
    synchronized (sipLayerCache) {
      SipLayer layer = sipLayerCache.get(key);
      if (layer == null) {
        int portFromProperties;
        try {
          portFromProperties = getPortFromProperties();
        } catch (XynaException e1) {
          throw new SipNotifyCreationException(e1);
        }
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("creating new siplayer for '" + key + "'. " + SipUserAgentImpl.class.getClassLoader());
          }
          layer = new SipLayer(PROPERTY_LOCALIP_FOR_SIP.get(), portFromProperties, key);
        } catch (Exception e) {
          throw new SipNotifyCreationException(e);
        }
        sipLayerCache.put(key, layer);
      }
      return layer;
    }
  }


  private static int getPortFromProperties() throws XynaException {
    String val = PROPERTY_LOCALPORTRANGE_FOR_SIP.get();
    if (val.indexOf("-") > -1) {
      String[] split = val.split("-");
      if (split.length != 2) {
        throw new XynaException("invalid value for xynaproperty " + PROPERTY_LOCALPORTRANGE_FOR_SIP.getPropertyName()
            + ".");
      }
      int low = Integer.valueOf(split[0]);
      int high = Integer.valueOf(split[1]);
      for (int iport = low; iport <= high; iport++) {
        boolean free = true;
        synchronized (sipLayerCache) {
          Iterator<String> it = sipLayerCache.keySet().iterator();
          while (it.hasNext()) {
            SipLayer sl = sipLayerCache.get(it.next());
            if (sl.getLocalPort() == iport) {
              free = false;
              break;
            }
          }
        }
        if (free) {
          return iport;
        }
      }
      throw new XynaException("No free port available for creating new SipLayer in range " + val);
    } else {
      int port = Integer.valueOf(val);
      synchronized (sipLayerCache) {
        Iterator<String> it = sipLayerCache.keySet().iterator();
        while (it.hasNext()) {
          SipLayer sl = sipLayerCache.get(it.next());
          if (sl.getLocalPort() == port) {
            throw new XynaException("No free port available to create new sip layer."
                + " Please provide port range in property " + PROPERTY_LOCALPORTRANGE_FOR_SIP);
          }
        }
      }
      return port;
    }
  }

  
/*  public static void main(String[] args) throws XynaException {
    Random r = new Random();
    for (int i = 0; i<100; i++) {
      String proxy = "proxy" + r.nextInt(4);
      SipLayer layer = getLayer(proxy, new Port(7777));
      System.out.println("proxy= " + proxy + " layer=" + layer + " layerport=" + layer.getLocalPort());
    }
  }*/

}
