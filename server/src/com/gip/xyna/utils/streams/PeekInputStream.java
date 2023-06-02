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
package com.gip.xyna.utils.streams;

import java.io.IOException;
import java.io.InputStream;


/**
 *
 */
public class PeekInputStream extends InputStream {


  private InputStream in;
  private int[] peek;
  private int peekPos; //Position, ab der gepeekte Daten in peek liegen
  private boolean peeked;
  
  public PeekInputStream(InputStream in, int peekSize) {
    this.in = in;
    this.peek = new int[peekSize];
    this.peekPos = peekSize;
    this.peeked = false;
  }
  
  public int[] peek() throws IOException {
    int p =0; //nächster zu füllender Platz in peek
    if( peekPos > 0 ) {
      //bisherige Peek-Daten verschieben 
      for( ; peekPos< peek.length; ++p, ++peekPos) {
        peek[p] = peek[peekPos];
      }
    }
    //Lesen der Daten aus in
    boolean eof = false;
    for( ; p<peek.length; ++p ) {
      int r = in.read();
      peek[p]=r;
      if( r == -1 ) {
        eof = true;
        break;
      }
    }
    if( eof ) {
      //nicht mehr lesbare Peek-Daten mit -1 füllen
      for( ; p<peek.length; ++p ) {
        peek[p] = -1;
      }
    }
    //nächstes lesen findet Daten in peek
    peekPos = 0;
    peeked = true;
    return peek;
  }

  
  @Override
  public int read() throws IOException {
    if( peeked ) {
      int r = peek[peekPos];
      peek[peekPos] = -1;
      ++peekPos;
      peeked = peekPos < peek.length;
      return r;
    } else {
      return in.read();
    }
  }

}
