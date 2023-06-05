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

package com.gip.xyna.xdev.xfractmod.xmdm;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;


/**
 * achtung: beim refactoring auch das zugehörige MDM XML ändern
 */
public abstract class XynaExceptionBase extends XynaException implements GeneralXynaObject {


  private static final long serialVersionUID = 1L;


  public XynaExceptionBase(String code) {
    super(code);
  }


  public XynaExceptionBase(String[] codeAndArgs) {
    super(codeAndArgs);
  }


  public XynaExceptionBase(String code, String arg) {
    super(code, arg);
  }


  public XynaExceptionBase(String code, String[] args) {
    super(code, args);
  }


  public XynaExceptionBase(String code, Throwable cause) {
    super(code, cause);
  }


  public XynaExceptionBase(String[] codeAndArgs, Throwable cause) {
    super(codeAndArgs, cause);
  }


  public XynaExceptionBase(String code, String arg, Throwable cause) {
    super(code, arg, cause);
  }


  public XynaExceptionBase(String code, String[] args, Throwable cause) {
    super(code, args, cause);
  }


  public String toXml() {
    return toXml(null);
  }


  public String toXml(String varName) {
    return toXml(varName, false);
  }


  public abstract String toXml(String varName, boolean onlyContent);

  public abstract XynaExceptionBase cloneWithoutCause();

  public XynaExceptionBase cloneWithoutCause(boolean deep) {
    if (deep) {
      return cloneWithoutCause();
    } else {
      throw new UnsupportedOperationException("shallow clone not supported for xynaExceptionBase");
    }
  }
  
  /**
   * erhält den cause falls vorhanden
   * legt eine tiefe Kopie an
   */
  public XynaExceptionBase clone() {
    return clone(true);
  }

  /**
   * erhält den cause falls vorhanden
   * @param deep
   */
  public XynaExceptionBase clone(boolean deep) {
    if (getCause() == null) {
      return cloneWithoutCause(deep);
    } else {
      return (XynaExceptionBase)cloneWithoutCause(deep).initCause(getCause());
    }
  }
  

  public static String[] transformCodeAndArgs(String code, String[] args) {
    String[] argsAggregated;
    if (args != null) {
      argsAggregated = new String[1 + args.length];
      argsAggregated[0] = code;
      System.arraycopy(args, 0, argsAggregated, 1, args.length);
    } else {
      argsAggregated = new String[] {code};
    }
    return argsAggregated;
  }

  @Override
  public Long getRevision() {
    if(getClass().getClassLoader() instanceof ClassLoaderBase) {
      return ((ClassLoaderBase)getClass().getClassLoader()).getRevision();
    } else {
      return super.getRevision();
    }
  }
  
}
