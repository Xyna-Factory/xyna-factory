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
package com.gip.xyna.templateprovider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xact.templates.VelocityTemplatePart;

import com.gip.xyna.templateprovider.evaluation.Constraint;
import com.gip.xyna.templateprovider.evaluation.ConstraintsEvaluator;
import com.gip.xyna.templateprovider.evaluation.ConstraintsParser;



/**
 * Template part.
 */
public final class TemplatePart {

  private static final Pattern MACROPATTERN = Pattern.compile("^\\s*#"
      + TemplateGenerator.SUB_TEMPLATE_PART_NAME_PREFIX + "((?:[A-Za-z0-9_]+))\\(\\)\\s*$");
  private static final Pattern TRAILING_COMMENT_PATTERN = Pattern.compile(".*##[^\\n]*$", Pattern.DOTALL);

  private static final Pattern TEMPLATE_TYPE_PATTERN = Pattern.compile("[A-Z]([A-Z0-9_]{0,15})");
  private static final Pattern TEMPLATE_PART_PATTERN = Pattern.compile("[A-Za-z0-9_]{1,128}");

    private final long id;
    private final String templateType;
    private final String part;
    private final ConstraintsEvaluator constraintsEvaluator;
    private final String content;

    private final Set<String> referencedParts;

    public TemplatePart(final int id, final String templateType, final String partName,
            final ConstraintsEvaluator constraintsEvaluator, String content) {
        if (id < 0) {
            throw new IllegalArgumentException("Expected id to be larger or equal to 0, but was: <" + id + ">.");
        } else if (templateType == null) {
            throw new IllegalArgumentException("Template type may not be null.");
        } else if (!TEMPLATE_TYPE_PATTERN.matcher(templateType).matches()) {
            throw new IllegalArgumentException("Invalid template type name: <" + templateType + ">.");
        } else if (partName == null) {
            throw new IllegalArgumentException("Part name may not be null.");
        } else if (!TEMPLATE_PART_PATTERN.matcher(partName).matches()) {
            throw new IllegalArgumentException("Invalid part name: <" + partName + ">.");
        } else if (constraintsEvaluator == null) {
            throw new IllegalArgumentException("Constraints evaluator may not be null.");
        } else if (content == null) {
            throw new IllegalArgumentException("Content may not be null.");
        }
        this.id = id;
        this.templateType = templateType;
        this.part = partName;
        this.constraintsEvaluator = constraintsEvaluator;
        //bugz 9423: kommentare am ende von subtemplates ohne nachfolgenden zeilenumbruch: zeilenumbruch erzwingen!
        if (!TemplateGenerator.MAIN_TEMPLATE_PART_NAME.equals(partName)) {
          Matcher matcher = TRAILING_COMMENT_PATTERN.matcher(content);
          if (matcher.matches()) {
            content += "\n";
          }
        } 
        this.content = content;
        this.referencedParts = getReferencedPartsFromContent(content);
    }

    public TemplatePart(VelocityTemplatePart tp) {
      id=tp.getId();
      templateType=tp.getApplication()+"."+tp.getScope()+"."+tp.getType();
      part=tp.getPart();
      Set<Constraint> constraints=ConstraintsParser.parseToSetOfConstraints(tp.getConstraintSet());
      constraintsEvaluator=new ConstraintsEvaluator(constraints, tp.getScore());
      String newContent=tp.getContent();//bugz 9423: kommentare am ende von subtemplates ohne nachfolgenden zeilenumbruch: zeilenumbruch erzwingen!
      if (!TemplateGenerator.MAIN_TEMPLATE_PART_NAME.equals(part)) {
          Matcher matcher = TRAILING_COMMENT_PATTERN.matcher(newContent);
          if (matcher.matches()) {
            newContent += "\n";
          }
      }
      content=newContent;      
      referencedParts=getReferencedPartsFromContent(content);
    }

    private Set<String> getReferencedPartsFromContent(final String content) {
        Set<String> partNames = new HashSet<String>();
        String[] lines = content.split("\n");
        for (String line : lines) {
          Matcher matcher = MACROPATTERN.matcher(line);
          if (matcher.matches()) {
            String subTemplateName = matcher.group(1);
            if (TemplateGenerator.MAIN_TEMPLATE_PART_NAME.equals(subTemplateName)) {
              throw new IllegalArgumentException("Main template may not be referenced: <" + content + ">.");
            }
            partNames.add(subTemplateName);
          }
        }
        return Collections.unmodifiableSet(partNames);
    }

    public long getId() {
        return this.id;
    }

    public String getTemplateType() {
        return this.templateType;
    }

    public String getPartName() {
        return this.part;
    }

    public ConstraintsEvaluator getConstraintsEvaluator() {
        return this.constraintsEvaluator;
    }

    public String getContent() {
        return this.content;
    }

    public Set<String> getReferencedParts() {
        return this.referencedParts;
    }

    public String toString() {
        // TODO externalize strings
        StringBuilder sb = new StringBuilder();
        sb.append("TemplatePart:{id:<");
        sb.append(this.id);
        sb.append(">,templateType:<");
        sb.append(this.templateType);
        sb.append(">,partName:<");
        sb.append(this.part);
        sb.append(">,constraintsEvaluator:<");
        sb.append(this.constraintsEvaluator);
        sb.append(">,content:<");
        sb.append(this.content);
        sb.append(">}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + constraintsEvaluator.hashCode();
        result = prime * result + content.hashCode();
        result = prime * result + ((Long)id).hashCode();
        result = prime * result + part.hashCode();
        result = prime * result + referencedParts.hashCode();
        result = prime * result + templateType.hashCode();
        return result;
    }


  @Override
  public boolean equals(Object obj) {

    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }

    TemplatePart other = (TemplatePart) obj;
    if (id != other.id) {
      return false;
    }
    if (!templateType.equals(other.templateType)) {
      return false;
    }
    if (!part.equals(other.part)) {
      return false;
    }
    if (!constraintsEvaluator.equals(other.constraintsEvaluator)) {
      return false;
    }
    if (!content.equals(other.content)) {
      return false;
    }
    if (!referencedParts.equals(other.referencedParts)) {
      return false;
    }

    return true;

  }
}
