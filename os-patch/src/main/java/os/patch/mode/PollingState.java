package os.patch.mode;

import lombok.Getter;
import lombok.Setter;
import os.patch.enums.PatchStep;

import java.time.Duration;

@Getter
@Setter
public class PollingState {
    private final PatchStep step;
    private final Long jobId;
    private final String serverId;
    private int currentAttempt;
    private final int maxAttempts;
    private final Duration pollingInterval;

    public PollingState(PatchStep step, Long jobId, String serverId) {
        this.step = step;
        this.jobId = jobId;
        this.serverId = serverId;
        this.currentAttempt = 0;
        this.maxAttempts = step.getMaxPollingAttempts();
        this.pollingInterval = step.getPollingInterval();
    }

    public boolean shouldContinuePolling() {
        return currentAttempt < maxAttempts;
    }

    public void incrementAttempt() {
        this.currentAttempt++;
    }

    public Duration getRemainingTime() {
        return pollingInterval.multipliedBy(maxAttempts - currentAttempt);
    }

    public String getProgress() {
        return String.format("%d/%d", currentAttempt, maxAttempts);
    }
}