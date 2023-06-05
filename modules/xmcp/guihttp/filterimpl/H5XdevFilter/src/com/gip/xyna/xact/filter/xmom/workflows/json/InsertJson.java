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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonStringVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public class InsertJson extends PositionJson {

  private int revision;
  private XMOMGuiJson content;
  private String contentString;
  private String name; //for remoteDestination
  
  private InsertJson() {
  }
  
  public InsertJson(int revision, String relativeTo, RelativePosition relativePosition) {
    super(relativeTo,relativePosition);
    this.revision = revision;
  }
  
  public InsertJson(int revision, String relativeTo, int insideIndex) {
    super(relativeTo,insideIndex);
    this.revision = revision;
  }

  public void setContent(XMOMGuiJson content) {
    this.content = content;
  }

  public static JsonVisitor<InsertJson> getJsonVisitor() {
   return new InsertJsonVisitor();
  }
  
  public String getContentString() {
    return contentString;
  }
  
  public int getRevision() {
    return revision;
  }
  
  public XMOMGuiJson getContent() {
    return content;
  }
  
  public String getName() {
    return name;
  }
  
  public void parseContent(JsonVisitor<? extends XMOMGuiJson> jsonVisitor) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    this.content = jp.parse(contentString, jsonVisitor);
  }

  private static class InsertJsonVisitor extends PositionJsonVisitor<InsertJson> {
    
    @Override
    protected InsertJson create() {
      return new InsertJson();
    }
    
    @Override
    public void attribute(String label, String value, Type type) {
      super.attribute(label, value, type);
      if( label.equals("revision") ) {
        get().revision = Integer.valueOf(value);
        return;
      }
      else if(label.equals("name")) {
        get().name = value;
        return;
      }
    }

    @Override
    public JsonVisitor<?> objectStarts(String label) {
      return new JsonStringVisitor();
    }
    
    @Override
    public void object(String label, Object value) {
      if( label.equals("content") ) {
        get().contentString = (String)value;
        return;
      }
    }

  }

}
