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

package com.gip.xyna.xnwh.persistence.memory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.memory.index.ConditionType;
import com.gip.xyna.xnwh.persistence.memory.index.Index;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.Condition;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.Operator;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.OrderBy;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.ParsedQuery;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.PreparedQueryCompilationException;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.PreparedQueryBuildException;
import com.gip.xyna.xnwh.persistence.memory.sqlparsing.PreparedQueryParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class PreparedQueryCreator {

  private static final Logger logger = CentralFactoryLogging.getLogger(PreparedQueryCreator.class);
  private static final String PREPARED_QUERY_FOR_MEMORY_CLASS = PreparedQueryForMemory.class.getSimpleName();
  private static final String PREPARED_COUNT_QUERY_FOR_MEMORY_CLASS = PreparedCountQueryForMemory.class.getSimpleName();
  private static final String PARAMETER = Parameter.class.getSimpleName();
  private static final String MEMORY_BASE_RESULTSET_CLASSNAME = MemoryBaseResultSet.class.getSimpleName();
  private static final String STORABLE = Storable.class.getSimpleName();

  private AtomicInteger cnt = new AtomicInteger(0);
  private GeneratedMemoryClassesClassLoader cl;
  private final long uniqueId;
  private String indexColumnName = "";

  PreparedQueryCreator(long uniqueId) throws PersistenceLayerException {
    this.uniqueId = uniqueId;
    File persdir = new File(Constants.PERSISTENCE_GEN_CLASSES_CLASSDIR);
    if (!persdir.exists()) {
      persdir.mkdirs();
      logger.debug("created directory " + Constants.PERSISTENCE_GEN_CLASSES_CLASSDIR + " for generated query-classes of "
          + XynaMemoryPersistenceLayer.class.getSimpleName());
    }
    cl = new GeneratedMemoryClassesClassLoader(PreparedQueryCreator.class.getClassLoader());
  }

  public <E> Class<IPreparedQueryForMemory<E>> createClass(Query<E> query, TableInfo t)
                  throws PersistenceLayerException {

    String classPath = Constants.PERSISTENCE_GEN_CLASSES_PACKAGE + ".i" + uniqueId;
    String className = "T" + query.getTable() + cnt.getAndAdd(1);

    // parse
    ParsedQuery pq;
    try {
      pq = parse(query.getSqlString());
    } catch (PreparedQueryParsingException e) {
      throw new XNWH_GeneralPersistenceLayerException("Failed to parse the following query: " + query.getSqlString(), e);
    }

    // generate
    CodeBuffer cb;
    try {
      cb = generateJava(pq, classPath, className, t);
    } catch (PreparedQueryBuildException e) {
      throw new XNWH_GeneralPersistenceLayerException("Failed to create prepared query for the following query: "
          + query.getSqlString(), e);
    }
    String fqClassName = classPath + "." + className;
    saveJavaToFile(cb, fqClassName);

    try {
      // compile    
      compile(fqClassName, t.getBackingClass().getClassLoader(), query.getReader().getClass().getClassLoader());
    } catch (PreparedQueryCompilationException e) {
      throw new XNWH_GeneralPersistenceLayerException("Failed to create prepared query for the following query: "
          + query.getSqlString(), e);
    } finally {
      delete(fqClassName);
    }

    Class<IPreparedQueryForMemory<E>> c = loadClass(fqClassName);
    return c;

  }


  private static final String _METHODNAME_IS_FOR_UPDATE_ORIG = "isForUpdate";
  private static final String METHODNAME_IS_FOR_UPDATE;
  
  private static final String _METHODNAME_GET_NEW_RESULTSET_ORIG = "getNewResultSet";
  private static final String METHODNAME_GET_NEW_RESULTSET;
  
  private static final String _METHODNAME_GET_PERSISTENCELAYER_ORIG = "getPersistenceLayer";
  private static final String METHODNAME_GET_PERSISTENCELAYER;
  
  private static final String _METHODNAME_CHECK_WHERECLAUSE_ORIG = "checkWhereClause";
  private static final String METHODNAME_CHECK_WHERECLAUSE;
  
  private static final String _METHODNAME_GET_COMPARATOR_ORIG = "getComparator";
  private static final String METHODNAME_GET_COMPARATOR;
  
  private static final String _METHODNAME_IS_ORDERED_ORIG = "isOrdered";
  private static final String METHODNAME_IS_ORDERED;
  
  private static final String _METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX_ORIG = "orderByDiffersFromIndex";
  private static final String METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX;
  
  private static final String _METHODNAME_IS_INDEX_ORDER_REVERSED_ORIG = "isIndexOrderReversed";
  private static final String METHODNAME_IS_INDEX_ORDER_REVERSED;
  
  
  private static final String _METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN_ORIG = "getFixedTargetValueOfIndexedColumn";
  private static final String METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN;
  
  private static final String _METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER_ORIG = "getPositionOfIndexedParameter";
  private static final String METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER;
  
  private static final String _METHODNAME_GET_INDEXED_COLUMN_INDEX_ORIG = "getIndexedColumnIndex";
  private static final String METHODNAME_GET_INDEXED_COLUMN_INDEX;
  
  private static final String _METHODNAME_GET_INDEXED_CONDITION_TYPE_ORIG = "getIndexedConditionType";
  private static final String METHODNAME_GET_INDEXED_CONDITION_TYPE;
  static {
    //methoden namen auf diese art gespeichert k�nnen von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_IS_FOR_UPDATE = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_IS_FOR_UPDATE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_FOR_UPDATE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_NEW_RESULTSET = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_NEW_RESULTSET_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_NEW_RESULTSET_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_PERSISTENCELAYER = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_PERSISTENCELAYER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_PERSISTENCELAYER_ORIG + " not found", e);
    }
    try {
      METHODNAME_CHECK_WHERECLAUSE = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_CHECK_WHERECLAUSE_ORIG, Storable.class, Parameter.class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_CHECK_WHERECLAUSE_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_COMPARATOR = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_COMPARATOR_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_COMPARATOR_ORIG + " not found", e);
    }
    try {
      METHODNAME_IS_ORDERED = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_IS_ORDERED_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_ORDERED_ORIG + " not found", e);
    }
    try {
      METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX_ORIG + " not found", e);
    }
    try {
      METHODNAME_IS_INDEX_ORDER_REVERSED = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_IS_INDEX_ORDER_REVERSED_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_IS_INDEX_ORDER_REVERSED_ORIG + " not found", e);
    }   
    try {
      METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_INDEXED_COLUMN_INDEX = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_INDEXED_COLUMN_INDEX_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_INDEXED_COLUMN_INDEX_ORIG + " not found", e);
    }
    try {
      METHODNAME_GET_INDEXED_CONDITION_TYPE = PreparedQueryForMemory.class.getDeclaredMethod(_METHODNAME_GET_INDEXED_CONDITION_TYPE_ORIG).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_GET_INDEXED_CONDITION_TYPE_ORIG + " not found", e);
    }
    
  }


  CodeBuffer generateJava(ParsedQuery parsedQuery, String path, String className, TableInfo t)
      throws PreparedQueryBuildException {

    CodeBuffer cb = new CodeBuffer("Xyna Factory Warehouse");
    cb.addLine("package ", path);
    cb.addLB();
    cb.addLine("import ", Storable.class.getName());
    if (parsedQuery.isCountQuery()) {
      cb.addLine("import ", PreparedCountQueryForMemory.class.getName());
    } else {
      cb.addLine("import ", PreparedQueryForMemory.class.getName());
    }
    cb.addLine("import ", Comparator.class.getName());
    cb.addLine("import ", MemoryRowData.class.getName());
    cb.addLine("import ", MemoryBaseResultSet.class.getName());
    cb.addLine("import ", Parameter.class.getName());
    cb.addLine("import ", SQLException.class.getName());
    cb.addLine("import ", Query.class.getName());
    cb.addLine("import ", PersistenceLayer.class.getName());
    cb.addLine("import ", Pattern.class.getName());
    cb.addLine("import ", UnderlyingDataNotFoundException.class.getName());

    cb.addLine("import ", Index.class.getName());
    cb.addLine("import ", ConditionType.class.getName());

    cb.addLB();

    if (!parsedQuery.isCountQuery()) {
      cb.addLine("public class ", className, " extends ", PREPARED_QUERY_FOR_MEMORY_CLASS, "  {");
    } else {
      cb.addLine("public class ", className, " extends ", PREPARED_COUNT_QUERY_FOR_MEMORY_CLASS, " {");
    }
    cb.addLB();

    //konstruktor
    cb.addLine("public ", className, "(", Query.class.getSimpleName(), " query, ", PersistenceLayer.class.getSimpleName(),
               " persistenceLayer) {");
    cb.addLine("super(query, persistenceLayer)");
    cb.addLine("}").addLB();

    if (!parsedQuery.isCountQuery()) {

      cb.addLine("public boolean ", METHODNAME_IS_FOR_UPDATE, "() {");
      cb.addLine("return " + parsedQuery.isForUpdate());
      cb.addLine("}").addLB();

      // getter for resultset
      cb.addLine("protected ", MEMORY_BASE_RESULTSET_CLASSNAME, " ", METHODNAME_GET_NEW_RESULTSET, "() {");
      cb.addLine("return new MyResultSet(", METHODNAME_GET_PERSISTENCELAYER, "(), " + parsedQuery.isForUpdate() + ")");
      cb.addLine("}").addLB();

      MemoryResultSetCreator.create(cb, t, "MyResultSet", MEMORY_BASE_RESULTSET_CLASSNAME);

      // end of resultset
      cb.addLine("}").addLB();

    }

    handleConditions(cb, parsedQuery.getWhereClause(), t, new AtomicInteger(-1), false);

    boolean existingWhereClause = parsedQuery.getWhereClause() != null;
    Map<String, String> conditionBooleanStrings;
    if (existingWhereClause
        && (conditionBooleanStrings = parsedQuery.getWhereClause().generateWhereConditionBooleans(t, 0)) != null) {

      for (Entry<String, String> condition : conditionBooleanStrings.entrySet()) {
        cb.addLine("private boolean ", condition.getKey(), "(", STORABLE, " s", ", ", PARAMETER, " p) {");
        cb.addLine("return ", condition.getValue());
        cb.addLine("}").addLB();
      }

      cb.addLine("protected boolean ", METHODNAME_CHECK_WHERECLAUSE, "(", STORABLE, " s, ", PARAMETER, " p) {");
      // TODO add check for parameter object (not null and length) to avoid NullPointerException/ArrayIndexOutOfBoundsException 

      String ifClause = "if ";
      String condition = parsedQuery.getWhereClause().generateWhereConditionExpression(t, 0);

      boolean needExtraBraces = false;
      int helper = 0;
      for (int i = 0; i < condition.length(); i++) {
        if (condition.charAt(i) == '(') {
          helper++;
        } else if (condition.charAt(i) == ')') {
          helper--;
        }
        if (helper == 0 && i != 0 && i < condition.length() - 1) {
          needExtraBraces = true;
        }
      }

      if (!needExtraBraces && condition.startsWith("(") && condition.endsWith(")")) {
        ifClause += condition;
      } else {
        ifClause += "(" + condition + ")";
      }
      cb.addLine(ifClause, " {");
      cb.addLine("return true");
      cb.addLine("}");
      cb.addLine("return false");

    } else {
      cb.addLine("protected boolean ", METHODNAME_CHECK_WHERECLAUSE, "(", STORABLE, " s, ", PARAMETER, " p) {");
      cb.addLine("return true");
    }
    cb.addLine("}").addLB();
    
    OrderBy indexOrder = null;
    
    if (parsedQuery.getOrderBys().length > 0) {
      cb.addLine("private static ", Comparator.class.getSimpleName() ," comparator").addLB();

      cb.addLine("public ", Comparator.class.getSimpleName(), " ", METHODNAME_GET_COMPARATOR, "() {");
      cb.addLine("if (comparator == null) {");
      cb.addLine("synchronized(", className, ".class) {");
      cb.addLine("if (comparator == null) {");

      cb.addLine("final ", PersistenceLayer.class.getSimpleName(), " persistenceLayerF = ", METHODNAME_GET_PERSISTENCELAYER, "()");
      cb.addLine("comparator = new ", Comparator.class.getSimpleName(), "() {").addLB();
      cb.addLine("public int compare(", Object.class.getSimpleName(), " o1, ",
                 Object.class.getSimpleName(), " o2) {");
      int cnt = 0;
      for (OrderBy orderBy : parsedQuery.getOrderBys()) {
        cnt++;
        /*        int p1 = ((OrderInstanceDetails) o1.getData()).getPriority();
        int p2 = ((OrderInstanceDetails) o2.getData()).getPriority();
        if (!(p1 == p2)) {
          return p1 > p2 ? 1 : -1;
        }
        if (s1 == null) {
          if (s2 == null) {
            return 0;
          }
          return 1;
        }
        if (s2 == null) {
          return -1;
        }
        String s1 = ((OrderInstanceDetails) o1.getData()).getCustom0();
        String s2 = ((OrderInstanceDetails) o2.getData()).getCustom0();
        if (!s1.equals(s2)) {
          return s1.compareTo(s2);
        }
         * 
         */
        
        if (orderBy.getColumnName().equals(indexColumnName)) {
          indexOrder = orderBy;
        }
        
        String bigger = orderBy.isAsc() ? "1" : "-1";
        String smaller = orderBy.isAsc() ? "-1" : "1";
        Class<?> type = null;
        String var1 = "v" + cnt + "_1";
        String var2 = "v" + cnt + "_2";
        boolean found = false;
        String getter = "";
        for (ColumnDeclaration cd : t.getColTypes()) {
          if (cd.getName().equalsIgnoreCase(orderBy.getColumnName())) {
            getter = cd.getGetter();
            type = cd.getType();
            found = true;
            break;
          }
        }
        if (!found) {
          throw new PreparedQueryBuildException("Column <" + orderBy.getColumnName() + "> unknown");
        }

        cb.addLine(type.getName(), " ", var1);
        cb.addLine(type.getName(), " ", var2);
        cb.addLine(var1, " = ((", t.getBackingClass().getCanonicalName(), ") o1).", getter, "()");
        cb.addLine(var2, " = ((", t.getBackingClass().getCanonicalName(), ") o2).", getter, "()");
        
        boolean isPrimitiveType = Condition.primitiveTypes.contains(type.getName());
        boolean isObjectOfPrimitiveType = Condition.objectsOfPrimitiveTypes.contains(type);
        if (isPrimitiveType || isObjectOfPrimitiveType) {
          if (isPrimitiveType) {
            cb.addLine("if (", var1, " != ", var2, ") {");
          } else {
            addNullChecksInComparator(cb, var1, var2);
            cb.addLine("if (!", var1, ".equals(", var2, ")) {");
          }
          if (type.getName().equalsIgnoreCase("boolean")) {
            cb.addLine("return ", var1, " ? ", bigger, " : ", smaller);
          } else {
            cb.addLine("return ", var1, " > ", var2, " ? ", bigger, " : ", smaller);
          }
        } else {
          addNullChecksInComparator(cb, var1, var2);
          cb.addLine("if (!", var1, ".equals(", var2, ")) {");
          if (Comparable.class.isAssignableFrom(type)) {
            cb.addLine("return ", (!orderBy.isAsc() ? "-" : ""), var1, ".compareTo(", var2, ")");
          } else {
            cb.addLine("return ", (!orderBy.isAsc() ? "-" : ""), var1, ".toString().compareTo(", var2, ".toString())");
          }
        }
        cb.addLine("}");
      }
      cb.addLine("return 0");
      cb.addLine("}");
      cb.addLine("};");
      cb.addLB();

      cb.addLine("}");
      cb.addLine("}");
      cb.addLine("}");
      cb.addLine("return comparator");
      cb.addLine("}").addLB();
      cb.addLine("public boolean ", METHODNAME_IS_ORDERED, "() {");
      cb.addLine("return true");
      cb.addLine("}").addLB();
    } else if (!parsedQuery.isCountQuery()) {
      cb.addLine("public ", Comparator.class.getSimpleName(), "<", MemoryRowData.class.getSimpleName(), "> getComparator() {");
      cb.addLine("return super.", METHODNAME_GET_COMPARATOR, "()");
      cb.addLine("}").addLB();
      cb.addLine("public boolean ", METHODNAME_IS_ORDERED, "() {");
      cb.addLine("return false");
      cb.addLine("}").addLB();
    }
    
    boolean orderByDiffersFromIndex;
    if (parsedQuery.getOrderBys().length <= 0) {
      orderByDiffersFromIndex = false;
    } else {
      if (parsedQuery.getOrderBys().length > 1) {
        orderByDiffersFromIndex = true;
      } else {
        if (parsedQuery.getOrderBys()[0].getColumnName().equals(indexColumnName) || indexColumnName.length() <= 0) {
          orderByDiffersFromIndex = false;
        } else {
          orderByDiffersFromIndex = true;
        }
      }
    }
    cb.addLine("protected boolean ", METHODNAME_ORDER_BY_DIFFERS_FROM_INDEX, "() {");
    if (orderByDiffersFromIndex) {
      cb.addLine("return true");
    } else {
      cb.addLine("return false");
    }
    cb.addLine("}").addLB();
    
    cb.addLine("protected boolean ", METHODNAME_IS_INDEX_ORDER_REVERSED, "() {");
    if ((indexOrder != null) && (!indexOrder.isAsc())) {
      cb.addLine("return true");
    } else {
      cb.addLine("return false");
    }
    
    cb.addLine("}").addLB();

    //end of class
    cb.addLine("}");
    return cb;
  }
  

  /**
   * @return true if the condition contained an index
   */
  private boolean handleConditions(CodeBuffer cb, Condition condition, TableInfo t, AtomicInteger parameterPositionHelper, boolean skipCodeGen) {
    if (condition != null) {
      if (checkIndexLocally(cb, condition, t, parameterPositionHelper, skipCodeGen)) {
        return true;
      }

      //check subconditions
      List<Condition> subconditions = condition.getConditions();
      if (subconditions != null) {
        conditionsLoop : for (int i = 0; i < subconditions.size(); i++) {
          if (subconditions.size() > i + 1 && condition.getOperators().get(i) == Operator.JAVA_OPERATOR_OR) {
            //falls conditions mit OR verbunden sind, kann man sie nicht f�r den index nutzen
            //trotzdem rest der condition parsen, um parameterpositionhelper korrekt zu z�hlen
            skipCodeGen = true;
          }
          Condition con = subconditions.get(i);
          if (subconditions != null) {
            boolean subConditionsContainedIndex = handleConditions(cb, con, t, parameterPositionHelper, skipCodeGen);
            if (subConditionsContainedIndex) {
              return true;
            } else {
              continue conditionsLoop;
            }
          }
        }
      }
    }
    return false;
  }


  private boolean checkIndexLocally(CodeBuffer cb, Condition con, TableInfo t, AtomicInteger parameterPositionHelper, boolean skipCodeGen) {
    if (con.getExpr1() != null && con.getExpr2() != null) {
      boolean isFixedValue = false;
      if (con.getExpr1().trim().equals("?") || con.getExpr2().trim().equals("?")) {
        parameterPositionHelper.getAndIncrement();
      } else {
        isFixedValue = true;
      }

      if (skipCodeGen) {
        return false;
      }
      int columnIndex = -1;
      for (ColumnDeclaration c : t.getColTypes()) {
        columnIndex++;
        if (c.getIndexType() != IndexType.NONE && (con.getExpr1().equals(c.getName()) || con.getExpr2().equals(c.getName()))) {

          ConditionType conType = null;
          switch (con.getConditionOperator()) {
            case EQUALS :
              conType = ConditionType.EQUALS;
              break;
            case SMALLER :
              conType = ConditionType.SMALLER;
              break;
            case GREATER :
              conType = ConditionType.BIGGER;
              break;
            default :
              return false;
          }

          if (isFixedValue) {
            cb.addLine("protected ", Comparable.class.getSimpleName(), "<?> ", METHODNAME_GET_FIXED_TARGETVALUE_OF_INDEXED_COLUMN, "() {");
            String type = c.getJavaType();
            String fixedValue;
            if (con.getExpr1().equals(c.getName())) {
              fixedValue = con.getExpr2();
            } else {
              fixedValue = con.getExpr1();
            }
            if (type.equalsIgnoreCase("long")) {
              cb.addLine("return Long.valueOf(\"", fixedValue, "\")");
            } else if (type.equals("int") || type.equals("Integer")) {
              cb.addLine("return Integer.valueOf(\"" + fixedValue, "\")");
            } else if (type.equalsIgnoreCase("double")) {
              cb.addLine("return Double.valueOf(\"" + fixedValue, "\")");
            } else if (type.equalsIgnoreCase("byte")) {
              cb.addLine("return Byte.valueOf(\"" + fixedValue, "\")");
            } else if (type.equalsIgnoreCase("boolean")) {
              cb.addLine("return Boolean.valueOf(\"" + fixedValue, "\")");
            } else if (type.equalsIgnoreCase("float")) {
              cb.addLine("return Float.valueOf(\"" + fixedValue, "\")");
            } else {
              cb.addLine("return \"", fixedValue, "\"");
            }
            cb.addLine("}").addLB();
          } else {
            cb.addLine("protected int ", METHODNAME_GET_POSITION_OF_INDEXED_PARAMETER, "() {");
            cb.addLine("return " + parameterPositionHelper.get());
            cb.addLine("}").addLB();
          }

          cb.addLine("protected int ", METHODNAME_GET_INDEXED_COLUMN_INDEX, "() {");
          cb.addLine("return " + columnIndex);
          cb.addLine("}").addLB();

          cb.addLine("protected ", ConditionType.class.getSimpleName(), " ", METHODNAME_GET_INDEXED_CONDITION_TYPE, "() {");
          cb.addLine("return ", ConditionType.class.getSimpleName(), ".", conType.name());
          cb.addLine("}").addLB();

          indexColumnName = c.getName();
          return true;

        }
      }
    }
    return false;
  }

  private static void addNullChecksInComparator(CodeBuffer cb, String var1, String var2) {
    cb.addLine("if (", var1, " == null) {");
    cb.addLine("if (", var2, " == null) {");
    cb.addLine("return 0");
    cb.addLine("}");
    cb.addLine("return 1");
    cb.addLine("}");
    cb.addLine("if (", var2, " == null) {");
    cb.addLine("return -1");
    cb.addLine("}");
  }


  ParsedQuery parse(String sqlString) throws PreparedQueryParsingException  {
    return new ParsedQuery(sqlString);
  }


  private void saveJavaToFile(CodeBuffer cb, String fqClassName) throws PersistenceLayerException {
    String s = cb.toString();
    try {
      GenerationBase.save(new String[] {s}, fqClassName);
    } catch (XynaException e) {
      throw new XNWH_GeneralPersistenceLayerException("could not save javafile <" + fqClassName + ">", e);
    }
  }


  private void compile(String fqClassName, ClassLoader... additionalClassLoader) throws PersistenceLayerException,
      PreparedQueryCompilationException {
    //erst urls sammeln f�r den classpath f�rs compile.
    //ausserdem bei dem hiesigen classloader alle additionalclassloader registrieren, 

    ClassLoader thisClassLoader = getClass().getClassLoader();
    while (thisClassLoader != null && !(thisClassLoader instanceof URLClassLoader)) {
      thisClassLoader = thisClassLoader.getParent();
    }
    //thisClassLoader ist der erste ClassLoader in der Hierarchie von hier aus Richtung BootClassLoader, der ein URLClassLoader ist
    if (thisClassLoader == null || !(thisClassLoader instanceof URLClassLoader)) {
      throw new RuntimeException("Failed to obtain jars of memory persistence layer");
    }
    HashSet<String> librariesContainingThis = new HashSet<String>();
    for (URL url : ((URLClassLoader) thisClassLoader).getURLs()) {
      librariesContainingThis.add(url.getFile());
    }
    
    if (additionalClassLoader != null) {      
      ClassLoader systemCL = ClassLoader.getSystemClassLoader();
      Set<ClassLoader> processedCLs = new HashSet<ClassLoader>();
      processedCLs.add(thisClassLoader);
      
      for (ClassLoader cl : additionalClassLoader) {
               
        while (!processedCLs.contains(cl)) {

          if (cl instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) cl).getURLs()) {
              librariesContainingThis.add(url.getFile());
            }
          }
          processedCLs.add(cl);
          if (cl == systemCL) {
            break;
          }
          cl = cl.getParent();
        }
      }
    }
    String classPath = GenerationBase.flattenClassPathSet(librariesContainingThis);
    
    try {
      GenerationBase.compile(fqClassName, classPath, Constants.PERSISTENCE_GEN_CLASSES_CLASSDIR);
    } catch (XynaException e) {
      throw new PreparedQueryCompilationException("compileerror", e);
    }
  }


  private void delete(String fqClassName) throws PersistenceLayerException {
    GenerationBase.deleteGeneratedJavaUnlocked(fqClassName);
  }


  /**
   * zus�tzliche url um generierte klassen zu finden
   */
  private static class GeneratedMemoryClassesClassLoader extends URLClassLoader {

    public GeneratedMemoryClassesClassLoader(ClassLoader parent) throws PersistenceLayerException {
      super(getUrls(), parent);
    }

  }


  private static URL[] getUrls() throws PersistenceLayerException {
    try {
      return new URL[] {new File(Constants.PERSISTENCE_GEN_CLASSES_CLASSDIR).toURL()};
    } catch (MalformedURLException e) {
      throw new RuntimeException("problem creating classloader", e);
    }
  }


  private <E> Class<IPreparedQueryForMemory<E>> loadClass(String className) throws PersistenceLayerException {
    try {
      return (Class<IPreparedQueryForMemory<E>>) cl.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("generated class not found with classloader " + cl, e);
    }
  }

}
