package com.megasena.sync.identidade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoIdentidadeRepository extends JpaRepository<EventoIdentidade, Long> {
}
