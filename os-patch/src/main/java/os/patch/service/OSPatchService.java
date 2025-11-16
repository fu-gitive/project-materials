package os.patch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import os.patch.entity.dto.JobResponse;
import os.patch.entity.dto.PatchRequest;
import os.patch.enums.PatchStep;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OSPatchService {
    private final PatchStepExecutor stepExecutor;
    private final PatchProgressTracker progressTracker;

    private final ConcurrentMap<String, JobResponse> stepJobResponses = new ConcurrentHashMap<>();

    @Async
    public CompletableFuture<Void> executePatchFlow(PatchRequest request) {
        String serverId = request.serverId();
        log.info("Starting patch flow for server: {}", serverId);
        progressTracker.initializeServer(serverId);

        return CompletableFuture
                .runAsync(() -> executeSequentialSteps(request, serverId))
                .whenComplete((result, error) -> {
                    stepExecutor.cancelPolling(serverId);
                    stepJobResponses.remove(serverId);

                    if (error != null) {
                        log.error("Patch flow failed for server {}: {}", serverId, error.getMessage());
                        progressTracker.updateStatus(serverId, "FAILED",
                                "Patch flow failed: " + error.getMessage());
                    } else {
                        log.info("Patch flow completed successfully for server: {}", serverId);
                        progressTracker.updateStatus(serverId, "COMPLETED",
                                "All patch steps completed successfully");
                    }
                });
    }

    private void executeSequentialSteps(PatchRequest request, String serverId) {
        PatchStep[] executionOrder = PatchStep.getExecutionOrder();

        for (int i = 0; i < executionOrder.length; i++) {
            PatchStep currentStep = executionOrder[i];
            try {
                if (currentStep.isPostRequest()) {
                    // 执行POST步骤
                    JobResponse stepResponse = stepExecutor.executeStep(currentStep, request, serverId)
                            .block();

                    if (stepResponse == null || stepResponse.jobId() == null) {
                        throw new RuntimeException("Failed to execute step: " + currentStep.getEndpoint());
                    }

                    // 存储job响应，供后续轮询使用
                    stepJobResponses.put(serverId + "_" + currentStep.getEndpoint(), stepResponse);
                    log.info("Step {} submitted with job ID: {}", currentStep.getEndpoint(), stepResponse.jobId());

                } else if (PatchStep.JOB_STATUS.equals(currentStep) && i > 0) {
                    // 查找前一个POST步骤来轮询其状态
                    PatchStep previousPostStep = findPreviousPostStep(executionOrder, i);
                    if (previousPostStep != null) {
                        String jobKey = serverId + "_" + previousPostStep.getEndpoint();
                        JobResponse stepResponse = stepJobResponses.get(jobKey);

                        if (stepResponse != null && stepResponse.jobId() != null) {
                            log.info("Starting polling for step {} with job ID: {}",
                                    previousPostStep.getEndpoint(), stepResponse.jobId());

                            JobResponse finalResponse = stepExecutor.pollJobStatus(
                                    previousPostStep, stepResponse.jobId(), serverId
                            ).block();

                            if (finalResponse == null || !"successful".equals(finalResponse.status())) {
                                throw new RuntimeException("Step " + previousPostStep.getEndpoint() +
                                        " did not complete successfully. Final status: " +
                                        (finalResponse != null ? finalResponse.status() : "null"));
                            }

                            log.info("Step {} completed successfully", previousPostStep.getEndpoint());
                        } else {
                            throw new RuntimeException("No job ID found for step: " + previousPostStep.getEndpoint());
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed at step " + currentStep.getEndpoint() +
                        " for server " + serverId, e);
            }
        }
    }

    private PatchStep findPreviousPostStep(PatchStep[] executionOrder, int currentIndex) {
        for (int i = currentIndex - 1; i >= 0; i--) {
            if (executionOrder[i].isPostRequest()) {
                return executionOrder[i];
            }
        }
        return null;
    }

    public CompletableFuture<Void> executeBatchPatch(List<PatchRequest> requests) {
        log.info("Starting batch patch for {} servers", requests.size());

        List<CompletableFuture<Void>> futures = requests.stream()
                .map(this::executePatchFlow)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Batch patch completed with errors: {}", error.getMessage());
                    } else {
                        log.info("Batch patch completed successfully for all servers");
                    }
                });
    }

    public List<PatchRequest> validateAndFilterRequests(List<PatchRequest> requests) {
        return requests.stream()
                .filter(request -> request.serverId() != null && !request.serverId().trim().isEmpty())
                .filter(request -> !progressTracker.isServerInProgress(request.serverId()))
                .toList();
    }

    public PollingState getPollingState(String serverId) {
        return stepExecutor.getPollingState(serverId);
    }
}