/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.TypeMappingEntry;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo.Type;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember.TypeInfoMemberBuilder;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.UnsupportedJavaTypeException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 * (bijektives (!!)) mapping von typen:
 *
 * (A)            <XSDTypeName>                        <--->          <XynaObject FQClassName>
 *
 *
 * XSDElement/Attribute-Namen in der Form:
 * (B) <namespace>:<XSDTypeName>:<MemberVar-/Attribute-Name>:('a'|'e')    <------>       <XynaObject FQClassName>:<MemberVarName>
 *
 *
 * ausserdem der root-elementname in der Form (damit man xyna objekte wieder nach xml serialisieren kann
 * braucht man initial einen element-namen, der nicht notwendigerweise gleich dem typenamen sein muss):
 * (C)       <namespace>:<RootElementName>:'_root'           <------>            <XynaObject FQClassName>:'_root'
 *
 *
 * für toXML benötigt man die reihenfolge der elemente und die information, ob man den wert
 * als attribut oder element schreiben muss. dazu gibt es noch folgendes mapping:
 * (D)      <namespace>:<XSDTypeName>:<MemberVar-/Attribute-Name>:<index>('a'|'e')     <------->      <XynaObject FQClassName>:<MemberVarName>:'xml'
 *   dabei steht a für attribute, e für element.
 */
public class TypeMappingEntryHelper {

  private static Logger logger = CentralFactoryLogging.getLogger(TypeMappingEntryHelper.class);
  private static IDGenerator idGenerator = new IDGeneratorInFactory();
  private String idForTypeMapping;

  public TypeMappingEntryHelper(String idForTypeMapping) {
    this.idForTypeMapping = idForTypeMapping;
  }
  
  public interface IDGenerator {
    public long getId();
  }
  public static class IDGeneratorInFactory implements IDGenerator {
    com.gip.xyna.idgeneration.IDGenerator idgen;
    
    public long getId() {
      if( idgen == null ) {
        try {
          idgen = com.gip.xyna.idgeneration.IDGenerator.getInstance();
        } catch (XynaException e) {
          throw new RuntimeException(e);
        }
      }
      return idgen.getUniqueId();
    }
    
  }
  
  public static void setIdGenerator(IDGenerator idGenerator) {
    TypeMappingEntryHelper.idGenerator = idGenerator;
  }

