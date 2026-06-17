package com.megasena.sync.concurso;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConcursoRepository extends JpaRepository<Concurso, Integer> {

    Optional<Concurso> findTopByOrderByNumeroDesc();

    boolean existsByNumero(Integer numero);

    Optional<Concurso> findByNumero(Integer numero);
}
