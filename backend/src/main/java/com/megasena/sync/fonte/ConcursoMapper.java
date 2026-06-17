package com.megasena.sync.fonte;

import com.megasena.sync.concurso.Concurso;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
public class ConcursoMapper {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public Concurso toConcurso(CaixaConcursoResponse response) {
        Concurso concurso = new Concurso();
        concurso.setNumero(response.getNumero());
        concurso.setDataSorteio(parseDate(response.getDataApuracao()));
        concurso.setValorPremio(extractSenaPrize(response));

        for (String d : response.getListaDezenas()) {
            concurso.addDezena(Integer.parseInt(d));
        }

        return concurso;
    }

    LocalDate parseDate(String dataApuracao) {
        return LocalDate.parse(dataApuracao, DATE_FORMAT);
    }

    BigDecimal extractSenaPrize(CaixaConcursoResponse response) {
        return response.getListaRateioPremio().stream()
                .filter(r -> "Sena".equalsIgnoreCase(r.getDescricaoFaixa()))
                .map(CaixaConcursoResponse.RateioPremio::getValorPremio)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
