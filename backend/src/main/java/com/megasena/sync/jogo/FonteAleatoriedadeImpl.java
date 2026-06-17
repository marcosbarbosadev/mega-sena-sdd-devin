package com.megasena.sync.jogo;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Component
public class FonteAleatoriedadeImpl implements FonteAleatoriedade {

    private final SecureRandom random = new SecureRandom();

    @Override
    public List<Integer> gerarDezenas(int quantidade) {
        List<Integer> pool = new ArrayList<>(IntStream.rangeClosed(1, 60).boxed().toList());
        Collections.shuffle(pool, random);
        List<Integer> dezenas = new ArrayList<>(pool.subList(0, quantidade));
        Collections.sort(dezenas);
        return dezenas;
    }
}
