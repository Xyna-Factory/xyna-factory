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
package xfmg.xfctrl.datamodel.csv.impl.fields;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;

import xfmg.xfctrl.datamodel.csv.impl.CSVXynaObjectWriter.Header;
import xfmg.xfctrl.datamodel.csv.impl.fields.XmomField.FieldType;
import xfmg.xfctrl.datamodel.csv.parameter.StringTransformation;

public class XmomFieldParser {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(XmomFieldParser.class);

  private Set<String> ignoredPaths;
  private Set<String> ignoredPathsUsed;
  private Map<String, StringTransformation> stringTransformations;
  private List<XmomField> xmomFields = new ArrayList<>();
  private Map<XmomListField, XmomFieldParser> xmomListFields = new HashMap<>();
  
  
  public XmomFieldParser(Set<String> ignoredPaths, Map<String, StringTransformation> stringTransformations) {
    this.ignoredPaths = ignoredPaths;
    this.stringTransformations = stringTransformations;
  }

  public void parse(String fqTypeName) {
    throw new UnsupportedOperationException();
  }

  public void parse(Class<? extends GeneralXynaObject> xoClass) throws XPRC_InvalidPackageNameException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
    GenerationBaseCache cache = new GenerationBaseCache();
    String fqXmlName = xoClass.getAnnotation(XynaObjectAnnotation.class).fqXmlName();
    long revision = RevisionManagement.getRevisionByClass(xoClass);
    DomOrExceptionGenerationBase dom = DOM.getOrCreateInstance(fqXmlName, cache, revision);
    dom.parseGeneration(true, false, false);
    parse(dom);
  }
  
  
  
  private void parse(DomOrExceptionGenerationBase dom) {
    ignoredPathsUsed = new HashSet<>();
    parse(dom, new NoParentXmomField());
    //Validierung: Sind alle Angaben aus ignoredPaths und stringTransformations verwendet worden?
    List<String> unused = new ArrayList<>();
    for( String ip : ignoredPaths ) {
      if( ! ignoredPathsUsed.contains(ip) ) {
        unused.add(ip); 
      }
    }
    Set<String> usedPath = new HashSet<>();
    for( XmomField xf : xmomFields ) {
      usedPath.add( xf.getPath() );
    }
    for( String sp : stringTransformations.keySet() ) {
      if( ! usedPath.contains(sp) ) {
        unused.add(sp); 
      }
    }
    if( ! unused.isEmpty() ) {
      throw new IllegalStateException("Invalid paths: "+unused);
    }
  }

  public List<XmomField> getXmomFields() {
    return xmomFields;
  }
  public Map<XmomListField, XmomFieldParser> getXmomListFields() {
    return xmomListFields;
  }
  
  
  private void parse(DomOrExceptionGenerationBase dom, XmomField parent) {
    if( stringTransformations.containsKey(parent.getPath() ) ) {
      logger.info("StringTrafo " + parent.getPath()  );
      xmomFields.add( new XmomFieldStringTransformation(parent, stringTransformations.get(parent.getPath() ) ) );
      return; //StringTransformation, deswegen keine Unter-Felder suchen
    }
    for (AVariable v : dom.getAllMemberVarsIncludingInherited()) {
      parse(v, parent);
    }
  }
  
  private void parse(AVariable v, XmomField parent) {
    String path = parent.getPath()+"."+v.getVarName();
    if( ignoredPaths.contains(path) ) {
      ignoredPathsUsed.add(path);
      return;
    }
    if( v.isJavaBaseType() ) {
      if( v.isList() ) {
        logger.trace("Adding new baseType-list-field under " + path + ": " + v.getVarName());
        xmomListFields.put( new XmomListField(v.getLabel(), v.getVarName(), parent, transformType(v), null), new XmomFieldParser(Collections.emptySet(), Collections.emptyMap()));
      } else {
        logger.trace("Adding new baseType-field under " + path + ": " + v.getVarName());
        xmomFields.add( new XmomSimpleField(v.getLabel(), v.getVarName(), parent, transformType(v), null) );
      }
    } else if( v.getDomOrExceptionObject() != null ) {
      XmomField xf = null;
      XmomFieldParser xfp;
      if( v.isList() ) {
        logger.trace("Adding new " + v.getFQClassName() + "-list-field under " + path + ": " + v.getVarName());
        XmomListField xlf = new XmomListField(v.getLabel(), v.getVarName(), parent, null, v.getFQClassName());
        xfp = new XmomFieldParser(ignoredPaths, stringTransformations);
        xfp.ignoredPathsUsed = new HashSet<>();
        xmomListFields.put( xlf, xfp );
        xf = xlf;
      } else {
        logger.trace("Adding new " + v.getFQClassName() + "-field under " + path + ": " + v.getVarName());
        xf = new XmomSimpleField(v.getLabel(), v.getVarName(), parent, null, v.getFQClassName());
        xfp = this;
      }
      DomOrExceptionGenerationBase dom = v.getDomOrExceptionObject();
      xfp.parse( dom, xf);
      ignoredPathsUsed.addAll(xfp.ignoredPathsUsed);
    }
  }

  private FieldType transformType(AVariable v) {
    switch (v.getJavaTypeEnum()) {
      case BOOLEAN :
      case BOOLEAN_OBJ :
        return FieldType.BOOLEAN;
      case DOUBLE :
      case DOUBLE_OBJ :
        return FieldType.DOUBLE;
      case INT :
      case INTEGER :
        return FieldType.INT;
      case LONG :
      case LONG_OBJ :
        return FieldType.LONG;
      case STRING :
        return FieldType.STRING;
      default :
        throw new RuntimeException("unexpected member var type: " + v.getJavaTypeEnum());
    }
  }

  public XmomField findField(String headerVal, Header htype) {
    logger.debug("findField: " + headerVal);
    for (XmomField f : xmomFields) {
      if (matches(f, headerVal, htype)) {
        logger.debug("Match!");
        return f;
      }
    }
    return null;
  }

  private boolean matches(XmomField f, String headerVal, Header htype) {
    if( logger.isTraceEnabled() ) logger.trace("matches "+ headerVal + " ? label "+ f.getLabel() + " path " + f.getPath());
    switch (htype) {
      case LABELS :
        return f.getLabel().equals(headerVal);
      case PATHS :
        return f.getPath().equals(headerVal);
      case NONE :
        throw new RuntimeException("unsupported header type " + htype.name());
    }
    return false;
  }

  public XmomField findListField(String headerVal, Header htype) {
    for (Entry<XmomListField, XmomFieldParser> xmomListFieldEntry : xmomListFields.entrySet()) {
      XmomFieldParser subParser = xmomListFieldEntry.getValue();
      XmomField field = subParser.findField(headerVal, htype);
      if (field != null) {
        return field;
      }
      field = subParser.findListField(headerVal, htype);
      if (field != null) {
        return field;
      } else {
        if (headerVal.equals(xmomListFieldEntry.getKey().getPath())) {
          return xmomListFieldEntry.getKey();
        }
      }
    }
    return null;
  }

}
