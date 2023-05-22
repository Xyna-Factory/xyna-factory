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
package xmcp.factorymanager.impl.converter.payload;



import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;



public class MetaInfo implements JsonSerializable {

  public final static String FULL_QUALIFIED_NAME = "fqn";
  public final static String RUNTIME_CONTEXT = "rtc";

  private String fqn;
  private RuntimeContext rtc;

  private MetaInfo() {
    
  }
  
  public MetaInfo(String fqn) {
    this();
    this.fqn = fqn;
  }
  
  public MetaInfo(String fqn, RuntimeContext rtc) {
    this(fqn);
    this.rtc = rtc;
  }

  public static JsonVisitor<MetaInfo> getJsonVisitor() {
    return new MetaInfoVisitor();
  }

  public String getFqName() {
    return fqn;
  }
  
  public RuntimeContext getRuntimeContext() {
    return rtc;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder().append("fqn: ").append(fqn).append("\n").append("rtc: ")
                    .append(rtc.toString()).append("\n");
    return sb.toString();
  }


  private static class MetaInfoVisitor extends EmptyJsonVisitor<MetaInfo> {

    MetaInfo mi = new MetaInfo();


    @Override
    public MetaInfo get() {
      return mi;
    }


    @Override
    public MetaInfo getAndReset() {
      MetaInfo ret = mi;
      mi = new MetaInfo();
      return ret;
    }


    @Override
    public void attribute(String label, String value, Type type) {
      if (label.equals(FULL_QUALIFIED_NAME)) {
        mi.fqn = value;
        return;
      }
    }


    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      if (label.equals(RUNTIME_CONTEXT)) {
        return new RuntimeContextVisitor();
      } else {
        return super.objectStarts(label);
      }
    }


    @Override
    public void object(String label, Object value) throws UnexpectedJSONContentException {
      if (label.equals(RUNTIME_CONTEXT)) {
        mi.rtc = (RuntimeContext) value;
      }
    }


  }


  @Override
  public void toJson(JsonBuilder jb) {
    jb.addOptionalStringAttribute(FULL_QUALIFIED_NAME, fqn);
    jb.addOptionalObjectAttribute(RUNTIME_CONTEXT, rtc == null ? null : new RuntimeContextJson(rtc));
  }

}