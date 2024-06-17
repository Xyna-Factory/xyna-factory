/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.locator;



import java.util.TimeZone;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import xmcp.xypilot.impl.gen.template.TemplateConfiguration;
import xmcp.xypilot.impl.gen.template.preprocess.ApplyPrefix;



public class ConfigurationLocator {

  public static final String RESOURCE_PACKAGE_PATH = "/res";
  public static final String TEMPLATE_PACKAGE_PATH = RESOURCE_PACKAGE_PATH + "/templates";
  public static final String PIPELINE_PACKAGE_PATH = RESOURCE_PACKAGE_PATH + "/pipelines";
  
  public static Configuration getTemplateConfiguration() {
    // Create your Configuration instance, and specify if up to what FreeMarker
    // version (here 2.3.32) do you want to apply the fixes that are not 100%
    // backward-compatible. See the Configuration JavaDoc for details.

    // Also specify the source where the template files come from.
    // Here templates are packaged with the application and are retrieved from the classpath.
    ClassTemplateLoader ctl = new ClassTemplateLoader(ConfigurationLocator.class, TEMPLATE_PACKAGE_PATH);
    Configuration cfg = TemplateConfiguration.getBaseConfiguration(Configuration.VERSION_2_3_32, ctl, new ApplyPrefix());

    // Set the preferred charset template files are stored in. UTF-8 is
    // a good choice in most applications:
    cfg.setDefaultEncoding("UTF-8");

    // Sets how errors will appear.
    // During web page *development* TemplateExceptionHandler.HTML_DEBUG_HANDLER is better.
    cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

    // Don't log exceptions inside FreeMarker that it will thrown at you anyway:
    cfg.setLogTemplateExceptions(false);

    // Wrap unchecked exceptions thrown during template processing into TemplateException-s:
    cfg.setWrapUncheckedExceptions(true);

    // Do not fall back to higher scopes when reading a null loop variable:
    cfg.setFallbackOnNullLoopVariable(false);

    // To accomodate to how JDBC returns values; see Javadoc!
    cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

    return cfg;
  }
}
