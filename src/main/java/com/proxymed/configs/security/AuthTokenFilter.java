package com.proxymed.configs.security;

import com.proxymed.configs.InfosBadge;
import com.proxymed.configs.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Lit le badge "Authorization: Bearer ..." et, s'il est valide, place l'UtilisateurConnecte
 * dans le SecurityContext avec les autorites ROLE_xxx. S'il est absent ou invalide, la requete
 * continue sans authentification : WebSecurityConfig decide alors (401 si l'adresse est protegee).
 */
@Component
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    /** Pose sur la requete quand un badge est present mais refuse, pour le message du 401. */
    static final String ATTRIBUT_BADGE_INVALIDE = AuthTokenFilter.class.getName() + ".badgeInvalide";

    private static final String PREFIXE_BEARER = "Bearer ";

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwt = parseJwt(request);
        if (jwt != null) {
            Optional<InfosBadge> badge = jwtUtils.verifierToken(jwt);
            if (badge.isPresent()) {
                authentifier(badge.get(), request);
            } else {
                request.setAttribute(ATTRIBUT_BADGE_INVALIDE, Boolean.TRUE);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Le role vient du claim "roles" du badge, relu a chaque requete, et devient l'autorite
     * Spring "ROLE_" + role : c'est elle que verifie @PreAuthorize("hasAnyRole('MEDECIN_JUNIOR', ...)").
     */
    private void authentifier(InfosBadge badge, HttpServletRequest request) {
        UtilisateurConnecte principal = new UtilisateurConnecte(badge.utilisateurId(), badge.email(), badge.roles());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                badge.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (headerAuth != null && headerAuth.startsWith(PREFIXE_BEARER)) {
            return headerAuth.substring(PREFIXE_BEARER.length());
        }
        return null;
    }
}
