package com.atsumeru.web.configuration;

import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.GUType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ServerConfig {
    public static final List<String> ROLES = Arrays.asList("ADMIN", "USER");
    public static final List<String> AUTHORITIES = Arrays.asList("IMPORTER", "UPLOADER", "METADATA_UPDATER", "DOWNLOAD_FILES");

    @Autowired
    private ApplicationArguments applicationArguments;

    private boolean getArgsBooleanValue(String optionName, boolean def) {
        List<String> args = applicationArguments.getOptionValues(optionName);
        return GUArray.isEmpty(args) || GUType.getBoolDef(args.get(0), def);
    }
}
