--liquibase formatted sql

-- Une consultation n'a plus qu'une seule fiche signee. La contrainte d'unicite est le garde-fou
-- contre deux imports simultanes (le service verifie deja avant d'enregistrer).

--changeset Mody:009-1-suppression-doublons-fiche-signee
-- On garde la fiche la plus ancienne de chaque consultation (a date egale, le plus petit id).
DELETE FROM fiche_signee f
WHERE EXISTS (SELECT 1
              FROM fiche_signee autre
              WHERE autre.consultation_id = f.consultation_id
                AND (autre.date_import < f.date_import
                    OR (autre.date_import = f.date_import AND autre.id < f.id)));

--changeset Mody:009-2-unicite-fiche-signee-consultation
ALTER TABLE fiche_signee ADD CONSTRAINT uk_fiche_signee_consultation UNIQUE (consultation_id);

--changeset Mody:009-3-suppression-index-fiche-signee-consultation
-- Redondant avec l'index cree par la contrainte d'unicite.
DROP INDEX idx_fiche_signee_consultation;
