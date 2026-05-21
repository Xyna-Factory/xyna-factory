/*
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
 */
package xmcp.xypilot.impl.gen.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import xmcp.xypilot.ExceptionMessage;
import xmcp.xypilot.impl.gen.model.ExceptionModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;

/**
 * Parses the completion returned by the ai server to a list of exception messages, i.e. pairs of language code and message.
 * @see Parser
 */
public class ExceptionParser implements Parser<List<ExceptionMessage>, ExceptionModel> {

    private static Logger logger = Logger.getLogger("XyPilot");

    private static String EXCEPTION_MESSAGE_REGEX = ".*\"(.*?)\"\\s*[,:;-=]\\s*\"(.*?)\".*";
    private static Pattern EXCEPTION_MESSAGE_PATTERN = Pattern.compile(EXCEPTION_MESSAGE_REGEX);

    private static List<String> ALLOWED_LANG_KEYS = List.of("EN", "DE");


    @Override
    public List<ExceptionMessage> parse(String input, ExceptionModel dataModel) {
        return parseExceptionMessages(input);
    }


    /**
     * Parses a string as an exception message. Must contain two groups:
     * "\"<lang>\", \"<msg>\""
     *
     * @param text
     * @return null if message could not be parsed
     */
    public static ExceptionMessage parseExceptionMessage(String text) {
        // replace escaped quotes with single quotes
        text = text.replaceAll("\\\\\"", "'");

        Matcher matcher = EXCEPTION_MESSAGE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            return null;
        }

        String lang = matcher.group(1);
        String msg = matcher.group(2);

        if (lang == null || lang.length() == 0 || msg == null || msg.length() == 0 || !ALLOWED_LANG_KEYS.contains(lang.toUpperCase())) {
            return null;
        }

        // fix missing closing % in message
        msg = msg.replaceAll("%(\\d+)([^%])", "%$1%$2");

        return new ExceptionMessage(lang, msg);
    }


    public static List<ExceptionMessage> parseExceptionMessages(String text) {
        List<ExceptionMessage> messages = new ArrayList<>();

        text.lines().forEach((line) -> {
            ExceptionMessage msg = parseExceptionMessage(line);
            if (msg != null) {
                messages.add(msg);
            } else {
                logger.warn("Couldn't parse exceptionMessage (" + line + ")");
            }
        });

        return messages;
    }

}
