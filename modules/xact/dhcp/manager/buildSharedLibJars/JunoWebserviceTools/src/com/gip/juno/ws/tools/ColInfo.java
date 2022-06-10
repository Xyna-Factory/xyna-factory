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

package com.gip.juno.ws.tools;

import java.rmi.RemoteException;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.enums.IfNoVal;
import com.gip.juno.ws.enums.InputFormat;
import com.gip.juno.ws.enums.InputType;
import com.gip.juno.ws.enums.IpMirrored;
import com.gip.juno.ws.enums.LookupStyle;
import com.gip.juno.ws.enums.OptionalCol;
import com.gip.juno.ws.enums.Pk;
import com.gip.juno.ws.enums.Updates;
import com.gip.juno.ws.enums.VirtualCol;
import com.gip.juno.ws.enums.Visible;
import com.gip.juno.ws.exceptions.DPPWebserviceIllegalArgumentException;
import com.gip.juno.ws.exceptions.MessageBuilder;
import com.gip.juno.ws.tools.multiuser.MultiUserTools;

public class ColInfo {
  /**
   * name of column
   */
  public String name = "";
  
  /**
   * name of column in database (to build SQL commands)
   */
  public String dbname = "";
  
  /**
   * name of column in xml message
   */
  public String xmlName = "";
  
  /**
   * datatype of column
   */
  public ColType type = ColType.string;
  
  /**
   * indicates, if column should be visible in gui
   */
  public Visible visible = Visible.True;
  
  /**
   * column name in gui
   */
  public String guiname = "";
  
  /**
   * indicates if column is a primary key for the table
   */
  public Pk pk = Pk.False;
  
  /**
   * indicates if updates for this column should be allowed in the gui
   */
  public Updates updates = Updates.False;
  
  /**
   * name of child table
   */
  public String childTable = "";
  
  /**
   * name of parent table (to look up foreign values associated with foreign key constraints)
   */
  public String parentTable = "";
  
  /**
   * name of column ant "other end" of foreign key constraint
   */
  public String parentCol = "";
  
  /**
   * number of column (used to fix the sequence of the columns in the gui)
   */
  public int num = 0;
  
  /**
   * 
   */
  public String inputType = "";
  
  /**
   * for known inputFormats the gui will add an appropriate validator for this column if supported
   */
  public String inputFormat = "";
  
  /**
   * column name in other table, used for lookup operation
   */
  public String lookupCol = "";
  
  /**
   * indicates if table contains ip address that has to be mirrored when sending it to the gui or webservice
   */
  public IpMirrored ipMirrored = IpMirrored.False;
  
  /**
   * simulate an auto-increment column by selecting the max value of the column +1 
   * for inserts and updates
   */
  public boolean autoIncrement = false;
  
  /**
   * virtual = Virtual.True means that this column does not exist in the DB; 
   * specifically, it will not be included in "select" statements
   */
  public VirtualCol virtual = VirtualCol.False;
  
  /**
   * OptionalCol.True signals that optional attribute in MetaInfo will be set for the GUI 
   */
  public OptionalCol optional = OptionalCol.False;
  
  /**
   * if true, to strings in this column will be added a linebreak at the end if there isn't already one 
   */
  public boolean endsWithLinebreak = false;
  
  /**
   * if true, call trim() on string values
   */
  public boolean doTrim = false;
  
  /**
   * IfNoVal.EmptyString : insert an empty string, '', into the database instead of null values
   */
  public IfNoVal ifNoVal = IfNoVal.EmptyString;
  
  /**
   * Value = LookupStyle.csv means that string in column is a comma-separated list, 
   * -> lookup and replace each value separately
   */
  public LookupStyle lookupStyle = LookupStyle.singleval;
  
  /**
   * remove all spaces in String before inserting into database
   */
  public boolean removeSpaces = false;
  
  /**
   * insert current time into database by insert operations 
   */
  public boolean insertCurrentTime = false;
  
  /**
   * value to insert if ifNoVal is set to IfNoVal.Default
   */
  public String defaultValue = "";
  
