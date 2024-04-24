package xmcp.xypilot.impl.gen.template;

import java.io.File;
import java.io.IOException;
import java.util.TimeZone;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import xmcp.xypilot.impl.gen.template.preprocess.ApplyPrefix;
import xmcp.xypilot.impl.gen.template.preprocess.RemoveComments;

public class TestTemplateConfiguration {
    public static final String templateDirectory = "bin/xmcp/xypilot/res/templates";

    private Configuration cfg;

    public TestTemplateConfiguration() throws IOException {
        // Create your Configuration instance, and specify if up to what FreeMarker
        // version (here 2.3.32) do you want to apply the fixes that are not 100%
        // backward-compatible. See the Configuration JavaDoc for details.
        cfg = new Configuration(Configuration.VERSION_2_3_32);

        // Specify the source where the template files come from. Here I set a
        // plain directory for it, but non-file-system sources are possible too:
        cfg.setTemplateLoader(new CustomTemplateLoader(
            new FileTemplateLoader(new File(templateDirectory)),
            new RemoveComments(),
            new ApplyPrefix()
        ));

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

        // Add custom directives
        cfg.setSharedVariable("apply", new ApplyDirective());
    }

    public Configuration get() {
        return cfg;
    }
}
