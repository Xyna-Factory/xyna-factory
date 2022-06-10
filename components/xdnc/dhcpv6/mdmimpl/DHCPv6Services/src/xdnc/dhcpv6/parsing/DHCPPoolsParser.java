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
package xdnc.dhcpv6.parsing;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;



public class DHCPPoolsParser {

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPPoolsParser.class);
  private static final Pattern SHARED_NETWORK_PATTERN = Pattern.compile("^shared-network ([^ #]+)\\{ #(.*)$");
  private static final Pattern LINE_TO_IGNORE_PATTERN = Pattern.compile(".*\\s+has no .*");
  private static final Pattern EMPTY_LINE_PATTERN = Pattern.compile("^\\s*$");
  private static final Pattern CMTS_LINKADDRESSES_PATTERN = Pattern.compile("^#(.+)$");
  private static final String IPV6_BLOCK = "[0-9a-fA-F]{0,4}";
  private static final String IPV6 = IPV6_BLOCK + "(?::" + IPV6_BLOCK + "){0,7}";
  private static final String IPV6_SUBNET = "(" + IPV6 + ")\\s*/(\\d{1,3})";
  private static final Pattern SUBNET_PATTERN = Pattern.compile("^\\s*subnet6\\s+" + IPV6_SUBNET + "\\s+\\{\\s*$");
  private static final Pattern ID_PATTERN = Pattern.compile("^\\s*#ID\\s*(\\d+)$");
  private static final Pattern RANGE_PATTERN = Pattern.compile("^\\s*(range6|prefix6)\\s+(" + IPV6 + ")\\s+(" + IPV6 + ")((?: /(\\d{1,3}))?);\\s*#(.*)$");  
  private static final Pattern ALLOWMEMBERS_PATTERN = Pattern.compile("^\\s+allow members of .*$");
  //private static final Pattern OPTION_PATTERN = Pattern.compile("^\\s*option\\s+(\\d+(?:\\.\\d+)?)\\s+(.*);\\s*$");
  private static final Pattern OPTION_PATTERN = Pattern.compile("^\\s*option\\s+([\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+(?:\\.[\\-a-zA-Z_0-9]+)?)?)\\s+(.*);\\s*$");
  private static final Pattern CLOSING_BRACE_PATTERN = Pattern.compile("^\\s*}\\s*$");
  private BufferedReader reader;
  private List<SharedNetwork> sharedNetworks;
  private int lineNumber;
  private String line;


  public DHCPPoolsParser(InputStream is) {
    this.reader = new BufferedReader(new InputStreamReader(is));
    sharedNetworks = new ArrayList<SharedNetwork>();
  }
  
  public List<SharedNetwork> getSharedNetworks() {
    return sharedNetworks;
  }


  public void parse() throws IOException, DHCPPoolsFormatException {
    lineNumber = -1;
    if (readNextLine()) {
      while (true) {
        SharedNetwork sharedNetwork;
        try {
          sharedNetwork = parseSharedNetwork();
          if (sharedNetwork == null) {
            break;
          }
          sharedNetworks.add(sharedNetwork);
        } catch (DHCPPoolsIgnoreLineException e) {
          // ignoriere diesen Eintrag
        }
        if (line == null) {
          break;
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("parsing finished");
      }
    }
  }


  /**
   * gibt true zurück, falls eine weitere zeile gelesen werden konnte
   */
  private boolean readNextLine() throws IOException {
    boolean b = null != (line = reader.readLine());
    if (b) {
      lineNumber++;
    }
    return b;
  }


  //aktuelle zeile ist die erste, die überprüft wird
  private SharedNetwork parseSharedNetwork() throws IOException, DHCPPoolsFormatException, DHCPPoolsIgnoreLineException {
    if (!gotoNextNonEmptyLine()) {
      return null;
    }

    Matcher matcher = SHARED_NETWORK_PATTERN.matcher(line);
    if (matcher.matches()) {
      String name = matcher.group(1);
      String comment = matcher.group(2);
      SharedNetwork sharedNetwork = new SharedNetwork(name, comment);
      checkFileDoesntEndHere(readNextLine());
      parseLinkAddresses(sharedNetwork);

      while (true) {
        Subnet subnet = parseSubnet();
        if (subnet == null) {
          break;
        }
        sharedNetwork.addSubnet(subnet);
      }
      
      skipClosingBrace();
      
      return sharedNetwork;
    } else if (LINE_TO_IGNORE_PATTERN.matcher(line).matches()){
      checkFileDoesntEndHere(readNextLine());
      throw new DHCPPoolsIgnoreLineException("found line <" + lineNumber + "> to ignore: " +line);
    } else {
      throw new DHCPPoolsFormatException("expected shared Network in line <" + lineNumber + ">: " + line);
    }
    //cursor ist auf der ersten zeile, die nicht mehr zum shared network gehört.
  }


  private boolean skipClosingBrace() throws IOException, DHCPPoolsFormatException {
    Matcher matcher = CLOSING_BRACE_PATTERN.matcher(line);
    if (matcher.matches()) {
      return readNextLine();
    } else {
      throw new DHCPPoolsFormatException("expected closing brace in line <" + lineNumber + ">: " + line);
    }
  }


  private Subnet parseSubnet() throws IOException, DHCPPoolsFormatException {
    checkFileDoesntEndHere(gotoNextNonEmptyLine());

    Matcher matcher = SUBNET_PATTERN.matcher(line);
    if (matcher.matches()) {
      String address = matcher.group(1);
      int prefixlength = Integer.valueOf(matcher.group(2));
      Subnet subnet = new Subnet(address, prefixlength);

      checkFileDoesntEndHere(readNextLine());
      String id = parseId();
      subnet.setGUIId(Long.valueOf(id));
      
      checkFileDoesntEndHere(readNextLine());
      
      //options
      while (true) {
        Option option = parseOption();
        if (option == null) {
          break;
        }
        subnet.addOption(option);
      }

      
      while (true) {
        Range range = parseRange();
        if (range == null) {
          break;
        }
        subnet.addRange(range);
      }
      
      checkFileDoesntEndHere(skipClosingBrace());

      return subnet;
    } else {
      return null;
    }
  }
  
  private void checkFileDoesntEndHere(boolean fileDoesntEndHere) throws DHCPPoolsFormatException {
    if (!fileDoesntEndHere) {
      throw new DHCPPoolsFormatException("unexpected end of file at line <" + lineNumber + ">: " + line);
    }
  }


  private Range parseRange() throws IOException, DHCPPoolsFormatException {
    checkFileDoesntEndHere(gotoNextNonEmptyLine());
    
    Matcher matcher = RANGE_PATTERN.matcher(line);
    if (matcher.matches()) {
      boolean isPrefix = matcher.group(1).equals("prefix6");
      String ipStart = matcher.group(2);
      String ipEnd = matcher.group(3);
      String prefixLengthString = matcher.group(4);
      int prefixLength = 128;
      String pooltype;
      if (isPrefix) {
        if (prefixLengthString.length() == 0) {
          throw new RuntimeException();
        }
        prefixLength = Integer.valueOf(matcher.group(5));
        pooltype = matcher.group(6);
      } else {
        pooltype = matcher.group(6);
      }
      Range range = new Range(ipStart, ipEnd, prefixLength, pooltype);
      
      //id
      checkFileDoesntEndHere(readNextLine());
      String id = parseId();
      range.setGUIId(Long.valueOf(id));
      
      //allow members "bla"
      while (true) {
        checkFileDoesntEndHere(readNextLine());
        if (!skipAllowMembers()) {
          break;
        }
      }
            
      //options
      while (true) {
        Option option = parseOption();
        if (option == null) {
          break;
        }
        range.addOption(option);
      }
      
      return range;
    } else {
      return null;
    }
  }


  private Option parseOption() throws IOException, DHCPPoolsFormatException {
    Matcher matcher = OPTION_PATTERN.matcher(line);
    if (matcher.matches()) {
      String optionNumber = matcher.group(1);
      String optionValue = matcher.group(2);
      Option option = new Option(optionNumber, optionValue);
      
      checkFileDoesntEndHere(readNextLine());
      return option;
    }
    return null;
  }


  private boolean skipAllowMembers() {
    Matcher matcher = ALLOWMEMBERS_PATTERN.matcher(line);
    if (matcher.matches()) {
      return true;
    }
    return false;
  }


  private String parseId() throws DHCPPoolsFormatException {
    Matcher matcher = ID_PATTERN.matcher(line);
    if (matcher.matches()) {
      return matcher.group(1);
    } else {
      throw new DHCPPoolsFormatException("expected id-comment in line <" + lineNumber + ">: " + line);
    }
  }


  /**
   * liest solange zeilen aus bis das ende des streams oder eine nicht leere zeile gefunden wurde. gibt true zurück,
   * falls die zuletzt ausgelesene zeile eine nicht leere zeile war. gibt false zurück, falls ende des streams erreicht.
   */
  private boolean gotoNextNonEmptyLine() throws IOException {
    while (true) {
      Matcher matcher = EMPTY_LINE_PATTERN.matcher(line);
      if (!matcher.matches()) {
        return true;
      }
      if (!readNextLine()) {
        break;
      }
    }
    return false;
  }


  private void parseLinkAddresses(SharedNetwork sharedNetwork) throws DHCPPoolsFormatException, IOException {
    Matcher matcher = CMTS_LINKADDRESSES_PATTERN.matcher(line);
    if (matcher.matches()) {
      sharedNetwork.setLinkAddresses(matcher.group(1));
    } else {
      throw new DHCPPoolsFormatException("expected link addresses in line <" + lineNumber + ">: " + line);
    }
    checkFileDoesntEndHere(readNextLine());
  }


  public static class DHCPPoolsFormatException extends Exception {

    public DHCPPoolsFormatException(String string) {
      super(string);
    }
    //FIXME remove
  }

  public static class DHCPPoolsIgnoreLineException extends Exception {

    public DHCPPoolsIgnoreLineException(String string) {
      super(string);
    }
    //FIXME remove
  }

}
