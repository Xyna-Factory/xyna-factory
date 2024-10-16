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
package com.gip.xyna._1_5.exceptions;

import java.io.InputStream;
import java.net.URL;
import org.apache.log4j.Logger;
import java.net.URLConnection;
import com.gip.xyna.utils.exceptions.ExceptionStorage;

//DO NOT CHANGE
//GENERATED BY com.gip.xyna.utils.exceptions.utils.codegen.JavaClass 2010-05-27T17:32:44Z;
public class Codes2 {

  public static Logger logger = Logger.getLogger(Codes2.class.getName());
  public static final String CODE_AN_ERROR = "XYNATEST-00001";
  public static final String CODE_XYNATEST_00002a_Es_ist__0__ein_Fehle = "XYNATEST-00002a";
  static {
    {
      try {
        URL url = Codes2.class.getResource("/ExampleExceptionStorage.xml");
        if (url != null) {
          URLConnection urlcon = (URLConnection) url.openConnection();
          //deactivate cache to not get an old version
          boolean b = urlcon.getUseCaches();
          urlcon.setUseCaches(false);
          try {
            InputStream is = urlcon.getInputStream();
            try {
              if (is == null) {
                throw new Exception("Resource ExampleExceptionStorage.xml not found.");
              }
              ExceptionStorage.loadFromStream(is);
            } finally {
              is.close();
            }
          } finally {
            //reset caching!
            urlcon.setDefaultUseCaches(b);
          }
        } else {
          throw new Exception(" Resource ExampleExceptionStorage.xml not found.");
        }
      } catch (Exception e) {
        logger.error("Error loading Errormessages.", e);
        e.printStackTrace();
      }
    }
  }

  public static final String[] CODE_UNEXPECTED_ERROR(int errorDescription) {
    return new String[]{"XYNATEST-00002", "" + errorDescription};
  }

}
