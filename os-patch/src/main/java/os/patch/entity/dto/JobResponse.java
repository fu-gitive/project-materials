package os.patch.entity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record JobResponse(
        @JsonProperty("job_id") Long jobId,
        String status,
        String started,
        String finished,
        JobOutput output
) {
    @Builder
    public record JobOutput(String details) {}
}

