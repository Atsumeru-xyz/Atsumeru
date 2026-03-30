package xyz.atsumeru.web.controller.rest.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import xyz.atsumeru.web.Beans;
import xyz.atsumeru.web.configuration.ServerConfiguration;
import xyz.atsumeru.web.enums.Genre;
import xyz.atsumeru.web.enums.LibraryPresentation;
import xyz.atsumeru.web.helper.RestHelper;
import xyz.atsumeru.web.manager.cache.AtsumeruCache;
import xyz.atsumeru.web.model.AccessToken;
import xyz.atsumeru.web.model.AtsumeruMessage;
import xyz.atsumeru.web.model.GenreModel;
import xyz.atsumeru.web.model.UserAccessConstants;
import xyz.atsumeru.web.model.book.BookArchive;
import xyz.atsumeru.web.model.book.IBaseBookItem;
import xyz.atsumeru.web.model.database.AtsumeruUser;
import xyz.atsumeru.web.repository.CategoryRepository;
import xyz.atsumeru.web.security.JWTManager;
import xyz.atsumeru.web.security.repository.UsersRepository;
import xyz.atsumeru.web.util.ArrayUtils;
import xyz.atsumeru.web.util.StringUtils;
import xyz.atsumeru.web.util.comparator.AlphanumComparator;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "API for managing Users")
public class UsersApiController {
    private final UsersRepository userService;

    public UsersApiController(UsersRepository userService) {
        this.userService = userService;
    }

    @Operation(summary = "About me", description = "Get info about current user")
    @GetMapping("/me")
    public AtsumeruUser aboutMe(HttpServletRequest request) {
        return userService.getUserFromRequest(request);
    }

    @GetMapping("/token")
    public AccessToken getAccessToken(Authentication auth) {
        return Optional.ofNullable(auth)
                .map(Principal::getName)
                .map(JWTManager::generateToken)
                .map(AccessToken::new)
                .orElseGet(() -> new AccessToken("debug"));
    }

    @Operation(summary = "User list", description = "Get list of created users")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/list")
    public List<AtsumeruUser> listUsers() {
        return userService.getAllUsers();
    }

    @Operation(summary = "User access constants", description = "Get list of available user Roles and Authorities")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/constants", "/authorities", "/roles"})
    public UserAccessConstants listUserAccessConstants() {
        return new UserAccessConstants(
                ServerConfiguration.ROLES,
                ServerConfiguration.AUTHORITIES,
                CategoryRepository.getCategories(),
                Arrays.stream(Genre.values())
                        .map(GenreModel::new)
                        .collect(Collectors.toList()),
                Beans.getBooksDaoManager()
                        .queryAll(BookArchive.class, LibraryPresentation.ARCHIVES)
                        .stream()
                        .map(IBaseBookItem.class::cast)
                        .map(IBaseBookItem::getTags)
                        .filter(StringUtils::isNotEmpty)
                        .map(ArrayUtils::splitString)
                        .flatMap(Collection::stream)
                        .distinct()
                        .sorted(AlphanumComparator::compareStrings)
                        .collect(Collectors.toList())
        );
    }

    @Operation(summary = "Create user", description = "Create new user with Roles, Authorities and optional access restrictions to Categories, Genres and Tags")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/create")
    public ResponseEntity<AtsumeruMessage> createUser(@RequestBody AtsumeruUser atsumeruUser) {
        boolean userExists = userService.isUserExists(atsumeruUser);

        String responseMessage = checkUserCreateOrUpdateError(atsumeruUser, userExists, false);
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;

        if (StringUtils.isEmpty(responseMessage)) {
            userService.saveUser(atsumeruUser, true);
            responseMessage = String.valueOf(userService.getUserByUsername(atsumeruUser.getUserName()).getId());
            statusCode = HttpStatus.CREATED;
        }

        return RestHelper.createResponseMessage(responseMessage, statusCode.value(), HttpStatus.OK);
    }

    @Operation(summary = "Update/Edit user", description = "Update user. Change Password, Roles, Authorities and optional access restrictions")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping({"/update", "/edit"})
    public ResponseEntity<AtsumeruMessage> updateUser(@RequestBody AtsumeruUser atsumeruUser) {
        AtsumeruUser atsumeruUserInDb = userService.getUserById(atsumeruUser.getId());
        AtsumeruUser atsumeruUserInDbByUsername = userService.getUserByUsername(atsumeruUser.getUserName());
        boolean anotherUsersWithUsernameExists = atsumeruUserInDbByUsername != null
                && !atsumeruUserInDb.getId().equals(atsumeruUserInDbByUsername.getId());

        String responseMessage = checkUserCreateOrUpdateError(atsumeruUser, anotherUsersWithUsernameExists, true);
        HttpStatus statusCode = HttpStatus.BAD_REQUEST;

        if (StringUtils.isEmpty(responseMessage)) {
            atsumeruUserInDb = userService.getUserById(atsumeruUser.getId());
            if (StringUtils.isEmpty(atsumeruUser.getPassword())) {
                atsumeruUser.setPassword(atsumeruUserInDb.getPassword());
            }
            userService.saveUser(atsumeruUser, !atsumeruUser.getPassword().equals(atsumeruUserInDb.getPassword()));
            responseMessage = "User successfully created";
            statusCode = HttpStatus.CREATED;
        }

        AtsumeruCache.evictAll();

        return RestHelper.createResponseMessage(responseMessage, statusCode.value(), HttpStatus.OK);
    }

    @Operation(summary = "Delete user", description = "Delete user by id")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<AtsumeruMessage> deleteUser(@RequestParam(name = "user_id") int userId) {
        boolean success = userService.deleteUser(userId);
        return RestHelper.createResponseMessage(
                success ? "User successfully deleted" : "Unable to delete user",
                success ? HttpStatus.OK : HttpStatus.BAD_REQUEST
        );
    }

    @Nullable
    private String checkUserCreateOrUpdateError(@NonNull AtsumeruUser atsumeruUser, boolean userExists, boolean allowEmptyPassword) {
        if (!allowEmptyPassword && (StringUtils.isEmpty(atsumeruUser.getPassword()) || atsumeruUser.getPassword().length() < 6)) {
            return "Password must be at least 6 characters long!";
        } else if (StringUtils.isEmpty(atsumeruUser.getUserName())) {
            return "Username can't be empty!";
        } else if (StringUtils.isEmpty(atsumeruUser.getRoles())) {
            return "Roles can't be empty!";
        } else if (userExists) {
            return "User " + atsumeruUser.getUserName() + " already exists!";
        }
        return null;
    }
}
