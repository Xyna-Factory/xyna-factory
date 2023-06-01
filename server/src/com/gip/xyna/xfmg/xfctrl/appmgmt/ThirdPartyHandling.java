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
package com.gip.xyna.xfmg.xfctrl.appmgmt;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;


public class ThirdPartyHandling {

  
  private static Logger logger = CentralFactoryLogging.getLogger(ThirdPartyHandling.class);
  
  private static class Licenses {

    private String prefix;

    
    public Licenses(String simpleJarFileName) {
      if( simpleJarFileName.toLowerCase().endsWith(".jar") ) {
        prefix = simpleJarFileName.substring(0, simpleJarFileName.length()-4 );
      } else {
        throw new IllegalStateException("Unexpected jar name "+ simpleJarFileName);
      }
    }


    public Set<String> allLicensesIn(String dir) {
      Set<String> licences = new HashSet<String>();   //gesammelte Lizenzen
      Set<String> additional = new HashSet<String>(); //weitere Dateien
      Set<String> groups = new HashSet<String>();     //temporär gesammelte Lizenz-Gruppen
      for( String f : new File(dir).list() ) {
        if( f.startsWith(prefix) && ! f.toLowerCase().endsWith(".jar") ) {
          
          licences.add(f);
          if( f.substring( prefix.length()).contains("GROUP") ) {
            groups.add(f);
          }
        } else {
          additional.add(f);
        }
      }
      
      while( ! groups.isEmpty() ) {
        String group = groups.iterator().next();
        List<String> files = parseGroup(dir, group);
        for( String f : files ) {
          if( additional.remove(f) ) { //Datei ist vorhanden, Entfernen, damit nicht doppelt verarbeitet
            licences.add(f);
            if( f.contains("GROUP") ) {
              groups.add(f);
            }
          } else {
            if( ! licences.contains(f) ) {
              logger.warn("File "+f+" referenced from "+group+" not found");
            }
          }
        }
        groups.remove(group);
      }
      
      return licences;
    }


    private List<String> parseGroup(String dir, String group) {
      File groupFile = new File(dir, group);
      List<String> files = new ArrayList<String>();
      try {
        String groupContent = FileUtils.readFileAsString(groupFile);
        String[] lines = groupContent.split("\n");
        for( String line : lines ) {
          if( line.startsWith("#") ) continue;
          String trimmed = line.trim();
          if( trimmed.length() == 0 ) continue;
          files.add( trimmed );
        }
      } catch (Ex_FileWriteException e) {
        // unerwartet TODO Exception oder ignorieren?
      }
      return files;
    }
    
  }

  public static void copyThirdPartiesDir(File applicationDir, PrintStream statusOutputStream) {
    File thirdPartiesApp = new File(applicationDir, "third_parties");
    if( thirdPartiesApp.exists() ) {
      File thirdParties = new File("..", "third_parties");
      try {
        LicenceFilter licenceFilter = new LicenceFilter();
        //Lizenzen ermitteln
        List<File> licences = new ArrayList<File>();
        FileUtils.findFilesRecursively(thirdPartiesApp, licences, licenceFilter.learn() );
        //zugehörige JARs suchen
        List<File> jars = new ArrayList<File>();
        FileUtils.findFilesRecursively(applicationDir, jars, licenceFilter.jars() );
        
        for( File f : licences ) {
          FileUtils.copyFileToDir( f, thirdParties );
        }
        for( File f : jars ) {
          FileUtils.copyFileToDir( f, thirdParties );
        }
        
        ApplicationManagementImpl.output(statusOutputStream, "Copied "+licences.size()+" third party licences and "+jars.size()+" jars to "+thirdParties.getAbsolutePath() );
      
      } catch (Ex_FileAccessException e) {
        logger.warn("Could not copy third_parties "+thirdPartiesApp.getAbsolutePath() +" to "+thirdParties.getAbsolutePath(), e );
        ApplicationManagementImpl.output(statusOutputStream, "Failed to copy third party licences to "+thirdParties.getAbsolutePath() );
      }
    }
  }
  

  public static List<Pair<String, String>> copyLicensesForJar(String fromDir, String simpleJarFileName,
                                  String toDir, long revisionTo) {
    
    List<Pair<String, String>> copies = new ArrayList<Pair<String, String>>();
    String third_parties = RevisionManagement.getPathForRevision(PathType.THIRD_PARTIES, revisionTo);
    
    Licenses l = new Licenses(simpleJarFileName);
    for( String tp : l.allLicensesIn(fromDir) ) {
      
      copies.add( Pair.of( fromDir  + Constants.fileSeparator + tp, 
                           toDir + Constants.fileSeparator + tp ) );
      
      copies.add( Pair.of( fromDir  + Constants.fileSeparator + tp, 
                           third_parties  + Constants.fileSeparator + tp ) );
    }
    return copies;
  }

  public static List<Pair<String, String>> copyLicensesForDir(String fromDir, long revisionTo) {
    String third_parties = RevisionManagement.getPathForRevision(PathType.THIRD_PARTIES, revisionTo);
    List<Pair<String, String>> copies = new ArrayList<Pair<String, String>>();
    File from = new File(fromDir);
    for( String jar : from.list(new JarFilter()) ) {
      Licenses l = new Licenses(jar);
      for( String tp : l.allLicensesIn(fromDir) ) {
        copies.add( Pair.of( fromDir  + Constants.fileSeparator + tp, 
                             third_parties  + Constants.fileSeparator + tp ) );
      }
    }
    return copies;
  }
  
  private static class JarFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
      File f = new File(dir, name);
      if (f.isDirectory()) {
        return true;
      }
      return name.toLowerCase().endsWith(".jar");
    }
  }
  
  private static class LicenceFilter {
    static List<String> licenceParts = Arrays.asList("NOTICE", "LICENCE", "LICENSE", "GROUP",  "COPYRIGHT");

    private HashSet<String> baseNames = new HashSet<String>();
    public FilenameFilter learn() {
      return new LicenceFileFilter();
    }
    
    public FilenameFilter jars() {
      return new JarFileFilter();
    }

    private boolean isLicence(String name) {
      for( String lp : licenceParts ) {
        int idx = name.indexOf(lp);
        if( idx > 0 ) {
          String n = name.substring(0,idx);
          if( n.endsWith("-") ) {
            n = name.substring(0,idx-1);
          }
          baseNames.add(n.toLowerCase());
          return true;
        }
      }
      return false;
    }
    
    private boolean isJar(String name) {
      String lower = name.toLowerCase();
      if( ! lower.endsWith(".jar") ) {
        return false;
      }
      String n = lower.substring(0, lower.length()-4 );
      if( baseNames.contains(n) ) {
        return true;
      }
      return false;
    }

    private class LicenceFileFilter implements FilenameFilter {
      public boolean accept(File dir, String name) {
        File f = new File(dir, name);
        if (f.isDirectory()) {
          return true;
        }
        return isLicence(name);
      }
    }
    
    private class JarFileFilter implements FilenameFilter {
      public boolean accept(File dir, String name) {
        File f = new File(dir, name);
        if (f.isDirectory()) {
          return true;
        }
        return isJar(name);
      }

    }
    
    

  }
  
}
