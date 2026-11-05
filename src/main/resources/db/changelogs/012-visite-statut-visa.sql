--liquibase formatted sql

-- Nouveau parcours : la visite (fiche) est creee a l'impression par l'admin, pour un patient
-- existant, sans senior ni junior ; la fiche scannee importee attend ensuite le visa d'un senior.
-- Valeurs par defaut pour les lignes existantes : une consultation_initiale existante est une
-- visite INITIALE, une fiche deja importee est EN_ATTENTE de visa (aucun senior ni date inventes).

--changeset Mody:012-1-type-visite-consultation
ALTER TABLE consultation_initiale ADD COLUMN type_visite VARCHAR(20) NOT NULL DEFAULT 'INITIALE';
ALTER TABLE consultation_initiale
    ADD CONSTRAINT ck_consultation_type_visite CHECK (type_visite IN ('INITIALE', 'ROUTINE'));

--changeset Mody:012-2-senior-facultatif-consultation
-- Le senior n'est pas connu a l'impression de la fiche (le junior l'etait deja facultatif).
ALTER TABLE consultation_initiale ALTER COLUMN medecin_senior_id DROP NOT NULL;

--changeset Mody:012-3-statut-visa-fiche-signee
-- senior_visa_id : UUID du compte (claim "sub" du badge), sans cle etrangere, comme pour les demandes.
-- nom_senior_visa : nom du senior tel qu'au moment du visa. version : verrou optimiste (deux visas simultanes).
ALTER TABLE fiche_signee ADD COLUMN statut VARCHAR(20) NOT NULL DEFAULT 'EN_ATTENTE';
ALTER TABLE fiche_signee ADD COLUMN senior_visa_id UUID;
ALTER TABLE fiche_signee ADD COLUMN nom_senior_visa VARCHAR(200);
ALTER TABLE fiche_signee ADD COLUMN date_visa TIMESTAMP WITH TIME ZONE;
ALTER TABLE fiche_signee ADD COLUMN motif_renvoi VARCHAR(1000);
ALTER TABLE fiche_signee ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE fiche_signee
    ADD CONSTRAINT ck_fiche_signee_statut CHECK (statut IN ('EN_ATTENTE', 'VISEE', 'RENVOYEE'));
-- Une fiche VISEE porte toujours son senior et sa date ; une fiche RENVOYEE toujours son motif.
ALTER TABLE fiche_signee
    ADD CONSTRAINT ck_fiche_signee_visa CHECK (statut <> 'VISEE'
        OR (senior_visa_id IS NOT NULL AND nom_senior_visa IS NOT NULL AND date_visa IS NOT NULL));
ALTER TABLE fiche_signee
    ADD CONSTRAINT ck_fiche_signee_renvoi CHECK (statut <> 'RENVOYEE' OR motif_renvoi IS NOT NULL);

--changeset Mody:012-4-index-statut-fiche-signee
-- Liste des fiches EN_ATTENTE du senior.
CREATE INDEX idx_fiche_signee_statut ON fiche_signee (statut);
