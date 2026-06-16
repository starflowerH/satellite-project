package com.example.demo.filter;

import com.example.demo.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 * 从请求头中提取JWT Token，验证并设置用户信息到请求属性
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // 不需要认证的路径
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = extractToken(request);
        
        if (token != null && jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            Integer status = jwtUtil.getStatusFromToken(token);
            
            // 将用户信息设置到请求属性中
            request.setAttribute("currentUserId", userId);
            request.setAttribute("currentUserStatus", status);
            
            // 包装请求，添加便捷方法
            HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equals(name)) {
                        return String.valueOf(userId);
                    }
                    if ("X-User-Status".equals(name)) {
                        return String.valueOf(status);
                    }
                    return super.getHeader(name);
                }
            };
            
            filterChain.doFilter(wrappedRequest, response);
        } else {
            // 对于需要认证但没有有效Token的请求
            if (requiresAuthentication(path)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"未认证，请先登录\",\"data\":null}");
                return;
            }
            filterChain.doFilter(request, response);
        }
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        // 公开路径，不需要认证
        return path.startsWith("/auth/") ||
               path.startsWith("/hero/list") ||
               path.startsWith("/amap/") ||
               path.equals("/error");
    }

    private boolean requiresAuthentication(String path) {
        // 需要认证的路径
        return path.startsWith("/user/") ||
               path.startsWith("/agent/") ||
               path.startsWith("/hero/my/");
    }
}
