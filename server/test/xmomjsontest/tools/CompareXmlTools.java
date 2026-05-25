/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package xmomjsontest.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class CompareXmlTools {

  public void compareXml(Document doc1, String xml2) throws Exception {
    Document doc2 = XMLUtils.parseString(xml2, true);
    compareXml(doc1, doc2);
  }
  
  
  public void compareXml(Document doc1, Document doc2) throws Exception {
    log("### start xml comparison");
    Map<String, String> map1 = xmlToXPathMap(doc1);
    Map<String, String> map2 = xmlToXPathMap(doc2);
    for (String key : map1.keySet()) {
      String val1 = map1.get(key);
      String val2 = map2.get(key);
      if (val2 == null) {
        log("XML difference at " + key + ": " + val1 + " != null");
      } else {
        logCompare(key, val1, val2);
      }
    }
    for (String key : map2.keySet()) {
      String val1 = map1.get(key);
      String val2 = map2.get(key);
      if (val1 == null) {
        log("XML difference at " + key + ": null" + " != " + val2);
      }
    }
  }
  
  
  private void logCompare(String key, String val1, String val2) {
    String adapted1 = val1 == null ? "" : val1;
    String adapted2 = val2 == null ? "" : val2;
    boolean matches = adapted1.equals(adapted2);
    if (!matches) {
      log("XML difference at " + key + ": " + val1 + " != " + val2);
    }
  }
  
  
  public Map<String, String> xmlToXPathMap(Document doc) throws Exception {
    List<XmlElementExtractor.XPathValueData> paths = XmlElementExtractor.extractAllElements(doc);
    Map<String, String> map = new HashMap<>();
    for (XmlElementExtractor.XPathValueData data : paths) {
      map.put(data.xpath, data.value);
    }
    return map;
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
}
