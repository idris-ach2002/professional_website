package sorbonne.professional_website.cv.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.cv.config.CvGenerationProperties;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LatexCompileService {

    private final CvGenerationProperties properties;
    private final Map<String, CompiledLatex> compileCache = new ConcurrentHashMap<>();

    public LatexCompileService(CvGenerationProperties properties) {
        this.properties = properties;
    }

    public CompiledLatex compile(String latexSource) {
        return compile(latexSource, List.of());
    }

    public CompiledLatex compile(String latexSource, List<CvLatexAsset> assets) {
        String compiler = normalizeCompiler(properties.getCompiler());
        String cacheKey = cacheKey(compiler, latexSource, assets);
        CompiledLatex cached = compileCache.get(cacheKey);
        if (cached != null) {
            List<String> warnings = new ArrayList<>(cached.warnings());
            warnings.add("Résultat réutilisé depuis le cache de compilation LaTeX.");
            return new CompiledLatex(cached.success(), cached.pdfBytes(), cached.logs(), warnings, cached.compiler());
        }
        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("portfolio-cv-latex-");
            Path texFile = tempDirectory.resolve("main.tex");
            Files.writeString(texFile, makeRuntimeCompatible(latexSource), StandardCharsets.UTF_8);
            writeAssets(tempDirectory, assets);

            ProcessResult result = runCompiler(compiler, tempDirectory, texFile);
            Path pdfFile = tempDirectory.resolve("main.pdf");
            boolean pdfExists = Files.exists(pdfFile) && Files.size(pdfFile) > 0;
            boolean success = result.exitCode() == 0 && pdfExists;

            byte[] pdfBytes = success ? Files.readAllBytes(pdfFile) : new byte[0];
            List<String> warnings = new ArrayList<>();
            if (!success) {
                warnings.add("Compilation LaTeX échouée. Consulte les logs retournés par le backend.");
                String missingInput = extractMissingInput(result.logs());
                if (missingInput != null) {
                    warnings.add("Fichier ou package LaTeX manquant détecté : " + missingInput);
                }
            }

            CompiledLatex compiled = new CompiledLatex(
                    success,
                    pdfBytes,
                    result.logs(),
                    warnings,
                    compiler
            );
            if (success) {
                compileCache.put(cacheKey, compiled);
                if (compileCache.size() > 32) {
                    compileCache.keySet().stream().findFirst().ifPresent(compileCache::remove);
                }
            }
            return compiled;
        } catch (IOException e) {
            return new CompiledLatex(
                    false,
                    new byte[0],
                    "Erreur d'exécution du compilateur LaTeX ou d'accès aux fichiers temporaires : " + e.getMessage(),
                    List.of(
                            "Vérifie que le compilateur LaTeX existe dans l'image Docker backend.",
                            "Si le message était 'Input length = 3', il venait d'un décodage strict des logs LaTeX ; le backend lit maintenant ces logs de façon tolérante."
                    ),
                    compiler
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CompiledLatex(
                    false,
                    new byte[0],
                    "Compilation LaTeX interrompue.",
                    List.of("La compilation a été interrompue."),
                    compiler
            );
        } finally {
            if (tempDirectory != null) {
                deleteRecursively(tempDirectory);
            }
        }
    }


    private String cacheKey(String compiler, String latexSource, List<CvLatexAsset> assets) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((compiler == null ? "latexmk" : compiler).getBytes(StandardCharsets.UTF_8));
            digest.update((latexSource == null ? "" : latexSource).getBytes(StandardCharsets.UTF_8));
            if (assets != null) {
                assets.stream()
                        .filter(asset -> asset != null && asset.filename() != null && asset.bytes() != null)
                        .sorted((left, right) -> left.filename().compareTo(right.filename()))
                        .forEach(asset -> {
                            digest.update(asset.filename().getBytes(StandardCharsets.UTF_8));
                            digest.update(asset.bytes());
                        });
            }
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            return String.valueOf((compiler + "::" + latexSource).hashCode());
        }
    }

    private String makeRuntimeCompatible(String latexSource) {
        String source = latexSource == null ? "" : latexSource;

        // Le template historique utilisait ulem uniquement pour souligner les liens.
        // Sur certaines images Debian/TeX Live de production, ulem.sty peut manquer
        // malgré une installation TeX Live large. On évite donc cette dépendance
        // pour garder la compilation Docker/Render stable.
        source = source.replaceAll("(?m)^\\\\usepackage(?:\\[[^\\]]*\\])?\\{ulem\\}\\s*\\R?", "");
        source = source.replaceAll("(?m)^\\\\renewcommand\\{\\\\ULdepth\\}\\{[^}]*\\}\\s*\\R?", "");
        source = source.replace("\\uline{", "\\underline{");

        return source;
    }

    private void writeAssets(Path workDirectory, List<CvLatexAsset> assets) throws IOException {
        if (assets == null || assets.isEmpty()) {
            return;
        }

        for (CvLatexAsset asset : assets) {
            if (asset == null || asset.filename() == null || asset.bytes() == null || asset.bytes().length == 0) {
                continue;
            }

            String filename = asset.filename().trim();
            if (!filename.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,95}") || filename.contains("..")) {
                continue;
            }

            Path target = workDirectory.resolve(filename).normalize();
            if (!target.startsWith(workDirectory)) {
                continue;
            }

            Files.write(target, asset.bytes());
        }
    }

    private ProcessResult runCompiler(String compiler, Path workDirectory, Path texFile)
            throws IOException, InterruptedException {
        List<String> command = switch (compiler) {
            case "tectonic" -> List.of(
                    "tectonic",
                    "--keep-logs",
                    "--keep-intermediates",
                    "--outdir", workDirectory.toString(),
                    texFile.toString()
            );
            case "pdflatex" -> List.of(
                    "pdflatex",
                    "-interaction=nonstopmode",
                    "-halt-on-error",
                    "-file-line-error",
                    "-no-shell-escape",
                    "main.tex"
            );
            default -> List.of(
                    "latexmk",
                    "-pdf",
                    "-interaction=nonstopmode",
                    "-halt-on-error",
                    "-file-line-error",
                    "-no-shell-escape",
                    "main.tex"
            );
        };

        ProcessResult firstPass = runSinglePass(command, workDirectory, "compile-1.log");

        if ("pdflatex".equals(compiler) && firstPass.exitCode() == 0) {
            ProcessResult secondPass = runSinglePass(command, workDirectory, "compile-2.log");
            return new ProcessResult(
                    secondPass.exitCode(),
                    firstPass.logs() + "\n" + secondPass.logs()
            );
        }

        return firstPass;
    }

    private ProcessResult runSinglePass(List<String> command, Path workDirectory, String logFilename)
            throws IOException, InterruptedException {
        Path logFile = workDirectory.resolve(logFilename);
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(workDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(logFile.toFile());

        Process process = processBuilder.start();
        boolean finished = process.waitFor(timeout(), TimeUnit.SECONDS);
        String logs = readTextLenient(logFile);

        if (!finished) {
            process.destroyForcibly();
            return new ProcessResult(124, logs + "\nCompilation arrêtée : timeout de " + timeout() + " seconde(s).");
        }

        return new ProcessResult(process.exitValue(), logs);
    }


    private String readTextLenient(Path file) throws IOException {
        if (!Files.exists(file)) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length == 0) {
            return "";
        }

        // Les logs TeX/LaTeX peuvent contenir des octets non UTF-8 selon la distribution,
        // les packages ou la locale Docker. Une lecture stricte via Files.readString(..., UTF_8)
        // peut alors lever MalformedInputException avec un message du type "Input length = 3".
        // Ce n'est pas une absence de latexmk : on décode donc les logs de manière tolérante
        // afin de retourner la vraie erreur LaTeX au front.
        try {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException ignored) {
            return StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPLACE)
                    .onUnmappableCharacter(CodingErrorAction.REPLACE)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        }
    }

    private String extractMissingInput(String logs) {
        if (logs == null || logs.isBlank()) {
            return null;
        }
        Pattern pattern = Pattern.compile("File `([^`]+)' not found|Missing input file '([^']+)'");
        Matcher matcher = pattern.matcher(logs);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
    }

    private String normalizeCompiler(String compiler) {
        if (compiler == null || compiler.isBlank()) {
            return "latexmk";
        }
        String normalized = compiler.trim().toLowerCase(Locale.ROOT);
        if (List.of("latexmk", "pdflatex", "tectonic").contains(normalized)) {
            return normalized;
        }
        return "latexmk";
    }

    private long timeout() {
        return Math.max(5, properties.getTimeoutSeconds());
    }

    private void deleteRecursively(Path path) {
        try (var stream = Files.walk(path)) {
            stream
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(item -> {
                        try {
                            Files.deleteIfExists(item);
                        } catch (IOException ignored) {
                            // Temporary build directory cleanup is best-effort.
                        }
                    });
        } catch (IOException ignored) {
            // Temporary build directory cleanup is best-effort.
        }
    }

    private record ProcessResult(int exitCode, String logs) {
    }
}
