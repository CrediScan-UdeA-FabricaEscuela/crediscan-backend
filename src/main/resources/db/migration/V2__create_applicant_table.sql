CREATE TABLE applicant (
    id UUID PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    identification_encrypted VARCHAR(700) NOT NULL,
    identification_hash VARCHAR(128) NOT NULL,
    birth_date DATE NOT NULL,
    employment_type VARCHAR(30) NOT NULL,
    monthly_income NUMERIC(19,2) NOT NULL,
    work_experience_months INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE,
    created_by VARCHAR(100) NOT NULL,
    updated_by VARCHAR(100)
);

CREATE UNIQUE INDEX uk_applicant_identification_hash
    ON applicant (identification_hash);
