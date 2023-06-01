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
package com.gip.xyna.xdev.map.typegen;

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
import com.gip.xyna.xdev.map.types.FQName;
import com.gip.xyna.xdev.map.types.MemberType;
import com.gip.xyna.xdev.map.types.TypeInfo;
import com.gip.xyna.xdev.map.types.TypeInfo.Type;
import com.gip.xyna.xdev.map.types.TypeInfoMember;


/**
 * TypeInfoGenerator erzeugt eine List&lt;TypeInfo> f�r alle Datentypen aus dem XSD.
 * Dazu m�ssen die RootElemente aus dem XSD an {@link TypeInfoGenerator#addRootLevelElement(XSElementDeclaration)}
 * �bergeben werden.
 * Anschlie�end k�nnen die XMOM-Daten �ber {@link TypeInfoGenerator#createXMOMData(XmomDataCreator)}
 * in den TypeInfos erg�nzt werden.
 * 
 * Bekannte Bugs:
 * 1) eine model group (d.h. choice/sequence/all) kann maxOccurs > 1 haben
 * 2) Eine Choice mit Ableitungen eines BasisTypes k�nnte sch�n in ein einziges Basistype-Objekt gesteckt werden 
 * 
 */
public class TypeInfoGenerator {

  private static final Logger logger = CentralFactoryLogging.getLogger(TypeInfoGenerator.class);
  
  //Schutz vor Endlos-Rekursion durch Zirkel-Abhaengigkeiten f�r ComplexTypes
  private final Map<XSComplexTypeDefinition,TypeInfo> processedTypes = new HashMap<XSComplexTypeDefinition,TypeInfo>();
  private final Map<FQName,TypeInfo> namedTypes = new HashMap<FQName,TypeInfo>();
  //alle erzeugten TypeInfos
  private final List<TypeInfo> allTypeInfos = new ArrayList<TypeInfo>();
  private GenerationParameter generationParameter;
  
  public TypeInfoGenerator(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
  }
  
