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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.xerces.impl.dv.xs.XSSimpleTypeDecl;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.Constants;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.mapping.NamespacePrefixCache;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.MemberType;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo.Type;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;


/**
 * TypeInfoGenerator erzeugt eine List&lt;TypeInfo> für alle Datentypen aus dem XSD.
 * Dazu müssen die RootElemente aus dem XSD an {@link TypeInfoGenerator#addRootLevelElement(XSElementDeclaration)}
 * übergeben werden.
 * Anschließend können die XMOM-Daten über {@link TypeInfoGenerator#createXMOMData(XmomDataCreator)}
 * in den TypeInfos ergänzt werden.
 * 
 * Bekannte Bugs:
 * 1) eine model group (d.h. choice/sequence/all) kann maxOccurs > 1 haben
 * 2) Eine Choice mit Ableitungen eines BasisTypes könnte schön in ein einziges Basistype-Objekt gesteckt werden 
 * 
 */
public class TypeInfoGenerator {

  private static final Logger logger = CentralFactoryLogging.getLogger(TypeInfoGenerator.class);
  
  //Schutz vor Endlos-Rekursion durch Zirkel-Abhaengigkeiten für ComplexTypes
  private final Map<XSTypeDefinition,TypeInfo> processedTypes = new HashMap<XSTypeDefinition,TypeInfo>();
  private final Map<FQName,TypeInfo> namedTypes = new HashMap<FQName,TypeInfo>();
  private final Map<FQName,TypeInfo> generatedTypes = new HashMap<FQName,TypeInfo>();
  //alle erzeugten TypeInfos
  private final List<TypeInfo> allTypeInfos = new ArrayList<TypeInfo>();
  private GenerationParameter generationParameter;
  private TypeInfo anyType;
  private int anyTypeCount = 0;
  
  public TypeInfoGenerator(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
  }
  
  /**
   * wird nur mit XSD-Elementen auf Root-Ebene aufgerufen
   *
   * Erzeugt Typeinfo für das übergebene Toplevel-Element und gleichzeitig alle rekursiv abhängigen Typen.
   *
   * Rekursion wie folgt: 
   * (generateType steht für eine der drei Methoden generate{Simple,Complex,AnonymousComplex}Type)
   * generateType -> addTypeContext -> generateType oder
   *                                -> addChild -> generateType
   * @param el
   */
  public void addRootLevelElement(XSElementDeclaration el) {
    FQName fqName = new FQName(el.getNamespace(), el.getName());
    XSTypeDefinition type = el.getTypeDefinition();
    TypeInfo rootElementType;
    if (type instanceof XSComplexTypeDefinition) {
      rootElementType = generateComplexType((XSComplexTypeDefinition) type, fqName);
    } else if (type instanceof XSSimpleTypeDefinition) {
      rootElementType = generateSimpleType((XSSimpleTypeDefinition) type, fqName);
    } else {
      throw new UnsupportedOperationException("Unexpected type "+type);
    }
    if( logger.isDebugEnabled() ) {
      logger.debug( "Generated TypeInfo for rootElement "+fqName+": "+rootElementType );
    }
    if( rootElementType == null ) {
      logger.warn("could not create type " + el.getName());
    }
    rootElementType.addRootElement(fqName);
  }
  
  public void addType(XSObject type) {
    if( type instanceof XSComplexTypeDefinition ) {
      XSComplexTypeDefinition complexType = (XSComplexTypeDefinition) type;
      FQName name = new FQName(complexType.getNamespace(), complexType.getName());
      if( ! NamespacePrefixCache.NAMESPACE_XS.equals( complexType.getNamespace()) ) {
        TypeInfo typeInfo = namedTypes.get(name);
        if( typeInfo == null ) {
          generateNamedComplexType(complexType);
        }
      }
    } else if( type instanceof XSSimpleTypeDefinition ) {
      XSSimpleTypeDefinition simpleType = (XSSimpleTypeDefinition) type;
      if( ! NamespacePrefixCache.NAMESPACE_XS.equals( simpleType.getNamespace() ) ) {
        FQName name = new FQName(simpleType.getNamespace(), simpleType.getName());
        generateSimpleType(simpleType, name);
      }
    }
  }

