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
package com.gip.xyna.utils.console;


/**
 *
 */
public enum AnsiTextColor {

  normal("\u001b[0m"),
  
  black("\u001b[30m"),
  red("\u001b[31m"),
  green("\u001b[32m"),
  brown("\u001b[33m"),
  blue("\u001b[34m"),
  purple("\u001b[35m"),
  cyan("\u001b[36m"),
  lightgray("\u001b[37m"),
  
  
  bolddarkgray("\u001b[1;30m"),
  boldlightred("\u001b[1;31m"),
  boldlightgreen("\u001b[1;32m"),
  boldyellow("\u001b[1;33m"),
  boldlightblue("\u001b[1;34m"),
  boldlightpurple("\u001b[1;35m"),
  boldlightcyan("\u001b[1;36m"),
  boldwhite("\u001b[1;37m"),
  
  ;
 
  
  private String escapeSequence;

  AnsiTextColor( String escapeSequence) {
    this.escapeSequence = escapeSequence;
  }
  
  public String getEscapeSequence() {
    return escapeSequence;
  }
  
}
