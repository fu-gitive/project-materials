package os.patch.entity.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record PatchProgress(
        String serverId,
        String currentStep,
        String status,
        LocalDateTime startTime,
        LocalDateTime lastUpdateTime,
        String details,
        List<String> completedSteps
) {}