  private TypeInfo generateSimpleType(XSSimpleTypeDefinition simpleType, FQName fqName) {
    TypeInfo typeInfo = processedTypes.get(simpleType);
    if (typeInfo != null) {
      return typeInfo;
    }
    typeInfo = new TypeInfo( Type.Simple, fqName);
    allTypeInfos.add(typeInfo);
    typeInfo.addMemberAndIncrementPosition( TypeInfoMember.create(fqName, MemberType.Text).
                        simpleType(simpleType).qualified(true).build() );

    if( logger.isDebugEnabled() ) {
      logger.debug("Generated simple type info: " + typeInfo.toString());
    }
    processedTypes.put(simpleType, typeInfo);
    return typeInfo;
  }

  private TypeInfo generateComplexType(XSComplexTypeDefinition complexType, FQName name) {
    if( isAnyType(complexType) ) {
      return generateAnyType(complexType,name);
    }
    
    TypeInfo typeInfo = processedTypes.get(complexType);
    if (typeInfo != null) {
      return typeInfo;
    }
    if (complexType.getName() == null) {
      //anonymer complex type
      typeInfo = generateAnonymousComplexType(complexType, name);
    } else {
      //benannter complex type
      typeInfo = generateNamedComplexType(complexType);
    }
    return typeInfo;
  }
    
  private TypeInfo generateAnyType(XSComplexTypeDefinition complexType, FQName name) {
    anyTypeCount++;
    if (anyType == null) {
      anyType = Constants.createAnyType_TypeInfo();
    }
    return anyType;
  }
  
  public int getAnyTypeCount() {
    return anyTypeCount;
  }

  private TypeInfo generateNamedComplexType(XSComplexTypeDefinition complexType) {
    FQName name = new FQName(complexType.getNamespace(), complexType.getName());
    TypeInfo typeInfo = namedTypes.get(name);
    if( typeInfo != null ) {
      if( logger.isDebugEnabled() ) {
        logger.debug("Duplicate named complex type "+ name);
      }
      return typeInfo;
    }
    
    typeInfo = new TypeInfo( Type.Complex, name);
    allTypeInfos.add(typeInfo);
    
    //Cachen, damit Referenzen aufgelöst werden können
    processedTypes.put(complexType, typeInfo);
    namedTypes.put(name, typeInfo);
    addTypeContext(typeInfo, complexType);
    
    if( logger.isDebugEnabled() ) {
      logger.debug("Generated complex type info: " + typeInfo.toString());
    }
    return typeInfo;
  }
  

  private boolean isAnyType(XSComplexTypeDefinition complexType) {
    return complexType.getAttributeWildcard() != null
        && complexType.getAttributeWildcard().getConstraintType() == XSWildcard.NSCONSTRAINT_ANY;
  }

