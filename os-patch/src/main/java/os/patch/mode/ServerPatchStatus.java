package os.patch.mode;

import lombok.Getter;
import lombok.Setter;
import os.patch.entity.dto.JobResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class ServerPatchStatus {
    private final String serverId;
    private final AtomicReference<String> currentStep = new AtomicReference<>("Pending");
    private final AtomicReference<String> status = new AtomicReference<>("PENDING");
    private final LocalDateTime startTime;
    private final AtomicReference<LocalDateTime> lastUpdateTime;
    private final AtomicReference<String> details = new AtomicReference<>("");
    private final List<String> completedSteps = new CopyOnWriteArrayList<>();

    @Setter
    private AtomicReference<JobResponse> lastJobResponse = new AtomicReference<>();

    public ServerPatchStatus(String serverId) {
        this.serverId = serverId;
        this.startTime = LocalDateTime.now();
        this.lastUpdateTime = new AtomicReference<>(startTime);
    }

    public void setCurrentStep(String step) {
        this.currentStep.set(step);
        updateTimestamp();
    }

    public void setStatus(String status) {
        this.status.set(status);
        updateTimestamp();
    }

    public void setDetails(String details) {
        this.details.set(details);
        updateTimestamp();
    }

    public void addCompletedStep(String step) {
        completedSteps.add(step);
        updateTimestamp();
    }

    private void updateTimestamp() {
        this.lastUpdateTime.set(LocalDateTime.now());
    }
}