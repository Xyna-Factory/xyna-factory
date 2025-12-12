/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.yang.codedservice;

import org.w3c.dom.Element;

import xact.templates.Document;
import xact.templates.NETCONF;
import xmcp.yang.MessageId;
import xmcp.yang.misc.Constants;
import xmcp.yang.misc.XmlHelper;
import xmcp.yang.netconf.NetConfSessionId;


public class CSKillSession {

  public Document execute(MessageId messageId, NetConfSessionId sessionId) {
    return execute(messageId, null, sessionId);
  }

  public Document execute(MessageId messageId, NETCONF netconf, NetConfSessionId sessionId) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(Constants.NetConf.NETCONF_NSP)
                        .buildAndAppendAsDocumentRoot();
    if ((messageId != null) && (messageId.getId() != null)) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(Constants.NetConf.OperationNameTag.KILL_SESSION)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(rpc);
    if ((sessionId != null) && (sessionId.getSessionId() != null)) {
      helper.createElem(doc).elementName(Constants.NetConf.XmlTag.SESSION_ID).namespace(Constants.NetConf.NETCONF_NSP)
                            .text(sessionId.getSessionId()).buildAndAppendAsChild(opElem);
    }
    return new Document.Builder().text(helper.getDocumentString(doc)).documentType(netconf).instance();
  }

}