  private TypeInfo generateAnonymousComplexType(XSComplexTypeDefinition complexType, FQName name) {
    TypeInfo typeInfo = new TypeInfo( Type.Anonymous, name);
    
    //Cachen, damit Referenzen aufgelöst werden können
    processedTypes.put(complexType, typeInfo);
    addTypeContext(typeInfo, complexType);
    
    //Leider können AnonymousComplexType mehrfach auftreten mit gleicher Definition, 
    //werden aber nicht als doppelt erkannt.
    //Dies muss nun hier umständlich nachgeholt werden
    for( TypeInfo at : allTypeInfos ) {
      if( at.equals(typeInfo) ) {
        if( logger.isDebugEnabled() ) {
          logger.debug("Generated anonymous complex type info as duplicate: "+ typeInfo.toString());
        }
        processedTypes.put(complexType, at);
        return at;
      }
    }
    
    allTypeInfos.add(typeInfo);
    generatedTypes.put(typeInfo.getName(), typeInfo);
    if( logger.isDebugEnabled() ) {
      logger.debug("Generated anonymous complex type info: " + typeInfo.toString());
    }
    return typeInfo;
  }
  
  
  /**
   * untersucht XSD-Node genauer (Basis-Datentypen, Kindelemente, Attribute)
   *
   */
  private void addTypeContext(TypeInfo typeInfo, XSComplexTypeDefinition complexType) {
    // Pruefen, ob XSD-Datentyp von anderem Datentyp abgeleitet ist
    XSTypeDefinition baseType = complexType.getBaseType();
    if (baseType instanceof XSComplexTypeDefinition) {
      if( ! isAnyType((XSComplexTypeDefinition) baseType) ) {
        //kann nicht anonym sein, daher leere elementName und targetNS erlaubt
        typeInfo.setBaseType( generateComplexType( (XSComplexTypeDefinition) baseType, null) );
      }
    } else if( baseType instanceof XSSimpleTypeDecl ) {
      boolean qualified = true;
      typeInfo.addMemberAndIncrementPosition(TypeInfoMember.create(typeInfo.getName(), MemberType.Text).
                         simpleType((XSSimpleTypeDecl)baseType).qualified(qualified).build());
    } else {
      throw new UnsupportedOperationException("Complextype '" + complexType + "' hast unexpected base type "+baseType+" of class "+ baseType.getClass() );
    }

    //nach Kindelementen suchen
    XSParticle particle = complexType.getParticle(); //z.b. sequence oder sowas
    if (particle != null) {
      XSParticle baseTypeParticle = null;
      if (baseType != null) {
        baseTypeParticle = ((XSComplexTypeDefinition) baseType).getParticle();
      }
      if (baseTypeParticle != particle) { //könnte zum basetype gehören
        addChild(typeInfo, particle, baseTypeParticle, MinMaxOccurs.Mandatory);
      }
    }

    //Attribute behandeln
    XSObjectList attributes = complexType.getAttributeUses();
    for (int i = 0; i < attributes.getLength(); i++) {
      XSObject attribute = attributes.item(i);
      
      //Attribut könnte evtl doppelt sein, da die vom baseType geerbten Attribute hier nochmal gefunden werden
      if (baseType instanceof XSComplexTypeDefinition) {
        boolean attributeAlreadyDefined = false;
        XSComplexTypeDefinition baseTypeTD = (XSComplexTypeDefinition) baseType;
        for (int j = 0; j < baseTypeTD.getAttributeUses().getLength(); j++) {
          XSObject baseTypeAttribute = baseTypeTD.getAttributeUses().item(j);
          if (baseTypeAttribute == attribute) {
            attributeAlreadyDefined = true;
            break;
          }
        }
        if( attributeAlreadyDefined ) {
          continue;
        }
      }
      boolean optional;
      XSAttributeDeclaration attDecl = null;
      if (attribute instanceof XSAttributeDeclaration) {
        optional = true;
        attDecl = (XSAttributeDeclaration) attribute;
      } else if (attribute instanceof XSAttributeUse) {
        XSAttributeUse attributeUse = (XSAttributeUse) attribute;
        optional = ! attributeUse.getRequired();
        attDecl = attributeUse.getAttrDeclaration();
      } else {
        throw new UnsupportedOperationException("unsupported type of attribute: " + attribute.getClass().getName());
      }
      String namespace = attDecl.getNamespace();
      boolean qualified = false;
      if( namespace != null ) {
        qualified = true;
      } else {
        namespace = typeInfo.getName().getNamespace();
      }
      FQName name = new FQName(namespace, attDecl.getName());
      typeInfo.addMemberAndIncrementPosition( TypeInfoMember.create(name, MemberType.Attribute).
                          simpleType(attDecl.getTypeDefinition()).
                          qualified(qualified).optional(optional).build()); 
    }

  }
  

