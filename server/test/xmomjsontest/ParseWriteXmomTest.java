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

package xmomjsontest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;

import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;

import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.ParserXmomJson;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.ParserXmomXml;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.TreePath;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.WriterXmomJson;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.WriterXmomXml;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNavigator;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomNodeInfo;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;

import xmomjsontest.tools.CompareXmlTools;

public class ParseWriteXmomTest {

  private static final XPathFactory xpathFactory = XPathFactory.newInstance();
  
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
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public void test1() throws Exception {
    try {
      String txt = readFile("test/xmomjsontest/data/TestWf1.xml");
      log(txt);
      
      Document doc = XMLUtils.parseString(txt, true);
      XmomTree tree = new ParserXmomXml().build(doc);
      logXmomTree(tree);
      
      String json = new WriterXmomJson().toJsonString(tree);
      log(json);
      
      XmomTree tree2 = new ParserXmomJson().build(json);
      logXmomTree(tree2);
      String xml = new WriterXmomXml().toXmlString(tree2);
      log(xml);
      
      XmomTree tree3 = new ParserXmomXml().build(xml);
      logXmomTree(tree3);
      
      new CompareXmlTools().compareXml(doc, xml);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private void logXmomTree(XmomTree tree) {
    XmomNavigator nav = new XmomNavigator();
    List<TreePath> paths = nav.getAllPathsOfValueNodes(tree);
    for (TreePath path : paths) {
      Optional<XmomNodeInfo> info = nav.gotoPath(tree, path);
      log(path.asString() + " -> " + info.get().getValue().get());
    }
  }
  
  
  public static void main(String[] args) {
    try {
      new ParseWriteXmomTest().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
