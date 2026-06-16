package sorbonne.professional_website.cv.service;

import java.util.List;

record CompiledLatex(
        boolean success,
        byte[] pdfBytes,
        String logs,
        List<String> warnings,
        String compiler
) {
}
