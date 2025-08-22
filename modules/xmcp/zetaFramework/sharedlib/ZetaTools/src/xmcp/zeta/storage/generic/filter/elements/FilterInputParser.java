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

package xmcp.zeta.storage.generic.filter.elements;


public class FilterInputParser {

  // keine attribute, nur tool-klasse, wird von expression-parse-methoden durchgereicht?
  
  // parse
  // input: liste lexed-token 
  // -> root-container erzeugen
  // -> handle quotes
   
  // merge operators: && || als doppelte ops identifizieren und ersetzen?
  // schon hier check ob mehr als zwei hintereinander, dann fehler?
  
  // ? replace container
     
     
  // handle quotes (input liste lexed-token)
  // find first quote (single / double), find next (selber typ), dann replace-quotes
  
  // replace-quotes (liste lex-token, int startpos, int endpos)
  // output: neue liste lex-token (sublisten vorher u. nachher, quote-bereich durch neues literal ersetzt)
  // mit generisch replace() von container?
}
