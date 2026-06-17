package com.megasena.sync.support;

import com.megasena.sync.jogo.FonteAleatoriedade;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@Primary
public class FonteAleatoriedadeFake implements FonteAleatoriedade {

    private List<Integer> proximasDezenas;

    public void setProximasDezenas(List<Integer> dezenas) {
        this.proximasDezenas = dezenas;
    }

    @Override
    public List<Integer> gerarDezenas(int quantidade) {
        if (proximasDezenas != null && !proximasDezenas.isEmpty()) {
            List<Integer> resultado = proximasDezenas.subList(0, quantidade);
            proximasDezenas = null;
            return List.copyOf(resultado);
        }
        return IntStream.rangeClosed(1, quantidade).boxed().toList();
    }
}
