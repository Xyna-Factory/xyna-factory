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
import xmcp.yang.YangMappingCollection;
import xmcp.yang.misc.Constants;
import xmcp.yang.misc.XmlHelper;
import xmcp.yang.netconf.NetConfFilter;
import static xmcp.yang.misc.Constants.NetConf.OperationNameTag;
import static xmcp.yang.misc.Constants.NetConf.XmlTag;
import static xmcp.yang.misc.Constants.NetConf;


public class CSGet {

  @Deprecated
  public Document execute(MessageId messageId, NetConfFilter filter) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(NetConf.NETCONF_NSP)
                        .buildAndAppendAsDocumentRoot();
    if ((messageId != null) && (messageId.getId() != null)) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(OperationNameTag.GET).namespace(NetConf.NETCONF_NSP).buildAndAppendAsChild(rpc);
    if (filter != null) {
      Element filterElem =
          helper.createElem(doc).elementName(NetConf.XmlTag.FILTER).namespace(NetConf.NETCONF_NSP).buildAndAppendAsChild(opElem);
      if (filter.getTypeAttribute() != null) {
        filterElem.setAttribute(NetConf.XmlAttribute.TYPE, filter.getTypeAttribute());
      }
      if (filter.getFilterSubtree() != null) {
        YangMappingCollection ymc = filter.getFilterSubtree();
        Document tmp = ymc.createXml();
        if (tmp != null && tmp.getText() != null && !tmp.getText().isBlank()) {
          helper.appendXmlSubtree(doc, filterElem, tmp.getText());
        }
      }
    }
    return new Document.Builder().text(helper.getDocumentString(doc)).instance();
  }

  public Document execute(MessageId messageId, NETCONF netconf, YangMappingCollection mappings) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(NetConf.NETCONF_NSP).buildAndAppendAsDocumentRoot();
    if (messageId != null && messageId.getId() != null) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(OperationNameTag.GET).namespace(NetConf.NETCONF_NSP).buildAndAppendAsChild(rpc);
    Element filterElem = helper.createElem(doc).elementName(XmlTag.FILTER).namespace(NetConf.NETCONF_NSP).buildAndAppendAsChild(opElem);
    filterElem.setAttribute(NetConf.XmlAttribute.TYPE, NetConf.EnumValue.SUBTREE);
    Document mappingsDoc = mappings.createXml();
    if (mappingsDoc != null && mappingsDoc.getText() != null && !mappingsDoc.getText().isBlank()) {
      helper.appendXmlSubtree(doc, filterElem, mappingsDoc.getText());
    }

    return new Document.Builder().text(helper.getDocumentString(doc)).documentType(netconf).instance();
  }
}
