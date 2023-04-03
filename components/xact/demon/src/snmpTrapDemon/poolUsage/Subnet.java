/*---------------------------------------------------
 * Copyright GIP AG 2009
 * (http://www.gip.com)
 *
 * Heinrich-von-Brentano-Str. 2
 * 55130 Mainz
 *----------------------------------------------------
 * $Revision: 134477 $
 * $Date: 2013-05-31 14:13:46 +0200 (Fr, 31 Mai 2013) $
 *----------------------------------------------------
 */
package snmpTrapDemon.poolUsage;


/**
 *
 *
 */
public final class Subnet {

  /**
   * Diese Exception wird von Subnet.parse(String subnet) geworfen, wenn der 
   * String keine gültiges Subnet darstellt
   *
   */
  public static class InvalidSubnetException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidSubnetException(String message) {
      super(message);
    }
    
  }

  private IPAddress ipSubnet;
  private IPAddress netMask;
  private String asString;
  
  private Subnet() {
    //internal use only
  }
  private Subnet(IPAddress ipSubnet, IPAddress netMask) {
    //internal use only
    this.ipSubnet = ipSubnet;
    this.netMask = netMask;
    this.asString = ipSubnet+"/"+Long.bitCount(netMask.toLong());
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if( ! ( obj instanceof Subnet ) ) {
      return false;
    }
    return asString == ((Subnet)obj).asString;
  }

  @Override
  public int hashCode() {
    return asString.hashCode();
  }

  
  
  /**
   * Bau eines Subnets aus dem angegebenen String im Format "192.168.0.0/16"
   * @param ip
   * @return
   * @throws InvalidSubnetException
   */
  public static Subnet parse( String subnet ) throws InvalidSubnetException {
    String[] partsS = subnet.split( "/" );
    if( partsS.length != 2  ) {
      throw new InvalidSubnetException( "no separator (\"/\")" );
    }
    
    IPAddress ipSubnet = IPAddress.parse( partsS[0]);
    IPAddress netMask = parseNetMask( partsS[1] );
    
    if( ! isSubnet(ipSubnet,netMask) ) {
      throw new InvalidSubnetException( "not a valid subnet" );
    }
    return new Subnet(ipSubnet,netMask);
  }
  
  /**
   * @param subnet
   * @param netMask
   * @return
   */
  public static Subnet parse(String subnet, String netMask) {
    IPAddress sn = IPAddress.parse(subnet);
    IPAddress nm = IPAddress.parse(netMask);
    if( ! checkNetMask(nm) ) {
      throw new InvalidSubnetException( netMask + " is not a valid netMask" );
    }
    if( ! isSubnet(sn,nm) ) {
      throw new InvalidSubnetException( "not a valid subnet" );
    }
    return new Subnet(sn,nm);
  }

  /**
   * @param nm
   * @return
   */
  public static boolean checkNetMask(IPAddress nm) {
    long nml = nm.toLong();
    return nml == netMask( Long.bitCount(nml));
  }
  
  /**
   * Test, ob die IP subnet nur aus dem Netzwerkteil besteht
   * @param sn subNet
   * @param nm NetMask
   * @return
   */
  private static boolean isSubnet( IPAddress sn, IPAddress nm) {
    //der Geräteteil der IP wird mit &nml entfernt
    return (sn.toLong()&nm.toLong()) == sn.toLong();
  }

  /**
   * @param string
   * @return
   */
  private static IPAddress parseNetMask(String nm) {
    int bits = 0;
    try { 
      bits = Integer.parseInt(nm);
      if( bits<0 || bits > 32 ) {
        throw new InvalidSubnetException( "invalid netmask " );
      }
    } catch( NumberFormatException e ) {
      throw new InvalidSubnetException( "invalid netmask " );
    }
    return IPAddress.parse( netMask(bits) );
  }

  /**
   * @param bits
   * @return
   */
  private static long netMask(int bits) {
    long full = 0xFFFFFFFFL; //alle Bits gesetzt
    long notNm = full >> bits; //invertierte Netzmaske
    return full ^ notNm; //IpAddresse zur Netzmaske 
  }
  
  /**
   * @return
   */
  public IPAddress getSubnet() {
    return ipSubnet;
  }

  /**
   * @return
   */
  public IPAddress getNetMask() {
    return netMask;
  }

  @Override
  public String toString() {
    return asString;
  }

  /**
   * Ist die IP im Subnet enthalten
   * @param ip
   * @return
   */
  public boolean contains(IPAddress ip) {
    long ipl = ip.toLong();
    //Test, ob Netzwerkteile der IP subnet und ip übereinstimmen, 
    //der Geräteteil der IP wird mit &netMaskL entfernt
    return (ipl&netMask.toLong()) == ipSubnet.toLong();
  }
  
  /**
   * Liefert die erste IP-Adresse im angebenen SubNet
   * @param ipAddress
   * @return
   */
  public IPAddress getFirstAddress() {
    int[] parts = ipSubnet.toInts();
    parts[3] += 1; //erste Adresse, eines über der SubNetz-Adresse
    return IPAddress.parse( parts );
  }

}
