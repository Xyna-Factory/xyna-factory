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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.junit.Assert;

import com.gip.xyna.utils.misc.CsvUtils.CSVDocument;
import com.gip.xyna.utils.misc.CsvUtils.CSVIterator;



public class CsvUtilsTest extends TestCase {
  
  
  public void testCountOccurrences() {
    Assert.assertEquals( 2, CsvUtils.countOccurrences("x", "xx") );
    Assert.assertEquals( 1, CsvUtils.countOccurrences("x", "x") );
    Assert.assertEquals( 0, CsvUtils.countOccurrences("x", "") );
    Assert.assertEquals( 3, CsvUtils.countOccurrences("xx", "xxxxxx") );
    Assert.assertEquals( 1, CsvUtils.countOccurrences("x", "axa") );
    Assert.assertEquals( 2, CsvUtils.countOccurrences("x", "xax") );
    Assert.assertEquals( 1, CsvUtils.countOccurrences("xy", "xaxy") );
    Assert.assertEquals( -1, CsvUtils.countOccurrences("", "xaxy") );
  }
  
  public void testStartsWithNConsecutiveOccurrences() {
    Assert.assertEquals( -1, CsvUtils.startsWithNConsecutiveOccurrences("", "xaxy") );
    Assert.assertEquals( 1, CsvUtils.startsWithNConsecutiveOccurrences("x", "xaxy") );
    Assert.assertEquals( 3, CsvUtils.startsWithNConsecutiveOccurrences("x", "xxxaxy") );
    Assert.assertEquals( 0, CsvUtils.startsWithNConsecutiveOccurrences("y", "xxxaxy") );
    Assert.assertEquals( 2, CsvUtils.startsWithNConsecutiveOccurrences("xy", "xyxyxaxy") );
  }
  
  public void testSimple() {
    List<String> list = Arrays.asList("a","b","c");
    String csv = CsvUtils.toCSV(list);
    Assert.assertEquals( "a,b,c", csv );
    
    csv = csv+",d";
    List<String> list2 = new ArrayList<String>();
    for( String p : CsvUtils.iterate(csv) ) {
      list2.add(p);
    }
    Assert.assertEquals( "[a, b, c, d]", list2.toString() );
  }
    
  public void testComplex() {
    List<String> list = Arrays.asList("a","b","c,d","e, f",null,"g\"h", "\"", ",,");
    String csv = CsvUtils.toCSV(list);
    Assert.assertEquals( "a,b,\"c,d\",\"e, f\",,\"g\"\"h\",\"\"\"\",\",,\"", csv );
    
    List<String> list2 = new ArrayList<String>();
    for( String p : CsvUtils.iterate(csv) ) {
      if( p == null ) {
        list2.add("---");
      } else {
        list2.add("#"+p+"#");
      }
    }
    Assert.assertEquals( "[#a#, #b#, #c,d#, #e, f#, ---, #g\"h#, #\"#, #,,#]", list2.toString() );
  }
  
  public void testIntList() {
    List<Integer> list = Arrays.asList(1,3,4,null,7);
    
    String csv = CsvUtils.toCSV(list,",","\"");
    Assert.assertEquals( "1,3,4,,7", csv );
    
    List<String> list2 = new ArrayList<String>();
    for( String p : CsvUtils.iterate(csv) ) {
      list2.add(p);
    }
    Assert.assertEquals( "[1, 3, 4, null, 7]", list2.toString() );
  }
  
  
  public void testDifferentOutput() {
    List<String> list = Arrays.asList("a","b","c","d,e","f_g","h#i","j\"k");
    Assert.assertEquals( "a,b,c,\"d,e\",f_g,h#i,\"j\"\"k\"", CsvUtils.toCSV(list) );
    Assert.assertEquals( "a, b, c, d,e, f_g, h#i, \"j\"\"k\"", CsvUtils.toCSV(list, ", ", "\"") );
    Assert.assertEquals( "a#b#c#d,e#_f__g_#_h#i_#j\"k", CsvUtils.toCSV(list, "#", "_") );
  }
  
  public void testEmpty() {
    String csv = null;
    Assert.assertFalse(CsvUtils.iterate(csv).iterator().hasNext());
    
    csv = "";
    Iterator<String> it = CsvUtils.iterate(csv).iterator();
    Assert.assertFalse(it.hasNext());
  }
  
  public void testEmptyNew() {
    String csv = null;
    Assert.assertFalse(CsvUtils.iterate(csv, true).iterator().hasNext());
    
    csv = "";
    Iterator<String> it = CsvUtils.iterate(csv, true).iterator();
    Assert.assertTrue(it.hasNext());
    it.next();
    Assert.assertFalse(it.hasNext());
  }


  public void testParseCsvDocument1() {

    String[][] csvDocLineEntries = new String[3][3];
    csvDocLineEntries[0][0] = "a";
    csvDocLineEntries[0][1] = "\"b\n\"\"c\"";
    csvDocLineEntries[0][2] = "d";

    csvDocLineEntries[1][0] = "\"aa\nbcd\"";
    csvDocLineEntries[1][1] = "\"b\nc\"";
    csvDocLineEntries[1][2] = "d";

    csvDocLineEntries[2][0] = "\"aaaaa\"";
    csvDocLineEntries[2][1] = "\"b\nc\"";
    csvDocLineEntries[2][2] = "d";

    check(csvDocLineEntries, "\n");
    check(csvDocLineEntries, "\r\n");

  }


