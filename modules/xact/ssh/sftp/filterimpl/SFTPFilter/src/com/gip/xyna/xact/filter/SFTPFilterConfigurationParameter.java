package com.gip.xyna.xact.filter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;

public class SFTPFilterConfigurationParameter extends FilterConfigurationParameter {
    public final static String PROCESS_SFTP_ORDERTYPE = "xact.sftp.ProcessSFTPRequest";

    public static final StringParameter<String> FILTER_PREFIX = StringParameter.typeString("filterPrefix")
            .documentation(
                    Documentation.de("Präfix für Zuständigkeit des Filters").en("Prefix for this filter").build())
            .optional()
            .build();

    public static final StringParameter<String> ORDERTYPE = StringParameter.typeString("ordertype")
            .documentation(
                    Documentation.de("Ordertype für den Filter").en("Ordertype to run.").build())
            .defaultValue(PROCESS_SFTP_ORDERTYPE)
            .build();

    public static final List<StringParameter<?>> ALL_PARAMS = Arrays.<StringParameter<?>>asList(
            FILTER_PREFIX, ORDERTYPE);

    private Optional<String> filterPrefix = Optional.empty();
    private String ordertype = PROCESS_SFTP_ORDERTYPE;

    public Optional<String> getFilterPrefix() {
        return filterPrefix.map(f -> f.endsWith("/") ? f : f + "/");
    }

    public String getOrdertype() {
        return ordertype;
    }

    public FilterConfigurationParameter build(Map<String, Object> paramMap)
            throws XACT_InvalidFilterConfigurationParameterValueException {
        SFTPFilterConfigurationParameter result = new SFTPFilterConfigurationParameter();

        filterPrefix = Optional.ofNullable(FILTER_PREFIX.getFromMap(paramMap));
        ordertype = ORDERTYPE.getFromMap(paramMap);

        return result;
    }

    public List<StringParameter<?>> getAllStringParameters() {
        return ALL_PARAMS;
    }

}
