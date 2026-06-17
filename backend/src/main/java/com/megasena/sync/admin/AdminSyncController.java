package com.megasena.sync.admin;

import com.megasena.sync.config.GlobalExceptionHandler;
import com.megasena.sync.sincronizacao.OrigemSync;
import com.megasena.sync.sincronizacao.SincronizacaoService;
import com.megasena.sync.sincronizacao.SyncRun;
import com.megasena.sync.sincronizacao.SyncRunRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/sync")
public class AdminSyncController {

    private final SyncRunRepository syncRunRepository;
    private final SincronizacaoService sincronizacaoService;

    public AdminSyncController(SyncRunRepository syncRunRepository,
                               SincronizacaoService sincronizacaoService) {
        this.syncRunRepository = syncRunRepository;
        this.sincronizacaoService = sincronizacaoService;
    }

    @GetMapping("/status")
    public ResponseEntity<SyncRunResponse> getStatus() {
        SyncRun lastRun = syncRunRepository.findTopByOrderByIniciadoEmDesc()
                .orElseThrow(() -> new GlobalExceptionHandler.ResourceNotFoundException(
                        "Nenhuma sincronização registrada ainda"));
        return ResponseEntity.ok(SyncRunResponse.from(lastRun));
    }

    @PostMapping("/run")
    public ResponseEntity<SyncRunResponse> triggerManualSync() {
        SyncRun run = sincronizacaoService.sincronizar(OrigemSync.MANUAL);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SyncRunResponse.from(run));
    }
}
