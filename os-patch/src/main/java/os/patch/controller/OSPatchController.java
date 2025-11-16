package os.patch.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import os.patch.entity.dto.ApiResponse;
import os.patch.entity.dto.PatchProgress;
import os.patch.entity.dto.PatchRequest;
import os.patch.mode.PollingState;
import os.patch.service.OSPatchService;
import os.patch.service.PatchProgressTracker;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/os-patch")
@RequiredArgsConstructor
public class OSPatchController {
    private final OSPatchService osPatchService;
    private final PatchProgressTracker progressTracker;

    @PostMapping("/single")
    public CompletableFuture<ResponseEntity<ApiResponse<String>>> patchSingleServer(
            @RequestBody PatchRequest request) {

        log.info("Received patch request for server: {}", request.serverId());

        if (progressTracker.isServerInProgress(request.serverId())) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(ApiResponse.<String>builder()
                                    .status(HttpStatus.CONFLICT)
                                    .message("Server " + request.serverId() + " is already being patched")
                                    .data(null)
                                    .build())
            );
        }

        return osPatchService.executePatchFlow(request)
                .thenApply(v -> ResponseEntity.accepted()
                        .body(ApiResponse.<String>builder()
                                .status(HttpStatus.ACCEPTED)
                                .message("Patch process started successfully")
                                .data("Patch process started for server: " + request.serverId())
                                .build()))
                .exceptionally(ex -> ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(ApiResponse.<String>builder()
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .message("Failed to start patch process")
                                .data(ex.getMessage())
                                .build()));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<String>> patchBatchServers(@RequestBody List<PatchRequest> requests) {
        log.info("Received batch patch request for {} servers", requests.size());

        List<PatchRequest> validRequests = osPatchService.validateAndFilterRequests(requests);

        if (validRequests.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<String>builder()
                            .status(HttpStatus.BAD_REQUEST)
                            .message("No valid servers to patch")
                            .data("All servers are either invalid or already in progress")
                            .build());
        }

        if (validRequests.size() < requests.size()) {
            log.warn("Filtered out {} invalid requests, processing {} valid requests",
                    requests.size() - validRequests.size(), validRequests.size());
        }

        osPatchService.executeBatchPatch(validRequests)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Batch patch execution failed: {}", error.getMessage());
                    }
                });

        return ResponseEntity.accepted()
                .body(ApiResponse.<String>builder()
                        .status(HttpStatus.ACCEPTED)
                        .message("Batch patch process started")
                        .data("Batch patch started for " + validRequests.size() + " valid servers")
                        .build());
    }

    @GetMapping("/progress/{serverId}")
    public ResponseEntity<ApiResponse<PatchProgress>> getServerProgress(@PathVariable String serverId) {
        PatchProgress progress = progressTracker.getProgress(serverId);
        if (progress == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PatchProgress>builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message("Server not found or not being patched")
                            .data(null)
                            .build());
        }

        return ResponseEntity.ok(ApiResponse.<PatchProgress>builder()
                .status(HttpStatus.OK)
                .message("Progress retrieved successfully")
                .data(progress)
                .build());
    }

    @GetMapping("/progress")
    public ResponseEntity<ApiResponse<List<PatchProgress>>> getAllProgress() {
        List<PatchProgress> progress = progressTracker.getAllProgress();

        return ResponseEntity.ok(ApiResponse.<List<PatchProgress>>builder()
                .status(HttpStatus.OK)
                .message("All progress retrieved successfully")
                .data(progress)
                .build());
    }

    @GetMapping("/polling-status/{serverId}")
    public ResponseEntity<ApiResponse<PollingState>> getPollingStatus(@PathVariable String serverId) {
        PollingState pollingState = osPatchService.getPollingState(serverId);
        if (pollingState == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<PollingState>builder()
                            .status(HttpStatus.NOT_FOUND)
                            .message("No active polling found for server")
                            .data(null)
                            .build());
        }

        return ResponseEntity.ok(ApiResponse.<PollingState>builder()
                .status(HttpStatus.OK)
                .message("Polling status retrieved successfully")
                .data(pollingState)
                .build());
    }

    @PostMapping("/cancel/{serverId}")
    public ResponseEntity<ApiResponse<String>> cancelPatchOperation(@PathVariable String serverId) {
        osPatchService.getPollingState(serverId); // This will trigger cleanup through the cancel method

        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK)
                .message("Patch operation cancellation requested")
                .data("Cancellation requested for server: " + serverId)
                .build());
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK)
                .message("OS Patching Service is healthy")
                .data("Service is running with virtual threads")
                .build());
    }
}
