<#--
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
 -->
<#-- message entries for the exception as "lang", "message" pairs
  params:
    lastElementSep - boolean, if true, the last element in the list (if any) will have a trailing comma

  result:
    {[lang], [message]},
    {[lang], [message]},
    ...
    {[lang], [message]}(,) <eol>

  example:
    {"DE", "Fehler beim Lesen der Datei"},
    {"EN", "Error while reading the file"}
-->
<#macro message_entries lastElementSep=false>
<#list exception.getExceptionEntry().getMessages() as key, value>
{"${key}", "${value}"}<#if lastElementSep>,<#else><#sep>,</#sep></#if>
</#list>
</#macro>


<#-- documentation block for the (lang, message) map of the exception

  result:
    /**
     * Exception messages (lang, message) using %0%, %1%,... to refer to variables in messages
     * If possible include all variables in the message.
     * lang: DE, EN
     */ <eol>
-->
<#macro message_map_doc>
/**
 * Exception messages (lang, message) using %0%, %1%,... to refer to variables in messages
 * If possible include all variables in the message.
 * lang: DE, EN
 */
</#macro>


<#-- variable declaration for the (lang, message) map of the exception

  result:
    private static final Map<String, String> EXCEPTION_MESSAGES <inline>
-->
<#macro message_map_var>
<@compress single_line=true>
private static final Map<String, String> EXCEPTION_MESSAGES
</@compress>
</#macro>


<#-- variable declaration for the (lang, message) list of the exception

  result:
    private static final Pair<String, String>[] EXCEPTION_MESSAGES <inline>
-->
<#macro message_list_var>
<@compress single_line=true>
private static final Pair<String, String>[] EXCEPTION_MESSAGES
</@compress>
</#macro>


<#-- map lang -> message for the exception

  result:
    /**
    * Exception messages (lang, message) using %0%, %1%,... to refer to variables in messages
    * lang: DE, EN
    */
    private static final Map<String, String> EXCEPTION_MESSAGES = {
      [entries]
    }; <eol>

  example:
    /**
    * Exception messages (lang, message) using %0%, %1%,... to refer to variables in messages
    * lang: DE, EN
    */
    private static final Map<String, String> EXCEPTION_MESSAGES = {
      "de", "Fehler beim Lesen der Datei"
      "en", "Error while reading the file"
    };
-->
<#macro message_map>
<@message_map_doc/>
<@message_map_var/> = {
  <@exc.message_entries/>
};
</#macro>