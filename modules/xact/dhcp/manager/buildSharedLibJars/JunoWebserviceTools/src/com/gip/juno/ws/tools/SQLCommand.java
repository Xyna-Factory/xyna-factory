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

import java.util.ArrayList;
import java.util.List;


import com.gip.juno.ws.enums.ColType;
import com.gip.xyna.utils.db.Parameter;


class ParamBuilder {  
  //public ColName name; 
  public ColStrValue val;
  public ColType type;
  public ParamBuilder(ColStrValue val, ColType type) {
    //this.name = name;
    this.val = val;
    this.type = type;
  }
}

/**
 * class that stores SQL string and jdbc-parameters for a database command
 */
public class SQLCommand {
  
  private List<ParamBuilder> params = new ArrayList<ParamBuilder>();
  private List<ParamBuilder> conditionParams = new ArrayList<ParamBuilder>();
  
  public String sql = "";
  
  public SQLCommand() {
  }
  
  public SQLCommand(String sql) {
    this.sql = sql;
  }
  
  private SQLCommand(String sql, List<ParamBuilder> params, List<ParamBuilder> conditionParams) {
    this.sql = sql;
    this.params = params;
    this.conditionParams = conditionParams;
  }
  
  public void addParam(ColName name, ColStrValue val, ColType type) {
    params.add(new ParamBuilder(val, type));
  }
  
    
  public void addConditionParam(ColName name, ColStrValue val, ColType type) {
    conditionParams.add(new ParamBuilder(val, type));
  }
  
  public void addConditionParam(String name, String value) {
    conditionParams.add(new ParamBuilder(new ColStrValue(value), ColType.string));
  }
  
  public void addConditionParam(String value) {
    conditionParams.add(new ParamBuilder(new ColStrValue(value), ColType.string));
  }
  
  public Parameter buildParameter() {
    if ((params.size() == 0) && (conditionParams.size() == 0)) {
      return null;
    }    
    Parameter ret = new Parameter();
    for (ParamBuilder pb : params) {
      addParam(pb, ret); 
    }
    for (ParamBuilder pb : conditionParams) {
      addParam(pb, ret); 
    }
    return ret;
  }
  
  private void addParam(ParamBuilder pb, Parameter ret) {
    if ((pb.type == ColType.integer) && (!hasWildcard(pb.val.get()))) {
      Integer intval = new Integer(pb.val.get());
      ret.addParameter(intval);
    } else {
      ret.addParameter(pb.val.get());
    }
  }
  
  public void clearParams() {
    params = new ArrayList<ParamBuilder>();
    conditionParams = new ArrayList<ParamBuilder>();
  }
  
  private boolean hasWildcard(String val) {
    if (val.indexOf("%") >=0) {
      return true;
    }
    return false;
  }
  

  public SQLCommand clone() { // as deep as needed, although params are not cloned
    return new SQLCommand(sql, new ArrayList<ParamBuilder>(params), new ArrayList<ParamBuilder>(conditionParams));
  }

}

