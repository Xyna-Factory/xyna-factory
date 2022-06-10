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
package com.gip.xyna.xprc.xsched.orderseries;

import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.CollectionUtils;



/**
 *
 */
public class CorrelationIdTransformator {
  
  public static CommaMasker commaMasker = new CommaMasker();
  public static CommaUnmasker commaUnmasker = new CommaUnmasker();
  
  
  public static class CommaMasker implements CollectionUtils.Transformation<String,String> {
    private Pattern backslash = Pattern.compile("\\", Pattern.LITERAL);
    private Pattern comma = Pattern.compile(",", Pattern.LITERAL);

    public String transform(String from) {
      if( from.indexOf('\\') != -1 || from.indexOf(',') != -1 ) {
        String s = backslash.matcher(from).replaceAll("\\\\5c");
        return comma.matcher(s).replaceAll("\\\\2c");
      }
      return from;
    }
  }
  
  public static class CommaUnmasker implements CollectionUtils.Transformation<String,String> {
    private Pattern backslash = Pattern.compile("\\5c", Pattern.LITERAL);
    private Pattern comma = Pattern.compile("\\2c", Pattern.LITERAL);

    public String transform(String from) {
      if( from.indexOf('\\') != -1 ) {
        String s = backslash.matcher(from).replaceAll("\\\\");
        return comma.matcher(s).replaceAll(",");
      }
      return from;
    }
  }
  
}
