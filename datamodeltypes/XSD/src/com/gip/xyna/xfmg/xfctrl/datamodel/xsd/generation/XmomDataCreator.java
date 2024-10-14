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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.Documentation.DocumentedEnum;
import com.gip.xyna.utils.misc.StringSplitter;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


public class XmomDataCreator {

  private Set<String> allFQTypeNames = new HashSet<String>();
  private Set<String> allNames = new HashSet<String>();
  private Map<String,Pair<String,Boolean>> namespaceXmomPath = new HashMap<String,Pair<String,Boolean>>();
  private Set<String> xmomPaths = new HashSet<String>();
  
  private static final StringSplitter pathSplitter = new StringSplitter("[\\./]+");
  private static final StringSplitter pathParamSplitter = new StringSplitter("\\#\\w+");
  private static final Pattern NOT_ALLOWED_CHARS_IN_JAVA_PATH_PATTERN = Pattern.compile( "[^\\w.]+");
  private static final Pattern MULTIPLE_UNDERSCORE_PATTERN = Pattern.compile( "_+");
  
  private String suffixForAttribute;
  private boolean uppercaseFirstLetter;
  private String suffixForOptional;
  private GenerationParameter generationParameter; 

  public XmomDataCreator(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
    suffixForAttribute = generationParameter.getLabelCustomization().get(LabelCustomization.suffixForAttribute);
    suffixForOptional = generationParameter.getLabelCustomization().get(LabelCustomization.suffixForOptional);
    uppercaseFirstLetter = "true".equals(generationParameter.getLabelCustomization().get(LabelCustomization.uppercaseFirstLetter));
  }
  
  private enum ChangeReason {
    Unchanged,
    ReservedWord,
    Duplicate;
  }
  
  public static enum LabelCustomization implements DocumentedEnum {
    suffixForAttribute(Documentation.en("labels for attributes get this suffix").
                       de("Labels für Attribute erhalten diesen Suffix").
                       build()),
    suffixForOptional(Documentation.en("optional elements get this suffix").
                      de("Labels für optionale Elemente erhalten diesen Suffix").
                      build()),
    uppercaseFirstLetter(Documentation.en("label starts with uppercase letter").
                         de("Label beginnt mit Großbuchstaben").
                         build());
    private Documentation doc;

    private LabelCustomization(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }

  }

  public static enum PathPart implements DocumentedEnum {
    basepath(Documentation.en("basepath as in parameter 'basePath'" ).
         de("Basispfad wie in Parameter 'basePath' angegeben").
         build()),
    name(Documentation.en("Name of datamodel as in parameter 'name'" ).
             de("Name des Datenmodells wie in Parameter 'name' angegeben").
             build()),
    nshost(Documentation.en("host from xsd-namespace" ).
                de("Host aus dem XSD-Namespace").
                build()),
    nsrevhost(Documentation.en("host from xsd-namespace in reverse order" ).
                       de("Host aus dem XSD-Namespace in umgekehrter Reihenfolge").
                       build()),
    nspath(Documentation.en("path from xsd-namespace" ).
                       de("Pfad aus dem XSD-Namespace").
                       build()),
    nspath_noversion(Documentation.en("path from xsd-namespace without versions numbers" ).
                     de("Pfad aus dem XSD-Namespace ohne Versionsnummern").
                     build()),
    type_prefix(Documentation.en("prefix from type name (till first uppercase letter)" ).
                de("Prefix des Types, (bis zum ersten Großbuchstaben)").
                build()),           
    type_suffix(Documentation.en("suffix from type name (from last uppercase letter)" ).
                de("Suffix des Types, (ab dem letzten Großbuchstaben)").
                build()),
                ;
    
    private Documentation doc;

    private PathPart(Documentation doc) {
      this.doc = doc;
    }
    
    public Documentation getDocumentation() {
      return doc;
    }

  };

  //TODO GenerationBase.reservedWords hat diese liste auch
  private static final List<String> RESERVED_WORDS = Arrays.asList(
    "abstract", "assert", 
    "boolean",    "break",    "byte",
    "case",    "catch",  "char",   "class",    "const",    "continue",
    "default",    "do",    "double",
    "else",  "enum",   "extends",
    "false",    "final",    "finally",    "float",    "for",
    "goto",
    "if",    "implements",    "import",    "inner",    "instanceof",    "int",    "interface",
    "long",
    "native",    "new",    "null",
    "package",    "private",    "protected",    "public",
    "return", "returns",
    "short",    "static",    "strictfp",    "super",    "switch",    "synchronized",
    "this",    "throw", "throws",   "transient",    "true",    "try",
    "void",    "volatile",
    "while",
    //weitere durch Xynobject etc. reservierte Namen
    "instance" //reserviert wegen Builder in XynaObject

    //TODO: weitere...
  );
  
  private static final String RESERVED_SUFFIX = "0";
  private static final Pattern pattern = Pattern.compile(".*?((?:\\d+)?)");
  private static final Pattern usualNamespacePattern = Pattern.compile("https?://([\\w\\.]+)/(.*)");

