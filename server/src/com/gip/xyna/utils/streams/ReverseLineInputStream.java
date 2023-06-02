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



import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;



/**
 * ordnet die bytes des files so um, dass zeilenweise von hinten nach vorne gestreamed wird. zeilentrenner ist \n
 * 
 * funktioniert nicht für alle encodings, aber für UTF-8 funktioniert das.
 * file muss mit \n enden (oder leer sein).
 */
public class ReverseLineInputStream extends InputStream {

  /*
   *   bufferstart
   *       |
   *       v
   * ......[............\n
   * .............\n
   * ...\n
   * ...\n
   * A..x........]..........\nB
   *             ^
   *             |
   *         bufferend
   *         
   *    bufferstart (globaler index, darf negativ sein, wenn buffer den startbereich des files überschneidet)
   *    bufferend
   * x: currentPos (bufferrelativer index): von currentLineStart bis currentPos - 1 wurde bereits ausgelesen
   * A: currentLineStart (bufferrelativer index. erster index, der kein \n ist)
   * B: currentLineEnd (bufferrelativer index. falls index>buffer.length, dann endet die zeile nicht im buffer, sondern in (einem der/dem) nächsten)
   *    zeigt auf den index, der nach \n ist
   *    
   *    d.h. bei \n\n ist A=0, B=1 für das erste \n und A=1 und B=2 für das zweite
   *    
   */
  private int currentLineStart;
  private long currentLineEnd;
  private long currentPos;

  private long bufferStart;
  private final byte[] buffer;

  private final byte[] singleArr = new byte[1];

  private final RandomAccessFile f;


  public ReverseLineInputStream(File file, int buffersize) throws IOException {
    buffer = new byte[buffersize];
    currentLineEnd = -1;
    currentLineStart = 0;
    currentPos = 0;
    bufferStart = file.length();
    f = new RandomAccessFile(file, "r");
    if (f.length() > 0) {
      f.seek(f.length() - 1);
      if (f.read() != 0xA) {
        f.close();
        throw new RuntimeException("File '" + file.getAbsolutePath() + "' doesn't end with \\n");
      }
    }
  }


  //gehe von currentLineStart soweit zurück, bis ein zeilenumbruch oder start vom file gefunden wird, so dass dieser innerhalb des buffers liegt.
  private void findPrevLine() throws IOException {
    int c = currentLineStart - 2; //zeichen bei current-1 ist \n
    currentLineEnd = currentLineStart;
    if (c < 0) {
      readPreviousBuffer();
      c += buffer.length;
    }
    while (buffer[c] != 0xA) {
      c--;
      if (c < 0) {
        if (bufferStart <= 0) {
          //alle zeichen links vom filestart ignorieren
          c = (int) -bufferStart - 1;
          break;
        }
        readPreviousBuffer();
        c += buffer.length;
      }
    }
    if (bufferStart + c < 0) {
      c = (int) -bufferStart - 1;
    }
    currentLineStart = c + 1;
    currentPos = currentLineStart; //jetzt die nächste zeile von beginn aus auslesen
  }


  private void readPreviousBuffer() throws IOException {
    //den buffer nach vorne verschieben
    boolean startOfFileReached = bufferStart < buffer.length;
    if (startOfFileReached) {
      copyFromFile(0L, (int) bufferStart, buffer, (int) (buffer.length - bufferStart));
    } else {
      copyFromFile(bufferStart - buffer.length, buffer.length, buffer, 0);
    }
    bufferStart -= buffer.length; //kann negativ sein, schadet nix
    currentLineEnd += buffer.length;
  }


  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (len == 0) {
      return 0;
    }
    if (currentLineEnd == -1) {
      findPrevLine();
    }
    int read = 0;
    while (true) {
      int toEndOfLine = (int) (currentLineEnd - currentPos);
      if (toEndOfLine == 0) {
        if (read > 0) {
          return read;
        }
        return -1; //ist nichts mehr auszulesen
      }
      if (toEndOfLine > len) {
        copy(currentPos, b, off, len);
        currentPos += len;
        read += len;
        return read;
      }

      //es soll mindestens die gesamte aktuelle zeile fertig ausgelesen werden
      //erstmal die aktuelle zeile kopieren
      copy(currentPos, b, off, toEndOfLine);
      off += toEndOfLine;
      len -= toEndOfLine;
      read += toEndOfLine;
      if (bufferStart + currentPos <= 0) {
        currentPos = currentLineEnd;
        return read;
      }
      findPrevLine();
    }
  }


  //kopiere von relativer bufferposition "start" bis maximal zu currentLineEnd in das übergebene byte array
  private void copy(long start, byte[] b, int off, int len) throws IOException {
    long readFromBuffer = buffer.length - start;
    if (len <= readFromBuffer) {
      System.arraycopy(buffer, (int) start, b, off, len);
    } else {
      if (readFromBuffer > 0) {
        System.arraycopy(buffer, (int) start, b, off, (int) readFromBuffer);
        start += readFromBuffer;
        off += readFromBuffer;
        len -= readFromBuffer;
      }

      //rechts vom buffer den rest direkt aus dem file lesen
      copyFromFile(bufferStart + start, len, b, off);
    }
  }


  private void copyFromFile(long fileStart, int len, byte[] b, int off) throws IOException {
    f.seek(fileStart);
    int read = f.read(b, off, len);
    if (read < len) {
      throw new RuntimeException();
    }
  }


  public int read() throws IOException {
    int read = read(singleArr, 0, 1);
    if (read <= 0) {
      return -1;
    }
    return singleArr[0];
  }


  public void close() throws IOException {
    f.close();
  }
}