  private void addChild(TypeInfo typeInfo, XSParticle particle, XSParticle baseTypeParticle, MinMaxOccurs parentMinMaxOccurs) {
    XSTerm term = particle.getTerm();
    if (term instanceof XSElementDeclaration) {
      XSElementDeclaration element = (XSElementDeclaration) term;
      XSTypeDefinition elementType = element.getTypeDefinition();
      String nameSpace = element.getNamespace();
      boolean qualified = true;
      if (nameSpace == null) {
        if (element.getEnclosingCTDefinition() != null && element.getEnclosingCTDefinition().getNamespace() != null) {
          qualified = false;
          nameSpace = element.getEnclosingCTDefinition().getNamespace();
        } else {
          //TODO tja, was nun? gibts vielleicht keine namespaces?
          //den vom elementType zu nehmen ist auch nicht richtig...
          nameSpace = null;
        }
      }
      FQName name = new FQName(nameSpace, element.getName());
      
      if (elementType instanceof XSComplexTypeDefinition) {
        TypeInfo elementTypeInfo = generateComplexType((XSComplexTypeDefinition) elementType, name);
        if( elementTypeInfo == null ) {
          //logger.warn("No member type for "+typeInfo+ " " + element.getName());
          throw new UnsupportedOperationException("No member type for "+typeInfo+ " " + element.getName());
        } else {
          MemberType type = elementTypeInfo.isAny() ? MemberType.Any : MemberType.Element;
          typeInfo.addMemberAndIncrementPosition( TypeInfoMember.create(name, type).
                              occurs(MinMaxOccurs.combine(MinMaxOccurs.forParticle(particle), parentMinMaxOccurs) ).
                              complexType(elementTypeInfo).qualified(qualified).build() );
        }
      } else if (elementType instanceof XSSimpleTypeDefinition) {
        XSSimpleTypeDefinition simpleTypeDefinition = (XSSimpleTypeDefinition)elementType;
        boolean generateSimpleType = generationParameter.isGenerationOptions_namedSimpleTypes();
        if( generateSimpleType ) {
          //TODO woran kann besser erkannt werden, dass simpleType XSD-Standard-Typ ist?
          generateSimpleType = ! simpleTypeDefinition.getNamespaceItem().getDocumentLocations().isEmpty();
        }
        if( generateSimpleType ) {
          FQName simpleName = new FQName(nameSpace, simpleTypeDefinition.getName());
          TypeInfo simpleTypeInfo = generateSimpleType(simpleTypeDefinition, simpleName);
          typeInfo.addMemberAndIncrementPosition( TypeInfoMember.create(name, MemberType.Element).
                              occurs(MinMaxOccurs.combine(MinMaxOccurs.forParticle(particle), parentMinMaxOccurs)).
                              complexType(simpleTypeInfo).qualified(qualified).build() );
        } else {
          typeInfo.addMemberAndIncrementPosition( TypeInfoMember.create(name, MemberType.Element).
                              occurs(MinMaxOccurs.combine(MinMaxOccurs.forParticle(particle), parentMinMaxOccurs)).
                              simpleType(simpleTypeDefinition).qualified(qualified).build() );
        }
      } else {
        throw new UnsupportedOperationException("unsupported elementType " + elementType.getClass().getName());
      }
    } else if (term instanceof XSModelGroup) {
      addGroup(typeInfo, particle, baseTypeParticle, (XSModelGroup) term);
    } else if (term instanceof XSWildcard) {
      logger.warn("xsd contains wildcards, which are not supported and skilled.");
    } else {
      throw new UnsupportedOperationException("unsupported xsd type: " + term.getClass().getName());
    }
  }

