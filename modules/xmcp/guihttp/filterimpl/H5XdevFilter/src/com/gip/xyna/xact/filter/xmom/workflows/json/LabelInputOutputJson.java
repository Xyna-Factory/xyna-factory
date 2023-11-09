package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.JsonStringVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public class LabelInputOutputJson extends XMOMGuiJson {

  private String label;
  private List<String> inputStrings = new ArrayList<String>();
  private List<String> outputStrings = new ArrayList<String>();
  private List<VariableJson> inputJson = new ArrayList<VariableJson>();
  private List<VariableJson> outputJson = new ArrayList<VariableJson>();

  public LabelInputOutputJson() {}

  public String getLabel() {
    return label;
  }
  
  public List<VariableJson> getInputJson() {
    return inputJson;
  }

  public List<VariableJson> getOutputJson() {
    return outputJson;
  }

  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Label: ");
    builder.append(label);
    builder.append("\n");
    builder.append("input: ");
    builder.append(inputJson.stream().map(json->json.getLabel()).collect(Collectors.joining(", ")));
    builder.append("\n");
    builder.append("output: ");
    builder.append(outputJson.stream().map(json->json.getLabel()).collect(Collectors.joining(", ")));
    return builder.toString();
  }
  
  public static JsonVisitor<LabelInputOutputJson> getJsonVisitor() {
    return new LabelInputOutputJsonVisitor();
  }
  
  public void parseInputOutput() throws InvalidJSONException, UnexpectedJSONContentException {
    inputJson = parse(inputStrings);
    outputJson = parse(outputStrings);
  }
  
  private List<VariableJson> parse(List<String> inputOrOutput) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    List<VariableJson> ret = new ArrayList<>();
    for (String value: inputOrOutput) {
      ret.add(jp.parse(value, new VariableJson.VariableJsonVisitor()));
    }
    return ret;
  }

  private static class LabelInputOutputJsonVisitor extends EmptyJsonVisitor<LabelInputOutputJson> {
    LabelInputOutputJson lioj = new LabelInputOutputJson();

    @Override
    public LabelInputOutputJson get() {
      return lioj;
    }
    @Override
    public LabelInputOutputJson getAndReset() {
      LabelInputOutputJson ret = lioj;
      lioj = new LabelInputOutputJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals("label") ) {
        lioj.label = value;
        return;
      }
    }
    
    @Override
    public JsonVisitor<?> objectStarts(String label) {
      return new JsonStringVisitor();
    }
    
    @Override
    public void emptyList(String label) throws UnexpectedJSONContentException {
      if( label.equals("input") ) {
        lioj.inputStrings = new ArrayList<>();
        return;
      } else if( label.equals("output") ) {
        lioj.outputStrings = new ArrayList<>();
        return;
      }
    }
    
    @Override
    public void objectList(String label, List<Object> values) {
      if( label.equals("input") ) {
        lioj.inputStrings = values.stream().
            map(obj -> (String)obj).
            collect(java.util.stream.Collectors.toList());
        return;
      } else if( label.equals("output") ) {
        lioj.outputStrings = values.stream().
            map(obj -> (String)obj).
            collect(java.util.stream.Collectors.toList());
        return;
      }
    }

  }
}
