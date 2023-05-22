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
package xdnc.dhcpv6.parsing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

import junit.framework.TestCase;
import xdnc.dhcpv6.parsing.DHCPPoolsParser;
import xdnc.dhcpv6.parsing.Range;
import xdnc.dhcpv6.parsing.SharedNetwork;
import xdnc.dhcpv6.parsing.Subnet;
import xdnc.dhcpv6.parsing.DHCPPoolsParser.DHCPPoolsFormatException;


public class DHCPPoolsParserTest extends TestCase {

  public void test1() throws IOException, DHCPPoolsFormatException {
    String fileContent = "shared-network CMTS12{ #Standort Stuttgart\n" +
    "#fe80:0000:0000:0000:0224:81ff:fee3:0001,fe80:0000:0000:0000:0224:81ff:fee3:0002\n" +
    "  subnet6 fe80::/64 {\n" +
    "  #ID 2\n" +
    "    range6 fe80:0000:0000:0000:0224:81ff:fee3:1000 fe80:0000:0000:0000:022f:81ff:fee3:1020; #MTA-Pool\n" + 
    "    #ID 2\n" +
    "      allow members of \"MTAHybrid_DPP122MTA15\";\n" +
    "      allow members of \"MTABasic\";\n" + 
    "    range6 3ffe:0501:ffff:0100:0000:0000:0000:0001 3ffe:0501:ffff:0100:0000:0000:0000:2000; #CM-Pool\n" + 
    "    #ID 423\n" + 
    "      allow members of \"DOCSIS\";\n" +
    "    range6 3ffe:0502:ffff:0100:0000:0000:0000:0001 3ffe:0502:ffff:0100:0000:0000:0000:0001; #CPE-Pool\n" + 
    "    #ID 593\n" + 
    "      allow members of \"CPE_DPP122MTA15\";\n" +
    "      allow members of \"TR069_DPP122MTA15\";\n" +
    "      option 6 fd00::230:4ff:fe83:5ffa;\n" + 
    "    prefix6 3ffe:0501:ffff:0500:0000:0000:0000:0000 3ffe:0501:ffff:0505:0000:0000:0000:0000 /63; #CPE-Pool\n" + 
    "    #ID 1928\n" +
    "      allow members of \"CPE_DPP122MTA15\";\n" + 
    "      allow members of \"TR069_DPP122MTA15\";\n" + 
    "      option 6 fd00::230:48ff:fe83:5ffa,fd00::230:48ff:fe83:5ffa;\n" +
    "      option 7 bla;\n" +
    "  }\n" +
    "}\n";

    DHCPPoolsParser parser = new DHCPPoolsParser(new ByteArrayInputStream(fileContent.getBytes()));
    parser.parse();
    List<SharedNetwork> sharedNetworks = parser.getSharedNetworks();
    assertNotNull(sharedNetworks);
    assertEquals(1, sharedNetworks.size());
    SharedNetwork sharedNetwork = sharedNetworks.get(0);
    assertNotNull(sharedNetwork);
    assertEquals("CMTS12", sharedNetwork.getName());
    assertEquals("Standort Stuttgart", sharedNetwork.getComment());
    assertEquals("fe80:0000:0000:0000:0224:81ff:fee3:0001,fe80:0000:0000:0000:0224:81ff:fee3:0002", sharedNetwork
        .getLinkAddresses());
    assertNotNull(sharedNetwork.getSubnets());
    assertEquals(1, sharedNetwork.getSubnets().size());
    Subnet subnet = sharedNetwork.getSubnets().get(0);
    assertNotNull(subnet);
    assertEquals("fe80::", subnet.getAddress());
    assertEquals(64, subnet.getPrefixlength());
    assertEquals(2, subnet.getGUIId());
    assertNotNull(subnet.getRanges());
    assertEquals(4, subnet.getRanges().size());
    List<Range> ranges = subnet.getRanges();
    Range range1 = ranges.get(0);
    compareRange(range1, "fe80:0000:0000:0000:0224:81ff:fee3:1000", "fe80:0000:0000:0000:022f:81ff:fee3:1020",
                 "MTA-Pool", 2, 128, new String[][] {});

    Range range2 = ranges.get(1);
    compareRange(range2, "3ffe:0501:ffff:0100:0000:0000:0000:0001", "3ffe:0501:ffff:0100:0000:0000:0000:2000",
                 "CM-Pool", 423, 128, new String[][] {});

    Range range3 = ranges.get(2);
    compareRange(range3, "3ffe:0502:ffff:0100:0000:0000:0000:0001", "3ffe:0502:ffff:0100:0000:0000:0000:0001",
                 "CPE-Pool", 593, 128, new String[][] {new String[] {"6", "fd00::230:4ff:fe83:5ffa"}});

    Range range4 = ranges.get(3);
    compareRange(range4, "3ffe:0501:ffff:0500:0000:0000:0000:0000", "3ffe:0501:ffff:0505:0000:0000:0000:0000",
                 "CPE-Pool", 1928, 63,
                 new String[][] {new String[] {"6", "fd00::230:48ff:fe83:5ffa,fd00::230:48ff:fe83:5ffa"},
                     new String[] {"7", "bla"}});

  }


  private void compareRange(Range range, String start, String end, String pooltype, int id, int prefixlength,
                            String[][] options) {
    assertEquals(start, range.getIpStart());
    assertEquals(end, range.getIpEnd());
    assertEquals(pooltype, range.getPooltype());
    assertEquals(id, range.getGUIId());
    assertEquals(prefixlength, range.getPrefixlength());
    assertEquals(options.length, range.getOptions().size());
    for (int i = 0; i < options.length; i++) {
      String[] op = options[i];
      assertEquals(op[0], "" + range.getOptions().get(i).getOptionNumber());
      assertEquals(op[1], range.getOptions().get(i).getOptionValue());
    }
  }
  
}
