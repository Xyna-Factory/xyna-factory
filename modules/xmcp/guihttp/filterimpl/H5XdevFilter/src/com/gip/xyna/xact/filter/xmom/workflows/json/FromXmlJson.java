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



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class FromXmlJson extends XMOMGuiJson {

  private String xml;


  public String getXml() {
    return xml;
  }


  public void setXml(String xml) {
    this.xml = xml;
  }


  public static class FromXmlJsonVisitor extends EmptyJsonVisitor<FromXmlJson> {

    private FromXmlJson result;


    public FromXmlJsonVisitor() {
      result = new FromXmlJson();
    }


    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(Tags.TYPE)) {
        if (!(value.equals(Tags.XML))) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.XML);
        } else {
          return;
        }
      }
      if (label.equals(Tags.XML)) {
        result.setXml(value);
        return;
      }
    }


    @Override
    public FromXmlJson get() {
      return result;
    }


    @Override
    public FromXmlJson getAndReset() {
      FromXmlJson fxj = result;
      result = null;
      return fxj;
    }
  }
}

