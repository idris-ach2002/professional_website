package sorbonne.professional_website.cv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("cv.generation")
public class CvGenerationProperties {

    /**
     * Command used to compile LaTeX. Supported values: latexmk, pdflatex, tectonic.
     */
    private String compiler = "latexmk";

    /**
     * Maximum compilation duration before killing the process.
     */
    private long timeoutSeconds = 45;

    /**
     * Keep the generated .tex source next to the generated PDF when using save.
     */
    private boolean storeLatexSource = true;

    public String getCompiler() {
        return compiler;
    }

    public void setCompiler(String compiler) {
        this.compiler = compiler;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isStoreLatexSource() {
        return storeLatexSource;
    }

    public void setStoreLatexSource(boolean storeLatexSource) {
        this.storeLatexSource = storeLatexSource;
    }
}
