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
import xmcp.yang.netconf.NetConfConfig;
import xmcp.yang.netconf.NetConfTarget;
import xmcp.yang.netconf.enums.DefaultOperationMerge;
import xmcp.yang.netconf.enums.DefaultOperationNone;
import xmcp.yang.netconf.enums.DefaultOperationReplace;
import xmcp.yang.netconf.enums.ErrorOptionContinueOnError;
import xmcp.yang.netconf.enums.ErrorOptionRollbackOnError;
import xmcp.yang.netconf.enums.ErrorOptionStopOnError;
import xmcp.yang.netconf.enums.NetConfDefaultOperation;
import xmcp.yang.netconf.enums.NetConfErrorOption;
import xmcp.yang.netconf.enums.NetConfTestOption;
import xmcp.yang.netconf.enums.TestOptionSet;
import xmcp.yang.netconf.enums.TestOptionTestOnly;
import xmcp.yang.netconf.enums.TestOptionTestThenSet;


public class CSEditConfig {

  public Document execute(MessageId messageId, NetConfTarget target,
                          NetConfDefaultOperation defaultOp,
                          NetConfTestOption testOption,
                          NetConfErrorOption errorOption,
                          NetConfConfig config) {
    XmlHelper helper = new XmlHelper();
    org.w3c.dom.Document doc = helper.buildDocument();
    Element rpc = helper.createElem(doc).elementName(Constants.Rpc.TAG_NAME).namespace(Constants.NetConf.NETCONF_NSP)
                        .buildAndAppendAsDocumentRoot();
    if ((messageId != null) && (messageId.getId() != null)) {
      rpc.setAttribute(Constants.Rpc.ATTRIBUTE_NAME_MESSAGE_ID, messageId.getId());
    }
    Element opElem = helper.createElem(doc).elementName(Constants.NetConf.OperationNameTag.EDIT_CONFIG)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(rpc);
    Element targetElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.TARGET)
                               .namespace(Constants.NetConf.NETCONF_NSP).buildAndAppendAsChild(opElem);
    if (target != null) {
      if (target.getDatastoreName() != null) {
        helper.createElem(doc).elementName(target.getDatastoreName()).namespace(Constants.NetConf.NETCONF_NSP)
                              .buildAndAppendAsChild(targetElem);
      } else if (target.getURL() != null) {
        helper.createElem(doc).elementName(Constants.NetConf.XmlTag.URL).namespace(Constants.NetConf.NETCONF_NSP)
                              .text(target.getURL()).buildAndAppendAsChild(targetElem);
      }
    }
    handleDefaultOp(defaultOp, opElem, helper, doc);
    handleTestOption(testOption, opElem, helper, doc);
    handleErrorOption(errorOption, opElem, helper, doc);
    if (config != null) {
      Element confElem = helper.createElem(doc).elementName(Constants.NetConf.XmlTag.CONFIG).namespace(Constants.NetConf.NETCONF_NSP)
                               .buildAndAppendAsChild(opElem);
      if (config.getConfig() != null) {
        YangMappingCollection ymc = config.getConfig();
        Document tmp = ymc.createXml();
        if ((tmp != null) && (tmp.getText() != null)) {
          helper.appendXmlSubtree(doc, confElem, tmp.getText());
        }
      }
    }
    Document ret = new Document();
    ret.setText(helper.getDocumentString(doc));
    return ret;
  }

  
  private void handleDefaultOp(NetConfDefaultOperation defaultOp, Element parent, XmlHelper helper,
                               org.w3c.dom.Document doc) {
    if (defaultOp == null) { return; }
    String text = "";
    if (defaultOp instanceof DefaultOperationMerge) {
      text = Constants.NetConf.EnumValue.MERGE;
    } else if (defaultOp instanceof DefaultOperationReplace) {
      text = Constants.NetConf.EnumValue.REPLACE;
    } else if (defaultOp instanceof DefaultOperationNone) {
      text = Constants.NetConf.EnumValue.NONE;
    }
    helper.createElem(doc).elementName(Constants.NetConf.XmlTag.DEFAULT_OPERATION).namespace(Constants.NetConf.NETCONF_NSP)
                          .text(text).buildAndAppendAsChild(parent);
  }
  
  
  private void handleTestOption(NetConfTestOption testOption, Element parent, XmlHelper helper,
                                org.w3c.dom.Document doc) {
    if (testOption == null) { return; }
    String text = "";
    if (testOption instanceof TestOptionSet) {
      text = Constants.NetConf.EnumValue.SET;
    } else if (testOption instanceof TestOptionTestOnly) {
      text = Constants.NetConf.EnumValue.TEST_ONLY;
    } else if (testOption instanceof TestOptionTestThenSet) {
      text = Constants.NetConf.EnumValue.TEST_THEN_SET;
    }
    helper.createElem(doc).elementName(Constants.NetConf.XmlTag.TEST_OPTION).namespace(Constants.NetConf.NETCONF_NSP)
                          .text(text).buildAndAppendAsChild(parent);
  }
  
  
  private void handleErrorOption(NetConfErrorOption errorOption, Element parent, XmlHelper helper,
                                 org.w3c.dom.Document doc) {
    if (errorOption == null) { return; }
    String text = "";
    if (errorOption instanceof ErrorOptionContinueOnError) {
      text = Constants.NetConf.EnumValue.CONTINUE_ON_ERROR;
    } else if (errorOption instanceof ErrorOptionRollbackOnError) {
      text = Constants.NetConf.EnumValue.ROLLBACK_ON_ERROR;
    } else if (errorOption instanceof ErrorOptionStopOnError) {
      text = Constants.NetConf.EnumValue.STOP_ON_ERROR;
    }
    helper.createElem(doc).elementName(Constants.NetConf.XmlTag.TEST_OPTION).namespace(Constants.NetConf.NETCONF_NSP)
                          .text(text).buildAndAppendAsChild(parent);
  }
  
}
