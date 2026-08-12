-- Complete the project contract already exposed by the React administration.
-- Small ordered case-study lists are stored as JSON text in the project row to
-- keep the public read path free from additional collection-table round trips.

ALTER TABLE project
    ADD COLUMN IF NOT EXISTS architecture_url VARCHAR(512),
    ADD COLUMN IF NOT EXISTS slug VARCHAR(100),
    ADD COLUMN IF NOT EXISTS proof_tags_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_problem TEXT,
    ADD COLUMN IF NOT EXISTS case_study_context TEXT,
    ADD COLUMN IF NOT EXISTS case_study_role TEXT,
    ADD COLUMN IF NOT EXISTS case_study_architecture TEXT,
    ADD COLUMN IF NOT EXISTS case_study_next_steps TEXT,
    ADD COLUMN IF NOT EXISTS case_study_technical_choices_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_challenges_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_solutions_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_outcomes_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_results_json TEXT NOT NULL DEFAULT '[]',
    ADD COLUMN IF NOT EXISTS case_study_limits_json TEXT NOT NULL DEFAULT '[]';


-- Persisted slugs are unique within a website version. The partial index keeps
-- legacy NULL rows migratable while the application protects their effective
-- title-derived slugs until they are next saved.
CREATE UNIQUE INDEX IF NOT EXISTS ux_project_version_slug
    ON project (website_version_id, lower(slug))
    WHERE slug IS NOT NULL;