  public List<TypeMappingEntry> toTypeMappingEntries(TypeInfo typeInfo) {
    List<TypeMappingEntry> typeMappings = new ArrayList<TypeMappingEntry>(); 
    
    String keyA = typeInfo.getName().getNamespace()+":"+typeInfo.getName().getName();
    String valueA = javaNameFor(typeInfo);
    
    typeMappings.add( new TypeMappingEntry(idGenerator.getId(), idForTypeMapping, keyA, valueA) );

    if( typeInfo.isTypeUsedByRootElement() ) {
      String valueC = valueA+":_root";
      for( FQName root : typeInfo.getRootElements() ) {
        String keyC = root.getNamespace()+":"+root.getName()+":_root";
        typeMappings.add( new TypeMappingEntry(idGenerator.getId(), idForTypeMapping, keyC, valueC) );
      }
    }
    
    for (TypeInfoMember memberInfo : typeInfo.getMembers()) {
      String aetc = null;
      switch( memberInfo.getMemberType() ) {
        case Element: aetc = "e"; break;
        case Attribute: aetc = "a"; break;
        case Text: aetc = "t"; break;
        case Choice: aetc = "c"; break;
        case Any: aetc = "x"; break;
      }
      if( memberInfo.getMemberType() == MemberType.Choice ) {
        for( Pair<FQName,TypeInfo> cm : memberInfo.getChoiceMember() ) {
          typeMappings.add( createB( keyA, valueA, cm.getFirst(), aetc, memberInfo.getVarName(), javaNameFor(cm.getSecond()) ) );
        }
        typeMappings.add( createB( keyA, valueA, memberInfo.getName(), aetc, memberInfo.getVarName(), memberInfo.getVarType() ) );
        typeMappings.add( createD( keyA, valueA, aetc, memberInfo, typeInfo.getName() ) );
      } else {
        typeMappings.add( createB( keyA, valueA, memberInfo.getName(), aetc, memberInfo.getVarName(), memberInfo.getVarType() ) );
        typeMappings.add( createD( keyA, valueA, aetc, memberInfo, typeInfo.getName() ) );
      }
    }
    return typeMappings;
  }
  private String javaNameFor(TypeInfo typeInfo) {
    try {
      return GenerationBase.transformNameForJava(typeInfo.getXmomType().getFQTypeName());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }

  private TypeMappingEntry createB(String keyA, String valueA, FQName xmlName, String aetc, String varName, String varType) {
    String keyB = colonSeparated( keyA, 
                                  xmlName.getName(), 
                                  aetc );
    
    String valueB = colonSeparated( valueA, varName, varType );
    return new TypeMappingEntry(idGenerator.getId(), idForTypeMapping, keyB, valueB);
  }
  
  private TypeMappingEntry createD(String keyA, String valueA, String aetc, TypeInfoMember memberInfo, FQName typeName) {
    String namespace = null;
    if( ! namespaceEquals( memberInfo.getName(), typeName ) ) {
      namespace = "namespace="+memberInfo.getName().getNamespace();
    }
    String keyD = colonSeparated( keyA, 
                                  memberInfo.getName().getName(), 
                                  memberInfo.getPosition()+aetc,
                                  memberInfo.getMemberType().usage(memberInfo.isOptional()),
                                  memberInfo.getMemberType().form(memberInfo.isQualified()),
                                  namespace
        );
    String valueD = colonSeparated( valueA, memberInfo.getVarName(), "xml" );
    
    return new TypeMappingEntry(idGenerator.getId(), idForTypeMapping, keyD, valueD);
    
  }


  private String colonSeparated(String ... values) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for( String v : values ) {
      if( v != null ) {
        sb.append(sep).append(v);
        sep = ":";
      }
    }
    return sb.toString();
  }

  private boolean namespaceEquals(FQName name1, FQName name2) {
    String ns1 = name1.getNamespace();
    String ns2 = name2.getNamespace();
    if( ns1 == null ) {
      return ns2 == null;
    } else {
      return ns1.equals(ns2);
    }
  }

  public List<TypeInfo> importTypeMappingEntries(Collection<TypeMappingEntry> typeMappingEntries) {
    Map<String, ArrayList<TypeMappingEntry>> grouped = 
        CollectionUtils.group(typeMappingEntries, new TypeMappingEntryToFqClassName() );

    List<TypeInfo> typeInfos = new ArrayList<TypeInfo>();
    for( ArrayList<TypeMappingEntry> tmes : grouped.values() ) {
      typeInfos.add( toTypeInfo(tmes) );
    }
    return typeInfos;
  }
  
  private static class TypeMappingEntryToFqClassName implements CollectionUtils.Transformation<TypeMappingEntry, String> {

    public String transform(TypeMappingEntry tme) {
      String fqClassName = tme.getValue();
      int idx = fqClassName.indexOf(':');
      if( idx < 0 )  {
        return fqClassName;
      } else {
        return fqClassName.substring(0,idx);
      }
    }
    
  }


