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
package ex.test2;

import java.util.List;
import ex.test.TestException2;

//DO NOT CHANGE
//GENERATED BY com.gip.xyna.utils.exceptions.utils.codegen.JavaClass 2010-08-20T11:50:48Z;
public class TestException4 extends TestException2 {



  public TestException4(int errorDescription) {
    super(new String[]{"XYNATEST2-00003", errorDescription + ""});
    setErrorDescription(errorDescription);
  }

  public TestException4(int errorDescription, Throwable cause) {
    super(new String[]{"XYNATEST2-00003", errorDescription + ""}, cause);
    setErrorDescription(errorDescription);
  }

  protected TestException4(String[] args) {
    super(args);
  }

  protected TestException4(String[] args, Throwable cause) {
    super(args, cause);
  }

  public TestException4 initCause(Throwable t) {
    return (TestException4) super.initCause(t);
  }


}
