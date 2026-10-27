--liquibase formatted sql

-- Demandes de consultation initiale faites par un UTILISATEUR (patient ou proche), puis acceptees
-- ou refusees par un MEDECIN_SENIOR. Les comptes (demandeur, senior, junior) vivent dans le service
-- d'authentification : on ne garde que leur UUID (claim "sub" du badge), sans cle etrangere.

--changeset Mody:010-1-create-demande-consultation
CREATE TABLE demande_consultation
(
    id                UUID                     NOT NULL,
    demandeur_id      UUID                     NOT NULL,
    nom_patient       VARCHAR(200)             NOT NULL,
    pour_proche       BOOLEAN                  NOT NULL,
    motif             VARCHAR(2000)            NOT NULL,
    statut            VARCHAR(20)              NOT NULL DEFAULT 'EN_ATTENTE',
    medecin_senior_id UUID,
    junior_id         UUID,
    motif_refus       VARCHAR(1000),
    date_creation     TIMESTAMP WITH TIME ZONE NOT NULL,
    date_decision     TIMESTAMP WITH TIME ZONE,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT demande_consultation_pkey PRIMARY KEY (id),
    CONSTRAINT ck_demande_consultation_statut CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE'))
);

--changeset Mody:010-2-index-demande-consultation
CREATE INDEX idx_demande_consultation_demandeur ON demande_consultation (demandeur_id);
CREATE INDEX idx_demande_consultation_junior ON demande_consultation (junior_id);
CREATE INDEX idx_demande_consultation_statut ON demande_consultation (statut);