  public void testParseCsvDocument2() {

    String[][] csvDocLineEntries = new String[3][3];
    csvDocLineEntries[0][0] = "a\"\"\"\"";
    csvDocLineEntries[0][1] = "\"\"\"\"\"b\n\"\"c\"";
    csvDocLineEntries[0][2] = "d";

    csvDocLineEntries[1][0] = "\"aa\nbcd\"";
    csvDocLineEntries[1][1] = "\"b\nc\"";
    csvDocLineEntries[1][2] = "d";

    csvDocLineEntries[2][0] = "\"aaaaa\"";
    csvDocLineEntries[2][1] = "\"b\nc\"";
    csvDocLineEntries[2][2] = "d";

    check(csvDocLineEntries, "\n");
    check(csvDocLineEntries, "\r\n");

  }


  public void testParseCsvDocument_LinebreakInLastField() {

    String[][] csvDocLineEntries = new String[3][3];
    csvDocLineEntries[0][0] = "a";
    csvDocLineEntries[0][1] = "b";
    csvDocLineEntries[0][2] = "\"c\nd\"";

    csvDocLineEntries[1][0] = "\"aa\nbcd\"";
    csvDocLineEntries[1][1] = "\"b\nc\"";
    csvDocLineEntries[1][2] = "\"d\ne\" ";

    csvDocLineEntries[2][0] = "\"aaaaa\"";
    csvDocLineEntries[2][1] = "\"b\nc\"";
    csvDocLineEntries[2][2] = "d";

    check(csvDocLineEntries, "\n");
    check(csvDocLineEntries, "\r\n");

  }

  public void testCsvLineWithLineBreakAtEnd() {
    com.gip.xyna.utils.misc.CsvUtils.CSVDocument doc =
        new com.gip.xyna.utils.misc.CsvUtils.CSVDocument("a,b\nc,\n", com.gip.xyna.utils.misc.CsvUtils.CSV_SEPARATOR,
                                                         com.gip.xyna.utils.misc.CsvUtils.CSV_MASKER,
                                                         com.gip.xyna.utils.misc.CsvUtils.CSV_LINESEPARATOR, "");
    java.util.List<com.gip.xyna.utils.misc.CsvUtils.CSVIterator> lines = doc.getLines();
    assertTrue(lines.get(1).hasNext());
    assertEquals("c", lines.get(1).next());
    assertTrue(lines.get(1).hasNext());
    assertEquals("", lines.get(1).next());
    assertFalse(lines.get(1).hasNext());
  }
  
  public void testCsvLineWithTrailingSeparator() {
    com.gip.xyna.utils.misc.CsvUtils.CSVDocument doc =
        new com.gip.xyna.utils.misc.CsvUtils.CSVDocument("a,b\nc,", com.gip.xyna.utils.misc.CsvUtils.CSV_SEPARATOR,
                                                         com.gip.xyna.utils.misc.CsvUtils.CSV_MASKER,
                                                         com.gip.xyna.utils.misc.CsvUtils.CSV_LINESEPARATOR, "");
    java.util.List<com.gip.xyna.utils.misc.CsvUtils.CSVIterator> lines = doc.getLines();
    assertTrue(lines.get(1).hasNext());
    assertEquals("c", lines.get(1).next());
    assertTrue(lines.get(1).hasNext());
    assertEquals("", lines.get(1).next());
    assertFalse(lines.get(1).hasNext());
  }


  public void testWriteCsvDocument() {

    List<List<String>> matrix = new ArrayList<List<String>>();

    List<String> firstLine = new ArrayList<String>();
    firstLine.add("a");
    firstLine.add("\"a\nb\"\"");
    firstLine.add("\nc\n");
    matrix.add(firstLine);

    List<String> secondLine = new ArrayList<String>();
    secondLine.add("\na\n");
    secondLine.add("\"\"\"");
    secondLine.add("b\nc\nd");
    matrix.add(secondLine);

    // String csvResult = 
        CsvUtils.toCSVMultiLine(matrix, ",", "\"", "\n");

  }


  private static void check(String[][] csvDocLineEntryMatrix, String lineSeparator) {

    String[] csvDocLine = new String[csvDocLineEntryMatrix.length];
    for (int i = 0; i < csvDocLine.length; i++) {
      for (int j = 0; j < csvDocLineEntryMatrix[i].length; j++) {
        if (j != 0) {
          csvDocLine[i] += ",";
        } else {
          csvDocLine[i] = "";
        }
        csvDocLine[i] += csvDocLineEntryMatrix[i][j];
      }
    }

    String csvDocString = "";
    for (int i=0; i<csvDocLine.length; i++) {
      csvDocString += csvDocLine[i];
      if (i<csvDocLine.length-1) {
        csvDocString += lineSeparator;
      }
    }

    Iterable<String> iterable = CSVDocument.splitDocumentToLines(csvDocString, "\"", lineSeparator);
    Iterator<String> iter = iterable.iterator();
    for (int i=0; i<csvDocLine.length; i++) {
      Assert.assertEquals(true, iter.hasNext());
      Assert.assertEquals(csvDocLine[i], iter.next());
    }
    Assert.assertEquals(false, iter.hasNext());

    CSVDocument csvDoc = new CSVDocument(csvDocString, ",", "\"", lineSeparator, "");

    Assert.assertEquals(csvDocLine.length, csvDoc.getLines().size());

    for (int i = 0; i < csvDocLine.length; i++) {
      CSVIterator lineIterator = csvDoc.getLines().get(i);
      for (int j = 0; j < csvDocLineEntryMatrix[i].length; j++) {
        Assert.assertEquals("No next at " + i + "," + j, true, lineIterator.hasNext());
        Assert.assertEquals("Invalid at " + i + "," + j, lineIterator.unmask(csvDocLineEntryMatrix[i][j]),
                            lineIterator.next());
      }
      Assert.assertEquals(false, lineIterator.hasNext());
    }

  }


}
