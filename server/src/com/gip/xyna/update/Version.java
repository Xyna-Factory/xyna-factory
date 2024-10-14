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

package com.gip.xyna.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Version {

  private static final Pattern VERSIONPART_PATTERN = Pattern.compile("(.*?)(0*)(\\d+)");
  private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");

  private String[] parts;
  
  public Version(String v) {
    parts = SPLIT_PATTERN.split(v);
  }
  
  public Version(int major, int minor, int sub, int dev) {
    this("" + major, "" + minor, "" + sub, "" + dev);
  }
  
  public Version(String major, String minor, String sub, String dev) {
    parts = new String[]{major, minor, sub, dev};
  }
  
  public Version(String[] parts) {
    this.parts = parts;
  }
  
  public Version(Version v) {
    this.parts = new String[v.parts.length];
    for (int i = 0; i<v.parts.length; i++) {
      this.parts[i] = v.parts[i];
    }
  }

  public final boolean isStrictlyGreaterThan(Version otherVersion) {
    if (otherVersion == null) {
      return true;
    }
    if (equals(otherVersion)) {
      //ohne diese abfrage muss die schleife vollständig durchlaufen werden um equals zu bestimmen.
      return false;
    }
    int len = Math.min(parts.length, otherVersion.parts.length);
    for (int i = 0; i < len; i++) {
      Matcher matThis = VERSIONPART_PATTERN.matcher(parts[i]);
      String prefixThis;
      String leadingZerosThis;
      int numberThis;
      Matcher matOther = VERSIONPART_PATTERN.matcher(otherVersion.parts[i]);
      String prefixOther;
      String leadingZerosOther;
      int numberOther;
      if (matThis.matches()) {
        String numberString = matThis.group(3);
        if (numberString.length() < 8) {
          prefixThis = matThis.group(1);
          numberThis = Integer.parseInt(numberString);
        } else {
          prefixThis = parts[i];
          numberThis = -1;
        }
        leadingZerosThis = matThis.group(2);
      } else {
        prefixThis = parts[i];
        numberThis = -1;
        leadingZerosThis = "";
      }
      if (matOther.matches()) {
        String numberString = matOther.group(3);
        if (numberString.length() < 8) {
          prefixOther = matOther.group(1);
          numberOther = Integer.parseInt(numberString);
        } else {
          prefixOther = otherVersion.parts[i];
          numberOther = -1;
        }
        leadingZerosOther = matOther.group(2);
      } else {
        prefixOther = otherVersion.parts[i];
        numberOther = -1;
        leadingZerosOther = "";
      }
      
      int compareResult = prefixThis.compareTo(prefixOther);
      if (compareResult < 0) {
        return false;
      } else if (compareResult > 0) {
        return true;
      }
      //else prefixes gleich.
      
      //comparison is done lexicographically and "0" precedes "00"
      //however, we need to swap this behavior to ensure that
      //v001 precedes v01
      if( (numberOther == 0) == (numberThis == 0)) {
        compareResult = -leadingZerosThis.compareTo(leadingZerosOther);
        if(compareResult < 0) {
          return false;
        } else if(compareResult > 0) {
          return true;
        }
      }
      //else leading zeros gleich. oder genau eine der nummern ist 0
      
      compareResult = numberThis - numberOther;
      if (compareResult < 0) {
        return false;
      } else if (compareResult > 0) {
        return true;
      }
      
      //else == => nächsten versions-part testen
    }

    //bisher gleich
    if (parts.length > otherVersion.parts.length) {
      return true;
    } else if (parts.length < otherVersion.parts.length) {
      return false;
    }
    
    //nicht equals und nicht kleiner oder größer...
    throw new RuntimeException("versions are not comparable");
  }
  
  public boolean isEqualOrGreaterThan(Version otherVersion) {
    return equals(otherVersion) || isStrictlyGreaterThan(otherVersion);
  }

  
  public String getPart(int index) {
    if (index >= parts.length) {
      return null;
    }
    return parts[index];
  }
  
  
  public String getString() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i<parts.length; i++) {
      if (i>0) {
        sb.append(".");
      }
      sb.append(parts[i]);
    }
    return sb.toString();
  }
  
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o instanceof Version) {
      return getString().equals(((Version)o).getString());
    }
    return false;
  }
  
  public int hashCode() {
    return 131 + getString().hashCode();
  }
  
  public int length() {
    return parts.length;
  }

  /**
   * ruft {@link #increaseToMajorVersion(int, int)} mit i, 1 auf.
   */
  public Version increaseToNextMajorVersion(int i) {
    return increaseToMajorVersion(i, 1);
  }

  /**
   * major version = version, die mit nullen endet.
   * 
   * erhöht die i-te versionsstelle um &lt;increment&gt; (bei 1 anfangen zu zählen). die darauffolgenden versionstellen werden auf 0 gesetzt.
   * zb für i=4, increment=1 aus 3.0.0.12 mache 3.0.0.13
   * oder aus 3.0.0.alpha9 mache 3.0.0.alpha10
   * oder aus 3.0.0.bla mache 3.0.0.bla0
   * 
   * zb für i = 2, increment = 3 aus 3.0.0.12 mache 3.3.0.0
   * @return geändertes versionsobjekt
   */
  public Version increaseToMajorVersion(int i, int increment) {
    increase(i, increment);
    for (int j = i; j<parts.length; j++) {
      parts[j] = "0";
    }
    return this;
  }
  
  private static String increase(String v, int increment) {
    Matcher mat = VERSIONPART_PATTERN.matcher(v);
    if (mat.matches()) {
      String prefix = mat.group(1);
      String numberString = mat.group(3);
      long number = Long.parseLong(numberString);
      return prefix + (number+increment);
    } else {
      return v + "0";
    }
  }

  /**
   * funktioniert wie {@link #increaseToMajorVersion(int, int)}, nur dass nicht alle darauffolgenden versionsstellen auf 0 gesetzt werden.
   * es wird also nur die angegebene stelle um den angegebenen wert erhöht.
   */
  public void increase(int index, int value) {
    if (index > parts.length) {
      throw new IndexOutOfBoundsException("Could not access " + index + ". part of version.");
    }
    parts[index-1] = increase(parts[index-1], value);
  }

}
