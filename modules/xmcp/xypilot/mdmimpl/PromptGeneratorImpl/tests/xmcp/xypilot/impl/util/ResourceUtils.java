package xmcp.xypilot.impl.util;

import java.io.File;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;

public class ResourceUtils {
    public static final String resourceDirectory = "bin/xmcp/xypilot/res";

    public static String readFileFromResourceDirectory(String file) throws Ex_FileWriteException {
        return FileUtils.readFileAsString(new File(resourceDirectory, file));
    }
}
