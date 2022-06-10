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
package com.gip.xyna.xnwh.persistence.xmom.generation;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.compile.JavaSourceFromString;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.MaxLengthRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.RestrictionType;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restrictions;

public class StorableCodeBuilder {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(StorableCodeBuilder.class);
  
  public static final String STORABLE_CLASS_SUFFIX = "_Storable";
  public static final String COL_TYPENAME = "typename";

  private XMOMStorableStructureInformation rootDatatype;
  
  public StorableCodeBuilder(XMOMStorableStructureInformation rootDatatype) { 
    this.rootDatatype = rootDatatype;
  }
  
  
  /**
   * erstellt public static inner class die ein zu dem datentypen passendes storable beschreibt und zugehörige kompatibilitäts-resultsetreader  
   */
  public void generateStorableCode() throws PersistenceLayerException {
    //List of all used tablenames with their paths. The inner Array must have length 2 with the following attributes:
    //0: Tablename, 1: Path
    CodeGenerationVisitor cgv = new CodeGenerationVisitor();
    rootDatatype.traverse(cgv);
  }


  protected void createStorable(XMOMStorableStructureInformation rootXMOMStorable, StorableStructureInformation currentStructure, String path,
                                          boolean isList, List<Pair<String, String>> usedTableNames) {
    CodeBuffer cb = new CodeBuffer("xnwh");
    cb.addLine("package " + GenerationBase.getPackageNameFromFQName(currentStructure.getFqClassNameForStorable())).addLB();
    for (String anImport : DOM.getBasicImports()) {
      cb.addLine("import ", anImport);
    }
    cb.addLB();
    
    StorableStructureInformation rootBaseStorabeType = currentStructure.getSuperRootStorableInformation();
    
    String tableName = rootBaseStorabeType.getTableName();
    usedTableNames.add(Pair.of(tableName, path));
    
    String storableClassName = getSimpleClassNameForStorable(currentStructure, tableName);
    //!!!!!!!!!!! ACHTUNG! absichtlich "+" konkateniert, ansonsten wird am ende ein semikolon angehängt 
    if (!currentStructure.hasSuper()) {
      cb.addLine("@" + Persistable.class.getName() + "(tableName = " + storableClassName + ".TABLE_NAME, primaryKey = "
                      + storableClassName + ".COLUMN_PK)");
    }
    if (!currentStructure.hasSuper()) {
      cb.addLine("public class ", storableClassName, " extends ", Storable.class.getCanonicalName(),
                 "<", storableClassName, "> {");
    } else {
      cb.addLine("public class ", storableClassName, " extends ", getClassNameForStorable(currentStructure.getSuperEntry().getInfo(), tableName) , " {");
    }
    cb.addLB();
    cb.addLine("private static final long serialVersionUID = 1L").addLB();
    cb.addLine("public static final String TABLE_NAME = \"", tableName, "\"");

    if (!currentStructure.hasSuper()) {
      cb.addLine("/**");
      cb.addLine(" * equal to {@link COL_", currentStructure.getPrimaryKeyName().toUpperCase(), "}");
      cb.addLine(" */");
      cb.addLine("public static final String COLUMN_PK = \"", currentStructure.getPrimaryKeyName().toLowerCase(), "\"");
    }
    
    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumns()) {
      cb.addLine("public static final String COL_", column.getColumnName().toUpperCase(), " = \"", column.getColumnName(), "\"");
    }
    
    cb.addLB(2);

