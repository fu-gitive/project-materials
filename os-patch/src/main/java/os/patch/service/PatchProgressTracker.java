package os.patch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import os.patch.entity.dto.PatchProgress;
import os.patch.mode.ServerPatchStatus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatchProgressTracker {
    private final Map<String, ServerPatchStatus> serverStatusMap = new ConcurrentHashMap<>();

    public void initializeServer(String serverId) {
        log.debug("Initializing patch progress tracking for server: {}", serverId);
        serverStatusMap.put(serverId, new ServerPatchStatus(serverId));
    }

    public void updateStep(String serverId, String step) {
        ServerPatchStatus status = serverStatusMap.get(serverId);
        if (status != null) {
            log.debug("Updating step for server {}: {}", serverId, step);
            status.setCurrentStep(step);
        }
    }

    public void updateStatus(String serverId, String status, String details) {
        ServerPatchStatus serverStatus = serverStatusMap.get(serverId);
        if (serverStatus != null) {
            log.debug("Updating status for server {}: {} - {}", serverId, status, details);
            serverStatus.setStatus(status);
            serverStatus.setDetails(details);
        }
    }

    public void addCompletedStep(String serverId, String step) {
        ServerPatchStatus status = serverStatusMap.get(serverId);
        if (status != null) {
            log.debug("Marking step as completed for server {}: {}", serverId, step);
            status.addCompletedStep(step);
        }
    }

    public void removeServer(String serverId) {
        log.debug("Removing server from progress tracking: {}", serverId);
        serverStatusMap.remove(serverId);
    }

    public PatchProgress getProgress(String serverId) {
        ServerPatchStatus status = serverStatusMap.get(serverId);
        if (status == null) {
            return null;
        }

        return PatchProgress.builder()
                .serverId(status.getServerId())
                .currentStep(status.getCurrentStep())
                .status(status.getStatus())
                .startTime(status.getStartTime())
                .lastUpdateTime(status.getLastUpdateTime())
                .details(status.getDetails())
                .completedSteps(List.copyOf(status.getCompletedSteps()))
                .build();
    }

    public List<PatchProgress> getAllProgress() {
        return serverStatusMap.values().stream()
                .map(status -> PatchProgress.builder()
                        .serverId(status.getServerId())
                        .currentStep(status.getCurrentStep())
                        .status(status.getStatus())
                        .startTime(status.getStartTime())
                        .lastUpdateTime(status.getLastUpdateTime())
                        .details(status.getDetails())
                        .completedSteps(List.copyOf(status.getCompletedSteps()))
                        .build())
                .collect(Collectors.toList());
    }

    public boolean isServerCompleted(String serverId) {
        ServerPatchStatus status = serverStatusMap.get(serverId);
        return status != null &&
                ("COMPLETED".equals(status.getStatus()) ||
                        "FAILED".equals(status.getStatus()));
    }

    public boolean isServerInProgress(String serverId) {
        ServerPatchStatus status = serverStatusMap.get(serverId);
        return status != null &&
                ("PENDING".equals(status.getStatus()) ||
                        "RUNNING".equals(status.getStatus()) ||
                        status.getStatus().contains("POLLING") ||
                        status.getStatus().contains("STEP"));
    }
}