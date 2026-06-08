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
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomPointer;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomTree;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.XmomWalker;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcher;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherAll;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherHasValue;
import com.gip.xyna.xprc.xfractwfe.generation.xmom.matcher.NodeMatcherRefCountMulti;

import xmomjsontest.tools.CompareXmlTools;


public class ParseWriteXmomTest {

  //private static final XPathFactory xpathFactory = XPathFactory.newInstance();
  
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
  
  
  public void testParseWrite() throws Exception {
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
  
  
  public void testWalker() throws Exception {
    try {
      String txt = readFile("test/xmomjsontest/data/TestWf1.xml");
      log(txt);
      
      Document doc = XMLUtils.parseString(txt, true);
      XmomTree tree = new ParserXmomXml().build(doc);
      
      XmomWalker walker = new XmomWalker();
      List<XmomPointer> list = walker.findDescendants(tree, new NodeMatcherAll());
      logPointerList(list);
      
      list = walker.findDescendants(tree, new NodeMatcherHasValue());
      logPointerList(list);
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  public void testWalker2() throws Exception {
    try {
      String txt = readFile("test/xmomjsontest/data/TestWf1.xml");
      log(txt);
      
      Document doc = XMLUtils.parseString(txt, true);
      XmomTree tree = new ParserXmomXml().build(doc);
      logXmomTree(tree);
      
      XmomWalker walker = new XmomWalker();
      List<XmomPointer> list = walker.findDescendants(tree, new NodeMatcherRefCountMulti());
      logPointerList(list);
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private void logPointerList(List<XmomPointer> list) {
    log("");
    log("#####");
    for (XmomPointer xp : list) {
      String val = "";
      if (xp.getNodeInfo().getValue().isPresent()) {
        val = xp.getNodeInfo().getValue().get();
      }
      log(xp.getPath().asString() + " : " + val);
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
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new ParseWriteXmomTest().testWalker2();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
