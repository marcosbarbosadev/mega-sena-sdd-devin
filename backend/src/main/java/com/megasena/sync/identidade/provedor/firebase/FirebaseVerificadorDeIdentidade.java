package com.megasena.sync.identidade.provedor.firebase;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.megasena.sync.identidade.MetodoLogin;
import com.megasena.sync.identidade.provedor.IdentidadeInvalidaException;
import com.megasena.sync.identidade.provedor.IdentidadeVerificada;
import com.megasena.sync.identidade.provedor.ProvedorIndisponivelException;
import com.megasena.sync.identidade.provedor.VerificadorDeIdentidade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(FirebaseAuth.class)
public class FirebaseVerificadorDeIdentidade implements VerificadorDeIdentidade {

    private static final Logger log = LoggerFactory.getLogger(FirebaseVerificadorDeIdentidade.class);
    private final FirebaseAuth firebaseAuth;

    public FirebaseVerificadorDeIdentidade(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public IdentidadeVerificada verify(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            String uid = token.getUid();
            String email = token.getEmail();
            boolean emailVerified = token.isEmailVerified();
            Object provider = token.getClaims().get("firebase");
            MetodoLogin metodo = resolveMetodoLogin(provider);
            return new IdentidadeVerificada(uid, email, emailVerified, metodo);
        } catch (FirebaseAuthException e) {
            if (isNetworkError(e)) {
                throw new ProvedorIndisponivelException("Provedor de identidade indisponível", e);
            }
            throw new IdentidadeInvalidaException("Token inválido", e);
        }
    }

    @Override
    public void revogarSessoes(String uid) {
        try {
            firebaseAuth.revokeRefreshTokens(uid);
        } catch (FirebaseAuthException e) {
            if (isNetworkError(e)) {
                throw new ProvedorIndisponivelException("Provedor de identidade indisponível", e);
            }
            throw new IdentidadeInvalidaException("Erro ao revogar sessões", e);
        }
    }

    @SuppressWarnings("unchecked")
    private MetodoLogin resolveMetodoLogin(Object firebaseClaim) {
        if (firebaseClaim instanceof java.util.Map<?, ?> map) {
            Object signInProvider = map.get("sign_in_provider");
            if ("google.com".equals(signInProvider)) {
                return MetodoLogin.GOOGLE;
            }
        }
        return MetodoLogin.SENHA;
    }

    private boolean isNetworkError(FirebaseAuthException e) {
        String code = e.getAuthErrorCode() != null ? e.getAuthErrorCode().name() : "";
        return code.contains("UNAVAILABLE") || code.contains("INTERNAL")
                || (e.getCause() instanceof java.io.IOException);
    }
}
