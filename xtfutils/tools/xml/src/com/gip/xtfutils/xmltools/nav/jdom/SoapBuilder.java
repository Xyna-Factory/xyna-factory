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

package com.gip.xtfutils.xmltools.nav.jdom;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;

import com.gip.xtfutils.xmltools.XmlBuildException;
import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.XmlParseException;
import com.gip.xtfutils.xmltools.build.ElementNode;
import com.gip.xtfutils.xmltools.build.XmlAttribute;
import com.gip.xtfutils.xmltools.build.XmlBuilder;
import com.gip.xtfutils.xmltools.build.jdom.XmlJdomBuilder;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;
import com.gip.xtfutils.xmltools.soap.SoapHelper;



public class SoapBuilder {

  public static class Constant {
    public static class XmlTagName {
      public static final String BODY = "Body";
      public static final String ENVELOPE = "Envelope";
      public static final String HEADER = "Header";


      public static class WsSecurity {
        public static final String SECURITY = "Security";
        public static final String USERNAME_TOKEN = "UsernameToken";
        public static final String USERNAME = "Username";
        public static final String PASSWORD = "Password";
      }
    }

    public static class XmlNamespace {
      public static class Soap_1_1 {
        // soap 1.1
        public static final String ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
      }
      public static class Soap_1_2 {
        //soap 1.2
        public static final String ENVELOPENAMESPACE_2003 = "http://www.w3.org/2003/05/soap-envelope";
      }
      public static class WsSecurity {
        public static final String WSSE =
                      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
        public static final String WSU =
                      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
      }
    }

    public static final String SOAPPREFIX = "soap";
  }


  /**
   * returns XmlJdomBuilder where 'Envelope' and 'Body' tags are already opened;
   * namespace = "http://schemas.xmlsoap.org/soap/envelope/"
   * (soap 1.1)
   */
  public static XmlJdomBuilder createBuilderWithOpenedSOAPHeader() {
    XmlJdomBuilder builder = new XmlJdomBuilder();
    builder.openTag(Constant.XmlTagName.ENVELOPE).
                    setNamespace(XmlNamespace.setUri(Constant.XmlNamespace.Soap_1_1.ENVELOPE)
                                             .setPrefix(Constant.SOAPPREFIX));
    builder.openTag(Constant.XmlTagName.BODY);
    return builder;
  }


  /**
   * returns XmlJdomBuilder where 'Envelope' and 'Body' tags are already opened;
   * namespace = "http://www.w3.org/2003/05/soap-envelope"
   * (soap 1.2)
   */
  public static XmlJdomBuilder createBuilderWithOpenedSOAPHeader_2003() {
    XmlJdomBuilder builder = new XmlJdomBuilder();
    builder.openTag(Constant.XmlTagName.ENVELOPE).
                    setNamespace(XmlNamespace.setUri(Constant.XmlNamespace.Soap_1_2.ENVELOPENAMESPACE_2003)
                                             .setPrefix(Constant.SOAPPREFIX));
    builder.openTag(Constant.XmlTagName.BODY);
    return builder;
  }


