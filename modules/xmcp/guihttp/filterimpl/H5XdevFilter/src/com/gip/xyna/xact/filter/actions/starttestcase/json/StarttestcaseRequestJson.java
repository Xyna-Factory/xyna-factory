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

package com.gip.xyna.xact.filter.actions.starttestcase.json;



import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;



public class StarttestcaseRequestJson {

  private final static String TEST_CASE_NAME = "testCaseName";
  private final static String TEST_CASE_ID = "testCaseId";
  private final static String USER = "user";

  private String testCaseName;
  private Long testCaseId;
  private String user;


  public StarttestcaseRequestJson() {

  }


  public String getTestCaseName() {
    return testCaseName;
  }

  public Long getTestCaseId() {
    return testCaseId;
  }

  public String getUser() {
    return user;
  }

  public static JsonVisitor<StarttestcaseRequestJson> getJsonVisitor() {
    return new StarttestcaseParamsJsonVisitor();
  }


  private static class StarttestcaseParamsJsonVisitor extends EmptyJsonVisitor<StarttestcaseRequestJson> {

    public StarttestcaseParamsJsonVisitor() {

    }


    StarttestcaseRequestJson srj = new StarttestcaseRequestJson();


    @Override
    public StarttestcaseRequestJson get() {
      return srj;
    }


    @Override
    public StarttestcaseRequestJson getAndReset() {
      StarttestcaseRequestJson ret = srj;
      srj = new StarttestcaseRequestJson();
      return ret;
    }


    public void attribute(String label, String value, Type type) {
      if (label.equals(TEST_CASE_NAME)) {
        srj.testCaseName = value;
        return;
      }
      if (label.equals(TEST_CASE_ID)) {
        srj.testCaseId = Long.parseLong(value);
        return;
      }
      if (label.equals(USER)) {
        srj.user = value;
        return;
      }
    }


    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
    }

  }
}