  /**
   * Wiederherstellen eines TypeInfo aus den typeMappingEntries
   * 
   * TODO diese Wiederherstellung gelingt nur teilweise, da wichtige Daten nicht in den TypeMappingEntries 
   * gehalten werden, sondern aus den XynaObjecten stammen müssen.
   *
   * @param typeMappingEntries
   * @return
   */
  public TypeInfo toTypeInfo(List<TypeMappingEntry> typeMappingEntries) {
    List<Pair<String,String>> xmlData = new ArrayList<Pair<String,String>>();
    Map<String,String> typeData = new HashMap<String,String>();
    Map<String,List<Pair<String,String>>> choiceData = new HashMap<String,List<Pair<String,String>>>();
    
    FQName name = null;
    List<FQName> rootElements = null;
    XmomType xmomType = null;
      
    for( TypeMappingEntry tme : typeMappingEntries ) {
      String[] valueParts = tme.getValue().split(":");
      
      if( valueParts.length == 1 ) {
        //Type-Definition
        name = getTypeName(tme.getKeyv());
        //System.err.println(tme.getValue() );
        xmomType = XmomType.ofFQTypeName(tme.getValue());
      } else if( "_root".equals(valueParts[valueParts.length-1] ) ) {
        //Root-Element
        FQName rootName = getRootElementName( tme.getKeyv() );
        if( rootElements == null ) {
          rootElements = new ArrayList<FQName>();
        }
        rootElements.add(rootName);
      } else if( "xml".equals(valueParts[valueParts.length-1] ) ) {
        //XML-Daten
        xmlData.add(Pair.of(tme.getKeyv(), valueParts[1]));
      } else if( valueParts.length == 3 ) {
        //Datentypen oder Choice-Daten
        if( tme.getKeyv().endsWith(":c") ) {
          //Choice
          List<Pair<String,String>> choiceList = choiceData.get(valueParts[1]);
          if( choiceList == null ) {
            choiceList = new ArrayList<Pair<String,String>>();
            choiceData.put(valueParts[1], choiceList);
          }
          String[] keyParts = tme.getKeyv().split(":");
          
          choiceList.add( Pair.of(keyParts[keyParts.length-2], valueParts[2]) );
        } else {
          //normaler Datentype
          typeData.put(valueParts[1], valueParts[2]);
        }
      }
    }
    
    TypeInfo type = new TypeInfo(Type.Complex, name); //TODO Complex ist nur meistens richtig
    if( rootElements != null ) {
      for( FQName root : rootElements ) {
        type.addRootElement(root);
      }
    }
    
    String prefix = name.getNamespace()+":"+name.getName()+":";
    for( Pair<String,String> pair : xmlData ) {
      if( pair.getFirst().startsWith(prefix) ) {
        try {
          String keyvSuffix = pair.getFirst().substring(prefix.length());
          String varName = pair.getSecond();
          String varType = typeData.get(varName);
          List<Pair<String, String>> choiceList = choiceData.get(varName);
          if( choiceList != null ) {
            String[] keyvs = keyvSuffix.split(":");
            String choiceBaseName = keyvs[0];
             for( Pair<String,String> c : choiceList ) {
              if( choiceBaseName.equals( c.getFirst() ) ) {
                varType = c.getSecond();
              }
            }
          }
          TypeInfoMember tim = createMember( name, keyvSuffix, varName, varType, choiceList );
          type.addMemberDontChangePosition(tim);
        } catch( Exception e ) {
          logger.warn( "Unexpected XML-TypeMappingEntry with key "+pair.getFirst(), e );
        }
      } else {
        logger.warn( "Unexpected XML-TypeMappingEntry with key "+pair.getFirst() );
      }
    }

    type.setXmomType(xmomType);
    
    return type;
  }
  
  private FQName getTypeName(String key) {
    //Beispiel http:namespaceSuffix:Type
    int typeIdx = key.lastIndexOf(':');
    if( typeIdx > 0 ) {
      String namespace = key.substring(0,typeIdx);
      if( "null".equals(namespace) ) {
        namespace = null;
      }
      return new FQName( namespace, key.substring(typeIdx+1) );
    } else {
      return new FQName( null, key );
    }
  }


