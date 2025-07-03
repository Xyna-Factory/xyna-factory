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
import xmcp.yang.misc.DataStoreHelper;
import xmcp.yang.misc.XmlHelper;
import xmcp.yang.misc.DataStoreHelper.NetConfOperation;
import xmcp.yang.netconf.NetConfSource;
import xmcp.yang.netconf.NetConfTarget;

public class CSCopyConfig {

  public Document execute(MessageId messageId, NetConfTarget target, NetConfSource source) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(Constants.NetConf.NETCONF_NSP)
                        .buildAndAppendAsDocumentRoot();
    if ((messageId != null) && (messageId.getId() != null)) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(Constants.NetConf.OperationNameTag.COPY_CONFIG)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(rpc);
    Element targetElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.TARGET)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(opElem);
    if (target != null) {
      if (target.getDatastoreName() != null) {
        String datastore = new DataStoreHelper().getDataStoreTagName(target.getDatastoreName(), NetConfOperation.OTHER);
        helper.createElem(doc).elementName(datastore).namespace(Constants.NetConf.NETCONF_NSP)
                              .buildAndAppendAsChild(targetElem);
      } else if (target.getURL() != null) {
        helper.createElem(doc).elementName(Constants.NetConf.XmlTag.URL).namespace(Constants.NetConf.NETCONF_NSP)
                              .text(target.getURL()).buildAndAppendAsChild(targetElem);
      }
    }
    Element sourceElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.SOURCE)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(opElem);
    if (source != null) {
      if (source.getDatastoreName() != null) {
        String datastore = new DataStoreHelper().getDataStoreTagName(source.getDatastoreName(), NetConfOperation.OTHER);
        helper.createElem(doc).elementName(datastore).namespace(Constants.NetConf.NETCONF_NSP)
                              .buildAndAppendAsChild(sourceElem);
      } else if (source.getURL() != null) {
        helper.createElem(doc).elementName(Constants.NetConf.XmlTag.URL).namespace(Constants.NetConf.NETCONF_NSP)
                              .text(source.getURL()).buildAndAppendAsChild(sourceElem);
      }
    }
    
    Document ret = new Document();
    ret.setText(helper.getDocumentString(doc));
    return ret;
  }

}
