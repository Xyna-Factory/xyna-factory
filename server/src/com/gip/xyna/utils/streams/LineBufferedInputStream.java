/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * LineBufferedInputStream ist ein InputStream, der die readLine-Funktionalit�t von 
 * BufferedReader anbietet.
 * 
 * Damit ersetzt <code>new LineBufferedInputStream(in, LineMarker.LF, encoding);</code>  
 * die Zeile <code>new BufferedReader(new InputStreamReader(in, encoding);</code>
 * mit dem Vorteil, dass der InputStream nicht zu weit ausgelesen wird: der InputStreamReader 
 * liest einen gro�en Buffer voll, so dass diese Daten nicht mehr �ber den InputStream 
 * gelesen werden k�nnen.
 * 
 * Als Zeilentrenner k�nnen beliebige Zeichenketten dienen.
 * Leider ist das Verhalten etwas anders als beim BufferedReader: Die verschiedenen Zeilentrenner
 * {"\n","\r","\r\n"} k�nnen nicht gemischt erkannt werden.
 * 
 */
public class LineBufferedInputStream extends InputStream {

  public enum LineMarker {
    LF('\n'),
    CRLF('\r','\n');

    private byte[] bytes;
    private LineMarker(char ... cs) {
      bytes = new byte[cs.length];
      for( int i=0; i<cs.length; ++i) {
        bytes[i] = (byte)cs[i];
      }
    }
    
    public byte[] getBytes() {
      return bytes;
    }
    
  }
  
  private InputStream bufferedIn;
  private byte[] lineMarker;
  private String encoding;
  
  public LineBufferedInputStream(InputStream in, byte[] lineMarker) {
    this(in, lineMarker, null, true);
  }
  
  public LineBufferedInputStream(InputStream in, LineMarker lineMarker) {
    this(in, lineMarker.getBytes() );
  }
  
  public LineBufferedInputStream(InputStream in, LineMarker lineMarker, String encoding) {
    this(in, lineMarker.getBytes(), encoding, true);
  }

  public LineBufferedInputStream(InputStream in, byte[] lineMarker, String encoding) {
    this(in, lineMarker, encoding, true);
  }

  public LineBufferedInputStream(InputStream in, byte[] lineMarker, String encoding, boolean buffer) {
    this.bufferedIn = buffer ? new BufferedInputStream(in) : in;
    this.lineMarker = lineMarker;
    this.encoding = encoding;
  }

  @Override
  public int read() throws IOException {
    return bufferedIn.read();
  }

  @Override
  public int read(byte b[], int off, int len) throws IOException {
    return bufferedIn.read(b, off, len);
  }

  public ByteArrayOutputStream readLineAsByteArrayOutputStream() throws IOException {
    int length = lineMarker.length;
    int[] reverseBuffer = new int[length];
    int[] possibleMarker = new int[length];
    
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    
    int possibleMarkerPos = 0;
    boolean eof = false;
    int bufferPos = -1;
    while( true ) {
      int r;
      if( bufferPos == -1 ) {
        r = bufferedIn.read();
        if( r == -1 ) {
          eof = true;
          break; //Abbruch, da EOF erreicht
        }
      } else {
        r = reverseBuffer[bufferPos];
        --bufferPos;
      }
      //n�chstes Zeichen ist gelesen
      if( r == lineMarker[possibleMarkerPos] ) {
        possibleMarker[possibleMarkerPos] = r;
        ++possibleMarkerPos;
        if( possibleMarkerPos == length) {
          break; //Abbruch, da LineMarker gelesen
        }
      } else {
        if( possibleMarkerPos == 0 ) {
          //kein LineMarker
          baos.write(r);
        } else {
          //PossibleMarker ist unterbrochen, daher erstes Zeichen ausgeben 
          baos.write(possibleMarker[0]);
          //und Rest aus possibleMarker und r in den n�chsten Schleifen pr�fen
          reverseBuffer[++bufferPos] = r;
          for( int p=possibleMarkerPos-1; p>0; --p ) {
            reverseBuffer[++bufferPos] = possibleMarker[p];
          }
          possibleMarkerPos = 0;
        }
      }
    }
    if( eof && baos.size() == 0 ) {
      return null;
    }
    return baos;
  }
  
  public String readLine() throws IOException {
    ByteArrayOutputStream baos = readLineAsByteArrayOutputStream();
    if( baos != null ) {
      return encoding == null ? baos.toString() : baos.toString(encoding);
    } else {
      return null;
    }
  }
  
  public byte[] readLineAsBytes() throws IOException {
    ByteArrayOutputStream baos = readLineAsByteArrayOutputStream();
    if( baos != null ) {
      return baos.toByteArray();
    } else {
      return null;
    }
  }
    
}
