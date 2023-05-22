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

package com.gip.xyna.xact.filter.actions.startorder.json;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.util.xo.GenericResult;
import com.gip.xyna.xact.filter.util.xo.GenericVisitor;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class StartorderRequestJson {

  private final static String ORDERTYPE = "orderType";
  private final static String ASYNC_FLAG = "async";
  private final static String CUSTOMSTRINGCONTAINER = "customStringContainer";
  private final static String PRIORITY = "priority";
  private final static String INPUTSOURCEID = "inputSourceId";
  private final static String MONITORINGLEVEL = "monitoringLevel";
  private final static String INPUT = "input";
  private final static String TIMEOUT = "timeout";
  // private final static String LABELS = "labels";

  private String orderType;
  private boolean async = true;
  private List<String> customStringContainer;
  private int priority;
  private long inputSourceId;
  private Integer monitoringLevel;
  private Container input;
  private int timeout;
  // private JsonSerializable labels;


  public StartorderRequestJson() {
    input = new Container();
    customStringContainer = new ArrayList<String>();
  }


  public StartorderRequestJson(String orderType, boolean async, int priority, Container input, int timeout,
                               List<String> customStringContainer, long inputSourceId, Integer monitoringLevel) {
    this.orderType = orderType;
    this.async = async;
    this.priority = priority;
    this.input = input;
    this.timeout = timeout;
    this.customStringContainer = customStringContainer;
    this.inputSourceId = inputSourceId;
    this.monitoringLevel = monitoringLevel;
  }


  public String getOrderType() {
    return orderType;
  }


  public boolean isAsync() {
    return async;
  }


  public int getPriority() {
    return priority;
  }


  public Container getInput() {
    return input;
  }


  public int getTimeout() {
    return timeout;
  }


  public List<String> getCustomStringContainer() {
    return customStringContainer;
  }
  
  public String getCustomString(int i) {
    if (customStringContainer == null || customStringContainer.size() <= i) {
      return null;
    }
    return customStringContainer.get(i);
  }


  public long getInputSourceId() {
    return inputSourceId;
  }


  public Integer getMonitoringLevel() {
    return monitoringLevel;
  }

  public static JsonVisitor<StartorderRequestJson> getJsonVisitor() {
    return new StartorderParamsJsonVisitor();
  }


  public static JsonVisitor<StartorderRequestJson> getJsonVisitor(long revision) {
    return new StartorderParamsJsonVisitor(revision);
  }


  private static class StartorderParamsJsonVisitor extends EmptyJsonVisitor<StartorderRequestJson> {

    public long revision;


    public StartorderParamsJsonVisitor(long revision) {
      this.revision = revision;
    }


    public StartorderParamsJsonVisitor() {

    }


    StartorderRequestJson srj = new StartorderRequestJson();


    @Override
    public StartorderRequestJson get() {
      return srj;
    }


    @Override
    public StartorderRequestJson getAndReset() {
      StartorderRequestJson ret = srj;
      srj = new StartorderRequestJson();
      return ret;
    }


    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      if (label.equals(INPUT)) {
        return new GenericVisitor();
      } else {
        return this;
      }
    }


    @Override
    public void object(String label, Object value) throws UnexpectedJSONContentException {
      if (label.equals(INPUT)) {
        GenericResult gr = (GenericResult) value;
        try {
          Util.distributeMetaInfo(gr, revision);
          XynaObjectVisitor xov = new XynaObjectVisitor();
          gr.visit(xov, Collections.singletonList(XynaObjectVisitor.META_TAG));
          srj.input.add((GeneralXynaObject) xov.getAndReset());
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          throw new RuntimeException(e);
        }
      }
    }


    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      if (label.equals(INPUT)) {
        for (Object object : values) {
          GenericResult gr = (GenericResult) object;
          if (gr == null) {
            srj.input.add(null);
          } else {
            try {
              Util.distributeMetaInfo(gr, revision);
              XynaObjectVisitor xov = new XynaObjectVisitor();
              gr.visit(xov, Collections.singletonList(XynaObjectVisitor.META_TAG));
              srj.input.add((GeneralXynaObject) xov.getAndReset());
            } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
              throw new RuntimeException(e);
            }
          }
        }
      }
    }


    public void attribute(String label, String value, Type type) {
      if (label.equals(ORDERTYPE)) {
        srj.orderType = value;
        return;
      } else if (label.equals(ASYNC_FLAG)) {
        srj.async = Boolean.valueOf(value);
      } else if (label.equals(PRIORITY)) {
        srj.priority = Integer.valueOf(value);
      } else if (label.equals(TIMEOUT)) {
        srj.timeout = Integer.valueOf(value);
      } else if (label.equals(INPUTSOURCEID)) {
        srj.inputSourceId = Long.valueOf(value);
      } else if (label.equals(MONITORINGLEVEL)) {
        if (value == null) {
          srj.monitoringLevel = null;
        } else {
          srj.monitoringLevel = Integer.valueOf(value);
        }
      }
    }


    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
      if (label.equals(CUSTOMSTRINGCONTAINER)) {
        srj.customStringContainer.clear();
      }
    }


    @Override
    public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
      if (label.equals(CUSTOMSTRINGCONTAINER)) {
        //try {
        for (String str : values) {
          srj.customStringContainer.add(str);
        }
      } else if (label.equals(INPUT)) {
        for (String str : values) {
          if (str == null) {
            srj.input.add(null);
          }
        }
      } 
    }

  }

}
