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

package com.gip.juno.ws.handler;

import java.lang.reflect.Method;
import java.math.BigInteger;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.juno.ws.enums.OptionalCol;
import com.gip.juno.ws.enums.Updates;
import com.gip.juno.ws.enums.Visible;
import com.gip.juno.ws.enums.ColType;
import com.gip.juno.ws.exceptions.DPPWebserviceReflectionException;

import com.gip.juno.ws.tools.ColInfo;
import com.gip.juno.ws.tools.DBTableInfo;
import com.gip.juno.ws.tools.Constants;

/**
 * class that translates meta info of a database table by using reflection API
 */
public class MetaInfoTools<T> {

  public static class ColMetaInfo {
    public boolean Visible = false;
    public boolean Updates = false;
    public String Guiname;
    public String Colname;
    public BigInteger Colnum;
    public String Childtable;
    public String Parenttable;
    public String Parentcol;
    public String InputType;
    public String InputFormat;
    public String Optional;
  }
  
  private Class<?> _refClass;
  private Logger _logger;
  
  public MetaInfoTools(T ref, Logger logger) {
    _refClass = ref.getClass();
    _logger = logger;
  }
  
  public List<T> getMetaInfo(DBTableInfo table) throws RemoteException {
    List<T> ret = adaptMetaInfoList(createMetaInfoList(table));
    return ret;
  }
  
  private List<T> adaptMetaInfoList(List<ColMetaInfo> columns) throws RemoteException {
    List<T> ret = new ArrayList<T>();
    for (ColMetaInfo col : columns) {
      ret.add(adaptColMetaInfo(col));
    }
    return ret;
  }
  
  @SuppressWarnings("unchecked")
  private T adaptColMetaInfo(ColMetaInfo col) throws RemoteException {
    try {
      T ret = (T) _refClass.newInstance();
      
      ReflectionTools<T> reftools = new ReflectionTools<T>(ret);
      Method setter;
      
      setter = reftools.getBooleanSetter(Constants.MetaInfo.Visible);
      reftools.callBooleanSetter(ret, setter, col.Visible);
      
      setter = reftools.getBooleanSetter(Constants.MetaInfo.Updates);
      reftools.callBooleanSetter(ret, setter, col.Updates);
      
      setter = reftools.getBigIntegerSetter(Constants.MetaInfo.Colnum);
      reftools.callBigIntegerSetter(ret, setter, col.Colnum);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Guiname);
      reftools.callStringSetter(ret, setter, col.Guiname);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Colname);
      reftools.callStringSetter(ret, setter, col.Colname);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Childtable);
      reftools.callStringSetter(ret, setter, col.Childtable);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Parenttable);
      reftools.callStringSetter(ret, setter, col.Parenttable);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Parentcol);
      reftools.callStringSetter(ret, setter, col.Parentcol);
      
      setter = reftools.getStringSetter(Constants.MetaInfo.InputType);
      reftools.callStringSetter(ret, setter, col.InputType);
      
      try {
        setter = reftools.getStringSetter(Constants.MetaInfo.InputFormat);
        reftools.callStringSetter(ret, setter, col.InputFormat);
      } catch (Exception e) {
        //some MetaInfos don't support this
      }
      
      setter = reftools.getStringSetter(Constants.MetaInfo.Optional);
      reftools.callStringSetter(ret, setter, col.Optional);
      
      return ret;
    } catch (RemoteException e) {
      throw e;   
    } catch (Exception e) {
      throw new DPPWebserviceReflectionException("Error in adapting MetaInfo. ", e);
    }
  }
  
  public List<ColMetaInfo> createMetaInfoList(DBTableInfo table) {
    Map<String, ColInfo> cols = table.getColumns();
    List<ColMetaInfo> ret = new ArrayList<ColMetaInfo>();
    for (Map.Entry<String, ColInfo> entry : cols.entrySet()) {
      ret.add(getColMetaInfo(entry.getValue()));      
    }
    return ret;
  }
  
  private ColMetaInfo getColMetaInfo(ColInfo col) {
    ColMetaInfo ret = new ColMetaInfo();
  
    ret.Colname = col.xmlName;  
    int colnum = col.num;
    ret.Colnum = BigInteger.valueOf(colnum);
  
    ret.Guiname = col.guiname;
    if (col.updates == Updates.True) {
      ret.Updates = true;
    } else {
      ret.Updates = false;
    }
    if (col.visible == Visible.True) {
      ret.Visible = true;
    } else {
      ret.Visible = false;
    }
    if (!col.parentTable.equals("")) {
      ret.Parenttable = col.parentTable;
    }
    if (!col.parentCol.equals("")) {
      ret.Parentcol = col.parentCol;
    }
    if (!col.childTable.equals("")) {
      ret.Childtable = col.childTable;
    }
    if (col.type == ColType.integer) {
      ret.InputType = "int";
    }
    if (!col.inputType.equals("")) {
      ret.InputType = col.inputType;
    }
    if (!col.inputFormat.equals("")) {
      ret.InputFormat = col.inputFormat;
    }
    if (col.optional == OptionalCol.True) {
      ret.Optional = "true";
    }
    return ret;
  }
}
