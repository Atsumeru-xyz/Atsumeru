package com.atsumeru.web.controller.rest.user;

import com.atsumeru.web.configuration.ServerConfig;
import com.atsumeru.web.enums.Genre;
import com.atsumeru.web.model.GenreModel;
import com.atsumeru.web.model.book.BookArchive;
import com.atsumeru.web.model.book.IBaseBookItem;
import com.atsumeru.web.repository.BooksDatabaseRepository;
import com.atsumeru.web.repository.CategoryRepository;
import com.atsumeru.web.repository.UserDatabaseRepository;
import com.atsumeru.web.util.GUString;
import com.atsumeru.web.enums.LibraryPresentation;
import com.atsumeru.web.helper.RestHelper;
import com.atsumeru.web.manager.AtsumeruCacheManager;
import com.atsumeru.web.model.AtsumeruMessage;
import com.atsumeru.web.model.UserAccessConstants;
import com.atsumeru.web.model.database.User;
import com.atsumeru.web.util.GUArray;
import com.atsumeru.web.util.comparator.AlphanumComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UsersApiController {

    @Autowired
    UserDatabaseRepository userService;

    @GetMapping("/me")
    public User aboutMe(HttpServletRequest request) {
        return userService.getUserFromRequest(request);
    }

    @GetMapping("/list")
    public List<User> listUsers() {
        return userService.getAllUsers();
    }

    @GetMapping({"/constants", "/authorities", "/roles"})
    public UserAccessConstants listUserAccessConstants() {
        return new UserAccessConstants(
                ServerConfig.ROLES,
                ServerConfig.AUTHORITIES,
                CategoryRepository.getCategories(),
                Arrays.stream(Genre.values())
                        .map(GenreModel::new)
                        .collect(Collectors.toList()),
                BooksDatabaseRepository.getInstance()
                        .getDaoManager()
                        .queryAll(BookArchive.class, LibraryPresentation.ARCHIVES)
                        .stream()
                        .map(IBaseBookItem.class::cast)
                        .map(IBaseBookItem::getTags)
                        .filter(GUString::isNotEmpty)
                        .map(GUArray::splitString)
                        .flatMap(Collection::stream)
                        .distinct()
                        .sorted(AlphanumComparator::compareStrings)
                        .collect(Collectors.toList())
        );
    }

    @PutMapping("/create")
    public ResponseEntity<AtsumeruMessage> createUser(@RequestBody User user) {
        boolean userExists = userService.isUserExists(user);

        String responseMessage = checkUserCreateOrUpdateError(user, userExists, false);
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;

        if (GUString.isEmpty(responseMessage)) {
            userService.saveUser(user, true);
            responseMessage = String.valueOf(userService.getUserByUsername(user.getUserName()).getId());
            statusCode = HttpStatus.CREATED;
        }

        return RestHelper.createResponseMessage(responseMessage, statusCode.value(), HttpStatus.OK);
    }

    @PatchMapping({"/update", "/edit"})
    public ResponseEntity<AtsumeruMessage> updateUser(@RequestBody User user) {
        User userInDb = userService.getUserById(user.getId());
        User userInDbByUsername = userService.getUserByUsername(user.getUserName());
        boolean anotherUsersWithUsernameExists = userInDbByUsername != null
                && !userInDb.getId().equals(userInDbByUsername.getId());

        String responseMessage = checkUserCreateOrUpdateError(user, anotherUsersWithUsernameExists, true);
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;

        if (GUString.isEmpty(responseMessage)) {
            userInDb = userService.getUserById(user.getId());
            if (GUString.isEmpty(user.getPassword())) {
                user.setPassword(userInDb.getPassword());
            }
            userService.saveUser(user, !user.getPassword().equals(userInDb.getPassword()));
            responseMessage = "User successfully created";
            statusCode = HttpStatus.CREATED;
        }

        AtsumeruCacheManager.evictAll();

        return RestHelper.createResponseMessage(responseMessage, statusCode.value(), HttpStatus.OK);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<AtsumeruMessage> deleteUser(@RequestParam(name = "user_id") int userId) {
        boolean success = userService.deleteUser(userId);
        return RestHelper.createResponseMessage(
                success ? "User successfully deleted" : "Unable to delete user",
                success ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );
    }

    @Nullable
    private String checkUserCreateOrUpdateError(@NonNull User user, boolean userExists, boolean allowEmptyPassword) {
        if (!allowEmptyPassword && (GUString.isEmpty(user.getPassword()) || user.getPassword().length() < 6)) {
            return "Password must be at least 6 characters long!";
        } else if (GUString.isEmpty(user.getUserName())) {
            return "Username can't be empty!";
        } else if (GUString.isEmpty(user.getRoles())) {
            return "Roles can't be empty!";
        } else if (userExists) {
            return "User " + user.getUserName() + " already exists!";
        }
        return null;
    }
}
