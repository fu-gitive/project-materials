package os.patch.entity.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PatchRequest(
        String os,
        String buildNumber,
        String serverId,
        String operation,
        String changeRecord,
        String apiRequester,
        String disiredPatchLevel
) {}
