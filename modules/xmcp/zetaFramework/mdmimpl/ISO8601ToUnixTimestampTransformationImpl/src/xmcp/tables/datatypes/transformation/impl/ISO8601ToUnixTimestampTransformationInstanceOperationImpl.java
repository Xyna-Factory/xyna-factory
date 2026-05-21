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
package xmcp.tables.datatypes.transformation.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchFieldException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import xmcp.tables.datatypes.transformation.ColumnFilter;
import xmcp.tables.datatypes.transformation.ColumnPath;
import xmcp.tables.datatypes.transformation.ISO8601DateTime;
import xmcp.tables.datatypes.transformation.UnixTimestamp;
import xmcp.tables.datatypes.transformation.ISO8601ToUnixTimestampTransformationSuperProxy;
import xmcp.tables.datatypes.transformation.ISO8601ToUnixTimestampTransformationInstanceOperation;
import xmcp.tables.datatypes.transformation.ISO8601ToUnixTimestampTransformation;

import xmcp.tables.datatypes.transformation.impl.parser.DateTimeFilterLexer;
import xmcp.tables.datatypes.transformation.impl.parser.DateTimeFilterParser;


public class ISO8601ToUnixTimestampTransformationInstanceOperationImpl extends ISO8601ToUnixTimestampTransformationSuperProxy implements ISO8601ToUnixTimestampTransformationInstanceOperation {

  private static final long serialVersionUID = 1L;

  public ISO8601ToUnixTimestampTransformationInstanceOperationImpl(ISO8601ToUnixTimestampTransformation instanceVar) {
    super(instanceVar);
  }

  public ColumnFilter transformRequest(ColumnFilter columnFilter1) {
    var filter = ISO8601ToUnixTimestampTransformationInstanceOperationImpl.apply(columnFilter1.getValue(), null);

    // post process filter result:
    // ===========================
    if (filter != null && !filter.isEmpty()) {
      // replace " and white spaces
      filter = filter.replace("\"", "");
      filter = filter.replace(" ", "");

      // remove braces
      filter = filter.replace("(", "");
      filter = filter.replace(")", "");

      // remove &&
      filter = filter.replace("&&", "");

      // remove = because the TableHelper does not support <= and >=, yet
      filter = filter.replace("=", "");
    }

    columnFilter1.setValue(filter);
    return columnFilter1;
  }

  public ColumnFilter transformRequestWithPath(ColumnFilter columnFilter5, ColumnPath columnPath7) {
    // Implemented as code snippet!
    return null;
  }

  public ISO8601DateTime transformResponse(UnixTimestamp unixTimestamp11) {
    var d = new java.util.Date(unixTimestamp11.getValue());
    var formatter = new java.text.SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
    var dateTime = new ISO8601DateTime.Builder().instance();
    dateTime.setValue(formatter.format(d));
    return dateTime;
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


  public static String apply(final String filter, final String path/*, final String type*/) {

    if (filter == null || "".equals(filter.trim()))
      return null;

    // if (path == null || "".equals(path.trim()))
    //   throw new IllegalArgumentException("path can not be empty.");

    final List<String> errorMessages = new ArrayList<String>();
    BaseErrorListener listener = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
           String msg, RecognitionException e)
           {
             errorMessages.add("error at position" + String.valueOf(charPositionInLine) + ": " + msg);
           }
    };

    final DateTimeFilterLexer lex = new DateTimeFilterLexer(CharStreams.fromString(filter, "TransformDateTimeFilterConditionToTimestampInstanceOperationImpl"));
    lex.removeErrorListeners();
    lex.addErrorListener(listener);

    final CommonTokenStream tokens = new CommonTokenStream(lex);
    final DateTimeFilterParser p = new DateTimeFilterParser(tokens);
    p.removeErrorListeners();
    p.addErrorListener(listener);

    final DateTimeFilterVisitorImpl v = new DateTimeFilterVisitorImpl(path);

    String result = v.visitStart(p.start());

    if (errorMessages.size() > 0)
       throw new RuntimeException(errorMessages.stream().reduce("", String::concat));

    return result;
  };

}
