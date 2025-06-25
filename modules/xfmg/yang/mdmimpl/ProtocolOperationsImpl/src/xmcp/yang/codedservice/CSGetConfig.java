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
import xmcp.yang.MessageId;
import xmcp.yang.YangMappingCollection;
import xmcp.yang.misc.Constants;
import xmcp.yang.misc.XmlHelper;
import xmcp.yang.netconf.NetConfFilter;
import xmcp.yang.netconf.NetConfSource;


public class CSGetConfig {

  public Document execute(MessageId messageId, NetConfSource source, NetConfFilter filter) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(Constants.NetConf.NAMESPACE)
                        .buildAndAppendAsDocumentRoot();
    if ((messageId != null) && (messageId.getId() != null)) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(Constants.NetConf.OperationNameTag.GET_CONFIG)
                               .namespace(Constants.NetConf.NAMESPACE).buildAndAppendAsChild(rpc);
    Element sourceElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.SOURCE)
                               .namespace(Constants.NetConf.NAMESPACE).buildAndAppendAsChild(opElem);
    if (source != null) {
      if (source.getDatastoreName() != null) {
        helper.createElem(doc).elementName(source.getDatastoreName()).namespace(Constants.NetConf.NAMESPACE)
                              .buildAndAppendAsChild(sourceElem);
      } else if (source.getURL() != null) {
        helper.createElem(doc).elementName(Constants.NetConf.XmlTag.URL).namespace(Constants.NetConf.NAMESPACE)
                              .text(source.getURL()).buildAndAppendAsChild(sourceElem);
      }
    }
    if (filter != null) {
      Element filterElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.FILTER).namespace(Constants.NetConf.NAMESPACE)
                             .buildAndAppendAsChild(opElem);
      if (filter.getTypeAttribute() != null) {
        filterElem.setAttribute(Constants.NetConf.XmlAttribute.TYPE, filter.getTypeAttribute());
      }
      if (filter.getFilterSubtree() != null) {
        YangMappingCollection ymc = filter.getFilterSubtree();
        Document tmp = ymc.createXml();
        if ((tmp != null) && (tmp.getText() != null)) {
          helper.appendXmlSubtree(doc, filterElem, tmp.getText());
        }
      }
    }
    Document ret = new Document();
    ret.setText(helper.getDocumentString(doc));
    return ret;
  }
  
}