  private static final Pattern ALLOWED_JAVA_NAME_PATTERN = Pattern.compile( "[\\W]+");
  //private static final Pattern ALLOWED_JAVA_PATH_PATTERN = Pattern.compile( "[^\\w.]+");

  private static final Pattern COMMON_PREFIX_PATTERN = Pattern.compile("([A-Za-z][a-z0-9]*)[A-Z].*");
  private static final Pattern COMMON_SUFFIX_PATTERN = Pattern.compile(".*?([A-Za-z][a-z0-9]*)");
  private static final EnumSet<PathPart> TYPESPECIFIC_PATH_PART = EnumSet.of(PathPart.type_prefix, PathPart.type_suffix);
  

  public XmomType createXmomType(TypeInfo typeInfo) {
    String name = typeInfo.getName().getName();
    Pair<String, ChangeReason> pair = createName(allNames, name, false);
    
    String xmomPath = createXmomPath(typeInfo.getName());
    String xmomType = pair.getFirst();
    String xmomLabel = name;
    
    XmomType xmom = new XmomType(xmomPath, xmomType, xmomLabel);
    if( ! allFQTypeNames.add(xmom.getFQTypeName() ) ) {
      throw new IllegalStateException("Duplicate XMOM-Name "+xmom.getFQTypeName());
    }

    return xmom;
  }
 
  public String createVarName(HashSet<String> allVarNamesForType, TypeInfoMember typeInfoMember) {
    Pair<String, ChangeReason> pair = createName(allVarNamesForType, typeInfoMember.getName().getName(), true);
    return pair.getFirst();
  }
  
  public String createLabel(TypeInfoMember typeInfoMember) {
    StringBuilder xmomLabel = new StringBuilder(typeInfoMember.getName().getName());
    
    switch( typeInfoMember.getMemberType() ) {
      case Element:
        if( typeInfoMember.isOptional() && suffixForOptional != null ) {
          xmomLabel.append(suffixForOptional);
        }
        break;
      case Attribute:
        if( suffixForAttribute != null ) {
          xmomLabel.append(suffixForAttribute);
        }
        break;
      case Text:
        break;
    }
    
    if( uppercaseFirstLetter ) {
      xmomLabel.setCharAt(0, Character.toUpperCase(xmomLabel.charAt(0)) );
    }

    return xmomLabel.toString();
  }
  
  private Pair<String,ChangeReason> createName(Set<String> alreadyUsedNames, String nameIn, boolean isVarName ) {
    StringBuilder name = new StringBuilder( ALLOWED_JAVA_NAME_PATTERN.matcher(nameIn).replaceAll("_").replaceAll("_+", "_") );
    
    if( isVarName ) {
      name.setCharAt(0, Character.toLowerCase(name.charAt(0)) );
    } else {
      name.setCharAt(0, Character.toUpperCase(nameIn.charAt(0)) );
    }
     
    ChangeReason change = ChangeReason.Unchanged;
    
    //FIXME GenerationBase.isReservedVariableName(nameIn.toLowerCase());
    if( RESERVED_WORDS.contains(nameIn.toLowerCase()) ) {
      name.append(RESERVED_SUFFIX);
      change = ChangeReason.ReservedWord;
    }
    
    while( ! alreadyUsedNames.add(name.toString()) ) {
      increaseSuffixCounter(name);
      change = ChangeReason.Duplicate;
    }
    
    return Pair.of(name.toString(),change);
  }
  
  
  private void increaseSuffixCounter(StringBuilder varName) {
    Matcher matcher = pattern.matcher(varName);
    if (!matcher.matches()) {
      throw new RuntimeException();
    }
    String n = matcher.group(1);
    if (n.length() > 0) {
      varName.setLength(varName.indexOf(n));
      varName.append(Integer.valueOf(n) + 1);
    } else {
      varName.append(1);
    }
  }

  private String createXmomPath(FQName fqName) {
    String targetNS = fqName.getNamespace();
    
    Pair<String,Boolean> pair = namespaceXmomPath.get(targetNS);
    if( pair == null ) {
      pair = createPath(targetNS);
      namespaceXmomPath.put(targetNS,pair);
    }
    String xmomPath;
    if( pair.getSecond() ) {
      xmomPath = pair.getFirst();
    } else {
      //Pfad muss noch weiter angepasst werden
      String type = fqName.getName();
      xmomPath = createPath(pair.getFirst(), type);
    }
    xmomPaths.add(xmomPath);
    return xmomPath;
  }

  public Map<String, String> getNamespaceXmomPath() {
    Map<String, String> map = new HashMap<String, String>();
    for( Map.Entry<String,Pair<String,Boolean>> entry : namespaceXmomPath.entrySet() ) {
      map.put( entry.getKey(), entry.getValue().getFirst() );
    }
    return map;
  }

