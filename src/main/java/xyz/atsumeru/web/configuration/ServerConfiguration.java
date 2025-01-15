package xyz.atsumeru.web.configuration;

import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Configuration;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.TypeUtils;

import java.util.Arrays;
import java.util.List;

@Configuration
public class ServerConfiguration {
    public static final List<String> ROLES = Arrays.asList("ADMIN", "USER");
    public static final List<String> AUTHORITIES = Arrays.asList("IMPORTER", "UPLOADER", "METADATA_UPDATER", "DOWNLOAD_FILES");

    private final ApplicationArguments applicationArguments;

    public ServerConfiguration(ApplicationArguments applicationArguments) {
        this.applicationArguments = applicationArguments;
    }

    private boolean getArgsBooleanValue(String optionName, boolean def) {
        List<String> args = applicationArguments.getOptionValues(optionName);
        return ArrayUtils.isEmpty(args) || TypeUtils.getBoolDef(args.get(0), def);
    }
}