  /**
   * indicates that a check for correct attribute syntax should be performed
   * before insert operations 
   */
  public boolean checkAttributeSyntax = false;
  
  /**
   * indicates that a check for correct conditional syntax should be performed
   */
  public boolean checkConditionalSyntax = false;
  
  /**
   * indicates that the content of this column is a password and should not be dispayed
   */
  public boolean maskPassword = false;
  
  /**
   * indicates, if the value for this column is to be set by a database query (including lookup)
   */
  public boolean isSetByQuery = true;
  
  /**
   * indicates if column should contain only unique values, so that before changes an explicit
   * check should be performed 
   */
  public boolean checkUnique = false;
  
  /**
   * a query used to determine the value of a column on either creation or selection
   */
  public LookupQuery lookquery = null; 
  
  public AdditionalCheck[] additionalChecks = new AdditionalCheck[0];
  
  
  /**
   * Constructor
   */
  public ColInfo(String name) {
    dbname = name;
    guiname = name;
    xmlName = name;
    this.name = name;
  }
  
  public ColInfo setMaskPassword() {
    maskPassword = true;
    return this;
  }
  
  public ColInfo setCheckUnique() {
    checkUnique = true;
    return this;
  }
  
  public ColInfo setCheckConditionalSyntax() {
    checkConditionalSyntax = true;
    return this;
  }
  
  public ColInfo setCheckAttributeSyntax() {
    checkAttributeSyntax = true;
    return this;
  }
  
  public ColInfo setDefaultValue(String val) {
    if (val == null) {
      return this;
    }
    if (val.equals("")) {
      ifNoVal = IfNoVal.EmptyString;
    } else {
      ifNoVal = IfNoVal.DefaultValue;
      defaultValue = val;
    }
    setOptional();
    return this;
  }
  
  public ColInfo setInsertCurrentTime() {
    insertCurrentTime = true;
    return this;
  }
  
  public ColInfo setRemoveSpaces() {
    removeSpaces = true;
    return this;
  }
  
  public ColInfo setAutoIncrement() {
    autoIncrement = true;
    return this;
  }
  
  public ColInfo setLookupStyle(LookupStyle val) {
    lookupStyle = val;
    return this;
  }
  
  public ColInfo setDoTrim(boolean val) {
    doTrim = val;
    return this;
  }
  
  public ColInfo setIfNoVal(IfNoVal val) {
    ifNoVal = val;
    return this;
  }
  
  public ColInfo setEndsWithLinebreak(boolean val) {
    endsWithLinebreak = val;
    return this;
  }
  
  public ColInfo setEndsWithLinebreak() {
    endsWithLinebreak = true;
    return this;
  }
  
  public ColInfo setOptional() {
    optional = OptionalCol.True;
    return this;
  }
  
  public ColInfo setVirtual() {
    virtual = VirtualCol.True;
    if (lookupCol.trim().equals("")) {
      isSetByQuery = false; 
    }
    return this;
  }
  
  public ColInfo setIpMirrored(boolean val) {
    if (val) {
      ipMirrored = IpMirrored.True;
    } else {
      ipMirrored = IpMirrored.False;
    }
    return this;
  }
  
  public ColInfo setIpMirrored() {
    ipMirrored = IpMirrored.True;    
    return this;
  }
  
  public ColInfo setLookupCol(String val) {
    lookupCol = val;
    isSetByQuery = true;
    return this;
  }
  
  public ColInfo setChildTable(String val) {
    childTable = val;
    return this;
  }
  
  public ColInfo setInputType(String val) {
    inputType = val;
    return this;
  }
  
  public ColInfo setInputType(InputType inputType) {
    this.inputType = inputType.getStringRepresentation();
    return this;
  }
  
  public ColInfo setInputFormat(String inputFormat) {
    this.inputFormat = inputFormat;
    return this;
  }
  
  public ColInfo setInputFormat(InputFormat inputFormat) {
    this.inputFormat = inputFormat.getStringRepresentation();
    return this;
  }

