/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xmcp.yang.misc;


public class Constants {

  public static class NetConf {
    
    public static class XmlTag {
      public static final String SOURCE = "source";
      public static final String FILTER = "filter";
      public static final String TARGET = "targetdefault-operation";
      public static final String DEFAULT_OPERATION = "test-option";
      public static final String TEST_OPTION = "error-option";
      public static final String ERROR_OPTION = "error-option";
      public static final String CONFIG = "config";
      public static final String SESSION_ID = "session-id";
    }
    
    public static class XmlAttribute {
      public static final String TYPE = "type"; 
    }
  
    public static class EnumValue {
      public static final String MERGE = "merge";
      public static final String REPLACE = "replace";
      public static final String NONE = "none";
      public static final String TEST_THEN_SET = "test-then-set";
      public static final String SET = "set";
      public static final String TEST_ONLY = "test-only";
      public static final String STOP_ON_ERROR = "stop-on-error";
      public static final String CONTINUE_ON_ERROR = "continue-on-error";
      public static final String ROLLBACK_ON_ERROR = "rollback-on-error";
    }
  }
  
}
