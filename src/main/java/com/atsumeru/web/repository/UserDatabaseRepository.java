package com.atsumeru.web.repository;

import com.atsumeru.web.exception.UserNotFoundException;
import com.atsumeru.web.helper.PasswordGenerator;
import com.atsumeru.web.model.database.User;
import com.atsumeru.web.repository.dao.UsersDaoManager;
import com.atsumeru.web.util.FileUtils;
import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.util.Workspace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDatabaseRepository {
    private static final Logger logger = LoggerFactory.getLogger(UserDatabaseRepository.class.getSimpleName());
    public static final String ADMIN_USERNAME = "Admin";

    private UsersDaoManager usersDaoManager;

    @Autowired
    private ApplicationContext context;

    public UserDatabaseRepository() {
        connect();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void doAfterStart() {
        createAdminIfNeeded();
    }

    private void createAdminIfNeeded() {
        User adminUser = getUserByUsername(ADMIN_USERNAME);
        if (adminUser == null) {
            logger.warn("Admin user not found. Creating one...");

            String password = createPassword(10);

            adminUser = new User();
            adminUser.setUserName(ADMIN_USERNAME);
            adminUser.setPassword(encodePassword(password));
            adminUser.setRoles("ADMIN");
            adminUser.setAuthorities("ROLE_ADMIN");

            usersDaoManager.save(adminUser);

            logger.info("Admin user created with password = " + password);
        }
    }

    public User getUserFromRequest(HttpServletRequest request) {
        String userName = Optional.ofNullable(request.getUserPrincipal())
                .map(Principal::getName)
                .orElse(null);

        return Optional.ofNullable(getUserByUsername(userName)).orElseThrow(UserNotFoundException::new);
    }

    public User getUserByUsername(String userName) {
        return usersDaoManager.query(userName);
    }

    public User getUserById(long id) {
        return usersDaoManager.query(id);
    }

    public List<User> getAllUsers(){
        return usersDaoManager.queryAll();
    }

    public boolean isUserExists(User user) {
        return isUserExists(user.getUserName());
    }

    public boolean isUserExists(String userName) {
        return StringUtils.isNotEmpty(userName) && usersDaoManager.query(userName) != null;
    }

    public void saveUser(User user, boolean isRawPassword) {
        if (StringUtils.isEmpty(user.getAuthorities())) {
            user.setAuthorities("USER");
        }
        if (isRawPassword) {
            user.setPassword(encodePassword(user.getPassword()));
        }
        usersDaoManager.save(user);
    }

    public boolean deleteUser(long id) {
        return usersDaoManager.deleteById(id);
    }

    private String encodePassword(String rawPassword) {
        return context.getBean(PasswordEncoder.class).encode(rawPassword);
    }

    public static String createPassword(int length) {
        PasswordGenerator passwordGenerator = new PasswordGenerator.PasswordGeneratorBuilder()
                .useDigits(true)
                .useLower(true)
                .useUpper(true)
                .build();

        return passwordGenerator.generate(length);
    }

    public void connect() {
        try {
            usersDaoManager = new UsersDaoManager(Workspace.DATABASES_DIR + "users.db");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        FileUtils.closeQuietly(usersDaoManager);
    }
}
