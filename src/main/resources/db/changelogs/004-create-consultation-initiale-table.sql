--liquibase formatted sql

--changeset Mody:004-1-create-consultation-initiale
CREATE TABLE consultation_initiale
(
    id                                     UUID                     NOT NULL,
    patient_id                             UUID                     NOT NULL,
    medecin_senior_id                      BIGINT                   NOT NULL,
    date_consultation                      DATE,
    heure_consultation                     TIME,
    statut                                 VARCHAR(20)              NOT NULL DEFAULT 'BROUILLON',

    origine_demande                        VARCHAR(30),
    origine_demande_autre_precision        VARCHAR(500),
    motif_principal_consultation           TEXT,

    precisions_maladies_chroniques         TEXT,
    allergies_connues                      BOOLEAN,
    precisions_allergies_medicamenteuses   TEXT,
    hospitalisations_anterieures           BOOLEAN,
    chirurgies_anterieures                 BOOLEAN,
    details_hospitalisations_chirurgies    TEXT,

    niveau_autonomie                       VARCHAR(30),
    risque_isolement                       VARCHAR(20),
    niveau_precarite                       VARCHAR(20),
    besoins_sociaux_identifies             TEXT,

    resume_syndromique_cim10               TEXT,
    decision_eligibilite                   VARCHAR(20),
    motif_non_eligibilite                  TEXT,
    medecin_junior_affecte_id              BIGINT,
    frequence_visites_suivi                VARCHAR(20),
    plan_suivi_personnalise                TEXT,
    traitement_prescrit                    TEXT,
    examens_biologiques_prescrits          VARCHAR(20),
    date_prochaine_consultation_senior     DATE,
    delai_recommande                       VARCHAR(255),

    signature_medecin_senior               BOOLEAN                  NOT NULL DEFAULT FALSE,
    horodatage_signature_medecin_senior    TIMESTAMP WITH TIME ZONE,
    signature_point_focal_dass             BOOLEAN                  NOT NULL DEFAULT FALSE,
    horodatage_signature_point_focal_dass  TIMESTAMP WITH TIME ZONE,
    nom_point_focal_dass                   VARCHAR(255),
    visa_directeur_samu                    BOOLEAN                  NOT NULL DEFAULT FALSE,
    horodatage_visa_directeur_samu         TIMESTAMP WITH TIME ZONE,
    nom_directeur_samu                     VARCHAR(255),

    numero_fiche_dmi                       VARCHAR(100),
    date_saisie_dmi                        DATE,
    saisie_par                             VARCHAR(255),

    CONSTRAINT consultation_initiale_pkey PRIMARY KEY (id),
    CONSTRAINT fk_consultation_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_consultation_medecin_senior FOREIGN KEY (medecin_senior_id) REFERENCES medecin (id),
    CONSTRAINT fk_consultation_medecin_junior FOREIGN KEY (medecin_junior_affecte_id) REFERENCES medecin (id)
);

--changeset Mody:004-2-index-consultation-filters
CREATE INDEX idx_consultation_statut ON consultation_initiale (statut);
CREATE INDEX idx_consultation_decision_eligibilite ON consultation_initiale (decision_eligibilite);
CREATE INDEX idx_consultation_patient ON consultation_initiale (patient_id);
CREATE INDEX idx_consultation_medecin_junior ON consultation_initiale (medecin_junior_affecte_id);
