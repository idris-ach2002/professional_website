-- The offer/application tracker was moved to a dedicated local project.
-- This migration removes the obsolete production table and sequence.
DROP TABLE IF EXISTS job_application CASCADE;
DROP SEQUENCE IF EXISTS job_application_seq;
