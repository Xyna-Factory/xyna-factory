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
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.session.Dataflow.LinkstateIn;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;



public class DataflowJson {

  private int revision;
  private String sourceId;
  private String targetId;
  private String branchId = null;
  private LinkstateIn type;

  
  private DataflowJson() {

  }


  public DataflowJson(int revision, String sourceId, String targetId, LinkstateIn type) {
    this.revision = revision;
    this.sourceId = sourceId;
    this.targetId = targetId;
    this.type = type;
  }


  public static JsonVisitor<DataflowJson> getJsonVisitor() {
    return new DataflowJsonVisitor();
  }

  public int getRevision() {
    return revision;
  }
  
  public String getSourceId() {
    return sourceId;
  }
  
  public String getTargetId() {
    return targetId;
  }
  
  public String getBranchId() {
    return branchId;
  }
  
  public LinkstateIn getType() {
    return type;
  }


  private static class DataflowJsonVisitor extends EmptyJsonVisitor<DataflowJson> {

    DataflowJson dj = new DataflowJson();

    @Override
    public DataflowJson get() {
      return dj;
    }

    @Override
    public DataflowJson getAndReset() {
      DataflowJson ret = dj;
      dj = new DataflowJson();

      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(Tags.REVISION) ) {
        dj.revision = Integer.valueOf(value);
        return;
      }
      if( label.equals(Tags.SOURCE_ID) ) {
        dj.sourceId = value;
        return;
      }
      if( label.equals(Tags.TARGET_ID) ) {
        dj.targetId = value;
        return;
      }
      if( label.equals(Tags.BRANCH_ID) ) {
        dj.branchId = value;
        return;
      }
      if( label.equals(Tags.CONNECTION_TYPE) ) {
        try {
          dj.type = LinkstateIn.valueOf(value.toUpperCase());
          return;
        } catch (Exception e) {
          throw new UnexpectedJSONContentException(label, e);
        }
      }

      super.attribute(label, value, type);
    }

  }

}
