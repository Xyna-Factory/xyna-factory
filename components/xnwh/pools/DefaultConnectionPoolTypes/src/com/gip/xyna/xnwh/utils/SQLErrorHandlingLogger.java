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
package com.gip.xyna.xnwh.utils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.gip.xyna.xnwh.exception.SQLRuntimeException;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.XynaSQLUtilsLogger;


public class SQLErrorHandlingLogger extends XynaSQLUtilsLogger {

  private final List<ErrorCodeHandlingElement> pipeline;
  
  public SQLErrorHandlingLogger(Logger logger, Level loglevel, List<ErrorCodeHandlingElement> pipeline) {
    this(logger, loglevel, ODSImpl.CONNECTIONCLASSNAME, pipeline);
  }


  public SQLErrorHandlingLogger(Logger logger, Level loglevel, String name, List<ErrorCodeHandlingElement> pipeline) {
    super(logger, name, loglevel);
    this.pipeline = pipeline;
  }
  

  public void logException(Exception e) {
    if (e instanceof SQLException) {
      SQLException se = (SQLException) e;
      for (ErrorCodeHandlingElement element : pipeline) {
        element.check(se);
      }
    } else {
      throw new SQLRuntimeException(e);
    }
  }
  
  
  public static SQLErrorHandlingLoggerBuilder builder() {
    return new SQLErrorHandlingLoggerBuilderImpl();
  }
  
  public static class SQLErrorHandlingLoggerBuilderImpl implements SQLErrorHandlingLoggerBuilder, SQLErrorHandlingLoggerBuilderWith {
    
    private List<ErrorCodeHandlingElement> pipeline = new ArrayList<ErrorCodeHandlingElement>();
    private int[] lastCodes;
    private boolean finished = false;
    
    public SQLErrorHandlingLoggerBuilder with(SQLErrorHandling handling) {
      if (finished) {
        throw new IllegalStateException("Logger configuration already finished");
      } else if (lastCodes == null) {
        throw new IllegalStateException("with before handleCodes");
      } else {
        pipeline.add(new CodeFilteringPipelineElement(lastCodes, handling));
        lastCodes = null;
      }
      return this;
    }

    public SQLErrorHandlingLoggerBuilderWith handleCode(int... codes) {
      if (finished) {
        throw new IllegalStateException("Logger configuration already finished");
      }
      lastCodes = codes;
      return this;
    }

    public SQLErrorHandlingLoggerBuilder arbitraryCheck(ErrorCodeHandlingElement e) throws SQLRuntimeException {
      if (finished) {
        throw new IllegalStateException("Logger configuration already finished");
      }
      pipeline.add(e);
      return this;
    }

    public SQLErrorHandlingLoggerBuilder otherwise(SQLErrorHandling handling) {
      if (finished) {
        throw new IllegalStateException("Logger configuration already finished");
      }
      pipeline.add(new ElsePipelineElement(handling));
      finished = true;
      return this;
    }

    public SQLErrorHandlingLogger build(Logger logger, Level loglevel) {
      return new SQLErrorHandlingLogger(logger, loglevel, pipeline);
    }
    
    
    public SQLErrorHandlingLogger build(Logger logger, Level loglevel, String name) {
      return new SQLErrorHandlingLogger(logger, loglevel, name, pipeline);
    }
    
  }
  
  public static interface SQLErrorHandlingLoggerBuilder {
    
    public SQLErrorHandlingLoggerBuilderWith handleCode(int... codes);
    
    public SQLErrorHandlingLoggerBuilder arbitraryCheck(ErrorCodeHandlingElement e) throws SQLRuntimeException;
    
    public SQLErrorHandlingLoggerBuilder otherwise(SQLErrorHandling handling);
    
    public SQLErrorHandlingLogger build(Logger logger, Level loglevel, String name);
    
    public SQLErrorHandlingLogger build(Logger logger, Level loglevel);
    
  }
  
  
  public static interface SQLErrorHandlingLoggerBuilderWith {
    
    public SQLErrorHandlingLoggerBuilder with(SQLErrorHandling handling);
    
  }
  
  
  
  public static interface ErrorCodeHandlingElement {
    
    public void check(SQLException e) throws SQLRuntimeException;
    
  }
  
  
  protected static class CodeFilteringPipelineElement implements ErrorCodeHandlingElement {

    private final int[] codes;
    private final SQLErrorHandling handling;
    
    protected CodeFilteringPipelineElement(int[] codes, SQLErrorHandling handling) {
      this.codes = new int[codes.length];
      System.arraycopy(codes, 0, this.codes, 0, codes.length);
      Arrays.sort(this.codes);
      this.handling = handling;
    }


    public void check(SQLException e) throws SQLRuntimeException {
      if (Arrays.binarySearch(codes, e.getErrorCode()) >= 0) {
        throw handling.getException(e);
      }
    }
    
  }
  
  
  protected static class ElsePipelineElement implements ErrorCodeHandlingElement {
    
    private final SQLErrorHandling handling;
    
    protected ElsePipelineElement(SQLErrorHandling handling) {
      this.handling = handling;
    }


    public void check(SQLException e) throws SQLRuntimeException {
      throw handling.getException(e);
    }
    
  }
  

}
