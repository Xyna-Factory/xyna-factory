// !DO NOT EDIT THIS FILE!
// This source file is generated by Oracle tools
// Contents may be subject to change
// For reporting problems, use the following
// Version = Oracle WebServices (10.1.3.1.1, build 070111.22769)
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
package com.gip.xyna._1_5.xsd.faults._1;

@SuppressWarnings("serial")
public class XynaFault_ctype extends Exception {
   private java.lang.String code;
   private java.lang.String summary;
   private java.lang.String details;

   public XynaFault_ctype() {
   }

   public XynaFault_ctype(java.lang.String code, java.lang.String summary,
         java.lang.String details) {
      this.code = code;
      this.summary = summary;
      this.details = details;
   }

   public void setCode(java.lang.String code) {
      this.code = code;
   }

   public void setSummary(java.lang.String summary) {
      this.summary = summary;
   }

   public void setDetails(java.lang.String details) {
      this.details = details;
   }

   public java.lang.String getCode() {
      return code;
   }

   public java.lang.String getSummary() {
      return summary;
   }

   public java.lang.String getDetails() {
      return details;
   }
   
   public String getMessage() {
     return "[" + getCode() + "] " + getSummary();
   }

}
