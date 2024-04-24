package xmcp.xypilot.impl.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.StringXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;

public class DOMUtils {

    public static final String resourceDirectory = "bin/xmcp/xypilot/res";

    public static StringXMLSource createXMLSourceFromFile(String directory, String fqn) throws Ex_FileWriteException {
        String xml = FileUtils.readFileAsString(new File(directory, fqn + ".xml"));
        Map<String, String> xmlSource = new HashMap<>();
        xmlSource.put(fqn, xml);
        return new StringXMLSource(xmlSource);
    }

    public static StringXMLSource createXMLSourceFromResourceDirectory(String fqn) throws Ex_FileWriteException {
        return createXMLSourceFromFile(resourceDirectory, fqn);
    }

    public static GenerationBase loadFromResourceDirectory(String fqn) throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
        StringXMLSource xmlSource = createXMLSourceFromResourceDirectory(fqn);
        GenerationBase gb = GenerationBase.getOrCreateInstance(fqn, new GenerationBaseCache(), -1l, xmlSource);
        gb.parseGeneration(false, false, false);
        return gb;
    }

    public static DOM loadDOMFromResourceDirectory(String fqn) throws Ex_FileWriteException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        GenerationBase gb = loadFromResourceDirectory(fqn);

        if (gb instanceof DOM)
            return (DOM) gb;
        else
            throw new ClassCastException(fqn + " is not a DOM");
    }

    public static DomOrExceptionGenerationBase loadDomOrExceptionFromResourceDirectory(String fqn) throws Ex_FileWriteException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        GenerationBase gb = loadFromResourceDirectory(fqn);

        if (gb instanceof DomOrExceptionGenerationBase)
            return (DomOrExceptionGenerationBase) gb;
        else
            throw new ClassCastException(fqn + " is not a DomOrExceptionGenerationBase");
    }

    public static ExceptionGeneration loadExceptionFromResourceDirectory(String fqn) throws Ex_FileWriteException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        GenerationBase gb = loadFromResourceDirectory(fqn);

        if (gb instanceof ExceptionGeneration)
            return (ExceptionGeneration) gb;
        else
            throw new ClassCastException(fqn + " is not an ExceptionGeneration");
    }

    public static WF loadWFFromResourceDirectory(String fqn) throws Ex_FileWriteException, XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException {
        GenerationBase gb = loadFromResourceDirectory(fqn);

        if (gb instanceof WF)
            return (WF) gb;
        else
            throw new ClassCastException(fqn + " is not a WF");
    }
}
