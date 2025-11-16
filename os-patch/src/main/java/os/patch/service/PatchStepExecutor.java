package os.patch.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import os.patch.entity.dto.JobResponse;
import os.patch.entity.dto.PatchRequest;
import os.patch.enums.PatchStep;
import os.patch.mode.PollingState;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatchStepExecutor {
    private final WebClient webClient;
    private final PatchProgressTracker progressTracker;

    @Value("${ospatch.auth.username}")
    private String username;

    @Value("${ospatch.auth.password}")
    private String password;

    private final Map<String, PollingState> pollingStateMap = new ConcurrentHashMap<>();

    private String getAuthHeader() {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }

    public Mono<JobResponse> executeStep(PatchStep step, PatchRequest request, String serverId) {
        if (!step.isPostRequest()) {
            return Mono.error(new IllegalArgumentException("Step " + step + " is not a POST request"));
        }

        log.info("Executing step {} for server {}", step.getEndpoint(), serverId);
        progressTracker.updateStep(serverId, step.getEndpoint());

        return webClient.post()
                .uri("/" + step.getEndpoint())
                .header(HttpHeaders.AUTHORIZATION, getAuthHeader())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(JobResponse.class)
                .doOnSuccess(response -> {
                    log.info("Step {} submitted successfully for server {}, job ID: {}",
                            step.getEndpoint(), serverId, response.jobId());
                    progressTracker.updateStatus(serverId, "STEP_SUBMITTED",
                            "Step " + step.getEndpoint() + " submitted with job ID: " + response.jobId());
                })
                .doOnError(error -> {
                    log.error("Failed to submit step {} for server {}: {}",
                            step.getEndpoint(), serverId, error.getMessage());
                    progressTracker.updateStatus(serverId, "STEP_FAILED",
                            "Failed to submit step " + step.getEndpoint() + ": " + error.getMessage());
                });
    }

    public Mono<JobResponse> pollJobStatus(PatchStep step, Long jobId, String serverId) {
        log.info("Starting job status polling for step {} (jobId: {}) with interval: {}s, max attempts: {}",
                step.getEndpoint(), jobId, step.getPollingInterval().getSeconds(), step.getMaxPollingAttempts());

        PollingState pollingState = new PollingState(step, jobId, serverId);
        pollingStateMap.put(serverId, pollingState);

        progressTracker.updateStatus(serverId, "POLLING_STARTED",
                String.format("Polling step %s (max %d attempts, interval %ds)",
                        step.getEndpoint(), step.getMaxPollingAttempts(), step.getPollingInterval().getSeconds()));

        return pollJobStatusWithRetry(pollingState);
    }

    private Mono<JobResponse> pollJobStatusWithRetry(PollingState pollingState) {
        PatchStep step = pollingState.getStep();
        Long jobId = pollingState.getJobId();
        String serverId = pollingState.getServerId();

        return Mono.defer(() -> {
            pollingState.incrementAttempt();
            int currentAttempt = pollingState.getCurrentAttempt();
            int maxAttempts = pollingState.getMaxAttempts();

            log.debug("Polling attempt {}/{} for step {} (jobId: {})",
                    currentAttempt, maxAttempts, step.getEndpoint(), jobId);

            return webClient.get()
                    .uri("/" + PatchStep.JOB_STATUS.getEndpoint() + "/" + jobId)
                    .header(HttpHeaders.AUTHORIZATION, getAuthHeader())
                    .retrieve()
                    .bodyToMono(JobResponse.class)
                    .flatMap(response -> handleJobResponse(response, pollingState))
                    .doOnError(error -> {
                        log.error("Error polling job status for step {} (attempt {}/{}): {}",
                                step.getEndpoint(), currentAttempt, maxAttempts, error.getMessage());

                        if (pollingState.shouldContinuePolling()) {
                            progressTracker.updateStatus(serverId, "POLLING_ERROR_RETRY",
                                    String.format("Polling error (attempt %d/%d): %s - Retrying...",
                                            currentAttempt, maxAttempts, error.getMessage()));
                        } else {
                            progressTracker.updateStatus(serverId, "POLLING_FAILED",
                                    String.format("Polling failed after %d attempts: %s",
                                            maxAttempts, error.getMessage()));
                            pollingStateMap.remove(serverId);
                        }
                    });
        });
    }

    private Mono<JobResponse> handleJobResponse(JobResponse response, PollingState pollingState) {
        PatchStep step = pollingState.getStep();
        String serverId = pollingState.getServerId();
        int currentAttempt = pollingState.getCurrentAttempt();
        int maxAttempts = pollingState.getMaxAttempts();

        log.debug("Job status for step {} (attempt {}/{}): {}",
                step.getEndpoint(), currentAttempt, maxAttempts, response.status());

        progressTracker.updateStatus(serverId, response.status().toUpperCase(),
                String.format("Step %s - %s (attempt %d/%d)",
                        step.getEndpoint(),
                        response.output() != null ? response.output().details() : "",
                        currentAttempt, maxAttempts));

        switch (response.status()) {
            case "running":
                if (pollingState.shouldContinuePolling()) {
                    Duration delay = pollingState.getPollingInterval();
                    log.debug("Job still running, next poll in {}s for step {}",
                            delay.getSeconds(), step.getEndpoint());

                    progressTracker.updateStatus(serverId, "POLLING_CONTINUES",
                            String.format("Step %s still running, next poll in %ds (attempt %d/%d)",
                                    step.getEndpoint(), delay.getSeconds(), currentAttempt, maxAttempts));

                    return Mono.delay(delay)
                            .then(Mono.defer(() -> pollJobStatusWithRetry(pollingState)));
                } else {
                    log.warn("Max polling attempts reached for step {} (jobId: {})",
                            step.getEndpoint(), pollingState.getJobId());
                    progressTracker.updateStatus(serverId, "POLLING_TIMEOUT",
                            String.format("Step %s - Max polling attempts (%d) reached, job still running",
                                    step.getEndpoint(), maxAttempts));
                    pollingStateMap.remove(serverId);
                    return Mono.error(new RuntimeException(
                            String.format("Max polling attempts (%d) reached for step %s, job still running",
                                    maxAttempts, step.getEndpoint())));
                }

            case "successful":
                log.info("Step {} completed successfully for server {} (attempt {}/{})",
                        step.getEndpoint(), serverId, currentAttempt, maxAttempts);
                progressTracker.addCompletedStep(serverId, step.getEndpoint());
                pollingStateMap.remove(serverId);
                return Mono.just(response);

            case "failed":
                log.error("Step {} failed for server {}: {}",
                        step.getEndpoint(), serverId,
                        response.output() != null ? response.output().details() : "");
                progressTracker.updateStatus(serverId, "STEP_FAILED",
                        "Step " + step.getEndpoint() + " failed: " +
                                (response.output() != null ? response.output().details() : ""));
                pollingStateMap.remove(serverId);
                return Mono.just(response);

            default:
                log.warn("Unknown job status for step {}: {}", step.getEndpoint(), response.status());
                progressTracker.updateStatus(serverId, "UNKNOWN_STATUS",
                        "Unknown job status: " + response.status());
                pollingStateMap.remove(serverId);
                return Mono.error(new RuntimeException("Unknown job status: " + response.status()));
        }
    }

    public PollingState getPollingState(String serverId) {
        return pollingStateMap.get(serverId);
    }

    public void cancelPolling(String serverId) {
        PollingState state = pollingStateMap.remove(serverId);
        if (state != null) {
            log.info("Cancelled polling for server {} (step: {})", serverId, state.getStep().getEndpoint());
        }
    }
}
