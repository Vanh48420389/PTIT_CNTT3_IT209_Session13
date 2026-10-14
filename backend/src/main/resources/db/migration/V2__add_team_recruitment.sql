-- V2: Add team recruitment and application tables
ALTER TABLE posts
    ADD COLUMN is_recruiting BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN target_members INT NULL,
    ADD COLUMN skills_required VARCHAR(255) NULL,
    ADD COLUMN contact_info VARCHAR(255) NULL,
    ADD COLUMN recruitment_status VARCHAR(20) NOT NULL DEFAULT 'OPEN';

CREATE TABLE team_applications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    message VARCHAR(500) NOT NULL,
    contact_info VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED')),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_application_post_applicant UNIQUE (post_id, applicant_id),
    CONSTRAINT fk_applications_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_applicant FOREIGN KEY (applicant_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_applications_post ON team_applications(post_id);
CREATE INDEX idx_applications_applicant ON team_applications(applicant_id);
