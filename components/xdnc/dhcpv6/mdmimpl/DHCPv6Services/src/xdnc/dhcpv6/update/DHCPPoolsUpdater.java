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
package xdnc.dhcpv6.update;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.zip.CRC32;

import org.apache.log4j.Logger;

import xdnc.dhcpv6.DHCPv6ODS;
import xdnc.dhcpv6.parsing.Option;
import xdnc.dhcpv6.parsing.Range;
import xdnc.dhcpv6.parsing.SharedNetwork;
import xdnc.dhcpv6.parsing.Subnet;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdnc.dhcpv6.db.storables.SuperPool;
import com.gip.xyna.xdnc.dhcpv6.ipv6.utils.IPv6SubnetUtil;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

public class DHCPPoolsUpdater {

  private static final Logger logger = CentralFactoryLogging.getLogger(DHCPPoolsUpdater.class);
  
  private ODS ods;
//  private Map<String, OrderedVirtualObjectPool<IP, Lease>> virtualPoolsCacheReference; //REFACTOR_VIRTUALPOOL
  private long lastfilemodification;
  String clusternode;


  public DHCPPoolsUpdater(ODS ods,long lastfilemodification, String clusternode) {
    this.ods = ods;
    //REFACTOR_VIRTUALPOOL
    //this.virtualPoolsCacheReference = virtualPoolsCache;
    this.lastfilemodification = lastfilemodification;
    this.clusternode = clusternode;
  }


  /**
   * �bernimmt alle daten zu ip ranges in die virtual pools. updated ausserdem die
   * ippoolstorables und options. entfernt nicht mehr ben�tigte pools aus persistence und aus virtualpoolcache. stellt
   * am ende sicher, dass alle virtualpool infos auch in HISTORY persistiert wurden (unabh�ngig von der normalerweise
   * aktiven {@link PoolPersistenceStrategy}.<p>
   * methode kann gefahrlos mehrfach aufgerufen werden.
   */
  public void updateData(List<SharedNetwork> sharedNetworks) throws PersistenceLayerException {
    //TODO fehlerbehandlung: was soll passieren, wenn beim update der virtual pools ein fehler passiert? 
    //     pools trotzdem updaten? oder versuch, virtual pools zur�ckzusetzen?

    

    
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Starting to update pooldata in Superpooltable ...");
    
    //kopie des caches, in dem man neue virtual pools anlegen kann.
    //Map<String, OrderedVirtualObjectPool<IP, Lease>> ownCacheCopy = new HashMap<String, OrderedVirtualObjectPool<IP, Lease>>(virtualPoolsCacheReference);  //REFACTOR_VIRTUALPOOL
    ODSConnection con = ods.openConnection();
    try
    {
      
  
      
      //Superpool
      //1. Auslesen
      Collection<? extends SuperPool> superPoolListInODS= DHCPv6ODS.queryODS(con, "SELECT * from "+SuperPool.TABLENAME+ " for update", new Parameter(), new SuperPool().getReader(),-1);
      Collection<? extends SuperPool> superPoolListInCfg=computeSuperPoolCollection(sharedNetworks);
      
      con.persistCollection(minus(superPoolListInCfg,superPoolListInODS));
      con.commit();
      con.persistCollection(differ(superPoolListInODS,superPoolListInCfg));
      con.commit();
      
      //con.persistCollection(clearRanges(minus(superPoolListInODS,superPoolListInCfg)));
      con.delete(minus(superPoolListInODS,superPoolListInCfg));
          
      con.commit();

    
    } finally {
      con.closeConnection();
    }
    logger.info("(xdnc.dhcpv6.DeploymentWorkflow)Finished updating pooldata in Superpooltable!");
  }


  

  //Alle Werte aus colB, deren Schl�ssel auch in  colA enthalten ist */
  static Collection<? extends SuperPool>  differ(Collection<? extends SuperPool> colA,Collection<? extends SuperPool> colB) {
    ArrayList<SuperPool>  ret=new ArrayList<SuperPool> ();
    for(SuperPool spA:colA){
      for(SuperPool spB:colB){
        if (spA.equalsKey(spB)&& spA.compareTo(spB)!=0){
          spB.setSuperpoolID(spA.getSuperpoolID());
          spB.setChecksum(spA.getChecksum());
          spB.setStartm(spA.getStartm());
          spB.setEnds(spA.getEnds());
          spB.setLeasecount(spA.getLeasecount());
          ret.add(spB);
        }
      }  
    }
    return ret;            
  }
  
