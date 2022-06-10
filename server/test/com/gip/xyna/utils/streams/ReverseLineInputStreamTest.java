/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.utils.streams;



import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xfmg.Constants;

import junit.framework.TestCase;



public class ReverseLineInputStreamTest extends TestCase {


  public void testExceptionWhenFileDoesntEndWithLinebreak() {
    createFile("testfile", "a\nb");
    try {
      List<String> list = parse("testfile");
      fail("expected failure");
    } catch (RuntimeException e) {

    }
  }


  private void createFile(String filename, String content) {
    try {
      FileUtils.writeStringToFile(content, new File(filename));
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  private List<String> parse(String filename) {
    return parse(filename, 1024 * 1024);
  }


  private List<String> parse(String filename, int buffersize) {
    List<String> lines = new ArrayList<>();
    try (BufferedReader r =
        new BufferedReader(new InputStreamReader(new ReverseLineInputStream(new File(filename), buffersize), Constants.DEFAULT_ENCODING),
                           buffersize)) {
      String line;
      while (null != (line = r.readLine())) {
        lines.add(line);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return lines;
  }


  private void compare(List<String> foundContentReverse, String expectedContentForward) {
    StringBuilder sb = new StringBuilder();
    for (int i = foundContentReverse.size() - 1; i >= 0; i--) {
      sb.append(foundContentReverse.get(i)).append("\n");
    }
    assertEquals(expectedContentForward, sb.toString());
  }


  public void testEmpty() {
    createFile("testfile", "\n");
    List<String> list = parse("testfile");
    compare(list, "\n");
  }


  public void testEmptyLine() {
    createFile("testfile", "\n\n");
    List<String> list = parse("testfile");
    compare(list, "\n\n");
  }


  public void testLongLine() {
    String source = "asdasdasdadweqwfeaergaerhaerghaergaergheraerbaerbaergaergeargaergfWEFSD\n";
    createFile("testfile", source);
    List<String> list = parse("testfile", 3);
    compare(list, source);
  }


  public void testManyLines() {
    String source = "\nasdas\ndasdadweqaü\n\näßdq    -\r\n\r\tfeaergaerh\naergha\nergaergheraerbaerbaergaergeargaergfWEFSD\n";
    createFile("testfile", source);
    List<String> list = parse("testfile", 5);
    compare(list, source.replace("\r\n", "\n").replace("\r\tfeaergaerh", "\tfeaergaerh\n")); //bufferedreader.readline sieht \r als zeilenumbruch
  }


  public void testPerformance() throws IOException, Ex_FileWriteException {
    File file = new File("testfile");
    createFile(file, 50 * 1024 * 1024, new Random());
    long t = System.currentTimeMillis();
    try (BufferedReader r = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file, 1024 * 1024)), 1024 * 1024)) {
      String line;
      int cnt = 0;
      while (null != (line = r.readLine())) {
        cnt++;
      }
      long diff = (System.currentTimeMillis() - t);
      assertTrue(diff < 10000);
      System.out.println(cnt + " lines in " + diff + " ms");
    }
  }


  private void createFile(File file, int size, Random rand) {
    StringBuilder sb = new StringBuilder();

    for (int i = 0; i < size; i++) {
      if (rand.nextDouble() < 0.03) {
        sb.append("\n");
      } else if (rand.nextBoolean()) {
        sb.append("a");
      } else {
        sb.append("b");
      }
    }
    sb.append("\n");
    //  System.out.println("file: " + sb.toString());
    try {
      FileUtils.writeStringToFile(sb.toString(), file);
    } catch (Ex_FileWriteException e) {
      throw new RuntimeException(e);
    }
  }


  public void testFileIdentity() throws IOException, Ex_FileWriteException {
    for (int k = 0; k < 1000; k++) {
      File file = new File("testfile");
      long seed = k + 12414423621L;
      Random rand = new Random(seed);
      createFile(file, rand.nextInt(1000), rand);

      List<String> lines = new ArrayList<>();
      try (BufferedReader r = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(file, 64)), 64)) {
        String line;
        while (null != (line = r.readLine())) {
          //   System.out.println("'" + line + "'");
          lines.add(line);
        }
      }

      try (BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
        String line;
        int cnt = lines.size() - 1;
        while (null != (line = r.readLine())) {
          //   System.out.println("read '" + line + "', expecting " + lines.get(cnt));
          if (!line.equals(lines.get(cnt))) {
            System.out.println("seed: " + seed);
            throw new RuntimeException("");
          }
          cnt--;
        }
        if (cnt >= 0) {
          System.out.println("seed: " + seed);
          throw new RuntimeException();
        }
      }
    }
  }

}
