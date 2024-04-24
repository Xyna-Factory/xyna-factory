package xmcp.xypilot.impl.gen.openai;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;

import xmcp.xypilot.impl.openai.CompletionBody;
import xmcp.xypilot.impl.util.ResourceUtils;

public class CompletionBodyTest {

    @Test
    public void toJSONTest() throws Ex_FileWriteException {
        CompletionBody completionBody = new CompletionBody();
        completionBody.model = "copilot-java";
        completionBody.n = Optional.of(1);
        completionBody.stop = Optional.of(List.of("\n */"));
        completionBody.prompt = "System.out.println(\"hello";
        completionBody.suffix = " world\");";
        String json = completionBody.toJSON();
        FileUtils.writeStringToFile(json, new File(ResourceUtils.resourceDirectory, "completion_body.json"));
    }

}
