package os.patch.enums;

import lombok.Getter;

import java.time.Duration;

@Getter
public enum PatchStep {
    SANITY_CHECKS("sanity-checks", true, Duration.ofSeconds(90), 5),
    PRE_REQUESTS("pre-requests", true, Duration.ofSeconds(100), 5),
    PATCH_REQUESTS("patch-requests", true, Duration.ofSeconds(720), 9),
    POST_REQUESTS("post-requests", true, Duration.ofSeconds(90), 7),
    OS_REPORTS("os-reports", true, Duration.ofSeconds(60), 5),
    JOB_STATUS("job-status", false, Duration.ofSeconds(30), 1);

    private final String endpoint;
    private final boolean isPostRequest;
    private final Duration pollingInterval;
    private final int maxPollingAttempts;

    PatchStep(String endpoint, boolean isPostRequest, Duration pollingInterval, int maxPollingAttempts) {
        this.endpoint = endpoint;
        this.isPostRequest = isPostRequest;
        this.pollingInterval = pollingInterval;
        this.maxPollingAttempts = maxPollingAttempts;
    }

    public static PatchStep[] getExecutionOrder() {
        return new PatchStep[]{
                SANITY_CHECKS, JOB_STATUS,
                PRE_REQUESTS, JOB_STATUS,
                PATCH_REQUESTS, JOB_STATUS,
                POST_REQUESTS, JOB_STATUS,
                OS_REPORTS
        };
    }
}