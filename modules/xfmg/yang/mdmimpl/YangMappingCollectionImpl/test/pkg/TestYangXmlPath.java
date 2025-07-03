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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import xmcp.yang.YangMappingPath;
import xmcp.yang.YangMappingPathElement;
import xmcp.yang.xml.CsvPathsAndNspsWithIds;
import xmcp.yang.xml.IdOfNamespaceMap;
import xmcp.yang.xml.ListKeyBuilder;
import xmcp.yang.xml.NamespaceOfIdMap;
import xmcp.yang.xml.XmomPathAdapter;
import xmcp.yang.xml.YangXmlPath;
import xmcp.yang.xml.YangXmlPathElem;
import xmcp.yang.xml.YangXmlPathList;


public class TestYangXmlPath {

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
  
  private ByteArrayInputStream textAsByteStream(String text) throws IOException {
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(text.getBytes());
    return is;
  }
  
  protected ByteArrayInputStream fileAsByteStream(File file) throws IOException {
    String text = readFile(file.getPath());
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(text.getBytes());
    return is;
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
  
  @Test
  public void test1() throws Exception {
    try {
      YangXmlPathList pathlist = new YangXmlPathList();
      YangXmlPath path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").
               addListKey(new ListKeyBuilder().listKeyElemName("dd3").listKeyValue("d-val-3").build()).build());
      path.add(YangXmlPathElem.builder().elemName("dd1").namespace("www.nsp-3.de/test-3").textValue("d%val-1").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd2").namespace("www.nsp-3.de/test-3").textValue("d%val-2").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").
               addListKey(new ListKeyBuilder().listKeyElemName("dd3").listKeyValue("d-val-3").build()).build());
      path.add(YangXmlPathElem.builder().elemName("dd3").namespace("www.nsp-3.de/test-3").textValue("d%val-3").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb2").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("cc2").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd4").namespace("www.nsp-4.de/test-4").textValue("d%val-4").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa2").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb3").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc3").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd4").namespace("www.nsp-4.de/test-4").textValue("d%val-5").build());
      pathlist.add(path);
      
      pathlist.sort();
      IdOfNamespaceMap map1 = new IdOfNamespaceMap();
      List<String> csvlist = pathlist.toCsvList(map1);
      for (String str : csvlist) {
        log(str);
      }
      assertEquals("aa1#0###,bb1#3###,cc1#1###dd3=d-val-3,dd1#1#d&percnt;val-1##", csvlist.get(2));
      
      log("namespaces: ");
      List<String> nsplist = map1.toPrefixNamespacePairList();
      Collections.sort(nsplist);
      for (String str : nsplist) {
        log(str);
      }
      assertEquals(nsplist.size(), 4);
      assertEquals("p0=www.nsp-1.de/test-1", nsplist.get(0));
      
      log(pathlist.toXml());
      
      NamespaceOfIdMap map2 = new NamespaceOfIdMap();
      map2.initFromPrefixNamespacePairs(nsplist);
      YangXmlPathList pathlist2 = YangXmlPathList.fromCsv(map2, csvlist);
      pathlist2.sort();
      
      List<String> csvlist2 = pathlist2.toCsvList(map1);
      for (String str : csvlist2) {
        log(str);
      }
      assertEquals(pathlist.getPathList().size(), 5);
      assertEquals(pathlist.getPathList().size(), pathlist2.getPathList().size());
      assertEquals(pathlist.getPathList().size(), csvlist.size());
      assertEquals(csvlist.size(), csvlist2.size());
      for (int i = 0; i < csvlist.size(); i++) {
        assertEquals(csvlist.get(i), csvlist2.get(i));
        assertTrue(pathlist.getPathList().get(i).equals(pathlist2.getPathList().get(i)));
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testMerge() throws Exception {
    try {
      YangXmlPathList pathlist = new YangXmlPathList();
      YangXmlPath path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").
               addListKey(new ListKeyBuilder().listKeyElemName("dd3").listKeyValue("d-val-3").build()).build());
      path.add(YangXmlPathElem.builder().elemName("dd1").namespace("www.nsp-3.de/test-3").textValue("d%val-1").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd2").namespace("www.nsp-3.de/test-3").textValue("d%val-2").build());
      pathlist.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb1").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc1").namespace("www.nsp-3.de/test-3").
               addListKey(new ListKeyBuilder().listKeyElemName("dd3").listKeyValue("d-val-3").build()).build());
      path.add(YangXmlPathElem.builder().elemName("dd3").namespace("www.nsp-3.de/test-3").textValue("d%val-3").build());
      pathlist.add(path);
      
      YangXmlPathList pathlist2 = new YangXmlPathList();
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa1").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb2").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("cc2").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd4").namespace("www.nsp-4.de/test-4").textValue("d%val-4").build());
      pathlist2.add(path);
      
      path = new YangXmlPath();
      path.add(YangXmlPathElem.builder().elemName("aa2").namespace("www.nsp-1.de/test-1").build());
      path.add(YangXmlPathElem.builder().elemName("bb3").namespace("www.nsp-2.de/test-2").build());
      path.add(YangXmlPathElem.builder().elemName("cc3").namespace("www.nsp-3.de/test-3").build());
      path.add(YangXmlPathElem.builder().elemName("dd4").namespace("www.nsp-4.de/test-4").textValue("d%val-5").build());
      pathlist2.add(path);
      
      pathlist.sort();
      pathlist2.sort();
      
      CsvPathsAndNspsWithIds csv1 = new CsvPathsAndNspsWithIds(pathlist);
      log(csv1);
      CsvPathsAndNspsWithIds csv2 = new CsvPathsAndNspsWithIds(pathlist2);
      log(csv2);
      CsvPathsAndNspsWithIds csv3 = csv1.merge(csv2);
      log(csv3);
      
      assertEquals(csv3.getCsvPathList().size(), 5);
      assertEquals(csv3.getCsvPathList().get(2), "aa1#0###,bb1#1###,cc1#2###dd3=d-val-3,dd3#2#d&percnt;val-3##");
      assertEquals(csv3.getCsvPathList().get(3), "aa1#0###,bb2#0###,cc2#2###,dd4#3#d&percnt;val-4##");
      
      YangXmlPathList pathlist3 = YangXmlPathList.fromCsv(csv3);
      pathlist3.sort();
      log(pathlist3.toXml());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testXmomAdapter() throws Exception {
    try {
      YangMappingPath path = new YangMappingPath();
      path.addToPath(buildYangMappingPathElement("aa1", "www.nsp-1.de/test-1"));
      path.addToPath(buildYangMappingPathElement("bb2", "www.nsp-2.de/test-2"));
      path.addToPath(buildYangMappingPathElement("cc3", "www.nsp-3.de/test-3"));
      path.addToPath(new YangMappingPathElement.Builder().elementName("dd4").namespace("www.nsp-1.de/test-1").instance());
      path.setValue("test-value-1");
      
      YangXmlPath adapted = new XmomPathAdapter().adapt(path);
      YangXmlPathList pathlist = new YangXmlPathList();
      pathlist.add(adapted);
      
      CsvPathsAndNspsWithIds csv = new CsvPathsAndNspsWithIds(pathlist);
      log(csv);
      assertEquals(csv.getCsvPathList().size(), 1);
      assertEquals(csv.getCsvPathList().get(0), "aa1#0###,bb2#1###,cc3#2###,dd4#0#test-value-1##");
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  
  @Test
  public void testListKeys_1() throws Exception {
    try {
      List<String> csvlist = new ArrayList<>();
      csvlist.add("c_root#0###,listIndex#1##0#,c_list_1#0###,c1#0#val-c1##true,");
      csvlist.add("c_root#0###,listIndex#1##0#,c_list_1#0###,c2#0#val-c2-new2##true,");
      csvlist.add("c_root#0###,listIndex#1##0#,c_list_1#0###,d_c#0###,listIndex#1##0#,e_list_1#0###,e1#0#val-e1-1##true,");
      csvlist.add("c_root#0###,listIndex#1##0#,c_list_1#0###,d_c#0###,listIndex#1##1#,e_list_1#0###,e2#0#e2-val##true,");
      csvlist.add("c_root#0###,listIndex#1##0#,c_list_1#0###,d_c#0###,d1#0#d1-val##,");
      csvlist.add("c_root#0###,listIndex#1##1#,c_list_1#0###,c1#0#c1-1##true");
      
      NamespaceOfIdMap nspmap = new NamespaceOfIdMap();
      nspmap.add(0, "http://www.gip.com/xyna/yang/test/testrpc_z_C_3");
      
      YangXmlPathList yxpl = YangXmlPathList.fromCsv(nspmap, csvlist);
      String xml = yxpl.toXml();
      log(xml);
      
      yxpl = yxpl.replaceListIndicesWithKeys();
      xml = yxpl.toXml();
      log(xml);
      
      CsvPathsAndNspsWithIds csv = new CsvPathsAndNspsWithIds(yxpl);
      log(csv);
      assertEquals(csv.getCsvPathList().size(), 6);
      assertEquals("c_root#0###,c_list_1#0###c1=val-c1%c2=val-c2-new2,d_c#0###,e_list_1#0###e2=e2-val,e2#0#e2-val##", 
                   csv.getCsvPathList().get(5));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testListKeyNamespaces() throws Exception {
    try {
      YangXmlPathList pathlist = new YangXmlPathList();
      {
        YangMappingPath path = new YangMappingPath();
        path.addToPath(buildYangMappingPathElement("aa1", "www.nsp-1.de/test-1"));
        path.addToPath(buildYangMappingPathElement("bb2", "www.nsp-2.de/test-2"));
        path.addToPath(buildYangMappingPathElement("cc3", "www.nsp-3.de/test-3", 0));
        path.addToPath(new YangMappingPathElement.Builder().elementName("dd4").namespace("www.nsp-1.de/test-1").instance());
        path.setValue("val-1");
        YangXmlPath adapted = new XmomPathAdapter().adapt(path);
        pathlist.add(adapted);
      }
      {
        YangMappingPath path = new YangMappingPath();
        path.addToPath(buildYangMappingPathElement("aa1", "www.nsp-1.de/test-1"));
        path.addToPath(buildYangMappingPathElement("bb2", "www.nsp-2.de/test-2"));
        path.addToPath(buildYangMappingPathElement("cc3", "www.nsp-3.de/test-3", 1));
        path.addToPath(new YangMappingPathElement.Builder().elementName("dd4").namespace("www.nsp-1.de/test-1").instance());
        path.setValue("val-2");
        YangXmlPath adapted = new XmomPathAdapter().adapt(path);
        pathlist.add(adapted);
      }
      CsvPathsAndNspsWithIds csvids = new CsvPathsAndNspsWithIds(pathlist);
      log(csvids);
      
      YangXmlPathList yxpl = YangXmlPathList.fromCsv(csvids);
      String xml = yxpl.toXml();
      log(xml);
      
      yxpl = yxpl.replaceListIndicesWithKeys();
      xml = yxpl.toXml();
      log(xml);
      
      CsvPathsAndNspsWithIds csv = new CsvPathsAndNspsWithIds(yxpl);
      log(csv);
      
      //assertEquals(csv.getCsvPathList().size(), 1);
      //assertEquals(csv.getCsvPathList().get(0), "aa1#0###,bb2#1###,cc3#2###,dd4#0#test-value-1##");
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private YangMappingPathElement buildYangMappingPathElement(String name, String nsp) {
    YangMappingPathElement ret = new YangMappingPathElement();
    ret.setElementName(name);
    ret.setNamespace(nsp);
    return ret;
  }
  
  private YangMappingPathElement buildYangMappingPathElement(String name, String nsp, int index) {
    YangMappingPathElement ret = new YangMappingPathElement();
    ret.setElementName(name);
    ret.setNamespace(nsp);
    ret.setListIndex(index);
    return ret;
  }
  
  private void log(CsvPathsAndNspsWithIds csv) {
    log("### csv:");
    for (String str : csv.getCsvPathList()) {
      log(str);
    }    
    log("### namespaces: ");
    List<String> nsplist = csv.getNamespaceWithIdList();
    for (String str : nsplist) {
      log(str);
    }
  }
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestYangXmlPath().testListKeyNamespaces();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
