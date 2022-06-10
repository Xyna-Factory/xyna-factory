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


package com.gip.juno.ws.tools;

/**
 * Class that collects constants that might be changed in future versions
 */
public class Constants {

  /**
   * tagname for location value in soap message 
   */
  public static final String standort = "Standort";
  
  /**
   * tagname for type value in soap message for table leases 
   */
  public static final String leasesTypeXmlName = "Type";

  /**
   * name of database
   */
  public static final String serviceSchema = "service";
  
  /**
   * name of database
   */
  public static final String auditSchema = "audit";
  
  /**
   * location name (in database table locations) for management instances
   */
  public static final String managementName = "Verwaltung";
  
  /**
   * tagnames in soap message for database table meta-info
   */
  public static class MetaInfo {
    public static final String Visible = "Visible";
    public static final String Updates = "Updates";
    public static final String Guiname = "Guiname";
    public static final String Colname = "Colname";
    public static final String Colnum = "Colnum";
    public static final String Childtable = "Childtable";
    public static final String Parenttable = "Parenttable";
    public static final String Parentcol = "Parentcol";
    public static final String InputType = "InputType";
    public static final String InputFormat = "InputFormat";
    public static final String Optional = "Optional";
  }
  
}