  /**
   * @param typeInfo
   * @param baseTypeParticle
   * @param term
   */
  private void addGroup(TypeInfo typeInfo, XSParticle particle, XSParticle baseTypeParticle, XSModelGroup group) {
    MinMaxOccurs mmo = MinMaxOccurs.forParticle(particle);
    
    //TODO: eine model group (d.h. choice/sequence/all) kann maxOccurs > 1 haben, hier particle.getMaxOccurs()
    // Was soll damit geschehen? -> Vererben...

    //List aller tatsächlichen Kinder erstellen
    List<XSParticle> children = new ArrayList<XSParticle>();
    XSObjectList childCandidates = group.getParticles();
    if (childCandidates != null) {
      for (int i = 0; i < childCandidates.getLength(); i++) {
        XSObject o = childCandidates.item(i);
        if (o instanceof XSParticle) {
          if (o != baseTypeParticle) { //könnte zum basetype gehören
            children.add( (XSParticle) o );
          }
        } else {
          throw new RuntimeException(o +" is no instance of XSParticle");
        }
      }
    }

    if( group.getCompositor() == XSModelGroup.COMPOSITOR_CHOICE ) {
      addChoice(typeInfo, baseTypeParticle, children, MinMaxOccurs.optional(mmo) );
    } else {
      for( XSParticle child : children ) {
        addChild(typeInfo, child, baseTypeParticle, mmo);
      }
    }
  }
  
  public enum MinMaxOccurs {
    Optional(true,false),
    Mandatory(false,false),
    OptionalList(true,true),
    List(false,true);
    
    private boolean optional;
    private boolean list;

    private MinMaxOccurs(boolean optional,boolean list) {
      this.optional = optional;
      this.list = list;
    }
    
    public static MinMaxOccurs optional(MinMaxOccurs mmo) {
      return valueOf(true, mmo.isList());
    }

    public static MinMaxOccurs combine(MinMaxOccurs minMaxOccurs, MinMaxOccurs parentMinMaxOccurs) {
      if( parentMinMaxOccurs == null ) {
        return minMaxOccurs;
      }
      return valueOf( minMaxOccurs.isOptional()|| parentMinMaxOccurs.isOptional(),
                      minMaxOccurs.isList()|| parentMinMaxOccurs.isList() );
    }

    public boolean isList() {
      return list;
    }
    
    public boolean isOptional() {
      return optional;
    }
    
    public static MinMaxOccurs forParticle(XSParticle particle) {
      return valueOf( particle.getMinOccurs() == 0, particle.getMaxOccurs() > 1 || particle.getMaxOccursUnbounded() );
    }

    private static MinMaxOccurs valueOf(boolean optional, boolean list) {
      if( list ) {
        if( optional ) {
          return OptionalList;
        } else {
          return List;
        }
      } else {
        if( optional ) {
          return Optional;
        } else {
          return Mandatory;
        }
      }
    }
    
  }
  
  
  /**
   * @param typeInfo
   * @param baseTypeParticle
   * @param children
   */
  private void addChoice(TypeInfo typeInfo, XSParticle baseTypeParticle, List<XSParticle> children, MinMaxOccurs parentMinMaxOccurs) {
    //nur gemeinsame Basis aller Kinder eintragen, wenn alles XSComplexTypeDefinition-Kinder sind
    boolean choiceAsList = true;
    List<Pair<FQName,TypeInfo>> chilrenTypes = new ArrayList<Pair<FQName,TypeInfo>>();
    for( XSParticle child : children ) {
      XSTerm ct = child.getTerm();
      if( ! (ct instanceof XSElementDeclaration) ) {
        break;
      }
      XSElementDeclaration ce = (XSElementDeclaration) ct;
      XSTypeDefinition cet = ce.getTypeDefinition();
      if( ! (cet instanceof XSComplexTypeDefinition) ) {
        break;
      }
      FQName cname = new FQName(ce.getNamespace(), ce.getName());
      TypeInfo ceti = generateComplexType((XSComplexTypeDefinition) cet, cname);
      chilrenTypes.add( Pair.of(cname, ceti ) );
    }
    if( generationParameter.isGenerationOptions_expandChoice() ) {
      choiceAsList = true; 
    } else if( chilrenTypes.size() < children.size() ) {
      //nicht alle Kinder waren complex, daher Choice in Liste umwandeln
      choiceAsList = true; 
    } else {
      //gemeinsame Oberklasse suchen
      TypeInfo commonBase = chilrenTypes.get(0).getSecond();
      for( Pair<FQName,TypeInfo> ti : chilrenTypes ) {
        commonBase = TypeInfo.commonBase(commonBase, ti.getSecond());
      }
      if( commonBase == null ) {
        choiceAsList = true; 
      } else {
        choiceAsList = false;
        boolean qualified = true; //FIXME woher? 
        TypeInfoMember choiceMember = TypeInfoMember.create(commonBase.getName(), MemberType.Choice).
            complexType(commonBase).qualified(qualified).choice(chilrenTypes).build();
        typeInfo.addMemberAndIncrementPosition( choiceMember );
      }
    }
    if( choiceAsList ) {
      for( XSParticle child : children ) {
        addChild(typeInfo, child, baseTypeParticle, parentMinMaxOccurs);
      }
    }
  }
  
