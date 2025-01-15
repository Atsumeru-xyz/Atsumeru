package xyz.atsumeru.web.security.repository;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;
import xyz.atsumeru.web.exception.UserNotFoundException;
import xyz.atsumeru.web.helper.PasswordGenerator;
import xyz.atsumeru.web.model.database.AtsumeruUser;
import xyz.atsumeru.web.repository.dao.UsersDaoManager;
import xyz.atsumeru.web.util.FileUtils;
import xyz.atsumeru.web.util.StringUtils;

import java.io.Closeable;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Repository
public class UsersRepository implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(UsersRepository.class.getSimpleName());
    public static final String ADMIN_USERNAME = "Admin";

    private final ApplicationContext context;
    private final UsersDaoManager usersDaoManager;

    public UsersRepository(ApplicationContext context, UsersDaoManager usersDaoManager) {
        this.context = context;
        this.usersDaoManager = usersDaoManager;
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener(ApplicationStartedEvent.class)
    public void doAfterStart() {
        createAdminIfNotExists();
    }

    private void createAdminIfNotExists() {
        AtsumeruUser adminUser = getUserByUsername(ADMIN_USERNAME);
        if (adminUser == null) {
            logger.warn("[Admin] user not found. Creating one...");

            String password = new PasswordGenerator.PasswordGeneratorBuilder()
                    .useDigits(true)
                    .useLower(true)
                    .useUpper(true)
                    .usePunctuation(true)
                    .build()
                    .generate(12);

            adminUser = new AtsumeruUser();
            adminUser.setUserName(ADMIN_USERNAME);
            adminUser.setPassword(encodePassword(password));
            adminUser.setRoles("ADMIN");
            adminUser.setAuthorities("ROLE_ADMIN");

            usersDaoManager.save(adminUser);

            logger.info("[Admin] user created with password = " + password);
        }
    }

    public AtsumeruUser getUserFromRequest(HttpServletRequest request) {
        return Optional.ofNullable(request.getUserPrincipal())
                .map(Principal::getName)
                .map(this::getUserByUsername)
                .orElseThrow(UserNotFoundException::new);
    }

    public AtsumeruUser getUserByUsername(String userName) {
        return usersDaoManager.query(userName);
    }

    public AtsumeruUser getUserById(long id) {
        return usersDaoManager.query(id);
    }

    public List<AtsumeruUser> getAllUsers(){
        return usersDaoManager.queryAll();
    }

    public boolean isUserExists(AtsumeruUser atsumeruUser) {
        return isUserExists(atsumeruUser.getUserName());
    }

    public boolean isUserExists(String userName) {
        return StringUtils.isNotEmpty(userName) && usersDaoManager.query(userName) != null;
    }

    public void saveUser(AtsumeruUser atsumeruUser, boolean isRawPassword) {
        if (StringUtils.isEmpty(atsumeruUser.getAuthorities())) {
            atsumeruUser.setAuthorities("USER");
        }
        if (isRawPassword) {
            atsumeruUser.setPassword(encodePassword(atsumeruUser.getPassword()));
        }
        usersDaoManager.save(atsumeruUser);
    }

    public boolean deleteUser(long id) {
        return usersDaoManager.deleteById(id);
    }

    private String encodePassword(String rawPassword) {
        return context.getBean(PasswordEncoder.class).encode(rawPassword);
    }

    @Override
    public void close() {
        FileUtils.closeLoudly(usersDaoManager);
    }
}
