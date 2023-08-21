package com.atsumeru.web.configuration;

import com.atsumeru.web.util.ArrayUtils;
import com.atsumeru.web.util.TypeUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ServerConfig {
    public static final List<String> ROLES = Arrays.asList("ADMIN", "USER");
    public static final List<String> AUTHORITIES = Arrays.asList("IMPORTER", "UPLOADER", "METADATA_UPDATER", "DOWNLOAD_FILES");

    private final ApplicationArguments applicationArguments;

    public ServerConfig(ApplicationArguments applicationArguments) {
        this.applicationArguments = applicationArguments;
    }

    private boolean getArgsBooleanValue(String optionName, boolean def) {
        List<String> args = applicationArguments.getOptionValues(optionName);
        return ArrayUtils.isEmpty(args) || TypeUtils.getBoolDef(args.get(0), def);
    }
}