  public List<TypeInfo> getAllTypeInfos() {
    return allTypeInfos;
  }

  public void createXMOMData(XmomDataCreator xmomDataCreator) {
    
    //zuerst alle ComplexTypes, da dort der Name bereits feststeht
    for( TypeInfo typeInfo : allTypeInfos ) {
      if( typeInfo.isComplex() ) {
        typeInfo.setXmomType( xmomDataCreator.createXmomType(typeInfo) );
      }
    }
    //bei Anonymous und Simple muss Typename erzeugt werden
    for( TypeInfo typeInfo : allTypeInfos ) {
      if( typeInfo.isAnonymous() ) {
        typeInfo.setXmomType( xmomDataCreator.createXmomType(typeInfo) );
      }
    }
    for( TypeInfo typeInfo : allTypeInfos ) {
      if( typeInfo.isSimple() ) {
        typeInfo.setXmomType( xmomDataCreator.createXmomType(typeInfo) );
      }
    }
    
    if( anyType != null ) {
      anyType.setXmomType( Constants.getAnyType_XmomType() );
    }
    
    //nun VariablenNamen anpassen, damit diese eindeutig sind
    //Eindeutigkeit muss auch bei Ableitungen gewährleistet sein, daher zuerst Basistypen suchen 
    HashMap<TypeInfo, HashSet<String>> baseVarNames = new HashMap<TypeInfo, HashSet<String>>();
    for( TypeInfo typeInfo : allTypeInfos ) {
      if( typeInfo.hasBaseType() ) {
        baseVarNames.put( typeInfo.getBaseType(), null);
      }
    }
    for( TypeInfo typeInfo : allTypeInfos ) {
      createUniqueVarNames(xmomDataCreator, baseVarNames, typeInfo);
    }
    
  }

  private HashSet<String> createUniqueVarNames(XmomDataCreator xmomDataCreator, HashMap<TypeInfo, HashSet<String>> baseVarNames, TypeInfo typeInfo) {
    HashSet<String> allVarNames = baseVarNames.get(typeInfo);
    if( allVarNames != null ) {
      return allVarNames; //bereits berechnet
    }
    
    allVarNames = new HashSet<String>();
    if( typeInfo.hasBaseType() ) {
      //von BaseType VarNames erben
      allVarNames.addAll( createUniqueVarNames(xmomDataCreator, baseVarNames,typeInfo.getBaseType()));
    }
    //eigene VarNames eindeutig machen und ergänzen
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      tim.createVarNameAndLabel(allVarNames, xmomDataCreator);
    }
    //allVarNames cachen, da evtl. für weitere Ableitungen relevant
    if( baseVarNames.containsKey(typeInfo) ) {
      baseVarNames.put(typeInfo,allVarNames);
    }
    return allVarNames;
  }

}
