package com.megasena.sync.config;

import com.megasena.sync.identidade.ResolvedorDeConta;
import com.megasena.sync.identidade.provedor.IdentidadeInvalidaException;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;
import com.megasena.sync.identidade.provedor.ProvedorIndisponivelException;
import com.megasena.sync.identidade.provedor.VerificadorDeIdentidade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class IdentidadeTokenFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(IdentidadeTokenFilter.class);

    private final VerificadorDeIdentidade verificador;
    private final ResolvedorDeConta resolvedorDeConta;

    public IdentidadeTokenFilter(VerificadorDeIdentidade verificador, ResolvedorDeConta resolvedorDeConta) {
        this.verificador = verificador;
        this.resolvedorDeConta = resolvedorDeConta;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                IdentidadeVerificada identidade = verificador.verify(token);
                UsuarioAutenticado usuario = resolvedorDeConta.resolver(identidade);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        usuario, null, usuario.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (IdentidadeInvalidaException e) {
                log.debug("Token inválido: {}", e.getMessage());
            } catch (ProvedorIndisponivelException e) {
                log.error("Provedor de identidade indisponível", e);
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":503,\"codigo\":\"PROVEDOR_INDISPONIVEL\",\"mensagem\":\"Serviço de identidade temporariamente indisponível.\"}");
                return;
            } catch (com.megasena.sync.identidade.EmailNaoVerificadoException e) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"status\":403,\"codigo\":\"EMAIL_NAO_VERIFICADO\",\"mensagem\":\"" + e.getMessage() + "\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
