package sorbonne.professional_website.cv.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.cv.dto.CvCompileJobResponse;
import sorbonne.professional_website.cv.dto.CvCompileJobStatusResponse;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.cv.dto.CvGenerationResponse;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class CvCompileJobService {

    private final CvGenerationService cvGenerationService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public CvCompileJobService(CvGenerationService cvGenerationService) {
        this.cvGenerationService = cvGenerationService;
    }

    public CvCompileJobResponse startPreviewJob(Long ownerId, Long versionId, CvGenerationRequest request) {
        String jobId = UUID.randomUUID().toString();
        JobState state = new JobState(jobId, ownerId, versionId);
        jobs.put(jobId, state);

        executorService.submit(() -> {
            try {
                state.status = "GENERATING_LATEX";
                state.progress = 20;
                state.step = "Génération de la source LaTeX";

                state.status = "COMPILING";
                state.progress = 55;
                state.step = "Compilation LaTeX dans l'image backend";
                CvGenerationResponse response = cvGenerationService.preview(ownerId, versionId, request);

                state.response = response;
                state.logs = response.logs();
                state.warnings = response.warnings();
                state.compiler = response.compiler();
                state.pdfUrl = response.pdfUrl();
                state.latexSource = response.latexSource();
                state.status = response.success() ? "SUCCESS" : "FAILED";
                state.progress = 100;
                state.step = response.success() ? "PDF généré" : "Compilation échouée";
            } catch (Exception exception) {
                state.status = "FAILED";
                state.progress = 100;
                state.step = "Erreur backend durant la compilation";
                state.logs = exception.getMessage();
                state.warnings = List.of("Erreur de job CV : " + exception.getMessage());
            } finally {
                state.finishedAt = Instant.now();
            }
        });

        return new CvCompileJobResponse(jobId, state.status, state.step, ownerId, versionId);
    }

    public CvCompileJobStatusResponse readJob(String jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return new CvCompileJobStatusResponse(
                    jobId,
                    "NOT_FOUND",
                    100,
                    "Job introuvable ou expiré",
                    null,
                    null,
                    "",
                    List.of("Job introuvable."),
                    null,
                    null,
                    null
            );
        }

        return new CvCompileJobStatusResponse(
                state.jobId,
                state.status,
                state.progress,
                state.step,
                state.pdfUrl,
                state.latexSource,
                state.logs,
                state.warnings,
                state.compiler,
                state.ownerId,
                state.versionId
        );
    }

    private static class JobState {
        final String jobId;
        final Long ownerId;
        final Long versionId;
        String status = "QUEUED";
        int progress = 5;
        String step = "Job ajouté à la file";
        String pdfUrl;
        String latexSource;
        String logs = "";
        List<String> warnings = List.of();
        String compiler;
        CvGenerationResponse response;
        Instant finishedAt;

        JobState(String jobId, Long ownerId, Long versionId) {
            this.jobId = jobId;
            this.ownerId = ownerId;
            this.versionId = versionId;
        }
    }
}
