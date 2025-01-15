package xyz.atsumeru.web.security.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import xyz.atsumeru.web.model.database.AtsumeruUser;
import xyz.atsumeru.web.security.repository.UsersRepository;

import java.util.HashSet;
import java.util.Set;

@Service
public class UsersDetailsService implements UserDetailsService {
    private final UsersRepository repository;

    public UsersDetailsService(UsersRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        AtsumeruUser atsumeruUser = repository.getUserByUsername(userName);
        if (atsumeruUser == null) {
            throw new UsernameNotFoundException("User with username " + userName + " not found");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String authority : atsumeruUser.getAuthoritiesSet()) {
            authorities.add(new SimpleGrantedAuthority(authority));
        }

        return new User(atsumeruUser.getUserName(), atsumeruUser.getPassword(), authorities);
    }

    public static boolean isUserInRole(Authentication authentication, String... roles) {
        boolean hasAnyRole = false;
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            for (String role : roles) {
                if (authority.getAuthority().replace("ROLE_", "").equals(role)) {
                    hasAnyRole = true;
                    break;
                }
            }
        }
        return hasAnyRole;
    }

    public static boolean isUserCanDownloadFiles(Authentication authentication) {
        return isUserInRole(authentication, "ADMIN", "DOWNLOAD_FILES");
    }

    public static boolean isIncludeFileInfoIntoResponse() {
        return UsersDetailsService.isUserInRole(
                SecurityContextHolder.getContext().getAuthentication(),
                "ADMIN", "UPLOADER"
        );
    }
}