  public ColInfo setParentCol(String val) {
    parentCol = val;
    return this;
  }

  public ColInfo setParentTable(String val) {
    parentTable = val;
    return this;
  }
  
  public ColInfo setGuiname(String val) {
    guiname = val;
    return this;
  }
  
  public ColInfo setXmlName(String val) {
    xmlName = val;
    return this;
  }

  public ColInfo setDBName(String val) {
    dbname = val;
    return this;
  }
  
  public ColInfo setType(ColType val) {
    type = val;
    return this;
  }
  
  public ColInfo setVisible(boolean val) {
    if (val) {
      visible = Visible.True;
    } else {
      visible = Visible.False;
    }
    return this;
  }
  
  public ColInfo setPk(boolean val) {
    if (val) {
      setPk();
    } else {
      pk = Pk.False;
    }
    return this;
  }
  
  public ColInfo setPk() {    
    pk = Pk.True;
    updates = Updates.False;
    return this;
  }
  
  public ColInfo setUpdates(boolean val) {
    if (val) {
      updates = Updates.True;
    } else {
      updates = Updates.False;
    }
    return this;
  }
  
  
  public ColInfo setAdditionalChecks(AdditionalCheck[] val) {
    additionalChecks = val;
    return this;
  }
  
  
  public ColInfo setLookupQuery(LookupQuery query) {
    this.lookquery = query;
    return this;
  }
  
  public enum LOOKUP_ON {
    INSERTION, MODIFICATION/*, SELECTION, DELETION*/
  }
  
  
  public static class LookupQuery {
    
    private final static String THROW_CONSTRAINT_VIOLATION_ON_EMPTY_LOOKUP_PROPERTYNAME = "hosts.throwOnEmptyCmtsipLookup";
    
    private final LOOKUP_ON[] whens;
    private final SQLCommand query;
    private final String[] localParameterIdentifiers;
    private final IfNoVal noValueHandling;
    
    public LookupQuery(SQLCommand lookup, String[] localParameterIdentifiers, IfNoVal noValueHandling, LOOKUP_ON... whens) {
      this.query = lookup;
      this.localParameterIdentifiers = localParameterIdentifiers;
      this.whens = whens;
      this.noValueHandling = noValueHandling;
    }
    
    public boolean lookupExecutionAppropriate(LOOKUP_ON now) {
      for (LOOKUP_ON when : whens) {
        if (when == now) {
          return true;
        }
      }
      return false;
    }
    
    public String[] getLocalParameterIdentifiers() {
      return localParameterIdentifiers;
    }
    
    public SQLCommand getSQLCommand() {
      return query;
    }
    
    
    public String verify(String input, ColInfo colInfo) throws RemoteException {
      if (input == null ||
          input.trim().length() == 0) {
        switch (noValueHandling) {
          case Null:
            return null;
          case DefaultValue:
            return colInfo.defaultValue;
          case EmptyString:
            return "";
          case IgnoreColumn: // should never happen
            return null;
          case ConstraintViolation:
            if (throwConstraintViolationOnEmptyLookup()) {
            throw new DPPWebserviceIllegalArgumentException(new MessageBuilder().setDescription("Lookup for column " 
                  + colInfo.guiname + " may not be empty.").setErrorNumber("00203").addParameter(colInfo.guiname));
            } else {
              return input;
            }
          default :
            throw new IllegalArgumentException("Invalid no value handling defined: " + noValueHandling);
        }
      } else {
        return input;
      }
    }
    
    private final static Logger logger = MultiUserTools.nop_logger;
    private static Properties wsProperties = null;
    
    public static synchronized boolean throwConstraintViolationOnEmptyLookup() throws RemoteException {
      if (wsProperties == null) {
        wsProperties = PropertiesHandler.getWsProperties();
      }
      try {
        return PropertiesHandler.getBooleanProperty(wsProperties, THROW_CONSTRAINT_VIOLATION_ON_EMPTY_LOOKUP_PROPERTYNAME, logger);
      } catch (Throwable t) {
        return false;
      }
    }
    
  }
  
  
  
  
}
