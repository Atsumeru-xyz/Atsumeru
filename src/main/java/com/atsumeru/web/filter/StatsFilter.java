package com.atsumeru.web.filter;

import com.atsumeru.web.util.StringUtils;
import com.atsumeru.web.controller.rest.service.ServicesApiController;
import com.atsumeru.web.helper.JavaHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

@Component
@WebFilter("/*")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StatsFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(StatsFilter.class.getSimpleName());

    @Override
    public void init(FilterConfig filterConfig) {
        // empty
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
        String requestURI = ((HttpServletRequest) req).getRequestURI();
        Instant start = Instant.now();
        try {
            chain.doFilter(req, resp);
        } finally {
            if (JavaHelper.isDebug() && !StringUtils.equalsIgnoreCase(requestURI, ServicesApiController.getStatusEndpoint())) {
                Instant finish = Instant.now();
                long time = Duration.between(start, finish).toMillis();
                logger.info("{}: {} ms ", requestURI, time);
            }
        }
    }

    @Override
    public void destroy() {
        // empty
    }
}