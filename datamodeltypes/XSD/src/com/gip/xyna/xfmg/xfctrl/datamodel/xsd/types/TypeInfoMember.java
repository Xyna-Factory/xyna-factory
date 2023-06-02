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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSSimpleTypeDefinition;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.RandomAccessArrayList;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.TypeInfoGenerator.MinMaxOccurs;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.XmomDataCreator;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;


/**
 * TypeInfoMember sammelt Informationen über die Membervariablen eines Datentyps.
 */
public class TypeInfoMember {
  
  private static Logger logger = CentralFactoryLogging.getLogger(TypeInfoMember.class);
  
  //Basisdaten für equals und hashCode
  private final FQName name;              //Name
  private final TypeInfo complexType;     //Typ: entweder XMOM-Type
  private final PrimitiveType simpleType; //     oder einfacher Java-Typ
  private final MemberType memberType;    //Element, Attribute, Text, Choice, Any
  
  //Basisdaten, nicht für equals und hashCode
  private final boolean optional;
  private final boolean qualified;  //Ausgabe im XML mit Namespace-Qualifizierung
  private final boolean list;
  
  //Erweiterungen, die nachträglich gesetzt werden und nicht in equals und hashCode verwendet werden dürfen
  private String varName;
  private String label;
  private int position;
  private List<Pair<FQName, TypeInfo>> choiceMember;
  
