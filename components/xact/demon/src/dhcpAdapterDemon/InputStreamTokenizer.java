/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * InputStreamTokenizer liest aus einem InputStream aus, zerlegt diesen in einzelne Abschnitte,
 * und gibt diese wie ein Iterator als String aus. 
 * 
 * Der InputStream wird dabei von {@link hasNext()} blockierend gelesen, bis die Zeichenkette 
 * {@code dataSuffix} gelesen wird. Die so erhaltenen Daten werden über {@link next()} dann
 * ausgegeben.
 * Dabei werden keine Zeichensatzkonvertierungen durchgeführt!
 *
 */
public class InputStreamTokenizer implements Iterable<String>, Iterator<String> {

  private InputStream inputStream;
  private String dataSuffix;
  private int capacity;
  private boolean eof;
  private String next;

  /**
   * @param inputStream zu lesender InputStream
   * @param dataSuffix Zeichenkette, die das Ende markiert
   * @param capacity geschätzte Länge des zu lesenden Strings, default 1000
   */
  public InputStreamTokenizer(InputStream inputStream, String dataSuffix, int capacity) {
    this.inputStream = inputStream;
    this.dataSuffix = dataSuffix == null? "\n" : dataSuffix;
    this.capacity = capacity <= 0? 1000: capacity;
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  public Iterator<String> iterator() {
    return this;
  }

  /**
   * Blockierendes Lesen aus dem InputStream.
   * 
   * Gelesen wird bis Zeichenkette {@code dataSuffix} erscheint oder ein EOF festgestellt wird.
   * @see java.util.Iterator#hasNext()
   */
  public boolean hasNext() {
    if( next != null ) {
      return true;
    }
    next = read();
    return next != null;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#next()
   */
  public String next() {
    String ret = next;
    next = null;
    return ret;
  }

  /**
   * Lesen aus dem InputStream
   * @return
   */
  private String read() {
    StringBuilder sb = new StringBuilder( capacity );
    int lastChar = dataSuffix.charAt(dataSuffix.length()-1);
    int c = -1;
    do {
      try {
        c = inputStream.read();
      } catch (IOException e) {
        throw new RuntimeException( e );
      }
      if( c == -1 ) {
        eof = true;
        break;
      }
      sb.append( (char) c );
      if( c == lastChar ) {
        if( sb.toString().endsWith(dataSuffix) ) {
          break;//gesuchte Daten sind gefunden
        }
      }
    } while( !eof );
    if( ! eof || sb.length() > 0 ) {
      return sb.toString();
    } else {
      return null; //keine Daten mehr verfügbar
    }
  }


  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  public void remove() {
    throw new UnsupportedOperationException();
  }

}