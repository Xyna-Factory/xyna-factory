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
package com.gip.xyna.utils.logging;

import java.util.logging.Logger;

/**
 *
 */
@Deprecated
public class XynaLogger extends Logger {

   /**
    * @param name
    * @param resourceBundleName
    */
   protected XynaLogger(String name, String resourceBundleName) {
      super(name, resourceBundleName);
      // TODO Auto-generated constructor stub
   }
   
   //TODO: SysLogger, SQLLogger, ConsoleLogger
   //TODO: LoggerFactory
   //TODO: define common methods

}
