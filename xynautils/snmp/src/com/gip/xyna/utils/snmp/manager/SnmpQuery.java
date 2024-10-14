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
package com.gip.xyna.utils.snmp.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.utils.snmp.exception.UnexpectedResponseDataException;
import com.gip.xyna.utils.snmp.varbind.VarBind;

/**
 * SnmpQuery searches via Walks with several search criteria and gives the found Varbinds back in several formats.
 * 
 * Examples:
 * List vbs = SnmpQuery.walk().snmpContext(snmpContext).oid(oid).caller(caller).forAll().returnVarBind();
 * List oids = SnmpQuery.walk().snmpContext(snmpContext).oid(oid).caller(caller).forAll().returnOidSubstring(0);
 * String ois = SnmpQuery.searchValue("value").snmpContext(snmpContext).oid(oid).caller(caller).forFirst().returnOidSubstring(0);
 *
 */
public class SnmpQuery {

  /**
   * convenience method for a simple walk instead of 
   * SnmpQuery.walk().snmpContext(snmpContext).oid(oid).caller(caller).forAll().returnVarBind(); 
   * @param snmpContext
   * @param oid
   * @param caller
   * @return
   */
  public static List<VarBind> walk(SnmpContext snmpContext, String oid, String caller) {
    ArrayList<VarBind> response = new ArrayList<VarBind>();
    for( SnmpWalker w = new SnmpWalker(snmpContext,oid,caller); w.hasNext(); ) {
      response.add( w.next() );
    }
    return response;
  }




  /**
   * walk: no search criterion
   * @return
   */
  public static WalkData walk() {
    WalkData wd = new WalkData();
    wd.filter = Filter.filterNone();
    return wd;
  }

  /**
   * search criterion: value is in the returned Varbinds
   * @param value
   * @return
   */
  public static WalkData searchValue(int value) {
    WalkData wd = new WalkData();
    wd.filter = Filter.searchValue(value);
    return wd;
  }
  /**
   * search criterion: value is in the returned Varbinds
   * @param value
   * @return
   */
  public static WalkData searchValue(String value) {
    WalkData wd = new WalkData();
    wd.filter = Filter.searchValue(value);
    return wd;
  }


  /**
   * search criterion: oidIndex ist part of the oid at the specified position
   * Example searchOid(3,17) finds oid .1.2.17
   * @param value
   * @return
   */
  public static WalkData searchOid( int pos, String oidIndex ) {
    WalkData wd = new WalkData();
    wd.filter = Filter.searchOid(pos,oidIndex);
    return wd;
  }

  private static abstract class Filter {
    /**
     * check, wether vb is valid output for the given search criterion
     * @param vb
     * @return
     */
    public abstract boolean isValidOutput(VarBind vb);

    private static Filter searchOid(final int pos, final String oidIndex) {
      return new Filter(){
        Pattern oidSplitter = Pattern.compile("\\.");
        @Override
        public boolean isValidOutput(VarBind vb) {
          String[] parts = oidSplitter.split(vb.getObjectIdentifier());
          if (pos >= parts.length) {
            throw new UnexpectedResponseDataException("Expected <" + vb.getObjectIdentifier()
                + "> to contain at least " + (pos + 1) + " parts.");
          }
          return parts[pos].equals(oidIndex);
        }
      };
    }

    private static Filter searchValue(final int value) {
      final Integer fi = Integer.valueOf(value);
      return new Filter(){
        @Override
        public boolean isValidOutput(VarBind vb) {
          return fi.equals( vb.getValue() );
        }
      };
    }

    private static Filter searchValue(final String value) {
      return new Filter(){
        @Override
        public boolean isValidOutput(VarBind vb) {
          return value.equals( vb.getValue() );
        }
      };
    }
    private static Filter filterNone() {
      return new Filter(){
        @Override
        public boolean isValidOutput(VarBind vb) {
          return true;
        }
      };
    }

  }



  public static class WalkData {
    public Filter filter;
    private Pattern oidSplitter;
    private String caller;
    private String oid;
    private SnmpContext snmpContext;


    /**
     * sets the oid
     * @param oid
     * @return
     */
    public WalkData oid(@SuppressWarnings("hiding") String oid) {
      this.oid = oid;
      return this;
    }

    /**
     * sets the caller (for logging purpose)
     * @param caller
     * @return
     */
    public WalkData caller(@SuppressWarnings("hiding") String caller) {
      this.caller = caller;
      return this;
    }

