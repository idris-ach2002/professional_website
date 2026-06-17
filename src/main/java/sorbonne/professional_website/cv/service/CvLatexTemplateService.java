package sorbonne.professional_website.cv.service;

import org.springframework.stereotype.Service;
import sorbonne.professional_website.cv.dto.CvGenerationRequest;
import sorbonne.professional_website.entity.ContactInfo;
import sorbonne.professional_website.entity.Experience;
import sorbonne.professional_website.entity.Owner;
import sorbonne.professional_website.entity.Profile;
import sorbonne.professional_website.entity.Project;
import sorbonne.professional_website.entity.Timeline;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.entity.enumerations.CategoryExperience;
import sorbonne.professional_website.entity.enumerations.Contact;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CvLatexTemplateService {

    private static final DateTimeFormatter MONTH_YEAR_FORMATTER = DateTimeFormatter
            .ofPattern("MMM. yyyy", Locale.FRENCH);

    public String buildLatex(WebsiteVersion version, CvGenerationRequest request) {
        Owner owner = version.getOwner();
        Profile profile = version.getProfile();
        Timeline timeline = version.getTimeline();

        List<Experience> allExperiences = safeList(timeline == null ? null : timeline.getExperiences());
        List<Experience> education = sortedExperiences(allExperiences).stream()
                .filter(experience -> experience.getCategory() == CategoryExperience.SCHOOL)
                .toList();
        List<Experience> professionalExperiences = sortedExperiences(allExperiences).stream()
                .filter(experience -> experience.getCategory() != CategoryExperience.SCHOOL)
                .limit(limitOrDefault(request == null ? null : request.experienceLimit(), 4))
                .toList();

        List<Project> projects = safeList(version.getProjects()).stream()
                .filter(project -> project.getPublished() == null || Boolean.TRUE.equals(project.getPublished()))
                .sorted(Comparator
                        .comparing((Project project) -> Boolean.TRUE.equals(project.getFeatured())).reversed()
                        .thenComparing(project -> project.getDisplayOrder() == null ? Integer.MAX_VALUE : project.getDisplayOrder())
                        .thenComparing(Project::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(limitOrDefault(request == null ? null : request.projectLimit(), 5))
                .toList();

        String primaryColor = normalizeHex(request == null || request.theme() == null ? null : request.theme().primaryColor(), "6E877E");
        String density = request == null || request.theme() == null || isBlank(request.theme().density())
                ? "compact"
                : request.theme().density();
        String headlineOverride = request == null || request.theme() == null ? null : request.theme().headline();
        boolean compact = !"detailed".equalsIgnoreCase(density);

        String fullName = joinNonBlank(" ", value(owner == null ? null : owner.getFirstName()), value(owner == null ? null : owner.getName()));
        String title = firstNonBlank(profile == null ? null : profile.getTitle(), "Développeur Java / Spring Boot");
        String subtitle = firstNonBlank(profile == null ? null : profile.getSubtitle(), "Java · Spring Boot · React");
        String headline = firstNonBlank(headlineOverride, profile == null ? null : profile.getShortDescription(), profile == null ? null : profile.getHeadline(), "Profil orienté qualité logicielle, architecture et impact utilisateur.");

        StringBuilder latex = new StringBuilder();
        appendPreamble(latex, primaryColor, compact);
        appendHeader(latex, owner, profile, fullName, title, subtitle, headline);
        appendMainColumns(latex, owner, profile, professionalExperiences, education, projects);
        latex.append("\\end{document}\n");
        return latex.toString();
    }

    private void appendPreamble(StringBuilder out, String primaryColor, boolean compact) {
        out.append("\\documentclass[11pt,a4paper]{article}\n")
                .append("\\usepackage[a4paper,margin=0.68cm]{geometry}\n")
                .append("\\usepackage[T1]{fontenc}\n")
                .append("\\usepackage[utf8]{inputenc}\n")
                .append("\\usepackage[french]{babel}\n")
                .append("\\usepackage[scaled=0.97]{helvet}\n")
                .append("\\renewcommand{\\familydefault}{\\sfdefault}\n")
                .append("\\usepackage[protrusion=true,expansion=false]{microtype}\n")
                .append("\\usepackage{xcolor}\n")
                .append("\\usepackage{enumitem}\n")
                .append("\\usepackage{graphicx}\n")
                .append("\\usepackage[most]{tcolorbox}\n")
                .append("\\usepackage[hidelinks]{hyperref}\n")
                .append("\\usepackage{ragged2e}\n")
                .append("\\usepackage{fontawesome5}\n")
                .append("\\usepackage{tabularx}\n")
                .append("\\usepackage{array}\n")
                .append("\\usepackage{tikz}\n")
                .append("\\usetikzlibrary{calc}\n")
                .append("\\pagestyle{empty}\n")
                .append("\\setlength{\\parindent}{0pt}\n")
                .append("\\setlength{\\parskip}{0pt}\n")
                .append("\\setlength{\\tabcolsep}{0pt}\n")
                .append("\\setlist[itemize]{leftmargin=10pt,itemsep=")
                .append(compact ? "1.4pt,topsep=1.4pt" : "2.2pt,topsep=2.2pt")
                .append(",parsep=0pt,partopsep=0pt}\n")
                .append("\\definecolor{ink}{HTML}{1C232A}\n")
                .append("\\definecolor{ink2}{HTML}{24313A}\n")
                .append("\\definecolor{accent}{HTML}{").append(primaryColor).append("}\n")
                .append("\\definecolor{muted}{HTML}{667680}\n")
                .append("\\definecolor{line}{HTML}{D8E1E4}\n")
                .append("\\definecolor{soft}{HTML}{F6F8F8}\n")
                .append("\\definecolor{chip}{HTML}{E9EFED}\n")
                .append("\\definecolor{cardbg}{HTML}{FCFDFD}\n")
                .append("\\color{ink}\n")
                .append("\\hypersetup{colorlinks=true,urlcolor=accent}\n")
                .append("\\tcbset{sharp corners,boxrule=0pt,left=8pt,right=8pt,top=6pt,bottom=6pt}\n")
                .append("\\urlstyle{same}\n")
                .append("\\newcommand{\\cvhref}[2]{\\href{#1}{\\textcolor{accent!95!black}{\\underline{#2}}}}\n")
                .append("\\newcommand{\\cvhreflight}[2]{\\href{#1}{\\textcolor{white!92}{\\underline{#2}}}}\n")
                .append("\\newcommand{\\sectiontitle}[1]{{\\color{accent!95!black}\\bfseries\\fontsize{15.8}{16.3}\\selectfont #1}\\par\\vspace{1pt}{\\color{line}\\rule{\\linewidth}{0.65pt}}\\par\\vspace{2pt}}\n")
                .append("\\newcommand{\\tagitem}[1]{\\tikz[baseline=(x.base)]\\node[rounded corners=5pt,fill=chip,draw=line,inner xsep=6pt,inner ysep=3pt,font=\\fontsize{9}{9}\\selectfont\\bfseries,text=ink] (x) {#1};}\n")
                .append("\\newcommand{\\softchip}[1]{\\tikz[baseline=(x.base)]\\node[rounded corners=5pt,fill=white,draw=line,inner xsep=5pt,inner ysep=4pt,font=\\fontsize{9.4}{9.6}\\selectfont\\bfseries,text=ink] (x) {#1};}\n")
                .append("\\newcommand{\\contactrow}[3]{{\\color{white}\\fontsize{9.6}{10}\\selectfont\\makebox[1.15em][c]{\\color{accent!55}#1}\\hspace{0.1em}\\textbf{#2}\\hspace{0.1em}#3}\\par\\vspace{1.7pt}}\n")
                .append("\\newcommand{\\skillcard}[4]{\\begin{tcolorbox}[colback=cardbg,colframe=line,boxrule=0.62pt,arc=2.2mm,left=7pt,right=7pt,top=4.8pt,bottom=8pt]{\\bfseries\\fontsize{11.4}{11.6}\\selectfont #1}\\par{\\color{muted}\\fontsize{9.5}{9.7}\\selectfont #2}\\par\\vspace{2pt}{\\fontsize{9.6}{9.9}\\selectfont #3}\\par\\vspace{5pt}{\\color{muted}\\fontsize{8.7}{9}\\selectfont \\textbf{Preuves :} #4}\\par\\end{tcolorbox}\\vspace{6pt}}\n")
                .append("\\newcommand{\\experienceentry}[4]{\\noindent\\begin{tabularx}{\\linewidth}{@{}X>{\\RaggedLeft\\arraybackslash}p{2.3cm}@{}}{\\bfseries\\fontsize{11.4}{11.6}\\selectfont #1} & {\\color{accent!95!black}\\bfseries\\fontsize{10}{10.2}\\selectfont #2}\\end{tabularx}\\par{\\color{muted}\\fontsize{9.8}{10}\\selectfont #3}\\par\\vspace{4pt}{\\fontsize{9.9}{10.3}\\selectfont #4}\\vspace{10pt}}\n")
                .append("\\newcommand{\\projectentry}[4]{{\\bfseries\\fontsize{11.2}{11.4}\\selectfont #1\\hfill\\color{muted}\\fontsize{9.5}{9.7}\\selectfont #2}\\par{\\fontsize{9.8}{10.2}\\selectfont #3}\\par{\\color{muted}\\fontsize{9.2}{9.5}\\selectfont #4}\\par\\vspace{8pt}}\n")
                .append("\\newcommand{\\entrytitle}[3]{\\noindent\\begin{tabularx}{\\linewidth}{@{}X>{\\RaggedLeft\\arraybackslash}p{2.35cm}@{}}{\\bfseries\\fontsize{11.2}{11.4}\\selectfont #1} & {\\color{accent!95!black}\\bfseries\\fontsize{9.5}{9.8}\\selectfont #2}\\end{tabularx}\\par{\\color{muted}\\fontsize{9.5}{9.8}\\selectfont #3}\\par\\vspace{1.4pt}}\n")
                .append("\\newcommand{\\langrow}[2]{\\noindent\\begin{tabularx}{\\linewidth}{@{}X r@{}}{\\bfseries\\fontsize{9.8}{10}\\selectfont #1} & {\\color{muted}\\fontsize{9.6}{9.8}\\selectfont #2}\\end{tabularx}\\par\\vspace{0.8pt}}\n")
                .append("\\newlength{\\headerpad}\\setlength{\\headerpad}{12pt}\n")
                .append("\\newlength{\\headergap}\\setlength{\\headergap}{12pt}\n")
                .append("\\newlength{\\headerinnerheight}\\setlength{\\headerinnerheight}{3.2cm}\n")
                .append("\\begin{document}\n")
                .append("\\enlargethispage{1.2cm}\n");
    }

    private void appendHeader(StringBuilder out, Owner owner, Profile profile, String fullName, String title, String subtitle, String headline) {
        String phone = contactValue(owner, Contact.PHONE_NUMBER, "");
        String email = contactValue(owner, Contact.EMAIL, "");
        String linkedin = contactValue(owner, Contact.LINKEDIN, "");
        String github = contactValue(owner, Contact.GITHUB, "");
        String portfolio = firstNonBlank(profile == null ? null : profile.getPortfolioUrl(), contactValue(owner, Contact.PORTFOLIO, ""));
        String address = firstNonBlank(profile == null ? null : profile.getLocation(), owner == null ? null : owner.getAddress());
        Integer age = owner == null ? null : owner.getAge();

        out.append("\\begin{tcolorbox}[enhanced,colback=ink,colframe=ink,arc=4mm,left=\\headerpad,right=\\headerpad,top=\\headerpad,bottom=\\headerpad,overlay={\\shade[inner color=ink2!60!ink, outer color=ink] (frame.north west) rectangle (frame.south east);\\begin{scope}[shift={(frame.south east)}, xshift=-2cm, yshift=1cm]\\foreach \\i in {1,...,15}{\\pgfmathsetseed{\\i*7}\\pgfmathsetmacro{\\xa}{rand*3.5}\\pgfmathsetmacro{\\ya}{rand*1.5}\\pgfmathsetmacro{\\xb}{rand*3.5}\\pgfmathsetmacro{\\yb}{rand*1.5}\\draw[accent, opacity=0.12, line width=0.4pt] (\\xa,\\ya) -- (\\xb,\\yb);\\fill[accent!60, opacity=0.2] (\\xa,\\ya) circle (1.2pt);}\\end{scope}\\draw[accent, opacity=0.3, line width=1pt] ([xshift=15pt, yshift=-5pt]frame.north west) -- ([xshift=60pt, yshift=-5pt]frame.north west);}]\n")
                .append("\\begin{tabularx}{\\linewidth}{@{}m{2.7cm}@{\\hspace{\\headergap}}X@{\\hspace{\\headergap}}>{\\raggedleft\\arraybackslash}m{4.8cm}@{}}\n")
                .append("\\begin{minipage}[c][\\headerinnerheight][c]{2.7cm}\\centering\n")
                .append("\\begin{tikzpicture}\\draw[accent, line width=1.5pt] (0,0) circle (1.25cm);\\draw[white!20, line width=0.5pt] (0,0) circle (1.35cm);\\fill[white!10] (-1.15,-1.15) rectangle (1.15,1.15);\\node[white!82,align=center] at (0,0) {\\fontsize{9}{9}\\selectfont CV\\\\PDF};\\fill[green!60!accent] (0.85,-0.85) circle (5pt);\\draw[ink, line width=1.5pt] (0.85,-0.85) circle (5pt);\\end{tikzpicture}\n")
                .append("\\end{minipage} &\n")
                .append("\\begin{minipage}[c][\\headerinnerheight][c]{\\linewidth}\n")
                .append("{\\fontsize{23}{23}\\selectfont\\color{white}\\bfseries ").append(tex(fullName)).append("}\\par\n")
                .append("\\vspace{4pt}\n")
                .append("{\\fontsize{14.6}{15.4}\\selectfont\\color{accent!90}\\bfseries\\MakeUppercase{").append(tex(title)).append("}}\\par\n")
                .append("\\vspace{3pt}\n")
                .append("{\\color{accent!72}\\fontsize{11}{11.4}\\selectfont\\bfseries ").append(tex(subtitle)).append("}\\par\n")
                .append("\\vspace{4pt}\n")
                .append("{\\color{white!86}\\fontsize{9.6}{10.4}\\selectfont ").append(tex(headline)).append("}\\par\n")
                .append("\\vspace{6pt}\n")
                .append("\\tagitem{\\faCalendar* \\hspace{2pt} Sept. 2026}\\hspace{0.35cm}\\tagitem{\\faCalendar* \\hspace{2pt} 12 mois}");
        if (age != null && age > 0) {
            out.append("\\hspace{0.35cm}\\tagitem{\\faBirthdayCake \\hspace{2pt} ").append(age).append(" ans}");
        }
        out.append("\n\\end{minipage} &\n")
                .append("\\begin{minipage}[c][\\headerinnerheight][c]{4.8cm}\\raggedleft\n");
        if (!isBlank(phone)) out.append("\\contactrow{\\faPhone}{}{").append(tex(phone)).append("}\n");
        if (!isBlank(address)) out.append("\\contactrow{\\faHome}{}{").append(tex(address)).append("}\n");
        if (!isBlank(email)) out.append("\\contactrow{\\faEnvelope}{}{\\cvhreflight{").append(url(email.startsWith("mailto:") ? email : "mailto:" + email)).append("}{").append(tex(email.replace("mailto:", ""))).append("}}\n");
        if (!isBlank(linkedin)) out.append("\\contactrow{\\faLinkedin}{LinkedIn}{\\cvhreflight{").append(url(linkedin)).append("}{/idris-achabou}}\n");
        if (!isBlank(github)) out.append("\\contactrow{\\faGithub}{GitHub}{\\cvhreflight{").append(url(github)).append("}{/idris-ach2002}}\n");
        if (!isBlank(portfolio)) out.append("\\contactrow{\\faGlobe}{Portfolio}{\\cvhreflight{").append(url(portfolio)).append("}{site-web}}\n");
        out.append("\\vspace{2pt}\\tikz \\draw[accent, line width=1pt] (0,0) -- (-1.5,0);\n")
                .append("\\end{minipage}\n")
                .append("\\end{tabularx}\n")
                .append("\\end{tcolorbox}\n")
                .append("\\vspace{0.04cm}\n");
    }

    private void appendMainColumns(StringBuilder out, Owner owner, Profile profile, List<Experience> experiences, List<Experience> education, List<Project> projects) {
        out.append("\\noindent\\makebox[\\textwidth][c]{\\scalebox{0.74}{\\begin{minipage}{1.351\\textwidth}\n")
                .append("\\noindent\n")
                .append("\\begin{minipage}[t]{0.338\\textwidth}\\vspace{0pt}\n");
        appendSidebar(out, owner, profile, projects, experiences);
        out.append("\\end{minipage}\n")
                .append("\\hfill\n")
                .append("\\begin{minipage}[t]{0.642\\textwidth}\\vspace{0pt}\n")
                .append("\\begin{tcolorbox}[colback=white,colframe=line,boxrule=0.75pt,arc=2.8mm,top=7pt,bottom=8pt,left=8pt,right=8pt]\n");
        appendExperienceSection(out, experiences);
        appendProjectsSection(out, projects);
        appendEducationSection(out, education);
        out.append("\\end{tcolorbox}\n")
                .append("\\end{minipage}\n")
                .append("\\end{minipage}}}\n");
    }

    private void appendSidebar(StringBuilder out, Owner owner, Profile profile, List<Project> projects, List<Experience> experiences) {
        out.append("\\begin{tcolorbox}[colback=soft,arc=2.8mm]\n")
                .append("\\sectiontitle{Langues}\n")
                .append("\\langrow{Français}{Bilingue} \\vspace{0.14cm}\n")
                .append("\\langrow{Anglais}{B2} \\vspace{0.14cm}\n")
                .append("\\langrow{Kabyle}{Langue maternelle}\n")
                .append("\\end{tcolorbox}\n")
                .append("\\vspace{0.25cm}\n")
                .append("\\begin{tcolorbox}[colback=soft,arc=2.8mm]\n")
                .append("\\sectiontitle{Compétences clés}\n");

        appendSkillCards(out, projects, experiences);

        out.append("\\end{tcolorbox}\n")
                .append("\\vspace{0.25cm}\n")
                .append("\\begin{tcolorbox}[colback=soft,arc=2.8mm]\n")
                .append("\\sectiontitle{Qualités}\n")
                .append("\\begin{center}\n")
                .append("\\softchip{Autonomie}\\hspace{6pt}\\softchip{Rigueur / qualité}\\par\\vspace{2.2pt}\n")
                .append("\\softchip{Engagement}\\hspace{6pt}\\softchip{Priorisation}\\par\\vspace{2.2pt}\n")
                .append("\\softchip{Communication technique}\\par\\vspace{2.2pt}\n")
                .append("\\softchip{Travail en équipe}\\par\\vspace{2.2pt}\n")
                .append("\\softchip{Sens des responsabilités}\n")
                .append("\\end{center}\n")
                .append("\\end{tcolorbox}\n");
    }

    private void appendSkillCards(StringBuilder out, List<Project> projects, List<Experience> experiences) {
        List<String> allStacks = projects.stream()
                .flatMap(project -> safeList(project.getStacks()).stream())
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .toList();

        appendSkillCard(out, "Java Backend · Spring Boot", "Java 21, Spring Boot, JPA/Hibernate, Flyway", "Services REST, persistance PostgreSQL, migrations, validation des entrées, structuration multicouche et robustesse applicative.", findProof(projects, "professional_website", "ais"));
        appendSkillCard(out, "Web applicatif · Interfaces métier", "React, Mantine, Tailwind, Symfony, Twig", "Interfaces orientées utilisateurs, formulaires, upload, prévisualisation PDF, pagination, export et admin panel.", findProof(projects, "front", "AIS_WEBSITE"));
        appendSkillCard(out, "Base de données · SQL", "PostgreSQL, SQL, JPA, JSON / XML", "MCD / MLD, normalisation 2NF/3NF/BCNF, requêtes avancées, triggers SQL et données structurées.", findProof(projects, "bdd", "portfolio"));
        appendSkillCard(out, "Qualité logicielle · Tests", joinLimited(allStacks, 5, "JUnit, QuickCheck, Hspec, Invariants"), "Tests unitaires et de propriétés, validation d'invariants, analyse d'anomalies et fiabilisation des traitements.", findProof(projects, "Megablast", "BCM"));
    }

    private void appendSkillCard(StringBuilder out, String title, String stack, String description, String proof) {
        out.append("\\skillcard\n  {").append(tex(title)).append("}\n  {").append(tex(stack)).append("}\n  {").append(tex(description)).append("}\n  {").append(proof).append("}\n\n");
    }

    private void appendExperienceSection(StringBuilder out, List<Experience> experiences) {
        out.append("\\sectiontitle{Expérience pertinente}\n");
        if (experiences.isEmpty()) {
            out.append("{\\color{muted}Aucune expérience professionnelle renseignée.}\\par\\vspace{6pt}\n");
            return;
        }
        for (Experience experience : experiences) {
            String title = joinNonBlank(" ", value(experience.getTitle()), "|", value(experience.getOrganization()));
            String dates = dateRange(experience.getStartDate(), experience.getEndDate(), experience.isCurrentPosition());
            String subtitle = joinNonBlank(" · ", value(experience.getLocation()), joinLimited(experience.getSkills(), 5, ""));
            String body = bulletize(firstNonBlank(experience.getDescription(), experience.getSummary()), 3);
            out.append("\\experienceentry\n  {").append(tex(title)).append("}\n  {").append(tex(dates)).append("}\n  {").append(tex(subtitle)).append("}\n  {").append(body).append("}\n");
        }
    }

    private void appendProjectsSection(StringBuilder out, List<Project> projects) {
        out.append("\\sectiontitle{Projets ciblés}\n");
        if (projects.isEmpty()) {
            out.append("{\\color{muted}Aucun projet publié renseigné.}\\par\\vspace{6pt}\n");
            return;
        }
        for (Project project : projects) {
            String title = project.getTitle();
            String stack = joinLimited(project.getStacks(), 6, value(project.getSubtitle()));
            String description = firstNonBlank(project.getShortDescription(), project.getDescription());
            String proof = projectProof(project);
            out.append("\\projectentry\n  {").append(tex(title)).append("}\n  {").append(tex(stack)).append("}\n  {").append(tex(description)).append("}\n  {").append(proof).append("}\n");
        }
    }

    private void appendEducationSection(StringBuilder out, List<Experience> education) {
        out.append("\\sectiontitle{Formation}\n");
        if (education.isEmpty()) {
            out.append("{\\color{muted}Aucune formation renseignée.}\\par\n");
            return;
        }
        for (Experience school : education) {
            String dates = dateRange(school.getStartDate(), school.getEndDate(), school.isCurrentPosition());
            String subtitle = joinNonBlank(" · ", value(school.getOrganization()), value(school.getLocation()));
            String body = firstNonBlank(school.getSummary(), school.getDescription());
            out.append("\\entrytitle{").append(tex(school.getTitle())).append("}{").append(tex(dates)).append("}{").append(tex(subtitle)).append("}\n")
                    .append("{\\fontsize{9.8}{10.2}\\selectfont ").append(tex(body)).append("}\\par\\vspace{7pt}\n");
        }
    }

    private String findProof(List<Project> projects, String firstKeyword, String fallbackKeyword) {
        return projects.stream()
                .filter(project -> containsIgnoreCase(project.getTitle(), firstKeyword) || containsIgnoreCase(project.getTitle(), fallbackKeyword))
                .findFirst()
                .map(this::projectProof)
                .orElse(tex(firstNonBlank(firstKeyword, fallbackKeyword)));
    }

    private String projectProof(Project project) {
        String github = value(project.getGithubUrl());
        String documentation = value(project.getDocumentationUrl());
        String label = firstNonBlank(project.getTitle(), "repo GitHub");
        if (!isBlank(github)) {
            return "\\cvhref{" + url(github) + "}{" + tex(shortLabel(label)) + "}";
        }
        if (!isBlank(documentation)) {
            return "\\cvhref{" + url(documentation) + "}{Documentation}";
        }
        return tex(shortLabel(label));
    }

    private String bulletize(String text, int maxItems) {
        if (isBlank(text)) {
            return "";
        }
        String[] parts = text
                .replace("\r", " ")
                .split("(?<=[.!?])\\s+|\\n+|\\s+—\\s+");
        List<String> items = new ArrayList<>();
        for (String part : parts) {
            String cleaned = part.trim();
            if (!cleaned.isBlank()) {
                items.add(cleaned);
            }
            if (items.size() >= maxItems) break;
        }
        if (items.size() <= 1) {
            return tex(items.isEmpty() ? text : items.get(0));
        }
        return "\\begin{itemize}\n" + items.stream()
                .map(item -> "\\item " + tex(item))
                .collect(Collectors.joining("\n")) + "\n\\end{itemize}";
    }

    private List<Experience> sortedExperiences(List<Experience> experiences) {
        return experiences.stream()
                .sorted(Comparator
                        .comparing((Experience experience) -> experience.getDisplayOrder() == null ? Integer.MAX_VALUE : experience.getDisplayOrder())
                        .thenComparing(Experience::getStartDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private String dateRange(LocalDate start, LocalDate end, boolean current) {
        if (start == null && end == null) return current ? "présent" : "";
        String startText = start == null ? "" : start.format(MONTH_YEAR_FORMATTER);
        String endText = current ? "présent" : (end == null ? "" : end.format(MONTH_YEAR_FORMATTER));
        if (isBlank(startText)) return endText;
        if (isBlank(endText)) return startText;
        return startText + " - " + endText;
    }

    private String contactValue(Owner owner, Contact type, String fallback) {
        if (owner == null || owner.getContacts() == null) {
            return fallback;
        }
        return owner.getContacts().stream()
                .filter(contact -> contact != null && contact.getType() == type)
                .map(ContactInfo::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(fallback);
    }

    private int limitOrDefault(Integer value, int fallback) {
        if (value == null || value <= 0) return fallback;
        return Math.min(value, 20);
    }

    private String joinLimited(List<String> values, int max, String fallback) {
        String joined = safeList(values).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .limit(max)
                .collect(Collectors.joining(", "));
        return joined.isBlank() ? fallback : joined;
    }

    private String joinNonBlank(String delimiter, String... values) {
        return List.of(values).stream()
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(delimiter));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return "";
    }

    private String shortLabel(String value) {
        String cleaned = value == null ? "projet" : value.trim();
        if (cleaned.length() <= 34) return cleaned;
        return cleaned.substring(0, 31) + "...";
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean containsIgnoreCase(String value, String needle) {
        if (value == null || needle == null || needle.isBlank()) return false;
        return value.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeHex(String raw, String fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        String candidate = raw.trim().replace("#", "").replaceAll("[^A-Fa-f0-9]", "");
        if (candidate.length() == 6) return candidate.toUpperCase(Locale.ROOT);
        return fallback;
    }

    private String tex(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\textbackslash{}")
                .replace("&", "\\&")
                .replace("%", "\\%")
                .replace("$", "\\$")
                .replace("#", "\\#")
                .replace("_", "\\_")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace("~", "\\textasciitilde{}")
                .replace("^", "\\textasciicircum{}")
                .replace("|", "\\textbar{}")
                .replace("<", "\\textless{}")
                .replace(">", "\\textgreater{}");
    }

    private String url(String value) {
        if (value == null) return "";
        return value.trim().replace("}", "").replace("{", "");
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
