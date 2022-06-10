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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;



/**
 *
 */
public class PeekInputStreamTest {

  
  public static void main(String[] args) throws IOException {
    
    //test1();
    
    test2();
    
    
  }

  private static void test1() throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream( "Lalala hallihallo".getBytes() );
    
    PeekInputStream pis = new PeekInputStream( bais, 4 );
    
    System.out.println( "peeked " + Arrays.toString(pis.peek()) );
    
    System.out.println( "read "+  pis.read() );
    System.out.println( "read "+  pis.read() );
    System.out.println( "peeked " + Arrays.toString(pis.peek()) );
    
    
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    StreamUtils.copy(pis, baos);
    System.out.println( new String(baos.toByteArray()) );

  }
  
  private static void test2() throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream( "Lalala hallihallo".getBytes() );
    ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
    ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
    
    TeeInputStream tis1 = new TeeInputStream(bais, baos1);
    PeekInputStream pis = new PeekInputStream( tis1, 4 );
    TeeInputStream tis2 = new TeeInputStream(pis, baos2);
    
    System.out.println( "peeked " + Arrays.toString(pis.peek()) );
    
    System.out.println( "read "+  tis2.read() );
    System.out.println( "read "+  tis2.read() );
    System.out.println( "peeked " + Arrays.toString(pis.peek()) );
    
    
    
    ByteArrayOutputStream baos3 = new ByteArrayOutputStream();
    
    StreamUtils.copy(tis2, baos3);
    System.out.println( new String(baos3.toByteArray()) );
    
    System.out.println( new String(baos1.toByteArray()) );
    System.out.println( new String(baos2.toByteArray()) );

  }

}
