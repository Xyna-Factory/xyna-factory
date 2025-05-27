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

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import xmcp.yang.xml.CsvPathsAndNspsWithIds;
import xmcp.yang.xml.IdOfNamespaceMap;
import xmcp.yang.xml.ListKeyBuilder;
import xmcp.yang.xml.NamespaceOfIdMap;
import xmcp.yang.xml.YangXmlPath;
import xmcp.yang.xml.YangXmlPathElem;
import xmcp.yang.xml.YangXmlPathList;


public class TestYangXmlPath {

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
      assertEquals("aa1#0##,bb1#1##,cc1#2##dd3=d-val-3,dd3#2#d&percnt;val-3#", csvlist.get(2));
      
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
      
      YangXmlPathList pathlist3 = YangXmlPathList.fromCsv(csv3);
      log(pathlist3.toXml());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
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
      new TestYangXmlPath().testMerge();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