  /**
   * wird nur mit XSD-Elementen auf Root-Ebene aufgerufen
   *
   * Erzeugt Typeinfo f�r das �bergebene Toplevel-Element und gleichzeitig alle rekursiv abh�ngigen Typen.
   *
   * Rekursion wie folgt: 
   * (generateType steht f�r eine der drei Methoden generate{Simple,Complex,AnonymousComplex}Type)
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
    logger.info( "Generated TypeInfo for rootElement "+fqName+": "+rootElementType );
    if( rootElementType == null ) {
      logger.warn("could not create type " + el.getName());
    }
    rootElementType.addRootElement(fqName);
  }

  private TypeInfo generateSimpleType(XSSimpleTypeDefinition simpleType, FQName fqName) {
    TypeInfo typeInfo = new TypeInfo( Type.Simple, fqName);
    allTypeInfos.add(typeInfo);
    typeInfo.addMember( TypeInfoMember.create(fqName, MemberType.Element).
                        simpleType(simpleType).qualified(true).build() );

    if( logger.isDebugEnabled() ) {
      logger.debug("Generated simple type info: " + typeInfo.toString());
    }
    return typeInfo;
  }

  private TypeInfo generateComplexType(XSComplexTypeDefinition complexType, FQName name) {
    TypeInfo typeInfo = processedTypes.get(complexType);
    if (typeInfo != null) {
      return typeInfo;
    }
    
    if (complexType.getName() == null) {
      //anonymer complex type
      typeInfo = generateAnonymousComplexType(complexType, name);
    } else if( isAnyType(complexType) ) {
      //AnyType
      typeInfo = new TypeInfo( Type.Simple, name);
      allTypeInfos.add(typeInfo);
      boolean qualified = complexType.getNamespace() != null;
      typeInfo.addMember(TypeInfoMember.create(name,MemberType.Element).
                         occurs(complexType.getParticle()).
                         simpleType(String.class).qualified(qualified).build());
      processedTypes.put(complexType, typeInfo);
      if( logger.isDebugEnabled() ) {
        logger.debug("Generated simple type info for AnyType: " + typeInfo.toString());
      }
    } else {
      //benannter complex type
      typeInfo = generateNamedComplexType(complexType);
    }
    return typeInfo;
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
    
    //Cachen, damit Referenzen aufgel�st werden k�nnen
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
    
    //Cachen, damit Referenzen aufgel�st werden k�nnen
    processedTypes.put(complexType, typeInfo);
    addTypeContext(typeInfo, complexType);
    
    //Leider k�nnen AnonymousComplexType mehrfach auftreten mit gleicher Definition, 
    //werden aber nicht als doppelt erkannt.
    //Dies muss nun hier umst�ndlich nachgeholt werden
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
      typeInfo.addMember(TypeInfoMember.create(typeInfo.getName(), MemberType.Text).
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
      if (baseTypeParticle != particle) { //k�nnte zum basetype geh�ren
        addChild(typeInfo, particle, baseTypeParticle);
      }
    }

    //Attribute behandeln
    XSObjectList attributes = complexType.getAttributeUses();
    for (int i = 0; i < attributes.getLength(); i++) {
      XSObject attribute = attributes.item(i);
      
      //Attribut k�nnte evtl doppelt sein, da die vom baseType geerbten Attribute hier nochmal gefunden werden
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
      typeInfo.addMember( TypeInfoMember.create(name, MemberType.Attribute).
                          simpleType(attDecl.getTypeDefinition()).
                          qualified(qualified).optional(optional).build()); 
    }

  }
  

  private void addChild(TypeInfo typeInfo, XSParticle particle, XSParticle baseTypeParticle) {
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
          typeInfo.addMember( TypeInfoMember.create(name, MemberType.Element).
                              occurs(particle).
                              complexType(elementTypeInfo).qualified(qualified).build() );
        }
      } else if (elementType instanceof XSSimpleTypeDefinition) {
        typeInfo.addMember( TypeInfoMember.create(name, MemberType.Element).
                            occurs(particle).
                            simpleType((XSSimpleTypeDefinition) elementType).qualified(qualified).build() );
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
    //TODO: eine model group (d.h. choice/sequence/all) kann maxOccurs > 1 haben, hier particle.getMaxOccurs()
    // Was soll damit geschehen?
    if( particle.getMaxOccurs() != 1 ) {
      logger.warn(group + " has maxOccurs "+particle.getMaxOccurs());
    }

    //List aller tats�chlichen Kinder erstellen
    List<XSParticle> children = new ArrayList<XSParticle>();
    XSObjectList childCandidates = group.getParticles();
    if (childCandidates != null) {
      for (int i = 0; i < childCandidates.getLength(); i++) {
        XSObject o = childCandidates.item(i);
        if (o instanceof XSParticle) {
          if (o != baseTypeParticle) { //k�nnte zum basetype geh�ren
            children.add( (XSParticle) o );
          }
        } else {
          throw new RuntimeException();
        }
      }
    }

    if( group.getCompositor() == XSModelGroup.COMPOSITOR_CHOICE ) {
      addChoice(typeInfo, baseTypeParticle, children );
    } else {
      for( XSParticle child : children ) {
        addChild(typeInfo, child, baseTypeParticle);
      }
    }
  }
  
  /**
   * @param typeInfo
   * @param baseTypeParticle
   * @param children
   */
  private void addChoice(TypeInfo typeInfo, XSParticle baseTypeParticle, List<XSParticle> children) {
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
        typeInfo.addMember( choiceMember );
      }
    }
    if( choiceAsList ) {
      for( XSParticle child : children ) {
        addChild(typeInfo, child, baseTypeParticle);
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
    
    //nun VariablenNamen anpassen, damit diese eindeutig sind
    //Eindeutigkeit muss auch bei Ableitungen gew�hrleistet sein, daher zuerst Basistypen suchen 
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
    //eigene VarNames eindeutig machen und erg�nzen
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      tim.createVarNameAndLabel(allVarNames, xmomDataCreator);
    }
    //allVarNames cachen, da evtl. f�r weitere Ableitungen relevant
    if( baseVarNames.containsKey(typeInfo) ) {
      baseVarNames.put(typeInfo,allVarNames);
    }
    return allVarNames;
  }

}