    /**
     * sets the snmpContext
     * @param snmpContext
     * @return
     */
    public WalkData snmpContext(@SuppressWarnings("hiding") SnmpContext snmpContext) {
      this.snmpContext = snmpContext;
      return this;
    }

    /**
     * only the first found data is required
     * @return
     */
    public ForFirst forFirst() {
      return new ForFirst( this );
    }

    /**
     * all found data is required
     * @return
     */
    public ForAll forAll() {
      return new ForAll( this );
    }



    /**
     * 
     * @param vb
     * @param start
     * @return substring of the oid
     */
    public String returnOidSubstring(VarBind vb, int start) {
      return vb.getObjectIdentifier().substring(start);
    }

    /**
     * @param vb
     * @param pos
     * @return Oid-Index at position pos
     */
    public int returnIndex(VarBind vb, int pos) {
      if( oidSplitter == null ) {
        oidSplitter = Pattern.compile("\\.");
      }
      String[] parts = oidSplitter.split(vb.getObjectIdentifier());
      if (parts.length <= pos) {
        throw new UnexpectedResponseDataException("Expected OID <" + vb.getObjectIdentifier()
            + "> to contain at least " + (pos + 1) + " elements.");
      }
      return Integer.parseInt( parts[pos] );
    }

    /**
     * Filters if given vb is a valid output
     * @param vb
     * @return
     */
    public boolean filter(VarBind vb) {
      return filter.isValidOutput(vb);
    }

  }


  /**
   * Handles searches with only one result
   */
  public static class ForFirst {
    WalkData wd;
    private ForFirst(WalkData WalkData) {
      wd = WalkData;
    }
    /**
     * @return
     */
    /**
     * @return first found varBind
     * @throw SnmpQueryException when no data is found
     */
    public VarBind returnVarBind() {
      VarBind vb = null;
      for( SnmpWalker w = new SnmpWalker(wd.snmpContext,wd.oid,wd.caller); w.hasNext(); ) {
        vb = w.next();
        if( wd.filter(vb)) {
          return vb; //found value
        }
      }
      throw new SnmpQueryException(SnmpQueryException.NO_DATA_FOUND);
    }
    /**
     * @param start
     * @return substring of the oid of the first found varBind
     */
    public String returnOidSubstring(int start) {
      return wd.returnOidSubstring(returnVarBind(),start);
    }
    /**
     * @param pos
     * @return Oid-Index at position pos
     */
    public int returnIndex(int pos) {
      return wd.returnIndex(returnVarBind(),pos);
    }


  }

  /**
   * Handles searches with many results
   */
  public static class ForAll {
    WalkData wd;
    private ForAll(WalkData walkData) {
      wd = walkData;
    }
    /**
     * @return List of VarBinds
     */
    public List<VarBind> returnVarBind() {
      //logger.debug("walk: "+ caller);
      ArrayList<VarBind> response = new ArrayList<VarBind>();
      for( SnmpWalker w = new SnmpWalker(wd.snmpContext,wd.oid,wd.caller); w.hasNext(); ) {
        VarBind vb = w.next();
        if( wd.filter(vb)) {
          response.add( vb );
        }
      }
      return response;
    }
    /**
     * @param start
     * @return List of substring of the oids of the first found varBind
     */
    public List<String> returnOidSubstring(int start) {
      ArrayList<String> response = new ArrayList<String>();
      for( SnmpWalker w = new SnmpWalker(wd.snmpContext,wd.oid,wd.caller); w.hasNext(); ) {
        VarBind vb = w.next();
        if( wd.filter(vb)) {
          response.add( wd.returnOidSubstring(vb,start) );
        }
      }
      return response;
    }
    /**
     * @param pos
     * @return List of Oid-Index at position pos
     */
    public ArrayList<Integer> returnIndex(int pos) {
      ArrayList<Integer> response = new ArrayList<Integer>();
      for( SnmpWalker w = new SnmpWalker(wd.snmpContext,wd.oid,wd.caller); w.hasNext(); ) {
        VarBind vb = w.next();
        if( wd.filter(vb) ) {
          response.add( Integer.valueOf( wd.returnIndex(vb,pos) ) );
        }
      }
      return response;
    }
  }

  /**
   * SnmpQueryException when somethings goes wrong  (no data found)
   */
  public static class SnmpQueryException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public SnmpQueryException(String msg) {
      super(msg);
    }
    public static final String NO_DATA_FOUND = "no data found";
  }
}
