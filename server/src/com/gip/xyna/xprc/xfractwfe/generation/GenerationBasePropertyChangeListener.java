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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryBase;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;



public class GenerationBasePropertyChangeListener implements IPropertyChangeListener {

  private static final Logger logger = CentralFactoryLogging.getLogger(GenerationBasePropertyChangeListener.class);

  private static volatile GenerationBasePropertyChangeListener _instance;

  // this needs to be true by default to allow for disabled XSD check when performing an update before the properties
  // have been set
  private volatile boolean validateXsdDisabled = true;

  private volatile boolean isRegistered = false;


  private GenerationBasePropertyChangeListener() {
    if (addThisAsPropertyChangeListener()) {
      logger.debug("Successfully registered as property change listener");
      isRegistered = true;
      propertyChanged();
    } else if (logger.isDebugEnabled()) {
      logger.debug("Could not register as property change listener, will try again on the next property request");
    }
  }


  GenerationBasePropertyChangeListener(String unused) {
  }


  public static GenerationBasePropertyChangeListener getInstance() {
    if (_instance == null) {
      synchronized (GenerationBasePropertyChangeListener.class) {
        if (_instance == null)
          _instance = new GenerationBasePropertyChangeListener();
      }
    } else if (!_instance.getIsRegistered()) {
      synchronized (_instance) {
        if (_instance.addThisAsPropertyChangeListener()) {
          _instance.isRegistered = true;
          _instance.propertyChanged();
        } else if (logger.isInfoEnabled()) {
          logger.info("Could not register as property change listener, will try again on the next property request");
        }
      }
    }
    return _instance;
  }


  static void setInstance(GenerationBasePropertyChangeListener instance) {
    _instance = instance;
  }


  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> result = new ArrayList<String>();
    result.add(XynaProperty.XYNA_DISABLE_XSD_VALIDATION);
    return result;
  }


  public void propertyChanged() {
    validateXsdDisabled = true;
    XynaFactoryBase xf = XynaFactory.getInstance();
    XynaFactoryManagementBase xfm = xf.getFactoryManagement();
    if (xfm != null) {
      String s = xfm.getProperty(XynaProperty.XYNA_DISABLE_XSD_VALIDATION);
      if (s != null) {
        validateXsdDisabled = Boolean.valueOf(s);
      }
    } else {
      // the factory management has not been loaded yet, so dont validate
      validateXsdDisabled = true;
    }
  }


  public boolean getValidateXsdDisabled() {
    return validateXsdDisabled;
  }


  private boolean addThisAsPropertyChangeListener() {
    try {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
                      .addPropertyChangeListener(this);
    } catch (NullPointerException e) {
      return false;
    }
    return true;
  }


  protected boolean getIsRegistered() {
    return isRegistered;
  }

}