    //variablename, correspondingvariable
    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumns()) {
      String childPath = path.isEmpty() ? column.getVariableName() : path + "." + column.getVariableName();
      String typeInfo = "";
      /*code for blobbed exception
       * if (column.isStorableVariable()) {
        typeInfo = ", type = " + ColumnType.class.getName() + "." + ColumnType.BLOBBED_JAVAOBJECT.name();
      }*/
      
      //index stuff
      String indexInfo = "";
      if (rootXMOMStorable.getPersistenceInformation().getConstraints().contains(childPath)) {
        indexInfo = ", index = " + IndexType.class.getName() + ".";
        if (rootXMOMStorable.usesHistorization()) {
          indexInfo += IndexType.MULTIPLE.name();
        } else {
          indexInfo += IndexType.UNIQUE.name();
        }
      } else if (column.isStorableVariable() &&
                 column.getStorableVariableType() == StorableVariableType.REFERENCE) {
        indexInfo += IndexType.MULTIPLE.name();
      }
      if (rootXMOMStorable.getPersistenceInformation().getIndices().contains(childPath) ||
          column.isPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP) ||
          column.isPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG) ||
          (column.isPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER) && rootXMOMStorable.usesHistorization()) ||
          (column.isStorableVariable() && column.getStorableVariableType() == StorableVariableType.REFERENCE)) {
        indexInfo = ", index = " + IndexType.class.getName() + "." + IndexType.MULTIPLE.name();
      }
      if (column.getType() == VarType.UNIQUE_HELPER_COL) {
        indexInfo = ", index = " + IndexType.class.getName() + "." + IndexType.UNIQUE.name();
      } else if(column.getType() == VarType.REFERENCE_FORWARD_FK || 
                column.getType() == VarType.EXPANSION_PARENT_FK) {
        indexInfo = ", index = " + IndexType.class.getName() + "." + IndexType.MULTIPLE.name();
      }
      
      
      String sizeInfo = "";
      Restrictions restrictions = column.getRestrictions();
      if (restrictions != null &&
          restrictions.hasRestriction(RestrictionType.MAX_LENGTH) && 
          restrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH).getLimit() >= 0) {
        sizeInfo = ", size = " + restrictions.<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH).getLimit();
      }
      
      //!!!!!!!!!!! ACHTUNG! absichtlich "+" konkateniert, ansonsten wird am ende ein semikolon angehängt
      cb.addLine("@" + Column.class.getName() + "(name = COL_" + column.getColumnName().toUpperCase() + typeInfo + indexInfo + sizeInfo + ")");
      cb.addLine("public ", column.getClassName(), " ", column.getColumnName()).addLB(2);
      //getter
      cb.addLine("public ", column.getClassName(), " ", DOM.buildGetter(column.getColumnName()), "() {");
      cb.addLine("return ", column.getColumnName());
      cb.addLine("}").addLB(2);
      //setter
      cb.addLine("public void ", DOM.buildSetter(column.getColumnName()), "(", column.getClassName(), " ", column.getColumnName(), ") {");
      cb.addLine("this.", column.getColumnName(), " = ", column.getColumnName());
      cb.addLine("}").addLB(2);
    }
       
    //empty constructor
    cb.addLine("public ", storableClassName, "() {");
    cb.addLine("}").addLB(2);
    
    //setAllFieldsFromData
    cb.addLine("public <U extends ", storableClassName, "> void setAllFieldsFromData(U data) {");
    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumns()) {
      cb.addLine("this.", column.getColumnName(), " = data.", DOM.buildGetter(column.getColumnName()), "()");
    }
    cb.addLine("}").addLB(2);

    
    if (!currentStructure.hasSuper()) {
      //getprimarykey methode
      cb.addLine("public Object getPrimaryKey() {");
      cb.addLine("return ", currentStructure.getPrimaryKeyName());
      cb.addLine("}").addLB(2);
    }

    
    //default resultsetreader
    cb.addLine("private ", ResultSetReader.class.getName(), "<", storableClassName, "> reader = new ",
               ResultSetReader.class.getName(), "() {");
    cb.addLB();
    cb.addLine("public ", storableClassName, " read(", ResultSet.class.getName(), " rs) throws ",
               SQLException.class.getName(), " {");
    cb.addLine(storableClassName, " result = new ", storableClassName, "()");

    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumnsRecursivly()) {
      if (column.isStorableVariable()) {
        cb.addLine("result.", DOM.buildSetter(column.getColumnName()), "((", column.getClassName(),
                   ") result.readBlobbedJavaObjectFromResultSet(rs, ", getClassNameForStorable(column.getParentStorableInfo(), tableName), ".COL_", column.getColumnName().toUpperCase(), "))");        
      } else {
        createCodeForResultSetGetter(cb, column.getColumnName(), column.getPrimitiveType(), getClassNameForStorable(column.getParentStorableInfo(), tableName) + ".COL_" + column.getColumnName().toUpperCase(), false,
                                     "result", false);
      }
    }
    
    cb.addLine("return result");
    cb.addLine("}");
    cb.addLB();
    cb.addLine("};").addLB(2);
    cb.addLine("public ", ResultSetReader.class.getName(), "<? extends ", storableClassName, "> getReader() {");
    cb.addLine("return reader");
    cb.addLine("}").addLB(2);

    String localDomClassName = XynaObject.class.getSimpleName();
    //reader für datentypen
    cb.addLine("public static final ", ResultSetReader.class.getName(), "<", localDomClassName, "> ",
               DOM.READER_FOR_DATATYPE, " = new ", ResultSetReader.class.getName(), "<", localDomClassName, ">() {");
    cb.addLB();
    cb.addLine("public ", localDomClassName, " read(", ResultSet.class.getName(), " rs) throws ",
               SQLException.class.getName(), " {");
    if (currentStructure.isAbstract()) {
      cb.addLine("return null");
    } else {
      //falls flattened, müssen hier die kindobjekte mit erzeugt werden
      createTypesRecursively(cb, rootXMOMStorable, currentStructure, storableClassName, path, new AtomicInteger(0));
  
      //für foreignKey ist hier nichts zu tun
      cb.addLine("return result0");
    }
    cb.addLine("}");
    cb.addLB();
    cb.addLine("};").addLB(2);
    

    String basetypeStorableClassName = currentStructure.getParentXMOMStorableInformation().getSuperRootStorableInformation().getFqClassNameForStorable();
    
    if (isList) {
      if (currentStructure.isSyntheticStorable()) {
        StorableColumnInformation column = currentStructure.getColInfoByVarType(VarType.DEFAULT);
        if (column == null) {
          column = currentStructure.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
        }
        String listType = column.isStorableVariable() ? XynaObject.class.getName() : column.getPrimitiveType().getObjectClassOfType();
        
        cb.addLine("public static List<", storableClassName, "> ", DOM.TRANSFORMDATATYPE_METHODNAME, "(List<", listType, 
                   "> list) {");
        cb.addLine("if (list == null) {");
        cb.addLine("return null");
        cb.addLine("}");
        cb.addLine("List<", storableClassName, "> resultList = new ArrayList<", storableClassName, ">()");
        cb.addLine("for (int i = 0; i < list.size(); i++) {");
        cb.addLine(listType, " dt = list.get(i)");
        cb.addLine(storableClassName, " result = new ", storableClassName, "()");
        
        cb.addLine("if (dt == null) {"); // result.column might be primitive and value might be null
        cb.addLine("continue;");
        cb.addLine("}");
        
        cb.addLine("try {");
        if (column.isStorableVariable()) {
          if (rootXMOMStorable.usesHistorization() &&
              column.getStorableVariableInformation() instanceof XMOMStorableStructureInformation &&
              ((XMOMStorableStructureInformation)column.getStorableVariableInformation().getSuperRootStorableInformation()).usesHistorization()) { // liste auf historisierte reference
            StorableColumnInformation declaredUniqueIdColumn = column.getStorableVariableInformation().getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
            cb.addLine("String primaryKeyValue = String.valueOf(dt.get(\"", declaredUniqueIdColumn.getVariableName(), "\")) + \"_\"");
            cb.addLine("if (!(Boolean)dt.get(\"", column.getStorableVariableInformation().getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG).getVariableName(), "\")) {");
            cb.addLine("primaryKeyValue += dt.get(\"", column.getStorableVariableInformation().getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).getVariableName(), "\")");
            cb.addLine("}");
            cb.addLine("result.", column.getColumnName(), " = ", "primaryKeyValue");
            cb.addLine("} catch (", XynaException.class.getName()," xe) {");
            cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
          } else {
            cb.addLine("result.", column.getColumnName(), " = (", column.getPrimitiveType().getObjectClassOfType(), ") dt.get(\"", column
                .getStorableVariableInformation().getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER).getVariableName(),
                       "\")");
            cb.addLine("} catch (", InvalidObjectPathException.class.getName(), " xe) {");
            cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
          }
        } else {
          cb.addLine("result.", column.getColumnName(), " = (", column.getPrimitiveType().getObjectClassOfType(), ") dt");
        }
        cb.addLine("} catch (", NullPointerException.class.getName(), " xe) {");
        cb.addLine("}");
        
        
        cb.addLine("result.", currentStructure.getColInfoByVarType(VarType.LIST_IDX).getColumnName(), " = i");
        cb.addLine("resultList.add(result)");
        cb.addLine("}"); //for
        cb.addLine("return resultList");
        cb.addLine("}").addLB(2);
      } else {
        cb.addLine("public static ", storableClassName, " ", DOM.TRANSFORMDATATYPE_METHODNAME, "(", XynaObject.class.getSimpleName(), " listElement, " + basetypeStorableClassName + " basetypeStorable) {");
        if (!currentStructure.isAbstract()) {
          cb.addLine(XynaObject.class.getSimpleName(), " dt = listElement");
          cb.addLine("if (dt == null) {");
          cb.addLine("return null");
          cb.addLine("}");
        }
      }
    } else {
      cb.addLine("public static ", storableClassName, " ", DOM.TRANSFORMDATATYPE_METHODNAME, "(", localDomClassName, " dt, ", basetypeStorableClassName, " basetypeStorable) {");   
    }
    
   
    if (!currentStructure.isSyntheticStorable()) {
      if (currentStructure.isAbstract()) {
        cb.addLine("return null");
        cb.addLine("}").addLB(2); //transformDatatype
      } else {
        cb.addLine(storableClassName, " result = new ", storableClassName, "()");
        if (path.length() == 0) {
          //basetype = result
          cb.addLine("if (basetypeStorable == null) {");
          cb.addLine("basetypeStorable = result");
          cb.addLine("}");
        }
    
        cb.addLine("result.", DOM.FILLDIRECTMEMBERS_METHODNAME, "(dt)");
        cb.addLine("return result");
        cb.addLine("}").addLB(2); //transformDatatype
      }
    
      cb.addLine("public void ", DOM.FILLDIRECTMEMBERS_METHODNAME, "(", localDomClassName, " dt) {");
      if (currentStructure.hasSuper()) {
        cb.addLine("super.", DOM.FILLDIRECTMEMBERS_METHODNAME, "(dt)");
      }
      cb.addLine(storableClassName, " result = this");    // TODO  remove this line and change generation in fillStorableRecursively to operate on this instead of result
      fillStorableRecursively(cb, rootXMOMStorable, currentStructure, path, path);
      if (rootXMOMStorable.usesHistorization() &&
          currentStructure instanceof XMOMStorableStructureInformation && // expansive typen übernehmen histo-info einfach "über" parentUid
          !currentStructure.hasSuper()) {
        StorableColumnInformation declaredUniqueIdColumn = rootXMOMStorable.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
        cb.addLine("try {");
        cb.addLine("String primaryKeyValue = String.valueOf(dt.get(\"", declaredUniqueIdColumn.getVariableName(), "\")) + \"_\"");
        cb.addLine("if (!(Boolean)dt.get(\"", rootXMOMStorable.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG).getVariableName(), "\")) {");
        cb.addLine("primaryKeyValue += dt.get(\"", rootXMOMStorable.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).getVariableName(), "\")");
        cb.addLine("}");
        cb.addLine("result.", rootXMOMStorable.getSuperRootStorableInformation().getPrimaryKeyName(), " = ", "primaryKeyValue");
        cb.addLine("} catch (", XynaException.class.getName()," xe) { throw new ", RuntimeException.class.getName(),"(xe); }");
      }
      cb.addLine("}").addLB(2); //fillDirectMembers
    }
    
    
    cb.addLine("}").addLB(2); //storable
   
    String storableFqName = GenerationBase.getPackageNameFromFQName(currentStructure.getFqClassNameForDatatype()) + "." + storableClassName;
    JavaSourceFromString source = new JavaSourceFromString(storableFqName, currentStructure.getFqClassNameForDatatype(), cb.toString(), currentStructure.getDefiningRevision());
    currentStructure.setStorableSource(source);
  }
  
  
  public String getSimpleClassNameForStorable(StorableStructureInformation ssi, String tableName) {
    return GenerationBase.getSimpleNameFromFQName(getClassNameForStorable(ssi, tableName));
  }

  
  public String getClassNameForStorable(StorableStructureInformation ssi, String tableName) {
    try {
      return getClassNameForStorable(GenerationBase.transformNameForJava(ssi.getFqXmlName()), tableName, ssi.getDefiningRevision());
    } catch (XPRC_InvalidPackageNameException e) {
      throw new IllegalArgumentException(e);
    }
  }

  
  public static String getClassNameForStorable(String fqDatatypeClassName, String tableName, long xmomStorableBaseRevision) {
    StringBuilder sb = new StringBuilder();
    sb.append(fqDatatypeClassName)
      .append("_")
      .append(tableName.replace('.','_'))
      .append("_");
    if (xmomStorableBaseRevision < 0) {
      sb.append("_")
        .append(Math.abs(xmomStorableBaseRevision));
    } else {
      sb.append(xmomStorableBaseRevision);
    }
    sb.append(STORABLE_CLASS_SUFFIX);
    return sb.toString();
    //return fqDatatypeClassName + "_" + tableName.replace('.','_') + "_" + Math.abs(xmomStorableBaseRevision) + STORABLE_CLASS_SUFFIX;
  }
  
  
  private void fillStorableRecursively(CodeBuffer cb, XMOMStorableStructureInformation rootStructure, StorableStructureInformation currentStructure, 
                                       String path, String basePath) {
    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumnsForDatatypeReader()) {
      if (!column.isStorableVariable()) {
        if (column.isList()) {
          //nichts zu tun -> extra tabelle
          continue;
        } else {
        }
      } else {
        if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
          continue;
        } else if (column.isList()) {
          //expansiv -> kein flattening über listenwertige komplexe member hinweg
          continue;
        } else {
          //expansiv -> extra tabelle
          continue;
        }
      }
      
      if (column.isFlattened()) {
        cb.addLine("try {");
        cb.addLine("result.", column.getColumnName(), " = (",column.getPrimitiveType().getObjectClassOfType(),")dt.get(\"", column.getPath(), "\")");
        cb.addLine("} catch (", NullPointerException.class.getName(), " xe) {");
        cb.addLine("} catch (", InvalidObjectPathException.class.getName()," xe) {");
        cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
        cb.addLine("}");
        continue;
      }
      
      String listCast = "";
      if (column.isList() && column.isStorableVariable()) {
        listCast = "(List)";
      } else {
        listCast = "("+column.getPrimitiveType().getObjectClassOfType()+")";
      }
      if (column.getType() == VarType.UNIQUE_HELPER_COL) {
        StorableColumnInformation currentVersionColumn = currentStructure.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
        StorableColumnInformation historizationColumn = currentStructure.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
        cb.addLine("try {");
        cb.addLine("String _", column.getColumnName(), " = String.valueOf(result.", DOM.buildGetter(column.getCorrespondingUniqueIdentifierColumn().getColumnName()),
                     "()) + \"_\"");
        cb.addLine("if (!(",currentVersionColumn.getPrimitiveType().getObjectClassOfType(),")dt.get(\"", currentVersionColumn.getVariableName(), "\")) {");
        cb.addLine("_", column.getColumnName(), " += dt.get(\"", historizationColumn.getVariableName(), "\")");
        cb.addLine("}");
        cb.addLine("result.", column.getColumnName(), " = _", column.getColumnName());
        cb.addLine("} catch (", NullPointerException.class.getName(), " xe) {");
        cb.addLine("} catch (", InvalidObjectPathException.class.getName()," xe) {");
        cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
        cb.addLine("}");
      } else if (column.getType() == VarType.REFERENCE_FORWARD_FK) {
        //für die fks nichts zu tun, die foreignkeys können nicht gesetzt werden, weil man das parentobjekt hier nicht kennt.             
        //pk von expandierten objekten wird auch separat gesetzt 
      } else {
        // TODO childpath, basePath ? 
        cb.addLine("try {");
        cb.addLine("result.", column.getColumnName(), " = ", listCast, "dt.get(\"", column.getVariableName(), "\")");
        cb.addLine("} catch (", NullPointerException.class.getName(), " xe) {");
        cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
        cb.addLine("} catch (", InvalidObjectPathException.class.getName()," xe) {");
        cb.addLine("throw new ", RuntimeException.class.getName(),"(xe)");
        cb.addLine("}");
      }
    }
  }

  
  /**
   * erzeugt code der art
   * <pre>
   * result0 = new Type();
   * result0.setX(rs.getString(nameX))
   * result0.setY(rs.getObject(nameY)); //expansiv
   * result1 = new SubType(); //flattened
   * result1.setZ(rs.getString(nameZ));
   * result0.setW(result1);
   * </pre> 
   */
  private void createTypesRecursively(CodeBuffer cb, XMOMStorableStructureInformation rootStructure, StorableStructureInformation currentStructure,
                                      String storableClassName, String path, AtomicInteger cnt) {
    String tableName = currentStructure.getSuperRootStorableInformation().getTableName();
    int localCnt = cnt.getAndIncrement();
    
    cb.addLine(XynaObject.class.getSimpleName(), " result" + localCnt);
    cb.addLine("try {");
    if (GenerationBase.isReservedServerObjectByFqClassName(currentStructure.getFqClassNameForDatatype())) {
      cb.addLine(" result" + localCnt, " = (", XynaObject.class.getSimpleName(), ") Class.forName(\"", 
                 currentStructure.getFqClassNameForDatatype(), "\").getConstructor().newInstance();");
    } else {
      cb.addLine(" result" + localCnt, " = (", XynaObject.class.getSimpleName(), ") ", XynaFactory.class.getName(),
                 ".getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher().getMDMClassLoader(\"",
                 currentStructure.getFqClassNameForDatatype(), "\", " + currentStructure.getRevision(), "L, true).loadClass(\"",
                 currentStructure.getFqClassNameForDatatype(), "\").getConstructor().newInstance();");
    }
    cb.addLine("} catch (", Exception.class.getName(), "  xe) {");
    cb.addLine(" throw new ", RuntimeException.class.getName(),"(xe)");
    cb.addLine("}");
    
    for (StorableColumnInformation column : currentStructure.getAllRelevantStorableColumnsForDatatypeReaderRecursivly()) {
      if (column.getType() == VarType.UNIQUE_HELPER_COL) {
        continue;
      }
      if (!column.isStorableVariable()) {
        if (column.isList()) {
          //unten behandeln
        } else {
          String colConstant = getClassNameForStorable(column.getParentStorableInfo(), tableName) + ".COL_"+ column.getColumnName().toUpperCase();
          if (column.isFlattened()) {
            String[] paths = column.getPath().split("\\.");
            for (int i = 0; i < paths.length - 1; i++) {
              
              if (i < paths.length - 2) {
                // set in parent resultObj
              }
            }
            String type = "";
            if (column.getPrimitiveType() == PrimitiveType.INT || column.getPrimitiveType() == PrimitiveType.INTEGER) {
              type = "Int"; //result.getInteger gibts nicht
            } else {
              type = column.getPrimitiveType().getObjectClassOfType(); //resultSet.getter methode ist immer entsprechend des objektnamens
            }
            int rsCnt = cnt.getAndIncrement();
            cb.addLine("try {");
            String rsValueName = "rsValue" + String.valueOf(rsCnt);
            cb.addLine(Object.class.getName()," ",rsValueName," = rs.get",type,"(",colConstant,")");
            cb.addLine("if (rs.wasNull()) {");
            cb.addLine(rsValueName," = null");
            cb.addLine("}");
            cb.addLine(StorableCodeBuilder.class.getName(), ".initialisingSet(result",String.valueOf(localCnt),", \"",column.getPath(),"\", ",rsValueName,")");
            cb.addLine("} catch (", XynaException.class.getName()," xe) { }");
          } else {
            createCodeForResultSetGetter(cb, column.getVariableName(), column.getPrimitiveType(), colConstant, true, "result" + localCnt, true);
          }
          continue;
        }
      } else {
        if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
          //unten behandeln
        } else if (column.isList()) {
          //keine rekursion, wird als eigenes storable gespeichert
        } else {
          //expansiv 
        }
      }
      
      cb.addLine("try {");
      if (column.isFlattened()) {
        cb.addLine(StorableCodeBuilder.class.getName(), ".initialisingSet(result",String.valueOf(localCnt),", \"",column.getPath(),"\", rs.getObject(\"",column.getColumnName(),"\"))");
      } else {
        cb.addLine("result" + localCnt, ".set(\"", column.getVariableName() , "\",  rs.getObject(\"", column.getColumnName(), "\"))");
      }
      cb.addLine("} catch (", XynaException.class.getName()," xe) { throw new ", RuntimeException.class.getName(),"(xe); }");
    }
  }
  

  public static boolean isStorableReference(DOM xmomStorableRoot, String path) {
    return xmomStorableRoot.getPersistenceInformation().getReferences().contains(path);
  }
  
  
  /**
   * getter der form .getX().getY() für den relativen pfad von fullPath relativ zu basePath. 
   */
  static String createGetterForRelativePath(String fullPath, String basePath) {
    StringBuilder getter = new StringBuilder();
    String relativePath = fullPath.substring(basePath.length());
    if (relativePath.length() > 0 && relativePath.startsWith(".")) {
      relativePath = relativePath.substring(1);
    }
    for (String part : relativePath.split("\\.")) {
      getter.append(".").append(DOM.buildGetter(part)).append("()");
    }
    return getter.toString();
  }
  
  
  public static boolean isInheritedFromStorable(DOM node) {
    if (node.getSuperClassGenerationObject() != null) {
      DOM d = node.getSuperClassGenerationObject();
      while (d.getSuperClassGenerationObject() != null) {
        d = d.getSuperClassGenerationObject();
      }
      //d ist die zugrundeliegende basisklasse
      if (d.getFqClassName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
        return true;
      }
    }
    return false;
  }
  
  private static void createCodeForResultSetGetter(CodeBuffer cb, String varName, PrimitiveType varType, String colName,
                                                           boolean useSetter, String parentName, boolean unversionedSetter) {
     if (varType == null) {
       logger.trace("CodeBuffer content before error: " + cb.toString());
       throw new RuntimeException("Missing varType for " + parentName + "." + colName);
     }
     String type = "";
     if (varType == PrimitiveType.INT || varType == PrimitiveType.INTEGER) {
       type = "Int"; //result.getInteger gibts nicht
     } else {
       type = varType.getObjectClassOfType(); //resultSet.getter methode ist immer entsprechend des objektnamens
     }
     if (useSetter) {
       String setter = "set(\"" + varName + "\", ";
       cb.addLine("try {");
       cb.addLine(parentName, ".", setter, "rs.get", type, "(", colName, "))");
       if (varType.isObject()) {
         cb.addLine("if (rs.wasNull()) {");
         cb.addLine(parentName, ".", setter, "null)");
         cb.addLine("}");
       }
       cb.addLine("} catch (", XynaException.class.getName()," xe) { throw new ", RuntimeException.class.getName(),"(xe); }");
     } else {
       cb.addLine(parentName, ".", varName, " = rs.get", type, "(", colName, ")");
       if (varType.isObject()) {
         cb.addLine("if (rs.wasNull()) {");
         cb.addLine(parentName, ".", varName, " = null");
         cb.addLine("}");
       }
     }
   }
  
  
  private static final XynaPropertyBoolean readEmptyWrappers = new XynaPropertyBoolean("xnwh.persistence.xmom.flattening.legacy.readEmptyWrappers", false);
  
  // FIXME lieber PathPart & Type - Tupel im StructureCache speichern bei Flattening
  // dann kann man alles direkt in den Code generieren
  public static void initialisingSet(GeneralXynaObject target, String path, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND, InvalidObjectPathException {
    if (path.contains(".")) {
      if (!readEmptyWrappers.get() &&
          value == null) {
        return;
      }
      String[] pathParts = path.split("\\.");
      GeneralXynaObject current = target;
      for (int i = 0; i < pathParts.length; i++) {
        String pathPart = pathParts[i];
        if (i >= pathParts.length - 1) {
          if (value == null) {
            return;
          }
          current.set(pathPart, value);
        } else {
          GeneralXynaObject obj = (GeneralXynaObject) current.get(pathPart);
          if (obj == null) {
            Field field = findField(pathPart, current.getClass());
            field.setAccessible(true);
            try {
              field.set(current, field.getType().newInstance());
            } catch (InstantiationException e) {
              throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
            } catch (IllegalAccessException e) {
              throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(path));
            }
          }
          current = (GeneralXynaObject) current.get(pathPart);
        }
      }
    } else {
      if (value == null) {
        return;
      }
      target.set(path, value);
    }
  }
  
  private static Field findField(String pathPart, Class<? extends GeneralXynaObject> base) throws InvalidObjectPathException {
    Field field = null;
    Class<?> current = base;
    while (field == null &&
           current != null &&
           GeneralXynaObject.class.isAssignableFrom(current)) {
      try {
        field = current.getDeclaredField(pathPart);
      } catch (NoSuchFieldException e) {
        // ntb
      }
      current = current.getSuperclass();
    }
    if (field == null) {
      throw new InvalidObjectPathException(new XDEV_PARAMETER_NAME_NOT_FOUND(pathPart));
    } else {
      return field;
    }
  }
  
  private class CodeGenerationVisitor implements StorableStructureVisitor {
    
    private final Stack<XMOMStorableStructureInformation> rootStack;
    private final List<Pair<String, String>> usedTableNames;
    private final Stack<StorableColumnInformation> pathStack;
    
    CodeGenerationVisitor() {
      rootStack = new Stack<>();
      pathStack = new Stack<>();
      usedTableNames = new ArrayList<>();
    }

    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (current instanceof XMOMStorableStructureInformation) {
        rootStack.push((XMOMStorableStructureInformation)current);
      }
      boolean isList = false;
      if (columnLink != null) {
        pathStack.push(columnLink);
        isList = columnLink.isList();
      }
      String path = buildPath();
      createStorable(rootStack.peek(), current, path, isList, usedTableNames);
    }

    private String buildPath() {
      StringBuilder pathBuilder = new StringBuilder();
      StorableColumnInformation previous = null;
      Iterator<StorableColumnInformation> sciIter = pathStack.iterator();
      while (sciIter.hasNext()) {
        StorableColumnInformation sci = sciIter.next();
        if (previous != null &&
            previous == sci) { // extension hierarchies are vistied through the same link
          previous = sci;
          continue;
        }
        if (sci.isFlattened()) {
          pathBuilder.append(sci.getPath());
        } else {
          pathBuilder.append(sci.getVariableName());
        }
        if (sciIter.hasNext()) {
          pathBuilder.append(".");
        }
        if (sci.isStorableVariable() &&
            sci.getStorableVariableType() == StorableVariableType.REFERENCE) {
          pathBuilder.delete(0, pathBuilder.length()); // clear path on reference
        }
        previous = sci;
      }
      return pathBuilder.toString();
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
      if (current instanceof XMOMStorableStructureInformation) {
        rootStack.pop();
      }
      if (columnLink != null) {
        pathStack.pop();  
      }
      
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
  }
}
