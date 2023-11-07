package com.gip.xyna.xact.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;

import xmcp.oas.datatype.URLPathParameter;

class PathParameter {

  private final HashMap<String, String> methodOrdertypeMapping = new HashMap<>();
  private String fqnOfInstanceDatatype;
  private Pattern pathRegEx;
  private String[] attributeNames;
  
  public PathParameter(String instanceDatatype, String path, String... attributes) {
    fqnOfInstanceDatatype = instanceDatatype;
    pathRegEx = Pattern.compile("^" + path + "$");
    attributeNames = attributes;
  }
  
  public void registerMethod(Method method, String ordertype) {
    methodOrdertypeMapping.put(method.toString().toUpperCase(), ordertype);
  }
  
  public String getOrdertype(String method) {
    return methodOrdertypeMapping.get(method.toUpperCase());
  }
  
  public boolean matchPath(String path) {
    Matcher matcher = pathRegEx.matcher(path);
    return matcher.find();
  }
  
  public List<URLPathParameter> getPathAttribute(String path) {
    ArrayList<URLPathParameter> ret = new ArrayList<>();
    Matcher matcher = pathRegEx.matcher(path);
    if (matcher.find()) {
      for (String attribute: attributeNames) {
        ret.add(new URLPathParameter.Builder().attribute(attribute).value(matcher.group(attribute)).instance());
      }
    }
    return ret;
  }
  
  public boolean hasPath(String path) {
    return pathRegEx.pattern().equals("^" + path + "$");
  }
  
  public String getInstanceDatatype() {
    return fqnOfInstanceDatatype;
  }
}
