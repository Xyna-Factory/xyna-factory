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


package pkg;

//import org.junit.jupiter.api.Test;  // if Junit 5 is used?
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xdev.yang.impl.Constants;
import xdev.yang.impl.YangCapabilityUtils;
import xdev.yang.impl.YangCapabilityUtils.YangDeviceCapability;


public class YangTest1 {

  public static Logger _logger = Logger.getLogger(YangTest1.class);

  
  public String readFile(String filename) {
    try {
      String line;
      StringBuilder builder = new StringBuilder("");
      BufferedReader f = new BufferedReader(
           new InputStreamReader(new FileInputStream(filename), "UTF8"));
      try {
        while ((line = f.readLine()) != null) {
          builder.append(line).append("\n");
        }
      }
      finally {
        f.close();
      }
      return builder.toString();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  
  @Test
  public void testXml_1() throws Exception {
    try {
      String txt = getDataFile("cap_zb_1.xml");
      log(txt);
      Document doc = XMLUtils.parseString(txt, true);
      Element elem = XMLUtils.getChildElementByName(doc.getDocumentElement(), Constants.TAG_CAPABILITIES, 
                                                    doc.getDocumentElement().getNamespaceURI());
      log(elem.getNodeName());
      log(elem.getLocalName());
      log(elem.getTagName());
      log(elem.getNamespaceURI());
      log(elem.getPrefix());
      log(elem.lookupNamespaceURI(elem.getPrefix()));
      
      assertEquals("capabilities", elem.getNodeName());
      assertEquals("capabilities", elem.getLocalName());
      assertEquals("capabilities", elem.getTagName());
      assertEquals(Constants.NETCONF_NS, elem.getNamespaceURI());
      assertEquals(null, elem.getPrefix());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void testXml_2() throws Exception {
    try {
      String txt = getDataFile("cap_zb_2.xml");
      log(txt);
    
      InputStream stream = new ByteArrayInputStream(txt.getBytes("UTF-8"));
  
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(stream);
      Element elem = doc.getDocumentElement();
    
      log(elem.getNodeName());
      log(elem.getLocalName());
      log(elem.getNamespaceURI());
      log(elem.getPrefix());
      log(elem.lookupNamespaceURI(elem.getPrefix()));
      
      assertEquals("nsp:hello", elem.getNodeName());
      assertEquals("hello", elem.getLocalName());
      assertEquals("nsp:hello", elem.getTagName());
      assertEquals(Constants.NETCONF_NS, elem.getNamespaceURI());
      assertEquals("nsp", elem.getPrefix());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void testCap_1() throws Exception {
    try {
      String txt = getDataFile("cap_zb_1.xml");
      log(txt);
      Document doc = XMLUtils.parseString(txt, true);
      List<YangDeviceCapability> list = YangCapabilityUtils.loadCapabilitiesFromHelloMessage(doc.getDocumentElement());
      for (YangDeviceCapability cap : list) {
        log(writeYangDeviceCapability(cap));
      }
      assertEquals(list.size(), 1);
      assertEquals("http://www.gip.com/xyna/yang/test/testrpc_zb_1", list.get(0).getRawInfo());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void testCap_2() throws Exception {
    try {
      String txt = getDataFile("meta_zb_2.xml");
      log(txt);
      List<YangDeviceCapability> list = YangCapabilityUtils.loadCapabilitiesImpl(List.of(txt));
      for (YangDeviceCapability cap : list) {
        log(writeYangDeviceCapability(cap));
      }
      assertEquals(list.size(), 1);
      assertEquals("http://www.gip.com/xyna/yang/test/testrpc_zb_2", list.get(0).getRawInfo());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void testCap_3() throws Exception {
    try {
      String txt = getDataFile("meta_zb_3.xml");
      log(txt);
      List<YangDeviceCapability> list = YangCapabilityUtils.loadCapabilitiesImpl(List.of(txt));
      for (YangDeviceCapability cap : list) {
        log(writeYangDeviceCapability(cap));
      }
      assertEquals(list.size(), 1);
      assertEquals("http://www.gip.com/xyna/yang/test/testrpc_zb_3", list.get(0).getNameSpace());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  @Test
  public void testCap_4() throws Exception {
    try {
      //String txt = readFile("mdmimpl/YangAppGenerationImpl/test/data/meta_zb_4.xml");
      //String txt = readFile("../data/meta_zb_4.xml");
      String txt = getDataFile("meta_zb_4.xml");
      log(txt);
      List<YangDeviceCapability> list = YangCapabilityUtils.loadCapabilitiesImpl(List.of(txt));
      assertEquals(list.size(), 1);
      assertEquals("http://www.gip.com/xyna/yang/test/testrpc_zb_4", list.get(0).getNameSpace());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  private String writeYangDeviceCapability(YangDeviceCapability cap) {
    StringBuilder s = new StringBuilder();
    s.append("### ").append(cap.getRawInfo());
    s.append("### ").append(cap.getNameSpace());
    return s.toString();
  }
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  private String getDataFilePath(String filename) throws Exception {
    Path path = getBasePath();  // classes dir
    path = path.getParent().resolve("test").resolve("data").resolve(filename);
    return path.toString();
  }
  
  private String getDataFile(String filename) throws Exception {
    String path = getDataFilePath(filename);
    return readFile(path);
  }
  
  private Path getBasePath() throws Exception {
    return Path.of(getClass().getClassLoader().getResource("").toURI());
  }
  
  public static void main(String[] args) {
    try {
      new YangTest1().testCap_4();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }
  
}
