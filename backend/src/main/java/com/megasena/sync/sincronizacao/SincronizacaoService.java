package com.megasena.sync.sincronizacao;

import com.megasena.sync.concurso.Concurso;
import com.megasena.sync.concurso.ConcursoRepository;
import com.megasena.sync.config.GlobalExceptionHandler;
import com.megasena.sync.fonte.CaixaConcursoResponse;
import com.megasena.sync.fonte.CaixaSourceClient;
import com.megasena.sync.fonte.ConcursoMapper;
import com.megasena.sync.fonte.ConcursoValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SincronizacaoService {

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoService.class);

    private final ConcursoRepository concursoRepository;
    private final SyncRunRepository syncRunRepository;
    private final CaixaSourceClient sourceClient;
    private final ConcursoValidator validator;
    private final ConcursoMapper mapper;

    public SincronizacaoService(ConcursoRepository concursoRepository,
                                SyncRunRepository syncRunRepository,
                                CaixaSourceClient sourceClient,
                                ConcursoValidator validator,
                                ConcursoMapper mapper) {
        this.concursoRepository = concursoRepository;
        this.syncRunRepository = syncRunRepository;
        this.sourceClient = sourceClient;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Transactional
    public SyncRun sincronizar(OrigemSync origem) {
        if (syncRunRepository.existsByStatus(StatusSync.EM_EXECUCAO)) {
            throw new GlobalExceptionHandler.ConflictException("Já existe uma sincronização em execução");
        }

        SyncRun run = SyncRun.iniciar(origem);
        run = syncRunRepository.save(run);

        long startTime = System.currentTimeMillis();
        log.info("Sync iniciada [origem={}, runId={}]", origem, run.getId());

        try {
            int maxStored = concursoRepository.findTopByOrderByNumeroDesc()
                    .map(Concurso::getNumero)
                    .orElse(0);

            CaixaConcursoResponse latest = sourceClient.buscarUltimo();
            if (latest == null || latest.getNumero() == null) {
                run.concluirFalha("Fonte retornou resposta nula ou sem número");
                syncRunRepository.save(run);
                logFinished(run, startTime);
                return run;
            }

            int latestNumber = latest.getNumero();

            if (maxStored >= latestNumber) {
                run.concluirSucesso(maxStored, maxStored, 0);
                syncRunRepository.save(run);
                log.info("Sync: nenhum concurso novo [maxStored={}, latest={}]", maxStored, latestNumber);
                logFinished(run, startTime);
                return run;
            }

            int imported = 0;
            int firstImported = maxStored + 1;

            for (int num = maxStored + 1; num <= latestNumber; num++) {
                try {
                    if (concursoRepository.existsByNumero(num)) {
                        continue;
                    }

                    CaixaConcursoResponse response;
                    if (num == latestNumber) {
                        response = latest;
                    } else {
                        response = sourceClient.buscarPorNumero(num);
                    }

                    if (response == null || !validator.isValid(response)) {
                        log.warn("Concurso {} rejeitado pela validação, pulando", num);
                        continue;
                    }

                    Concurso concurso = mapper.toConcurso(response);
                    concursoRepository.save(concurso);
                    imported++;
                } catch (Exception e) {
                    log.error("Erro ao importar concurso {}: {}", num, e.getMessage());
                    if (imported > 0) {
                        run.concluirParcial(firstImported, num - 1, imported, e.getMessage());
                        syncRunRepository.save(run);
                        logFinished(run, startTime);
                        return run;
                    }
                    run.concluirFalha("Erro ao importar concurso " + num + ": " + e.getMessage());
                    syncRunRepository.save(run);
                    logFinished(run, startTime);
                    return run;
                }
            }

            if (imported > 0) {
                run.concluirSucesso(firstImported, latestNumber, imported);
            } else {
                run.concluirSucesso(maxStored, latestNumber, 0);
            }
            syncRunRepository.save(run);
            logFinished(run, startTime);
            return run;

        } catch (Exception e) {
            log.error("Sync falhou [origem={}, runId={}]: {}", origem, run.getId(), e.getMessage());
            run.concluirFalha(e.getMessage());
            syncRunRepository.save(run);
            logFinished(run, startTime);
            return run;
        }
    }

    @Transactional
    public SyncRun cargaHistorica() {
        if (syncRunRepository.existsByStatus(StatusSync.EM_EXECUCAO)) {
            throw new GlobalExceptionHandler.ConflictException("Já existe uma sincronização em execução");
        }

        SyncRun run = SyncRun.iniciar(OrigemSync.MANUAL);
        run = syncRunRepository.save(run);

        long startTime = System.currentTimeMillis();
        log.info("Carga histórica iniciada [runId={}]", run.getId());

        try {
            CaixaConcursoResponse latest = sourceClient.buscarUltimo();
            if (latest == null || latest.getNumero() == null) {
                run.concluirFalha("Fonte retornou resposta nula ou sem número");
                syncRunRepository.save(run);
                logFinished(run, startTime);
                return run;
            }

            int latestNumber = latest.getNumero();
            int imported = 0;
            int firstImported = -1;
            int lastImported = -1;

            for (int num = 1; num <= latestNumber; num++) {
                try {
                    if (concursoRepository.existsByNumero(num)) {
                        continue;
                    }

                    CaixaConcursoResponse response;
                    if (num == latestNumber) {
                        response = latest;
                    } else {
                        response = sourceClient.buscarPorNumero(num);
                    }

                    if (response == null || !validator.isValid(response)) {
                        log.warn("Concurso {} rejeitado pela validação durante carga histórica", num);
                        continue;
                    }

                    Concurso concurso = mapper.toConcurso(response);
                    concursoRepository.save(concurso);
                    imported++;
                    if (firstImported == -1) firstImported = num;
                    lastImported = num;
                } catch (Exception e) {
                    log.error("Erro ao importar concurso {} durante carga histórica: {}", num, e.getMessage());
                    if (imported > 0) {
                        run.concluirParcial(firstImported, lastImported, imported, e.getMessage());
                    } else {
                        run.concluirFalha("Erro ao importar concurso " + num + ": " + e.getMessage());
                    }
                    syncRunRepository.save(run);
                    logFinished(run, startTime);
                    return run;
                }
            }

            if (imported > 0) {
                run.concluirSucesso(firstImported, lastImported, imported);
            } else {
                run.concluirSucesso(0, latestNumber, 0);
            }
            syncRunRepository.save(run);
            logFinished(run, startTime);
            return run;

        } catch (Exception e) {
            log.error("Carga histórica falhou [runId={}]: {}", run.getId(), e.getMessage());
            run.concluirFalha(e.getMessage());
            syncRunRepository.save(run);
            logFinished(run, startTime);
            return run;
        }
    }

    private void logFinished(SyncRun run, long startTime) {
        long durationMs = System.currentTimeMillis() - startTime;
        log.info("Sync finalizada [runId={}, origem={}, status={}, importados={}, faixa={}-{}, duracao={}ms]",
                run.getId(), run.getOrigem(), run.getStatus(),
                run.getConcursosImportados(),
                run.getConcursoInicial(), run.getConcursoFinal(),
                durationMs);
    }
}