  static Collection<? extends SuperPool>  minus(Collection<? extends SuperPool> colA,Collection<? extends SuperPool> colB) {
    ArrayList<SuperPool>  ret=new ArrayList<SuperPool> ();
    for(SuperPool sp:colA){
      if(!(in(sp,colB))){
        ret.add(sp);
      }
    }
    return ret;    
  }
  
  

  private static boolean in(SuperPool sp, Collection<? extends SuperPool> colB) {
    for(SuperPool sp2:colB){
      if(sp.equalsKey(sp2)){
        return true;
      }
    }
    return false;
  }


  private Collection <? extends SuperPool> computeSuperPoolCollection(List<SharedNetwork> sharedNetworks) {
    HashMap<String,SuperPool> ht=new  HashMap<String,SuperPool> ();
    for (SharedNetwork sharedN:sharedNetworks){
      //for(String cmtsip:sharedN.getLinkAddresses().split(",")){
        for(Subnet subn:sharedN.getSubnets()){
          for(Range r:subn.getRanges()){
            String key=r.getPooltype()+";"+sharedN.getLinkAddresses()+";"+r.getPrefixlength();
            if (!ht.containsKey(key)){
              SuperPool sp=new SuperPool();
              sp.setCmtsip(sharedN.getLinkAddresses());
              sp.setPooltype(r.getPooltype());
              sp.setChecksum("");
              sp.setRanges("");
              sp.setPrefixlength(r.getPrefixlength());
              ht.put(key, sp);
            }
            SuperPool sp=ht.get(key);
            sp.setRanges(sp.getRanges()+r.getIpStart()+"-"+r.getIpEnd()+",");
            sp.setCfgtimestamp(lastfilemodification);
            sp.setClusternode(clusternode);
            
            String attributes="";
            String fixedattributes="";

            List<Option> options = subn.getOptions();
            for(Option o:options)
            {
              if(o.getOptionNumber().equals("fixedattributes"))
              {
                fixedattributes=o.getOptionValue();
              }
              if(o.getOptionNumber().equals("attributes"))
              {
                attributes=o.getOptionValue();
              }
            }
            List<Option> rangeoptions = r.getOptions();
            for(Option o:rangeoptions)
            {
              if(o.getOptionNumber().equals("fixedattributes"))
              {
                fixedattributes=o.getOptionValue();
              }
              if(o.getOptionNumber().equals("attributes"))
              {
                attributes=o.getOptionValue();
              }
            }
            
            IPv6SubnetUtil parsedSubnet = IPv6SubnetUtil.parse(subn.getAddress()+"/"+subn.getPrefixlength());
            String subnetstart = parsedSubnet.calculateFirstIP().toString();
            String subnetend = parsedSubnet.calculateLastIP().toString();
            String subnetcontent = "<"+subnetstart+"-"+subnetend+"#"+fixedattributes+"#"+attributes+">,";
            
            if(sp.getSubnets()!=null && sp.getSubnets().length()>0)
            {
              sp.setSubnets(sp.getSubnets()+subnetcontent);
            }
            else
            {
              sp.setSubnets(subnetcontent);
            }


            
          }
        }
      //}        
    }
    
    Collection <SuperPool> superPoolListInCfg=ht.values();
    for(SuperPool sp:superPoolListInCfg){
      String[] rangesArray=sp.getRanges().split(",");
      Arrays.sort(rangesArray);
      String normalizedRanges="";
      for (String csPart:rangesArray){
        normalizedRanges=normalizedRanges+csPart.trim()+",";          
      }
      sp.setRanges(normalizedRanges);                
      
      String keyForCRC = sp.getCmtsip()+sp.getPooltype()+sp.getPrefixlength();
      try{
        CRC32 crc32=new java.util.zip.CRC32();
        crc32.update(keyForCRC.getBytes());
        sp.setSuperpoolID(crc32.getValue());
      }catch(Exception e){
        logger.error("Calculation of SuperPoolID as CRC failed! ("+keyForCRC+")"); 
      
      }
      //sp.setSuperpoolID(idGenerator.getUniqueId());
    }
    return superPoolListInCfg;
  }

  
} //end class
