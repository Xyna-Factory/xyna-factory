package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.DefaultType;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.Mandatory;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.ModelledRestriction.MaxLength;

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
public class MultipleRestrictionsTestObject extends UnsupportedGeneralXynaObject {
  
  private static final long serialVersionUID = -7160873642889246556L;
  
  public static final String MAN_MAX_VAR_NAME = "manMax";
  public static final String MAX_DEF_VAR_NAME = "maxDef";
  public static final String DEF_MAN_VAR_NAME = "defMan";
  public static final String MAN_MAX_DEF_VAR_NAME = "manMaxDef";
  
  public static final int MAN_MAX_LIMIT = 10000;
  public static final int MAX_DEF_LIMIT = 5000;
  public static final String MAX_DEF_DEFAULT_TYPE = "java.lang.String";
  public static final String DEF_MAN_DEFAULT_TYPE = "java.lang.Integer";
  public static final int MAN_MAX_DEF_LIMIT = 1337;
  public static final String MAN_MAX_DEF_DEFAULT_TYPE = "java.lang.String";
  

  @Mandatory
  @MaxLength(limit=MAN_MAX_LIMIT)
  private String manMax;
  
  @MaxLength(limit=MAX_DEF_LIMIT)
  @DefaultType(defaultType=MAX_DEF_DEFAULT_TYPE)
  private String maxDef; 
  
  @DefaultType(defaultType=DEF_MAN_DEFAULT_TYPE)
  @Mandatory
  private Integer defMan;
  
  @Mandatory
  @MaxLength(limit=MAN_MAX_DEF_LIMIT)
  @DefaultType(defaultType=MAN_MAX_DEF_DEFAULT_TYPE)
  private String manMaxDef;
  
  
  
  public static String getXMLDefinition() {
    return "<DataType xmlns=\"http://www.gip.com/xyna/xdev/xfractmod\" Label=\"MandatoryTestObject\" TypeName=\"MandatoryTestObject\" TypePath=\"bg.mandatory\" Version=\"1.8\">\n"+
           "  <Meta>\n"+
           "    <IsServiceGroupOnly>false</IsServiceGroupOnly>\n"+
           "  </Meta>\n"+
           "  <Data Label=\""+MAN_MAX_VAR_NAME+"\" VariableName=\""+MAN_MAX_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory/>\n"+
           "      <MaxLength>"+MAN_MAX_LIMIT+"</MaxLength>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+MAX_DEF_VAR_NAME+"\" VariableName=\""+MAX_DEF_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <MaxLength>"+MAX_DEF_LIMIT+"</MaxLength>\n"+
           "      <DefaultType>"+MAX_DEF_DEFAULT_TYPE+"</DefaultType>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+DEF_MAN_VAR_NAME+"\" VariableName=\""+DEF_MAN_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <DefaultType>"+DEF_MAN_DEFAULT_TYPE+"</DefaultType>\n"+
           "      <Mandatory />\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "  <Data Label=\""+MAN_MAX_DEF_VAR_NAME+"\" VariableName=\""+MAN_MAX_DEF_VAR_NAME+"\">\n"+
           "    <Meta>\n"+
           "      <Type>String</Type>\n"+
           "    </Meta>\n"+
           "    <Restriction>\n"+
           "      <Mandatory />\n"+
           "      <MaxLength>"+MAN_MAX_DEF_LIMIT+"</MaxLength>\n"+
           "      <DefaultType>"+MAN_MAX_DEF_DEFAULT_TYPE+"</DefaultType>\n"+
           "    </Restriction>\n"+
           "  </Data>\n"+
           "</DataType>";
  }
  
}
