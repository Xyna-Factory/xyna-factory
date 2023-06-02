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
package com.gip.xyna.xdev.map.types;

import java.util.HashSet;
import java.util.List;

import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.map.typegen.XmomDataCreator;


/**
 * TypeInfoMember sammelt Informationen über die Membervariablen eines Datentyps.
 */
public class TypeInfoMember {
  
  //Basisdaten für equals und hashCode
  private final FQName name;            //Name
  private final TypeInfo complexType;   //Typ: entweder XMOM-Type
  private final Class<?> simpleType;    //     oder einfacher Java-Typ
  private final MemberType memberType;  //Element, List, Attribute
  
  //Basisdaten, nicht für equals und hashCode
  private final boolean optional;
  private final boolean qualified;  //Ausgabe im XML mit Namespace-Qualifizierung
  private final boolean list;
  
  //Erweiterungen, die nachträglich gesetzt werden und nicht in equals und hashCode verwendet werden dürfen
  private String varName;
  private String label;
  private int position;
  private List<Pair<FQName, TypeInfo>> choiceMember;
  
  private TypeInfoMember(FQName name, MemberType memberType, boolean optional, boolean qualified, boolean list, TypeInfo complexType, Class<?> simpleType) {
    this.name = name;
    this.memberType = memberType;
    this.optional = optional;
    this.qualified = qualified;
    this.list = list;
    this.complexType = complexType;
    this.simpleType = simpleType;
  }

  public static TypeInfoMemberBuilder create() {
    return new TypeInfoMemberBuilder();
  }

  public static TypeInfoMemberBuilder create(FQName name, MemberType memberType) {
    return new TypeInfoMemberBuilder().name(name).memberType(memberType);
  }

  public void setVarName(String varName) {
    this.varName = varName;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public static class TypeInfoMemberBuilder {
    private FQName name;            //Name
    private TypeInfo complexType;   //Typ: entweder XMOM-Type
    private Class<?> simpleType;    //     oder einfacher Java-Typ
    private MemberType memberType;  //Element, Attribute, Text
    private boolean qualified;      //Ausgabe im XML mit Namespace-Qualifizierung
    private boolean optional;       //
    private boolean list;
    private int position;
    private String varName;
    private String label;
    private List<Pair<FQName, TypeInfo>> choice;
    
    public TypeInfoMemberBuilder name(FQName name) {
      this.name = name;
      return this;
    }
    public TypeInfoMemberBuilder memberType(MemberType memberType) {
      this.memberType = memberType;
      return this;
    }
    
    public TypeInfoMemberBuilder qualified(boolean qualified) {
      this.qualified = qualified;
      return this;
    }

    public TypeInfoMemberBuilder optional(boolean optional) {
      this.optional = optional;
      return this;
    }
    
    public TypeInfoMemberBuilder occurs(XSParticle particle) {
      if( particle.getMinOccurs() == 0 ) {
        optional = true;
      }
      if( particle.getMaxOccursUnbounded() || particle.getMaxOccurs() > 1) {
        list = true;
      }
      return this;
    }
    
    public TypeInfoMemberBuilder complexType(TypeInfo complexType) {
      this.complexType = complexType;
      this.simpleType = null;
      return this;
    }
    
    public TypeInfoMemberBuilder simpleType(Class<?> simpleType) {
      this.complexType = null;
      this.simpleType = simpleType;
      return this;
    }
    
    public TypeInfoMemberBuilder simpleType(XSSimpleTypeDefinition simpleTypeDefinition) {
      this.complexType = null;
      this.simpleType = getPrimTypeForType(simpleTypeDefinition);
      return this;
    }
    
    public TypeInfoMemberBuilder position(int position) {
      this.position = position;
      return this;
    }
    
    public TypeInfoMemberBuilder varName(String varName) {
      this.varName = varName;
      return this;
    }
    
    public TypeInfoMemberBuilder label(String label) {
      this.label = label;
      return this;
    }
    
    public TypeInfoMemberBuilder choice(List<Pair<FQName, TypeInfo>> choice) {
      this.choice = choice;
      return this;
    }

    public TypeInfoMember build() {
      if( complexType == null && simpleType == null ) {
        //throw new IllegalArgumentException("simpleType or complexType must be set"); FIXME
      }
      if( name == null ) {
        throw new IllegalArgumentException("name must not be null");
      }
      TypeInfoMember tim = new TypeInfoMember(name,memberType,optional,qualified,list,complexType,simpleType);
      tim.position = position;
      tim.varName = varName;
      tim.label = label;
      tim.choiceMember = choice;
      return tim;
    }
    
    private Class<?> getPrimTypeForType(XSSimpleTypeDefinition simpleTypeInfo) {
      int basicType = simpleTypeInfo.getBuiltInKind();
      if (XSConstants.BOOLEAN_DT == basicType) {
        return Boolean.class;
      } else if (XSConstants.BYTE_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.DECIMAL_DT == basicType) {
        return Double.class;
      } else if (XSConstants.DOUBLE_DT == basicType) {
        return Double.class;
      } else if (XSConstants.FLOAT_DT == basicType) {
        return Double.class; //TODO Float von Factory nicht unterstützt
      } else if (XSConstants.INT_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.INTEGER_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.LONG_DT == basicType) {
        return Long.class;
      } else if (XSConstants.SHORT_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.UNSIGNEDBYTE_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.UNSIGNEDINT_DT == basicType) {
        return Long.class;
      } else if (XSConstants.UNSIGNEDLONG_DT == basicType) {
        //TODO ok?
        return Long.class;
      } else if (XSConstants.UNSIGNEDLONG_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.NONNEGATIVEINTEGER_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.NONPOSITIVEINTEGER_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.NEGATIVEINTEGER_DT == basicType) {
        return Integer.class;
      } else if (XSConstants.POSITIVEINTEGER_DT == basicType) {
        return Integer.class;
      } else {
        return String.class;
      }
    }
    
  }
    
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("TypeInfoMember(").append(name.getName()).append(",");//.append(name.getNamespace());
    appendFullType(s);
    s.append(",type=").append(simpleType != null ? ("java_"+simpleType.getSimpleName()) : complexType);
    if( memberType == MemberType.Choice ) {
      s.append(",choice(");
      
      String defNS = name.getNamespace();
      String sep = "";
      
      for( Pair<FQName,TypeInfo> cm : choiceMember ) {
        s.append(sep).append(cm.getFirst().toOptionalQualifiedName(defNS))
         .append("->").append(cm.getSecond().getName().toOptionalQualifiedName(defNS));
        sep = ",";
      }
      s.append(")");
    }
    s.append(")");
    return s.toString();
  }

