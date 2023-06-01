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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import com.gip.xyna.utils.collections.trees.SimpleTreeSet.TreeElement;


/**
 *
 */
public class OID implements TreeElement<OID>, Comparable<OID> {

  private String oid;
  int[] oidInts;

  public OID(String oid) {
    this.oid = oid;
    this.oidInts = splitToInts(oid);
  }

  private int[] splitToInts(String oid) {
    String[] parts = oid.trim().split("\\."); 
    int[] ints = new int[parts.length];
    for( int p=0; p<parts.length; ++p ) {
      ints[p] = Integer.parseInt(parts[p]);
    }
    return ints;
  }

  public boolean hasChild(OID possibleChild) {
    int min = oidInts.length;
    if( possibleChild.oidInts.length < min ) {
      return false; //Child hat längere OID
    }
    for( int i=0; i<min; ++i ) {
      if( oidInts[i] != possibleChild.oidInts[i] ) {
        return false; //muss gleich sein
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return oid;
  }


  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((oid == null) ? 0 : oid.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    OID other = (OID) obj;
    if (oid == null) {
      if (other.oid != null)
        return false;
    } else if (!oid.equals(other.oid))
      return false;
    return true;
  }

  public int compareTo(OID o) {
    int min = o.oidInts.length;
    if( oidInts.length < min ) {
      min = oidInts.length;
    }
    for( int i=0; i<min; ++i ) {
      int d = oidInts[i] - o.oidInts[i];
      if( d != 0 ) {
        return d;
      }
    }
    return 0;
  }

}
