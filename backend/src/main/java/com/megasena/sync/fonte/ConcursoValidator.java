package com.megasena.sync.fonte;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ConcursoValidator {

    private static final Logger log = LoggerFactory.getLogger(ConcursoValidator.class);

    public boolean isValid(CaixaConcursoResponse response) {
        if (response.getNumero() == null || response.getNumero() <= 0) {
            log.warn("Concurso rejeitado: numero ausente ou <= 0");
            return false;
        }

        if (response.getDataApuracao() == null || response.getDataApuracao().isBlank()) {
            log.warn("Concurso {} rejeitado: dataApuracao ausente", response.getNumero());
            return false;
        }

        if (!isValidDezenas(response.getListaDezenas())) {
            log.warn("Concurso {} rejeitado: dezenas inválidas", response.getNumero());
            return false;
        }

        if (!hasValidSenaPrize(response)) {
            log.warn("Concurso {} rejeitado: faixa Sena ausente ou valorPremio < 0", response.getNumero());
            return false;
        }

        return true;
    }

    private boolean isValidDezenas(List<String> dezenas) {
        if (dezenas == null || dezenas.size() != 6) {
            return false;
        }
        Set<Integer> parsed = new HashSet<>();
        for (String d : dezenas) {
            try {
                int val = Integer.parseInt(d);
                if (val < 1 || val > 60) return false;
                parsed.add(val);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return parsed.size() == 6;
    }

    private boolean hasValidSenaPrize(CaixaConcursoResponse response) {
        if (response.getListaRateioPremio() == null) return false;
        return response.getListaRateioPremio().stream()
                .anyMatch(r -> "Sena".equalsIgnoreCase(r.getDescricaoFaixa())
                        && r.getValorPremio() != null
                        && r.getValorPremio().compareTo(BigDecimal.ZERO) >= 0);
    }
}