  public void appendFullType(StringBuilder sb) {
    String usage = memberType.usage(optional);
    if( usage != null ) {
      sb.append(usage).append(" ");
    }
    String form = memberType.form(qualified);
    if( form != null ) {
      sb.append(form).append(" ");
    }
    if( list ) {
      sb.append("List");
    } else {
      sb.append(memberType);
    }
  }

  
  public FQName getName() {
    return name;
  }
  
  
  public String getVarType() {
    if( simpleType != null ) {
      return simpleType.getSimpleName();
    } else {
      if( complexType != null ) {
        return complexType.getXmomType().getFQTypeName();
      } else {
        return "--";
      }
    }
  }

  public String getVarName() {
    return varName;
  }
  
  public String getLabel() {
    return label;
  }
  
  public boolean isComplex() {
    return complexType != null;
  }

  public boolean isSimple() {
    return simpleType != null;
  }

  public boolean isList() {
    return list;
  }
  
  public boolean isOptional() {
    return optional;
  }
  
  public Class<?> getSimpleType() {
    return simpleType;
  }
  
  public TypeInfo getComplexType() {
    return complexType;
  }

  public boolean isQualified() {
    return qualified;
  }
  
  public MemberType getMemberType() {
    return memberType;
  }
  
  public String getQualifiedNamespace() {
    return qualified ? name.getNamespace() : null;
  }

  public int getPosition() {
    return position;
  }
  
  public List<Pair<FQName, TypeInfo>> getChoiceMember() {
    return choiceMember;
  }

  public void setPosition(int position) {
    this.position = position;
  }
 
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((complexType == null) ? 0 : complexType.getName().hashCode()); //hier für rekursive typeInfos keine Endlosschleife!
    result = prime * result + ((memberType == null) ? 0 : memberType.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((simpleType == null) ? 0 : simpleType.hashCode());
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
    TypeInfoMember other = (TypeInfoMember) obj;
    if (complexType == null) {
      if (other.complexType != null)
        return false;
    } else if (!complexType.equals(other.complexType))
      return false;
    if (memberType != other.memberType)
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (simpleType == null) {
      if (other.simpleType != null)
        return false;
    } else if (!simpleType.equals(other.simpleType))
      return false;
    return true;
  }

  public void createVarNameAndLabel(HashSet<String> allVarNamesForType, XmomDataCreator xmomDataCreator) {
    this.varName = xmomDataCreator.createVarName(allVarNamesForType, this);
    this.label = xmomDataCreator.createLabel(this);
  }

}
