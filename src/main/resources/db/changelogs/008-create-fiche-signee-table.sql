--liquibase formatted sql

--changeset Mody:008-1-create-fiche-signee
CREATE TABLE fiche_signee
(
    id              UUID                     NOT NULL,
    consultation_id UUID                     NOT NULL,
    nom_fichier     VARCHAR(255)             NOT NULL,
    type_contenu    VARCHAR(100)             NOT NULL,
    taille_octets   BIGINT                   NOT NULL,
    date_import     TIMESTAMP WITH TIME ZONE NOT NULL,
    contenu         BYTEA                    NOT NULL,
    CONSTRAINT fiche_signee_pkey PRIMARY KEY (id),
    CONSTRAINT fk_fiche_signee_consultation FOREIGN KEY (consultation_id) REFERENCES consultation_initiale (id)
);
CREATE INDEX idx_fiche_signee_consultation ON fiche_signee (consultation_id);