  private FQName getRootElementName(String key) {
    //Beispiel http:namespaceSuffix:Element:_root
    int rootIdx = key.lastIndexOf(':');
    if( ! "_root".equals(key.substring(rootIdx+1)) ) {
      logger.warn( "Unexpected Root-TypeMappingEntry with key "+key);
      return null;
    }
    int nameIdx = key.lastIndexOf(':', rootIdx-1 );
    if( nameIdx > 0 ) {
      String namespace = key.substring(0,nameIdx);
      if( "null".equals(namespace) ) {
        namespace = null;
      }
      return new FQName( namespace, key.substring(nameIdx+1,rootIdx) );
    } else {
      return new FQName( null, key.substring(nameIdx+1,rootIdx) );
    }
  }
  
  /**
   * Anlegen eines Members aus den TypeMappingDaten mit Typ "xml" 
   * @param key
   * @param choiceList 
   * @param xmomName
   * @return
   */
  private TypeInfoMember createMember(FQName parent, String key, String varName, String varType, List<Pair<String, String>> choiceList) {
    String[] parts = key.split(":");
    
    String posType = parts[1];
    
    MemberType memberType = null;
    if( posType.endsWith("e") ) {
      memberType = MemberType.Element;
    } else if( posType.endsWith("a") ) {
      memberType = MemberType.Attribute;
    } else if( posType.endsWith("t") ) {
      memberType = MemberType.Text;
    } else if( posType.endsWith("c") ) {
      memberType = MemberType.Choice;
    } else if( posType.endsWith("x") ) {
      memberType = MemberType.Any;
    } else {
      throw new IllegalArgumentException("Invalid posType "+ posType );
    }
    int position = Integer.parseInt(posType.substring(0, posType.length()-1));
    
    
    String namespace = parent.getNamespace();
    boolean optional = memberType.isOptional(); //Elemente sind per default Pflicht, Attribute optional
    boolean qualified = memberType.isQualified(); //Elemente sind per default Qualified, Attribute Unqualified
    if( parts.length > 2 ) {
      //es gibt noch weitere Angaben!
      boolean namespaceFound = false;
      for( int p = 2; p<parts.length; ++p ) {
        if( namespaceFound ) {
          namespace = namespace+":"+parts[p];
        } else {
          if( parts[p].equals("optional") ) {
            optional = true;
          } else if( parts[p].equals("required") ) {
            optional = false;
          } else if( parts[p].startsWith("namespace=") ) {
            namespaceFound = true;
            namespace = parts[p].substring(10);
          } else if( parts[p].equals("unqualified") ) {
            qualified = false;
          } else if( parts[p].equals("qualified") ) {
            qualified = true;
          }
        }
      }
    }
    
    FQName name = new FQName( namespace, parts[0] );
    
    TypeInfoMemberBuilder timb = TypeInfoMember.create(name, memberType).
        label(name.getName()).
        position(position).
        optional(optional).qualified(qualified).varName(varName);
    if( varType != null ) {
      if( varType.contains(".") ) {
        TypeInfo ti = new TypeInfo(Type.Complex, new FQName("-","-") );
        ti.setXmomType( XmomType.ofFQTypeName(varType) );
        timb.complexType( ti );
      } else {
        try {
          PrimitiveType pt = PrimitiveType.create(varType);
          timb.simpleType(pt);
        } catch (UnsupportedJavaTypeException e) {
          logger.warn("Could not parse varType "+varType+" to PrimitiveType");
        }
      }
    }
    if( choiceList != null ) {
      List<Pair<FQName, TypeInfo>> choice = new ArrayList<Pair<FQName, TypeInfo>>();
      for( Pair<String, String> p : choiceList ) {
        FQName cname = new FQName(namespace, p.getFirst() );
        TypeInfo cti = new TypeInfo(Type.Complex, cname );
        cti.setXmomType(XmomType.ofFQTypeName(p.getSecond()));
        choice.add(Pair.of(cname, cti));
      }
      timb.choice(choice);
    }
    
    return timb.build();
  }

 

}
