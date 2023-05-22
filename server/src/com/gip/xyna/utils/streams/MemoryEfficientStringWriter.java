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



import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



/**
 * stringwriter/-buffer expandieren das interne character array immer um faktor 2. dadurch hat man bei gro�en strings ggf.
 * einen sehr hohen overhead. (kurzfristig faktor 3, um die daten in das neue array zu �bertragen).
 *
 * diese implementierung hat als overhead nur die l�nge des gr��ten in den writer geschriebenen teilstrings, und kurzfristig faktor 2 (bei getString()).
 */
public class MemoryEfficientStringWriter extends Writer {

  private static final Logger logger = CentralFactoryLogging.getLogger(MemoryEfficientStringWriter.class);

  private static final Constructor<String> easyStringConstructor;
  private static final Field stringbuildervalue;
  private static final Field stringbuildercoder;
  

  static {
    
    Field f = null;
    try {
      f = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("value");
      f.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
      logger.warn("could not access stringbuilder fields", e);
    }
    stringbuildervalue = f;
    
    f = null;
    try {
      f = Class.forName("java.lang.AbstractStringBuilder").getDeclaredField("coder");
      f.setAccessible(true);
    } catch (NoSuchFieldException | SecurityException | ClassNotFoundException e) {
      logger.warn("could not access stringbuilder fields", e);
    }
    stringbuildercoder = f;

    Constructor<String> constr = null;
    try {
      constr = String.class.getDeclaredConstructor(byte[].class, byte.class);
      constr.setAccessible(true);
    } catch (NoSuchMethodException e) {
      logger.warn("could not find string constructor (byte[], byte)", e);
    } catch (SecurityException e) {
      logger.warn("could not access string constructor (byte[], byte)", e);
    }
    easyStringConstructor = constr;
  }

  private static final int INCOMPLETE_MAX_LEN = 8 * 1024;

  private final List<String> parts = new ArrayList<String>();
  private final StringBuilder incomplete = new StringBuilder(INCOMPLETE_MAX_LEN);
  private int len = 0;


  @Override
  public void write(char[] cbuf, int off, int len) {
    checkIncomplete();
    parts.add(String.valueOf(cbuf, off, len));
    this.len += len;
  }


  private void checkIncomplete() {
    if (incomplete.length() > 0) {
      addIncomplete();
    }
  }


  @Override
  public void flush() {
  }


  @Override
  public void close() {
  }


  @Override
  public void write(int c) {
    if (incomplete.length() >= INCOMPLETE_MAX_LEN - 1) {
      addIncomplete();
    }
    incomplete.append((char) c);
  }


  private void addIncomplete() {
    parts.add(incomplete.toString());
    len += incomplete.length();
    incomplete.setLength(0);
  }


  @Override
  public void write(char[] cbuf) {
    checkIncomplete();
    parts.add(String.valueOf(cbuf));
    len += cbuf.length;
  }


  @Override
  public void write(String str) {
    checkIncomplete();
    parts.add(str);
    len += str.length();
  }


  @Override
  public void write(String str, int off, int len) {
    char[] cbuf = new char[len];
    str.getChars(off, off + len, cbuf, 0);
    write(cbuf, 0, len);
  }


  /**
   * berechnet den gesamtstring. danach ist der writer geleert.
   */
  public String getString() {
    checkIncomplete();
    if (len == 0) {
      return "";
    }
    //idee: erzeuge gro�es bytearray f�r den konktatenierten zielstring.    
    
    StringBuilder sb = new StringBuilder(len); //erzeugt intern das gro�e ziel byte-array
    int l = parts.size();
    for (int i = 0; i<l; i++) {
      String s = parts.get(i);
      parts.set(i, null); //speicherschonung
      sb.append(s);
    }

    //StringBuilder.toString erstellt eine zus�tzliche kopie des arrays. das will man sich sparen...
    //es w�re eine methode StringBuilder.toStringAndDestroy() n�tig, die m�sste dann keine defensive bytearray-kopie erzeugen.
    String ret = null;
    if (easyStringConstructor != null) {
      try {
        sb.setLength(0);
        ret = easyStringConstructor.newInstance(stringbuildervalue.get(sb), stringbuildercoder.get(sb));
      } catch (Exception e) {
        logger.warn(null, e);
        ret = null;
      }
    }
    len = 0; //erneutes getString soll leeren string zur�ckgeben

    if (ret == null) {
      ret = sb.toString();
      sb.setLength(0);
    }
    return ret;
  }

}