  private TypeInfoMember(FQName name, MemberType memberType, boolean optional, boolean qualified, boolean list, TypeInfo complexType, PrimitiveType simpleType) {
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
    private static SimpleTypes simpleTypes = new SimpleTypes();

    private FQName name;              //Name
    private TypeInfo complexType;     //Typ: entweder XMOM-Type
    private PrimitiveType simpleType; //     oder einfacher Java-Typ
    private MemberType memberType;    //Element, Attribute, Text, Choice, Any
    private boolean qualified;        //Ausgabe im XML mit Namespace-Qualifizierung
    private boolean optional;         //
    private boolean list;
    private int position;
    private String varName;
    private String label;
    private int simpleTypeDef = -1;
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
    
    public TypeInfoMemberBuilder list(boolean list) {
      this.list = list;
      return this;
    }
    
    public TypeInfoMemberBuilder occurs(MinMaxOccurs minMaxOccurs) {
      optional = minMaxOccurs.isOptional();
      list = minMaxOccurs.isList();
      return this;
    }
    
    public TypeInfoMemberBuilder complexType(TypeInfo complexType) {
      this.complexType = complexType;
      this.simpleType = null;
      this.simpleTypeDef = -1;
      return this;
    }
    
    public TypeInfoMemberBuilder simpleType(PrimitiveType primitiveType) {
      this.complexType = null;
      this.simpleType = primitiveType;
      this.simpleTypeDef = -1;
      return this;
    }
    
    public TypeInfoMemberBuilder simpleType(XSSimpleTypeDefinition simpleTypeDefinition) {
      this.complexType = null;
      this.simpleType = null;
      switch( simpleTypeDefinition.getVariety() ) {
        case XSSimpleTypeDefinition.VARIETY_ABSENT: //The variety is absent for the anySimpleType definition.
          this.simpleTypeDef = XSConstants.STRING_DT;
          logger.warn( "unimplemented variety LIST -> String for " +simpleTypeDefinition.getName());
          break;
        case XSSimpleTypeDefinition.VARIETY_ATOMIC: //Atomic type.
          this.simpleTypeDef = simpleTypeDefinition.getBuiltInKind();
          break;
        case XSSimpleTypeDefinition.VARIETY_LIST: //List type.
          this.simpleTypeDef = XSConstants.STRING_DT;
          logger.warn( "unimplemented variety LIST -> String for " +simpleTypeDefinition.getName());
          break;
        case XSSimpleTypeDefinition.VARIETY_UNION : //Union type.
          org.apache.xerces.xs.XSObjectList members = simpleTypeDefinition.getMemberTypes();
          if( members != null && members.getLength() != 0 ) {
            for( int i=0; i<members.getLength(); ++i ) {
              
              if( members.item(i) instanceof XSSimpleTypeDecl ) {
                XSSimpleTypeDecl std = (XSSimpleTypeDecl)members.item(i);
                if( i == 0 ) {
                  this.simpleTypeDef = std.getBuiltInKind();
                } else {
                  if( this.simpleTypeDef != std.getBuiltInKind() ) {
                    logger.warn( "variety UNION: "+members.item(i)+" has different types -> String for " +simpleTypeDefinition.getName());
                    this.simpleTypeDef = XSConstants.STRING_DT;
                  }
                }
              } else {
                logger.warn( "variety UNION: "+members.item(i)+" is no XSSimpleTypeDecl -> String for " +simpleTypeDefinition.getName());
                this.simpleTypeDef = XSConstants.STRING_DT;
                break;
              }
              
              
            }
          } else {
            this.simpleTypeDef = XSConstants.STRING_DT;
          }
          break;
        default:
          logger.warn( "unexpected variety "+simpleTypeDefinition.getVariety()+" -> String for " +simpleTypeDefinition.getName());
          break;
      }
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
      if( simpleTypeDef != -1 ) {
        simpleType = simpleTypes.get(simpleTypeDef, optional);
      }
      if( complexType == null && simpleType == null ) {
        //throw new IllegalArgumentException("simpleType or complexType must be set");// FIXME
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
    
    private static class SimpleTypes {
      
      private List<PrimitiveType> mandatoryTypes;
      private List<PrimitiveType> optionalTypes;

      public SimpleTypes() {
        mandatoryTypes = new RandomAccessArrayList<PrimitiveType>(45);
        optionalTypes = new RandomAccessArrayList<PrimitiveType>(45);
        //String
        add(XSConstants.STRING_DT, PrimitiveType.STRING, PrimitiveType.STRING);
        //Boolean
        add(XSConstants.BOOLEAN_DT, PrimitiveType.BOOLEAN, PrimitiveType.BOOLEAN_OBJ);
        //Integer
        add(XSConstants.BYTE_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.INT_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.INTEGER_DT, PrimitiveType.INT, PrimitiveType.INTEGER);  //FIXME keine Längenbeschränkung!
        add(XSConstants.SHORT_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.UNSIGNEDBYTE_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.NONNEGATIVEINTEGER_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.NONPOSITIVEINTEGER_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.NEGATIVEINTEGER_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        add(XSConstants.POSITIVEINTEGER_DT, PrimitiveType.INT, PrimitiveType.INTEGER);
        //Long
        add(XSConstants.LONG_DT, PrimitiveType.LONG, PrimitiveType.LONG_OBJ);
        add(XSConstants.UNSIGNEDINT_DT, PrimitiveType.LONG, PrimitiveType.LONG_OBJ);
        add(XSConstants.UNSIGNEDLONG_DT, PrimitiveType.LONG, PrimitiveType.LONG_OBJ);//TODO ok?
        //Double
        add(XSConstants.DECIMAL_DT, PrimitiveType.DOUBLE, PrimitiveType.DOUBLE_OBJ);
        add(XSConstants.DOUBLE_DT, PrimitiveType.DOUBLE, PrimitiveType.DOUBLE_OBJ);
        add(XSConstants.FLOAT_DT, PrimitiveType.DOUBLE, PrimitiveType.DOUBLE_OBJ);//TODO Float von Factory nicht unterstützt
        
      }
      
      private void add(int index, PrimitiveType mandatory, PrimitiveType optional) {
        mandatoryTypes.set(index, mandatory);
        optionalTypes.set(index, optional);
      }
      
      public PrimitiveType get(int index, boolean optional) {
        PrimitiveType pt = null;
        if( optional ) {
          pt = optionalTypes.get(index);
        } else {
          pt = mandatoryTypes.get(index);
        }
        if( pt == null ) {
          logger.warn("Substituting type String for type "+index );
          pt = PrimitiveType.STRING;
        }
        return pt;
      }
    }
    
  }
    
  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append("TypeInfoMember(").append(name.getName()).append(",");//.append(name.getNamespace());
    appendFullType(s);
    s.append(",type=").append(simpleType != null ? ("java_"+simpleType.getClassOfType()) : complexType);
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
      return simpleType.getClassOfType();
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
  
  public PrimitiveType getSimpleType() {
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
