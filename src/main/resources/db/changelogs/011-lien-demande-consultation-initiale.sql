--liquibase formatted sql

-- Lien entre une demande de consultation et la consultation initiale (Fiche 1) commencee par le
-- medecin junior assigne. Vide tant que la consultation n'est pas commencee ; une consultation ne
-- peut etre liee qu'a une seule demande.

--changeset Mody:011-1-ajout-consultation-id-demande
ALTER TABLE demande_consultation ADD COLUMN consultation_id UUID;

--changeset Mody:011-2-contraintes-consultation-id-demande
ALTER TABLE demande_consultation
    ADD CONSTRAINT uk_demande_consultation_consultation UNIQUE (consultation_id);
ALTER TABLE demande_consultation
    ADD CONSTRAINT fk_demande_consultation_consultation FOREIGN KEY (consultation_id)
        REFERENCES consultation_initiale (id);
