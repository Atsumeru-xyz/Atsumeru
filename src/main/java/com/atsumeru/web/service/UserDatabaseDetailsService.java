package com.atsumeru.web.service;

import com.atsumeru.web.repository.UserDatabaseRepository;
import com.atsumeru.web.model.database.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UserDatabaseDetailsService implements UserDetailsService {
    @Autowired
    private UserDatabaseRepository repository;

    @Override
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        User user = repository.getUserByUsername(userName);
        if (user == null) {
            throw new UsernameNotFoundException("User with username " + userName + " not found");
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String authority : user.getAuthoritiesSet()) {
            authorities.add(new SimpleGrantedAuthority(authority));
        }

        return new org.springframework.security.core.userdetails.User(user.getUserName(), user.getPassword(), authorities);
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
        return UserDatabaseDetailsService.isUserInRole(
                SecurityContextHolder.getContext().getAuthentication(),
                "ADMIN", "UPLOADER"
        );
    }
}