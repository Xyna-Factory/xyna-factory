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

package com.gip.xyna;

import org.apache.log4j.Level;


public class XynaFactoryLogLevel extends Level {


  private static final long serialVersionUID = 2471550004524746019L;


  public static final int XYNA_FACTORY_LOG_LEVEL_INT = Level.ERROR_INT + 1;
  public static final String XYNA_FACTORY_LOG_LEVEL_STRING = "FACTORY";

  /**
   * <ul>
   *  <li>TRACE -&gt; 7</li> 
   *  <li>DEBUG -&gt; 7</li>
   *  <li>INFO -&gt; 6</li>
   *  <li>WARN -&gt; 4</li> 
   *  <li>ERROR -&gt; 3</li> 
   *  <li>FATAL -&gt; 0</li>
   *  <li>FACTORY -&gt; 3</li>
   * </ul>
   */
  public static final int SYSLOG_EQUIVALENT = 3;


  public static final XynaFactoryLogLevel Factory = new XynaFactoryLogLevel(XYNA_FACTORY_LOG_LEVEL_INT,
                                                                            XYNA_FACTORY_LOG_LEVEL_STRING,
                                                                            SYSLOG_EQUIVALENT);


  private XynaFactoryLogLevel(int level, String levelStr, int syslogEquivalent) {
    super(level, levelStr, syslogEquivalent);
  }

}