  private Pair<String,Boolean> createPath(String targetNS) {
    List<String> pathList = pathParamSplitter.split(generationParameter.getPathCustomization(), true);
    StringBuilder newPath = new StringBuilder();
    String sep = "";
    boolean finished = true; //alle PathPart ersetzt
    for( String pl : pathList ) {
      String newpl;
      if( pathParamSplitter.isSeparator(pl) ) {
        PathPart pp = PathPart.valueOf(pl.substring(1));
        if( TYPESPECIFIC_PATH_PART.contains(pp) ) {
          finished = false;
          newpl = pl;
        } else {
          String path = createPathReplacementNamespace(pp, targetNS);
          if( path == null ) {
            throw new IllegalStateException("Could not createPathReplacementNamespace for "+pp+", "+targetNS);
          }
          newpl = normalizePath(path);
        }
      } else {
        newpl = normalizePath(pl);
      }
      if( newpl.length() > 0 ) { 
        newPath.append(sep).append(newpl);
        sep = ".";
      }
    }
    return Pair.of(newPath.toString(),finished);
  }

  private String createPath( String path, String type) {
    List<String> pathList = pathParamSplitter.split(path, true);
    StringBuilder newPath = new StringBuilder();
    String sep = "";
    for( String pl : pathList ) {
      String newpl;
      if( pathParamSplitter.isSeparator(pl) ) {
        PathPart pp = PathPart.valueOf(pl.substring(1));
        if( TYPESPECIFIC_PATH_PART.contains(pp) ) {
          newpl = normalizePath( createPathReplacementType(pp, type) );
        } else {
          newpl = pl; //sollte nicht vorkommen!
        }
      } else {
        newpl = normalizePath(pl);
      }
      if( newpl.length() > 0 ) { 
        newPath.append(sep).append(newpl);
        sep = ".";
      }
    }
    return newPath.toString();
  }
 
  private boolean isEmpty(String string) {
    return string == null || string.trim().length() == 0 ;
  }
  /**
   * Modifizieren nicht erlaubter Pfad-Anteile:
   *  - Ersetzen aller Separatoren durch "."
   *  - keine Separatoren am Beginn und Ende
   *  - alle Namen klein
   *  - reservierte Namen mit RESERVED_SUFFIX ergänzt
   * @param path
   * @return
   */
  private String normalizePath(String path) {
    List<String> pathList = pathSplitter.split(path, false);
    StringBuilder newPath = new StringBuilder();
    String sep = "";
    for( String pl : pathList ) {
      newPath.append(sep).append(normalizePathListPart(pl));
      sep = ".";
    }
    return newPath.toString();
  }
  
  private String normalizePathListPart(String pl) {
    String pl2 = NOT_ALLOWED_CHARS_IN_JAVA_PATH_PATTERN.matcher(pl).replaceAll("_");
    pl2 = MULTIPLE_UNDERSCORE_PATTERN.matcher(pl2).replaceAll("_");
    //pl = pl.toLowerCase(); //TODO ok?
    if( RESERVED_WORDS.contains(pl2.toLowerCase()) ) {
      return pl2+RESERVED_SUFFIX;
    } else {
      return pl2;
    }
  }

  private String createPathReplacementNamespace(PathPart pp, String targetNS) {
    switch( pp ) {
      case basepath :
        return generationParameter.getBasePath();
      case name :
        return generationParameter.getDataModelName();
      default:
    }
    
    //Namespace-Auswertung
    if( isEmpty(targetNS) ) {
      return ""; //TODO emptyNs-path
    }
    Matcher m = usualNamespacePattern.matcher(targetNS);
    if( m.matches() ) {
      switch( pp ) {
        case nshost:
          return m.group(1);
        case nspath:
          return m.group(2);
        case nspath_noversion:
          String pnv = "/"+m.group(2)+"/";
          return pnv.replaceAll("/\\d*\\.?\\d*/", "/"); //TODO weitere Versions-Pattern "v\\d+"?
        case nsrevhost:
          String[] parts = m.group(1).split("\\.");
          StringBuilder path = new StringBuilder();
          for( int p=parts.length-1; p >= 0; --p ) {
            path.append(parts[p]).append(".");
          }
          return path.toString();
      }
    } else {
      if (targetNS.contains("://")) {
        targetNS = targetNS.substring(targetNS.indexOf("://") + 3);
      }
      switch( pp ) {
        case nshost:
          return "";
        case nspath:
          return targetNS;
        case nspath_noversion:
          return targetNS;
        case nsrevhost:
          return "";
      }
    }
    return ""; //dann halt nichts ausgeben
  }
 
  private String createPathReplacementType(PathPart pp, String type) {
    Matcher m = null;
    switch( pp ) {
      case type_prefix :
        m = COMMON_PREFIX_PATTERN.matcher(type);
        if( m.matches() ) {
          return m.group(1).toLowerCase(); 
        } else {
          return "";
        }
      case type_suffix :
        m = COMMON_SUFFIX_PATTERN.matcher(type);
        if( m.matches() ) {
          return m.group(1).toLowerCase(); 
        } else {
          return "";
        }
      default:
        return ""; //dann halt nichts ausgeben
    }
  } 
  
  public List<String> listXmomPaths() {
    List<String> xmomPaths = new ArrayList<String>(this.xmomPaths);
    Collections.sort(xmomPaths);
    return xmomPaths;
  }


}
