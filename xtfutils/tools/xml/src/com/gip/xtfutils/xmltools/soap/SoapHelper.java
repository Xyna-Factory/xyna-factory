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

package com.gip.xtfutils.xmltools.soap;

import com.gip.xtfutils.xmltools.nav.XmlNavigator;

public class SoapHelper {


  public static class Constant {
    public static class Xml {
      public static class TagName {
        public static final String ENVELOPE = "Envelope";
        public static final String BODY = "Body";
      }
    }
  }

  public static boolean isRootNodeSoapEnvelope(XmlNavigator nav) {
    if (nav.isEmpty()) { return false; }
    XmlNavigator root = nav.clone().gotoRoot();
    if (root.isEmpty()) { return false; }
    return (Constant.Xml.TagName.ENVELOPE.equals(root.getTagName()));
  }


  public static XmlNavigator getSoapBodyContent(XmlNavigator orig) {
    if (orig.isEmpty()) { return orig; }
    XmlNavigator nav = orig.clone().gotoRoot();
    if (!Constant.Xml.TagName.ENVELOPE.equals(nav.getTagName())) {
      return nav.buildEmpty();
    }
    nav.descend(Constant.Xml.TagName.BODY);
    nav.descendToFirstChild();
    return nav;
  }


  public static void navigateToSOAPBodyContent(XmlNavigator nav) {
    if (nav.isEmpty()) { return; }
    if (!Constant.Xml.TagName.ENVELOPE.equals(nav.getTagName())) {
      return;
    }
    nav.descend(Constant.Xml.TagName.BODY);
    nav.descendToFirstChild();
  }

}
