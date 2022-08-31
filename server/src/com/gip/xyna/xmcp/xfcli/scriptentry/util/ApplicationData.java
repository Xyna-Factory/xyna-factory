package com.gip.xyna.xmcp.xfcli.scriptentry.util;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry.XMOMXmlEntry;


public class ApplicationData {
  private Set<String> xmomEntries;
  private ApplicationXmlEntry applicationXmlEntry;
  
  
  public Set<String> getXmomEntries() {
    return xmomEntries;
  }
  
  public void setXmomEntries(Set<String> xmomEntries) {
    this.xmomEntries = xmomEntries;
  }

  
  public ApplicationXmlEntry getApplicationXmlEntry() {
    return applicationXmlEntry;
  }

  
  public void setApplicationXmlEntry(ApplicationXmlEntry applicationXmlEntry) {
    this.applicationXmlEntry = applicationXmlEntry;
  }
  
  public static ApplicationData collectXmomEntries(File appFile) throws Exception{
    Set<String> xmomEntries = new HashSet<String>();
    
    SAXParserFactory factory = SAXParserFactory.newInstance();
    SAXParser saxParser = factory.newSAXParser();
    ApplicationXmlHandler handler = new ApplicationXmlHandler();
    saxParser.parse(appFile, handler);
    ApplicationXmlEntry applicationXml = handler.getApplicationXmlEntry();
    List<XMOMXmlEntry> entries = applicationXml.getXmomEntries();
    
    entries.stream().forEach(x -> xmomEntries.add(x.getFqName()));
    
    ApplicationData result = new ApplicationData();
    result.setApplicationXmlEntry(applicationXml);
    result.setXmomEntries(xmomEntries);
    return result;
  }
}