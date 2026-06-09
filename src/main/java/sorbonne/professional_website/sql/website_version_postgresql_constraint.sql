-- PostgreSQL hard guarantee: only one active website version per owner.
-- JPA cannot express this partial unique index portably.

CREATE UNIQUE INDEX IF NOT EXISTS uk_one_active_website_version_per_owner
ON website_version(owner_id)
WHERE active = true;
