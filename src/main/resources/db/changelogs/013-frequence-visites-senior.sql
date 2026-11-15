--liquibase formatted sql

-- Frequence des visites decidee par le senior au moment du visa d'une fiche de visite INITIALE
-- (facultative). Valeurs de l'enum FrequenceVisites, deja utilisee par consultation_initiale.
-- Colonne sur fiche_signee (et non consultation_initiale.frequence_visites_suivi, qui reste la
-- donnee saisie sur la fiche) : elle est ecrite par la meme mise a jour conditionnelle que le visa.

--changeset Mody:013-1-frequence-visites-senior-fiche-signee
ALTER TABLE fiche_signee ADD COLUMN frequence_visites_senior VARCHAR(20);
ALTER TABLE fiche_signee
    ADD CONSTRAINT ck_fiche_signee_frequence_visites_senior
        CHECK (frequence_visites_senior IN ('HEBDOMADAIRE', 'BIMENSUELLE', 'MENSUELLE'));
-- Seule une fiche VISEE porte une frequence decidee par le senior.
ALTER TABLE fiche_signee
    ADD CONSTRAINT ck_fiche_signee_frequence_si_visee
        CHECK (frequence_visites_senior IS NULL OR statut = 'VISEE');
