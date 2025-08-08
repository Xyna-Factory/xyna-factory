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


package xmcp.oas.fman.tools;

import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;


public class OasGuiConstants {

  public static final String FQN_OAS_BASE_API = "xmcp.oas.datatype.OASBaseApi";
  public static final String OAS_BASE_APP_NAME = "OAS_Base";
  public static final String OP_SEARCH_SELECT = XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL.getColumnName() + "," +
                                                XMOMDatabaseEntryColumn.NAME.getColumnName() + "," +
                                                XMOMDatabaseEntryColumn.PATH.getColumnName() + "," +
                                                XMOMDatabaseEntryColumn.REVISION.getColumnName();
  
}
