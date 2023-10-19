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
package xact.ssh.impl;



import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.DetectedError;
import xact.connection.DeviceType;
import xact.connection.ManagedConnection;
import xact.connection.ReadTimeout;
import xact.connection.SendParameter;
import xact.ssh.SSHNETCONFConnection;
import xact.ssh.SSHNETCONFConnectionInstanceOperation;
import xact.ssh.SSHNETCONFConnectionSuperProxy;
import xact.ssh.SSHSendParameter;
import xact.ssh.NETCONF.Capability;
import xact.ssh.NETCONF.CapabilityKey;
import xact.templates.DocumentType;
import xact.templates.NETCONF;
import xfmg.xfmon.protocolmsg.ProtocolMessage;

//import org.bouncycastle.jce.provider.*;
//import com.hierynomus.asn1.encodingrules.*;
//import net.schmizz.sshj.common.IOUtils;
//import net.schmizz.sshj.connection.*;
//import net.schmizz.sshj.signature.*;
//import net.schmizz.sshj.transport.kex.*;
//import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
//import net.schmizz.sshj.userauth.keyprovider.*;
//import net.schmizz.sshj.userauth.method.*;
//import net.schmizz.sshj.*;

import net.schmizz.sshj.connection.channel.direct.Session;



public class SSHNETCONFConnectionInstanceOperationImpl extends SSHNETCONFConnectionSuperProxy
    implements
      SSHNETCONFConnectionInstanceOperation {

  private static final long serialVersionUID = 1L;

  private static final String NETCONF_BASE_1_0_MESSAGE_SEPERATOR = "]]>]]>";

  private String localHello = "<hello xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "<capabilities>"
      + "<capability>urn:ietf:params:netconf:base:1.0</capability>" + "</capabilities>" + "</hello>";
  private final Map<String, String> capabilities = new HashMap<>();
  private String remoteHello;


  public SSHNETCONFConnectionInstanceOperationImpl(SSHNETCONFConnection instanceVar) {
    super(instanceVar);
  }


  protected void initChannelAndStreams(SendParameter sendParameter, DocumentType documentType, DeviceType deviceType, Command cmd) {
    try {

      String netconfHello = // TODO add a list of additional capapilites to connectionParams?
          "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + localHello + NETCONF_BASE_1_0_MESSAGE_SEPERATOR;// TODO line seperator necessary ?

      SSHSendParameter sp;
      if (sendParameter == null) {
        sp = new SSHSendParameter.Builder().connectionTimeoutInMilliseconds(5000).readTimeoutInMilliseconds(2000)
            .reconnectAfterRestart(true).throwExceptionOnReadTimeout(true).instance();
      } else {
        sp = (SSHSendParameter) sendParameter;
      }
      //netconfChannel.connect((int)Math.min(sp.getConnectionTimeoutInMilliseconds(), Integer.MAX_VALUE));
      Session.Subsystem netconfChannel = getSession().startSubsystem("netconf");
      setChannelAndStreams(netconfChannel);

      getOutputStream().write(netconfHello.getBytes(Constants.DEFAULT_ENCODING));

      remoteHello = readFromInputStream(getInputStream(), netconfChannel, documentType, deviceType, sp.getReadTimeoutInMilliseconds(), cmd,
                                        getThrowReadTimeoutException(sp.getThrowExceptionOnReadTimeout()));

      parseCapabilities();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ReadTimeout e) {
      throw new RuntimeException(e);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
  }


  private void parseCapabilities() throws XPRC_XmlParsingException {
    /*
     * erwartete response sieht etwa so aus:
     * 
     * <hello xmlns="urn:ietf:params:xml:ns:netconf:base:1.0">
     <capabilities>
       <capability>
         urn:ietf:params:netconf:base:1.1
       </capability>
       <capability>
         urn:ietf:params:netconf:capability:startup:1.0
       </capability>
       <capability>
         http://example.net/router/2.3/myfeature
       </capability>
       <capability>
          urn:ietf:params:netconf:capability:url:1.0?scheme=http,ftp,file
       </capability>
     </capabilities>
     <session-id>4</session-id>
    </hello>
    
     capability-keys sind die capabilities bis zum '?'.
     */

    Document doc = XMLUtils.parseString(remoteHello.substring(0, remoteHello.indexOf(NETCONF_BASE_1_0_MESSAGE_SEPERATOR)));
    for (Element capEl : XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), "capability")) {
      String cap = XMLUtils.getTextContent(capEl).trim();
      int questionMarkIdx = cap.indexOf('?');
      String key = questionMarkIdx > -1 ? cap.substring(0, questionMarkIdx) : cap;
      capabilities.put(key, cap);
    }
  }


  public String getLocalHello() {
    return localHello;
  }


  public String getRemoteHello() {
    return remoteHello;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


  protected ProtocolMessage createPartialProtocolMessage(String content) {
    return null;
  }


  @Override
  public List<? extends Capability> getAllCapabilities() {
    lazyPrepare();
    return capabilities.values().stream().map(s -> new Capability(s)).collect(Collectors.toList());
  }


  private void lazyPrepare() {
    if (!prepared) {
      reconnectIfNecessary();
      initChannelAndStreams(getInstanceVar().getConnectionParameter().getDefaultSendParameter(), new NETCONF(), new DeviceType() {

        @Override
        public Boolean checkInteraction(CommandResponseTuple arg0, DocumentType arg1) {
          return false;
        }


        @Override
        public void cleanupAfterError(CommandResponseTuple arg0, DocumentType arg1, ManagedConnection arg2) {
        }


        @Override
        public DeviceType clone() {
          throw new RuntimeException();
        }


        @Override
        public DeviceType clone(boolean arg0) {
          throw new RuntimeException();
        }


        @Override
        public void detectCriticalError(CommandResponseTuple arg0, DocumentType arg1) throws DetectedError {
        }


        @Override
        public Command enrichCommand(Command c) {
          return c;
        }


        @Override
        public Boolean isResponseComplete(String response, DocumentType docType, ManagedConnection con, Command cmd) {
          return response.endsWith(NETCONF_BASE_1_0_MESSAGE_SEPERATOR);
        }


        @Override
        public CommandResponseTuple removeDeviceSpecifics(CommandResponseTuple c) {
          return c;
        }

      }, new Command(""));
      prepared = true;
    }
  }


  @Override
  public Capability getFullCapability(CapabilityKey ck) {
    return getFullCapabilityFromString(ck.getUri());
  }


  @Override
  public Capability getFullCapabilityFromString(String ck) {
    lazyPrepare();
    String cs = capabilities.get(ck);
    if (cs != null) {
      return new Capability(cs);
    }
    return null;
  }


  @Override
  public boolean hasCapability(CapabilityKey ck) {
    return hasCapabilityFromString(ck.getUri());
  }


  @Override
  public boolean hasCapabilityFromString(String ck) {
    lazyPrepare();
    return capabilities.containsKey(ck);
  }

}
