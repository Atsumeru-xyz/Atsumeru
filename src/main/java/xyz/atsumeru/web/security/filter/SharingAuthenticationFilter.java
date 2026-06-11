package xyz.atsumeru.web.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import xyz.atsumeru.web.model.database.ShareToken;
import xyz.atsumeru.web.repository.dao.UsersDaoManager;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
public class SharingAuthenticationFilter extends OncePerRequestFilter {
    private final UsersDaoManager usersDaoManager;

    public SharingAuthenticationFilter(UsersDaoManager usersDaoManager) {
        this.usersDaoManager = usersDaoManager;
    }

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws ServletException, IOException {
        String shareToken = extractShareToken(request.getRequestURI());
        if (shareToken != null) {
            try {
                ShareToken token = usersDaoManager.queryShareToken(shareToken);
                if (token != null && isNotExpired(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = createShareUserDetails(token);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            } catch (Exception ignored) {
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean isNotExpired(ShareToken token) {
        return token.getExpiresAt() == null || token.getExpiresAt() > System.currentTimeMillis();
    }

    private static UserDetails createShareUserDetails(ShareToken token) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (String authority : token.getAuthoritiesSet()) {
            authorities.add(new SimpleGrantedAuthority(authority));
        }
        return new User("share:" + token.getToken(), "", authorities);
    }

    private static String extractShareToken(String requestUri) {
        if (requestUri == null) {
            return null;
        }
        int shareIndex = requestUri.indexOf("/share/");
        if (shareIndex < 0) {
            return null;
        }
        String afterShare = requestUri.substring(shareIndex + "/share/".length());
        int slashIndex = afterShare.indexOf('/');
        if (slashIndex > 0) {
            return afterShare.substring(0, slashIndex);
        }
        return afterShare;
    }
}
