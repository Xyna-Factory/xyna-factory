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
package com.gip.xyna.xact.filter.json;



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;



public class CloseJson {

  private static final String LABEL_REVISION = "revision";
  private static final String LABEL_FORCE = "force";

  private int revision;
  private boolean force;
  private FQNameJson fqName;


  private CloseJson() {
  }


  public int getRevision() {
    return revision;
  }


  public boolean isForce() {
    return force;
  }


  public FQNameJson getFQName() {
    return fqName;
  }

  public static JsonVisitor<CloseJson> getJsonVisitor() {
    return new CloseJsonVisitor();
  }


  private static class CloseJsonVisitor extends EmptyJsonVisitor<CloseJson> {

    CloseJson cj = new CloseJson();


    @Override
    public CloseJson get() {
      return cj;
    }


    @Override
    public CloseJson getAndReset() {
      CloseJson ret = cj;
      cj = new CloseJson();
      return ret;
    }


    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(LABEL_REVISION)) {
        cj.revision = Integer.valueOf(value);
        return;
      }
      if (label.equals(LABEL_FORCE)) {
        cj.force = Boolean.valueOf(value);
        return;
      }

      throw new UnexpectedJSONContentException(label);
    }

  }


}
