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
package com.gip.xyna.xact.trigger;

import com.gip.xyna.xdev.xfractmod.xmdm.EnhancedStartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class MailStartParameter extends EnhancedStartParameter {

  private static Logger logger = CentralFactoryLogging.getLogger(MailStartParameter.class);
  
  private String mailAccount;
  private Duration pollingTime;
  private boolean readHeader;

  // the empty constructor may not be removed or throw exceptions! additional ones are possible, though.
  public MailStartParameter() {
  }

  @Override
  public StartParameter build(Map<String, Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException {
    MailStartParameter param = new MailStartParameter();
    param.mailAccount = MAIL_ACCOUNT.getFromMap(paramMap);
    param.pollingTime = POLLING_TIME.getFromMap(paramMap);
    param.readHeader = READ_HEADER.getFromMap(paramMap);
    return param;
  }

  @Override
  public List<String> convertToNewParameters(List<String> list)
      throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException {
    //gibt keine alten Parameter
    return list;
  }

  @Override
  public List<StringParameter<?>> getAllStringParameters() {
    return ALL_PARAMETERS;
  }
  
  
  
  public String getMailAccount() {
    return mailAccount;
  }
  
  public Duration getPollingTime() {
    return pollingTime;
  }
  
  public boolean getReadHeader() {
    return readHeader;
  }
  
  public static final StringParameter<String> MAIL_ACCOUNT = 
      StringParameter.typeString("mailAccount").
      documentation( Documentation.
          de("Name des verwendeten MailAccounts").
          en("Name of used mail account").build() ).
      mandatory().build();

  public static final StringParameter<Duration> POLLING_TIME = 
      StringParameter.typeDuration("pollingTime").
      documentation( Documentation.
          de("Wartezeit, nachdem keine Mail gefunden wurde").
          en("Wait time after no mail was found").build() ).
      defaultValue(Duration.valueOf("60 s")).build();
  
  public static final StringParameter<Boolean> READ_HEADER = 
      StringParameter.typeBoolean("readHeader").
      documentation( Documentation.
          de("Sollen Header-Informationen gelesen werden?").
          en("Should header informations be read?").build() ).
      defaultValue(Boolean.FALSE).build();

  private static final List<StringParameter<?>> ALL_PARAMETERS = 
      StringParameter.asList(MAIL_ACCOUNT, POLLING_TIME, READ_HEADER);

}
