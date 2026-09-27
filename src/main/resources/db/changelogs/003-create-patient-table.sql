--liquibase formatted sql

--changeset Mody:003-1-create-patient
CREATE TABLE patient
(
    id                             UUID         NOT NULL,
    numero_dossier_proxymed        VARCHAR(100) NOT NULL,
    numero_dmi                     VARCHAR(100),
    nom_complet                    VARCHAR(255) NOT NULL,
    date_naissance                 DATE         NOT NULL,
    sexe                           CHAR(1)      NOT NULL,
    telephone                      VARCHAR(30),
    adresse_domicile               VARCHAR(255),
    commune                        VARCHAR(255),
    quartier                       VARCHAR(255),
    personne_a_contacter_nom       VARCHAR(255),
    personne_a_contacter_telephone VARCHAR(30),
    couverture_sociale             VARCHAR(30),
    CONSTRAINT patient_pkey PRIMARY KEY (id),
    CONSTRAINT patient_numero_dossier_proxymed_key UNIQUE (numero_dossier_proxymed),
    CONSTRAINT patient_numero_dmi_key UNIQUE (numero_dmi)
);

--changeset Mody:003-2-index-patient-nom
CREATE INDEX idx_patient_nom_complet ON patient (nom_complet);
