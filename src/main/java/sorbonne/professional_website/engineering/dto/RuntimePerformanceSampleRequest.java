package sorbonne.professional_website.engineering.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RuntimePerformanceSampleRequest(
        @NotBlank @Size(max = 120) String buildId,
        @NotBlank @Size(max = 24) String runtimeProfile,
        @NotBlank @Size(max = 24) String memoryState,
        @DecimalMin("0") @DecimalMax("300") Double fps,
        @DecimalMin("0") @DecimalMax("10000") Double frameP95Ms,
        @Min(0) @Max(100000) Integer longTaskCount,
        @DecimalMin("0") @DecimalMax("60000") Double workerLatencyMs,
        @DecimalMin("0") @DecimalMax("60000") Double apiLatencyMs,
        @Min(0) @Max(100000) Integer activeResources
) { }
