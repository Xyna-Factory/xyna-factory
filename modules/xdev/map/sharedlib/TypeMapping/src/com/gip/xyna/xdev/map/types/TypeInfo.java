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
package com.gip.xyna.xdev.map.types;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 * TypeInfoMember sammelt Informationen �ber einen Datentyps.
 */
public class TypeInfo {
  
  //Basisdaten f�r equals und hashCode
  private final FQName name;      //Namespace und Name
  private TypeInfo baseTypeInfo;  //TypeInfo des Parents
  private final Type type;        //Simple, Complex, Anonymous
  private final List<TypeInfoMember> members = new ArrayList<TypeInfoMember>(); //Membervariablen
  
  //Erweiterungen, die nachtr�glich gesetzt werden und nicht in equals und hashCode verwendet werden d�rfen
  private XmomType xmomType; //Umsetzung im XMOM-Pfade, Namen und Labels
  private final List<FQName> rootElements = new ArrayList<FQName>(); //Type wird von diesen Root-Elementen verwendet  
  
  public enum Type {
    Simple, Complex, Anonymous;
  }
  
  
  public TypeInfo(Type type, FQName name) {
    this.type = type;
    this.name = name;
  }

  public void setBaseType(TypeInfo baseTypeInfo) {
    this.baseTypeInfo = baseTypeInfo;
  }

  public void setXmomType(XmomType xmomType) {
    if( xmomType == null ) {
      throw new IllegalArgumentException("xmomType must not be empty");
    }
    this.xmomType = xmomType;
  }
  
  public void addMember(TypeInfoMember member) {
    member.setPosition(members.size());
    members.add( member );
  }
  
  public void addMemberAtPosition(TypeInfoMember tim, int position) {
    for( int i=members.size(); i<position+1; ++i ) {
      members.add(null); //TODO RandomAccessArrayList...
    }
    members.set( position, tim );
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("TypeInfo(");
    s.append(type).append(",");
    if( name != null ) {
      s.append(name.getNamespace()).append(",").append(name.getName());
    } else {
      s.append("-,-");
    }
    if( baseTypeInfo != null ) {
      s.append(",base=").append(baseTypeInfo);
    }
    if( isTypeUsedByRootElement() ) {
      s.append(",root");
    }
    s.append(")");
    return s.toString();
  }
  
  public FQName getName() {
    return name;
  }

  public List<TypeInfoMember> getMembers() {
    return members;
  }
  
  public XmomType getXmomType() {
    return xmomType;
  }

  public TypeInfo getBaseTypeInfo() {
    return baseTypeInfo;
  }
  
  public boolean isComplex() {
    return type == Type.Complex;
  }
  public boolean isSimple() {
    return type == Type.Simple;
  }
  public boolean isAnonymous() {
    return type == Type.Anonymous;
  }
  
  public boolean hasType(Type type) {
    return this.type == type;
  }

  public TypeInfo getBaseType() {
    return baseTypeInfo;
  }

  public boolean hasBaseType() {
    return baseTypeInfo != null;
  }

  public boolean isTypeUsedByRootElement() {
    return ! rootElements.isEmpty();
  }

  public void addRootElement(FQName fqName) {
    rootElements.add(fqName);
  }

  public List<FQName> getRootElements() {
    return rootElements;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((baseTypeInfo == null) ? 0 : baseTypeInfo.hashCode());
    result = prime * result + ((members == null) ? 0 : members.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  /** 
   * equals bezieht sich nur auf die Basisdaten {baseTypeInfo, members, name, type},
   * hashCode ebenso. 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TypeInfo other = (TypeInfo) obj;
    if (baseTypeInfo == null) {
      if (other.baseTypeInfo != null)
        return false;
    } else if (!baseTypeInfo.equals(other.baseTypeInfo))
      return false;
    if (members == null) {
      if (other.members != null)
        return false;
    } else if (!members.equals(other.members))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (type != other.type)
      return false;
    return true;
  }
  
  /**
   * Liefert eine gemeinsamen BasisTyp b, so dass gilt:
   * t1{.getBaseType()}^n == b und t2{.getBaseType()}^m, nmit n, m aus [0,1,...[
   * @param t1
   * @param t2
   * @return gemeinsamer Basistyp oder null, wenn keiner existiert oder t1, t2 null sind
   */
  public static TypeInfo commonBase( TypeInfo t1, TypeInfo t2) {
    if( t1 == null || t2 == null ) {
      return null;
    }
    TypeInfo tp1 = t1;
    while( tp1 != null ) {
      TypeInfo tp2 = t2;
      while( tp2 != null ) {
        if( tp1.equals(tp2) ) {
          return tp1;
        }
        tp2 = tp2.getBaseType();
      }
      tp1 = tp1.getBaseType();
    }
    return null;
  }

  
  
}
