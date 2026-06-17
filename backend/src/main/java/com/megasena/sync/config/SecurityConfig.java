package com.megasena.sync.config;

import com.megasena.sync.identidade.AuditoriaIdentidadeService;
import com.megasena.sync.identidade.EstadoConta;
import com.megasena.sync.identidade.ResolvedorDeConta;
import com.megasena.sync.identidade.TipoEvento;
import com.megasena.sync.identidade.provedor.VerificadorDeIdentidade;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final VerificadorDeIdentidade verificador;
    private final ResolvedorDeConta resolvedorDeConta;
    private final AuditoriaIdentidadeService auditoria;

    public SecurityConfig(VerificadorDeIdentidade verificador,
                          ResolvedorDeConta resolvedorDeConta,
                          AuditoriaIdentidadeService auditoria) {
        this.verificador = verificador;
        this.resolvedorDeConta = resolvedorDeConta;
        this.auditoria = auditoria;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new IdentidadeTokenFilter(verificador, resolvedorDeConta),
                    UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Autenticação necessária.\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                    if (principal instanceof UsuarioAutenticado ua) {
                        String codigo;
                        String mensagem;
                        if (ua.getEstado() == EstadoConta.PENDENTE) {
                            codigo = "CONTA_PENDENTE";
                            mensagem = "Sua conta aguarda aprovação de um administrador.";
                        } else if (ua.getEstado() == EstadoConta.REPROVADO) {
                            codigo = "CONTA_REPROVADA";
                            mensagem = "Sua conta foi reprovada.";
                        } else {
                            codigo = "ACESSO_NEGADO";
                            mensagem = "Acesso negado.";
                        }
                        auditoria.registrar(ua.getUsuarioId(), TipoEvento.ACESSO_NEGADO, null, false, codigo);
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":403,\"codigo\":\"" + codigo + "\",\"mensagem\":\"" + mensagem + "\"}");
                    } else {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setContentType("application/json");
                        response.getWriter().write("{\"status\":403,\"codigo\":\"ACESSO_NEGADO\",\"mensagem\":\"Acesso negado.\"}");
                    }
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/concursos/**").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/perfil/**").hasRole("USUARIO")
                .requestMatchers("/api/jogos/**").hasRole("USUARIO")
                .requestMatchers("/api/conferencias/**").hasRole("USUARIO")
                .anyRequest().denyAll()
            );

        return http.build();
    }
}
