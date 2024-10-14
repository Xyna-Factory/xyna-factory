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
package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.Mandatory;

public class MandatoryTestObject extends UnsupportedGeneralXynaObject {
  
  private static final long serialVersionUID = -7160873642889246556L;
  
  public static final String ALWAYS_MANDATORY_VAR_NAME = "alwaysMandatory";
  public static final String NEVER_MANDATORY_VAR_NAME = "neverMandatory";
  public static final String STORE_MANDATORY_VAR_NAME = "storeMandatory";
  public static final String MULTIPLE_MANDATORY_VAR_NAME = "multipleMandatory";
  
  public static final String NEVER_UTILIZATION = "never";
  public static final String REASON1_UTILIZATION = "reason1";
  public static final String REASON2_UTILIZATION = "reason2";
  public static final String REASON3_UTILIZATION = "reason3";
  

  @Mandatory
  private String alwaysMandatory;
  
  @Mandatory(utilizationPolicy= {NEVER_UTILIZATION})
  private String neverMandatory; 
  
  @Mandatory(utilizationPolicy= {"xmomPersistence.onStore"})
  private String storeMandatory;
  
  @Mandatory(utilizationPolicy= {REASON1_UTILIZATION, REASON2_UTILIZATION, REASON3_UTILIZATION})
  private String multipleMandatory;
  
  
  
  public static String getXMLDefinition() {
    return "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"MandatoryTestObject\" TypeName=\"MandatoryTestObject\" TypePath=\"bg.mandatory\" Version=\"1.8\">\n"+
           "  <Meta>\n"+
           "    <IsServiceGroupOnly>false</IsServiceGroupOnly>\n"+
           "  </Meta>\n"+
           "  <Data Label=\""+ALWAYS_MANDATORY_VAR_NAME+"\" VariableName=\""+ALWAYS_MANDATORY_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory/>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+NEVER_MANDATORY_VAR_NAME+"\" VariableName=\""+NEVER_MANDATORY_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory UtilizationPolicy=\"never\"/>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+STORE_MANDATORY_VAR_NAME+"\" VariableName=\""+STORE_MANDATORY_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory UtilizationPolicy=\"xmomPersistence.onStore\"/>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+MULTIPLE_MANDATORY_VAR_NAME+"\" VariableName=\""+MULTIPLE_MANDATORY_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory UtilizationPolicy=\"reason1,reason2,reason3\"/>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "</DataType>";
  }
  
}