  public static String addSoapEnvelope(String xml) throws XmlParseException, XmlBuildException {
    Document oldDoc = JdomHelper.createDocument(xml);
    Document doc = new Document();
    Element env = new Element(Constant.XmlTagName.ENVELOPE,
                              Namespace.getNamespace(Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    Element body = new Element(Constant.XmlTagName.BODY,
                               Namespace.getNamespace(Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    doc.setRootElement(env);
    env.addContent(body);
    body.addContent(oldDoc.getRootElement().detach());
    return JdomHelper.documentToString(doc);
  }


  public static String addSoapEnvelopeWithoutDefaultNamespace(String xml) throws XmlParseException, XmlBuildException {
    Document oldDoc = JdomHelper.createDocument(xml);
    Document doc = new Document();
    Element env = new Element(Constant.XmlTagName.ENVELOPE,
                              Namespace.getNamespace("soap", Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    Element body = new Element(Constant.XmlTagName.BODY,
                               Namespace.getNamespace("soap", Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    doc.setRootElement(env);
    env.addContent(body);
    body.addContent(oldDoc.getRootElement().detach());
    return JdomHelper.documentToString(doc);
  }


  public static String addSoapEnvelopeWithWsSecData(String xml, String user, String password, String idAttribute)
                  throws XmlParseException, XmlBuildException {
    Document oldDoc = JdomHelper.createDocument(xml);
    Document doc = new Document();
    Element env = new Element(Constant.XmlTagName.ENVELOPE,
                              Namespace.getNamespace("soap", Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    Element body = new Element(Constant.XmlTagName.BODY,
                               Namespace.getNamespace("soap", Constant.XmlNamespace.Soap_1_1.ENVELOPE));
    doc.setRootElement(env);
    body.addContent(oldDoc.getRootElement().detach());

    env.addContent(buildWsSecurityHeader(user, password, idAttribute));
    env.addContent(body);

    return JdomHelper.documentToString(doc);
  }


  private static Element buildWsSecurityHeader(String user, String password, String idAttribute) {
    XmlJdomBuilder header = new XmlJdomBuilder();
    XmlNamespace soap = XmlNamespace.setUri(Constant.XmlNamespace.Soap_1_1.ENVELOPE).setPrefix("soap");
    XmlNamespace wsse = XmlNamespace.setUri(Constant.XmlNamespace.WsSecurity.WSSE).setPrefix("wsse");
    XmlNamespace wsu = XmlNamespace.setUri(Constant.XmlNamespace.WsSecurity.WSU).setPrefix("wsu");

    header.openTag(Constant.XmlTagName.HEADER, soap);
    {
      header.openTag(Constant.XmlTagName.WsSecurity.SECURITY, wsse);
      {
        XmlAttribute id = XmlAttribute.setName("Id").setValue(idAttribute).setNamespace(wsu);
        ElementNode token = header.openTag(Constant.XmlTagName.WsSecurity.USERNAME_TOKEN);
        if ((idAttribute != null) && (idAttribute.trim().length() > 0)) {
          token.addAttribute(id);
        }
        {
          header.addChildElementWithInnerText(Constant.XmlTagName.WsSecurity.USERNAME, user);
          header.addChildElementWithInnerText(Constant.XmlTagName.WsSecurity.PASSWORD, password).
                 addAttribute(XmlAttribute.setName("Type").setValue(
                 "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"));
        }
        header.closeTag(Constant.XmlTagName.WsSecurity.USERNAME_TOKEN);
      }
      header.closeTag(Constant.XmlTagName.WsSecurity.SECURITY);
    }
    header.closeTag(Constant.XmlTagName.HEADER);
    return header.buildDocument().detachRootElement();
  }


  public static String removeOptionalSoapEnvelope(String xml) throws XmlParseException, XmlBuildException {
    XmlNavigator nav = new JdomNavigator(xml);
    if (!SoapHelper.isRootNodeSoapEnvelope(nav)) {
      return xml;
    }
    nav = SoapHelper.getSoapBodyContent(nav);
    return nav.getSelfDescendantString();
  }


  public static List<String> getSoapBodyContent(String xml) throws XmlParseException, XmlBuildException {
    List<String> ret = new ArrayList<String>();
    XmlNavigator nav = new JdomNavigator(xml);
    if (!SoapHelper.isRootNodeSoapEnvelope(nav)) {
      ret.add(xml);
      return ret;
    }
    nav.descend(SoapHelper.Constant.Xml.TagName.BODY);
    List<XmlNavigator> nodes = nav.getAllChildren();
    for (XmlNavigator node : nodes) {
      String subtree = node.getSelfDescendantString();
      ret.add(subtree);
    }
    return ret;
  }

}
