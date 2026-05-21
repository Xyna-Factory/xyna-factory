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
package xact.connection.impl;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

import base.Text;
import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.ConnectionAlreadyClosed;
import xact.connection.DetectedError;
import xact.connection.ManagedConnection;
import xact.connection.ReadTimeout;
import xact.connection.Response;
import xact.connection.RuleBasedDeviceType;
import xact.connection.RuleBasedDeviceTypeInstanceOperation;
import xact.connection.RuleBasedDeviceTypeSuperProxy;
import xact.templates.Document;
import xact.templates.DocumentType;



public class RuleBasedDeviceTypeInstanceOperationImpl extends RuleBasedDeviceTypeSuperProxy implements RuleBasedDeviceTypeInstanceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(RuleBasedDeviceTypeInstanceOperationImpl.class);
  private static final long serialVersionUID = 1L;

  private static final Pattern splitLinePattern = Pattern.compile(Constants.LINE_SEPARATOR);
  private static final Pattern splitCommaPattern = Pattern.compile(",");
  private static final XynaPropertyString debugging =
      new XynaPropertyString("xact.connection.RuleBasedDeviceType.debug.devicenames", "")
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Comma separated list of device names (as used in the config file names). Whenever one of these devices evaluates rules the results will be logged on debug level.");
  private static final XynaPropertyInt restrictionLength = 
      new XynaPropertyInt("xact.connection.rulebaseddevicetype.promptMaxLength", -1)
          .setDefaultDocumentation(DocumentationLanguage.DE, "Maximale Länge des Ausgabeausschnitts welcher zur Prompt-Prüfung herrangezogen wird."
                                                           + " Die Eingabe von negative Werte deaktiviert die Beschränkung.")
          .setDefaultDocumentation(DocumentationLanguage.EN, "Maximum Length of the output excerpt to be used during prompt detection."
                                                           + " Use negative values for an unresticted prompt checking.");

  private static final Map<String, List<PromptParsing>> prompts = new ConcurrentHashMap<String, List<PromptParsing>>();
  private static final Map<String, List<MultiLine>> multiLines = new ConcurrentHashMap<String, List<MultiLine>>();
  private static final Map<String, List<IgnorePattern>> ignores = new ConcurrentHashMap<String, List<IgnorePattern>>();
  
  
  public RuleBasedDeviceTypeInstanceOperationImpl(RuleBasedDeviceType instanceVar) {
    super(instanceVar);
  }


  /*
   * reihenfolge entspricht der auswertung
   * leeres command -> command ist einfach nur enter
   * command = .* -> alle commands
   * achtung: response enthält meist noch das echo, das muss im regulären ausdruck berücksichtigt werden
   *PROMPTS
    <liste von 
       <command>;<prompt>[;<resultierender state>[;<autoresponse>]]
    >   
    MULTILINE
    <liste von
      <multiline command start>; <multiline content>; <multiline command end>
        d.h. es wird in dem document nach zeilen gesucht, die mit multiline command <start> beginnen und mit <end> enden und dazwischen nur <content> haben.
    >
    -> prompts und commands sind jeweils reguläre ausdrücke
    -> semikolons escapen (\;), zeilenumbrüche escapen (\n, \r), backslashes escapen (\\), \t ist erlaubt, kann aber auch direkt eingegeben werden
    -> # zu beginn der zeile sind kommentare
   */

  private static class CPattern {

    private final String pattern;
    private Pattern p;
    private boolean skip = false;

    public CPattern(String pattern) {
      this.pattern = pattern;
    }

    public Pattern getPattern() {
      if (skip) {
        if (logger.isDebugEnabled()) {
          logger.debug("skipping invalid pattern " + pattern);
        }
        return null;
      }
      if (p == null) {
        try {
          p = Pattern.compile(pattern, Pattern.DOTALL);
        } catch (PatternSyntaxException e) {
          logger.warn("Invalid pattern found: " + pattern, e);
          skip = true;
        }
      }
      return p;
    }


    public boolean matches(String s) {
      if (s == null) {
        s = "";
      }
      Pattern p = getPattern();
      if (p == null) {
        return false;
      }
      return getPattern().matcher(s).matches();
    }


    public String toString() {
      return pattern;
    }
  }

  private static class PromptParsing {

    private final CPattern prompt;
    private final CPattern command;
    private final String autoResponse;
    private final String state;


    public PromptParsing(String promptPattern, String commandPattern, String autoResponse, String state) {
      prompt = new CPattern(promptPattern);
      command = new CPattern(commandPattern);
      this.autoResponse = autoResponse;
      if (state != null && state.trim().length() == 0) {
        this.state = null;
      } else {
        this.state = state;
      }
    }


    public boolean matches(MatchCache mc, String cmd, String response) {
      if (mc.matchedCommands.contains(command.pattern)) {
        //nicht mehrfach die gleichen pattern probieren, wenn sie vorher mal nicht gematched haben
        return false;
      }
      if (mc.matchedResponses.contains(prompt.pattern)) {
        return false;
      }
      if (!command.matches(cmd == null ? "" : cmd)) {
        mc.matchedCommands.add(command.pattern);
        return false;
      }
      if (!prompt.matches(response)) {
        mc.matchedResponses.add(prompt.pattern);
        return false;
      }
      return true;
    }


    public String toString() {
      String s = "prompt=" + prompt + ", command=" + command;
      if (autoResponse != null) {
        s += ", autoResponse=" + autoResponse;
      }
      if (state != null) {
        s += ", state=" + state;
      }
      return s;
    }
  }


  private static class MultiLine {

    private final CPattern start;
    private final CPattern middle;
    private final CPattern end;


    public MultiLine(String start, String middle, String end) {
      this.start = new CPattern(start);
      this.middle = new CPattern(middle);
      this.end = new CPattern(end);
    }


    public boolean isStart(String part) {
      return start.matches(part);
    }


    public boolean isMiddle(String part) {
      return middle.matches(part);
    }


    public boolean isEnd(String part) {
      return end.matches(part);
    }


    public String toString() {
      return "start=" + start + ", middle=" + middle + ", end=" + end;
    }
  }
  
  private static class IgnorePattern {
    
    private final CPattern pattern;
    private final CPattern command;
    
    public IgnorePattern(String commandPattern, String pattern) {
      this.command = new CPattern(commandPattern);
      this.pattern = new CPattern(pattern);
    }
    
    public String toString() {
      return "command=" + command + ", ignore=" + pattern;
    }

    public String removeIgnoredParts(String cmd, String response) {
      if (!command.matches(cmd)) {
        return response;
      }
      return pattern.getPattern().matcher(response).replaceAll("");
    }
    
  }

  
  private String state;


  private void init(RuleBasedDeviceType instance) {
    String devName = getNameOfDevice(instance);
    String key = getKey(instance);
    if (prompts.containsKey(key)) {
      return;
    }

    String fileName = devName + ".conf";
    InputStream resourceAsStream = null;
    try {
      resourceAsStream = FileUtils.getInputStreamFromResource(fileName, instance.getClass().getClassLoader());
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("Could not access " + fileName + ".", e);
    }
    if (resourceAsStream == null) {
      //versuchen als File direkt zu laden
      String location = null;
      try {
        location =
            GenerationBase.getFileLocationOfServiceLibsForDeployment(instance.getClass().getName(),
                                                                     RevisionManagement.getRevisionByClass(instance.getClass()))
                + Constants.FILE_SEPARATOR + fileName;
        resourceAsStream = new FileInputStream(location);
      } catch (FileNotFoundException e) {
        if (logger.isTraceEnabled()) {
          logger.trace(null, e);
        }
      }
      if (resourceAsStream == null) {
        throw new RuntimeException("Didn't find rule file used by device type: " + fileName + ".");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Loading " + fileName + " from " + location + ".");
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("Loading " + fileName + " found by classloader.");
      }
    }

    StringBuilder sb;
    try {
      BufferedReader br;
      try {
        br = new BufferedReader(new InputStreamReader(resourceAsStream, Constants.DEFAULT_ENCODING));
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

      //file einlesen
      sb = new StringBuilder();
      char[] buffer = new char[2048];
      int countRead = 0;
      while ((countRead = br.read(buffer)) != -1) {
        String readData = String.valueOf(buffer, 0, countRead);
        sb.append(readData);
      }
    } catch (IOException e) {
      throw new RuntimeException("Could not read from " + fileName + ".", e);
    } finally {
      try {
        resourceAsStream.close();
      } catch (IOException e) {
        logger.warn("Could not close stream to " + fileName);
      }
    }

    List<PromptParsing> lprompts = new ArrayList<PromptParsing>();
    List<MultiLine> lmultiLines = new ArrayList<MultiLine>();
    List<IgnorePattern> lignorePatterns = new ArrayList<IgnorePattern>();

    String fileContentAsString = sb.toString();
    String[] lines = splitLinePattern.split(fileContentAsString);
    int state = 0;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      if (line.startsWith("#") || line.trim().length() == 0) {
        continue;
      }
      if (line.trim().equals("PROMPTS")) {
        state = 1;
        continue;
      } else if (line.trim().equals("MULTILINE")) {
        state = 2;
        continue;
      } else if (line.trim().equals("IGNORES")) {
        state = 3;
        continue;
      }
      if (state == 1) {
        String[] parts = splitSemikolonAndUnescape(line);
        if (parts.length == 2) {
          lprompts.add(new PromptParsing(parts[1], parts[0], null, null));
        } else if (parts.length == 3) {
          lprompts.add(new PromptParsing(parts[1], parts[0], null, parts[2]));
        } else if (parts.length == 4) {
          lprompts.add(new PromptParsing(parts[1], parts[0], parts[3], parts[2]));
        } else {
          logger.warn("Found invalid prompt specification in " + fileName + ": " + line);
          continue;
        }
      } else if (state == 2) {
        String[] parts = splitSemikolonAndUnescape(line);
        if (parts.length != 3) {
          logger.warn("Found invalid multiline specification in " + fileName + ": " + line);
          continue;
        }
        lmultiLines.add(new MultiLine(parts[0], parts[1], parts[2]));
      } else if (state == 3) {
        String[] parts = splitSemikolonAndUnescape(line);
        if (parts.length != 2) {
          logger.warn("Found invalid ignoreline specification in " + fileName + ": " + line);
          continue;
        }
        lignorePatterns.add(new IgnorePattern(parts[0], parts[1]));
      }
    }

    prompts.put(key, lprompts);
    multiLines.put(key, lmultiLines);
    ignores.put(key, lignorePatterns);

    if (logger.isInfoEnabled()) {
      logger.info("Parsing from " + fileName + " finished:");
      if (logger.isDebugEnabled()) {
        if (lprompts.size() > 0) {
          logger.debug("global prompts:");
          for (PromptParsing ps : lprompts) {
            logger.debug(ps);
          }
        }
        if (lmultiLines.size() > 0) {
          logger.debug("multi lines:");
          for (MultiLine ml : lmultiLines) {
            logger.debug(ml);
          }
        }
        if (lignorePatterns.size() > 0) {
          logger.debug("ignore lines:");
          for (IgnorePattern ip : lignorePatterns) {
            logger.debug(ip);
          }
        }
      }
    }
  }


  private String getKey(RuleBasedDeviceType instance) {
    return RevisionManagement.getRevisionByClass(instance.getClass()) + "-" + getNameOfDevice(instance);
  }


  protected String getNameOfDevice(RuleBasedDeviceType instance) {
    if (instance.getDeviceTypeId() != null) {
      return instance.getDeviceTypeId();
    }
    return instance.getClass().getSimpleName();
  }


  private static String[] splitSemikolonAndUnescape(String line) {
    StringBuilder sb = new StringBuilder();
    List<String> parts = new ArrayList<String>();
    int idx = -1;
    while (true) {
      idx++;
      if (idx >= line.length()) {
        parts.add(sb.toString());
        break;
      }
      char c = line.charAt(idx);
      switch (c) {
        case '\\' :
          //escaped zeichen
          idx++;
          if (idx >= line.length()) {
            logger.warn("Line is invalid because it ends with an unescaped backslash.");
            return new String[0];
          }
          c = line.charAt(idx);
          switch (c) {
            case '\\' :
              sb.append(c);
              break;
            case 'n' :
              sb.append('\n');
              break;
            case 'r' :
              sb.append('\r');
              break;
            case 't' :
              sb.append('\t');
              break;
            case ';' :
              sb.append(';');
              break;
            default :
              logger.warn("Line is invalid because it contains a backslash following a " + c + ". The backslash must be escaped.");
              return new String[0];
          }
          break;
        case ';' :
          parts.add(sb.toString());
          sb.setLength(0);
          break;
        default :
          sb.append(c);
          break;
      }
    }
    return parts.toArray(new String[parts.size()]);
  }


  private static class SentAutoResponse {

    private final String response;
    private final CommandResponseTuple autoResponse;
    private String[] parts;
    private int lineFoundIndex = 0;


    public SentAutoResponse(String response, CommandResponseTuple autoResponse) {
      this.response = response;
      this.autoResponse = autoResponse;
    }


    public int getInsertionAfterIndex(int prev, String[] lines) {
      if (parts == null) {
        parts = splitLinePattern.split(response);
      }
      for (int i = prev; i < lines.length; i++) {
        String s = lines[i];
        boolean found = false;
        for (int j = lineFoundIndex; j < parts.length; j++) {
          String t = parts[j];
          if (t.equals(s)) {
            lineFoundIndex = j;
            found = true;
            break;
          }
        }
        if (!found) {
          return i;
        }
      }
      return lines.length;
    }
  }


  private List<SentAutoResponse> sentAutoResponses;


  public Boolean checkInteraction(CommandResponseTuple response, DocumentType documentType) {
    return false;
  }


  public void cleanupAfterError(CommandResponseTuple response, DocumentType documentType, ManagedConnection managedConnection) {
    sentAutoResponses = null;
  }


  public void detectCriticalError(CommandResponseTuple response, DocumentType documentType) throws DetectedError {
  }


  public Command enrichCommand(Command command) {
    return command;
  }


  private static class MatchCache {

    private final Set<String> matchedResponses = new HashSet<String>();
    private final Set<String> matchedCommands = new HashSet<String>();
  }


  /*
   * TODO echos besser ausblenden - dann kann man die regulären ausdrücke für die prompterkennung verbessern
   *      achtung: echos gibt es nicht immer, typischer usecase: password-eingabe wird nicht geechoed
   * 
   */

  private boolean debug = false;

  public Boolean isResponseComplete(String response, DocumentType documentType, ManagedConnection managedConnection, Command command) {
    init(getInstanceVar());
    debug = logger.isDebugEnabled() && debug();
    MatchCache mc = new MatchCache();
    String cmd = command.getContent();
    String responseMinusIgnored = removeIgnoredParts(cmd, response);
    if (debug) {
      logger.debug("device=" + getNameOfDevice(getInstanceVar()) + ", state=" + state + ", cmd=<" + reduce(cmd) + ">");
      logger.debug("response complete? <" + reduce(response) + ">");
      if (response.length() != responseMinusIgnored.length()) {
        logger.debug("response w\\o ignored: <" + reduce(responseMinusIgnored) + ">");
      }
    }
    
    int restriction = restrictionLength.get();
    if (restriction > 0 && // restriction <=0 = no restriction
        responseMinusIgnored.length() > restriction) {
      responseMinusIgnored = responseMinusIgnored.substring(responseMinusIgnored.length() - restriction, responseMinusIgnored.length());
    }
    
    for (PromptParsing pp : prompts.get(getKey(getInstanceVar()))) {
      if (debug) {
        logger.debug("match " + pp);
      }
      if (pp.matches(mc, cmd, responseMinusIgnored)) {
        if (debug) {
          logger.debug("complete: yes");
        }
        if (pp.state != null) {
          state = pp.state;
        }
        if (pp.autoResponse != null) {
          if (debug) {
            logger.debug("sending autoresponse: " + pp.autoResponse);
          }
          try {
            Command autoResponseCmd = new Command(pp.autoResponse);
            try { // workaround to create unique timestamps in cases of auto response
              Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            CommandResponseTuple send =
                managedConnection.send(autoResponseCmd, documentType, getInstanceVar(), managedConnection.getConnectionParameter()
                    .getDefaultSendParameter());
            command.setContent(cmd + "\n" + autoResponseCmd.getContent());
            if (sentAutoResponses == null) {
              sentAutoResponses = new ArrayList<SentAutoResponse>();
            }
            sentAutoResponses.add(new SentAutoResponse(response, send));
          } catch (ConnectionAlreadyClosed e) {
            throw new RuntimeException("Could not send automatic response", e);
          } catch (ReadTimeout e) {
            throw new RuntimeException("Could not send automatic response", e);
          }
        }
        return true;
      }
    }
    if (debug) {
      logger.debug("complete: no");
    }
    return false;
  }

  
  private String removeIgnoredParts(String cmd, String response) {
    List<IgnorePattern> patterns = ignores.get(getKey(getInstanceVar()));
    if (patterns.size() == 0) {
      return response;
    }
    for (IgnorePattern ip : patterns) {
      response = ip.removeIgnoredParts(cmd, response);
    }
    return response;
  }


  private static String reduce(String r) {
    if (r == null) {
      return r;
    }
    if (r.length() > 100) {
      r = "[" + r.length() + "]" + r.substring(0, 45) + "..." + r.substring(r.length() - 45);
    }
    r = r.replaceAll("\n", Matcher.quoteReplacement("\\n"));
    r = r.replaceAll("\r", Matcher.quoteReplacement("\\r"));
    r = r.replaceAll("\t", Matcher.quoteReplacement("\\t"));
    r = r.replaceAll("\b", Matcher.quoteReplacement("\\b"));
    return r;
  }


  private boolean debug() {
    String deviceNamesToDebug = debugging.get();
    if (deviceNamesToDebug != null && deviceNamesToDebug.length() > 0) {
      String name = getNameOfDevice(instanceVar);
      for (String deviceName : splitCommaPattern.split(deviceNamesToDebug)) {
        if (deviceName.trim().equals(name)) {
          return true;
        }
      }
    }
    return false;
  }


  @Override
  public List<? extends Command> partitionCommands(Document doc) {
    init(getInstanceVar());
    debug = logger.isDebugEnabled() && debug();
    String s = doc.getReadBuffer();
    if (debug) {
      logger.debug("device=" + getNameOfDevice(getInstanceVar()) + ", state=" + state);
      logger.debug("partition command: <" + reduce(s) + ">");
    }
    String[] split = splitLinePattern.split(s);
    XynaObjectList<Command> commands = new XynaObjectList<Command>(xact.connection.Command.class);
    parts : for (int i = 0; i < split.length; i++) {
      String part = split[i];
      if (debug) {
        logger.debug("check multiline for command: <" + reduce(part) + ">");
      }
      if (part.trim().length() > 0) {
        for (MultiLine ml : multiLines.get(getKey(getInstanceVar()))) {
          if (debug) {
            logger.debug("check multiline: " + ml);
          }
          if (ml.isStart(part)) {
            int j = i + 1;
            if (j >= split.length) {
              if (debug) {
                logger.debug("multiline does not end in command");
              }
              continue parts;
            }
            part = split[j];
            while (ml.isMiddle(part) && !ml.isEnd(part)) {
              j++;
              if (j >= split.length) {
                if (debug) {
                  logger.debug("multiline does not end in command");
                }
                break;
              }
              part = split[j];
            }
            if (ml.isEnd(part)) {
              StringBuilder sb = new StringBuilder();
              for (int k = i; k <= j; k++) {
                if (k > i) {
                  sb.append("\n");
                }
                sb.append(split[k]);
              }
              commands.add(new Command(sb.toString()));
              i = j;
              if (debug) {
                logger.debug("multiline: yes. end=<" + reduce(split[j]) + ">");
              }
              continue parts;
            }
            //war kein multiline -> part zurücksetzen
            part = split[i];
          }
        }
        if (debug) {
          logger.debug("multiline: no");
        }
        commands.add(new Command(part));
      }
    }

    return commands;
  }


  public CommandResponseTuple removeDeviceSpecifics(CommandResponseTuple response) {

    /*
     * ergänze response um eventuelle autoresponses von oben.
     * dazu suche in der response nach der ersten zeile, die in der response von der autoresponse nicht vorgekommen war. das muss der index in der response sein, 
     * wo die autoresponse eingefügt werden muss
     */
    if (sentAutoResponses != null) {
      String[] lines = splitLinePattern.split(response.getResponse().getContent());
      int prev = 0;
      StringBuilder respContent = new StringBuilder();
      for (SentAutoResponse sar : sentAutoResponses) {
        int insertion = sar.getInsertionAfterIndex(prev, lines);
        for (int i = prev; i < insertion; i++) {
          if (i > 0) {
            respContent.append(Constants.LINE_SEPARATOR);
          }
          respContent.append(lines[i]);
        }
        prev = insertion;
        respContent.append(Constants.LINE_SEPARATOR);
        respContent.append(sar.autoResponse.getResponse().getContent());
      }
      Response r = new Response(respContent.toString());
      response.setResponse(r);
      sentAutoResponses = null;
    }
    return response;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


  public static void resetCache() {
    prompts.clear();
    multiLines.clear();
  }


  public Text getState() {
    return new Text(state);
  }


}
