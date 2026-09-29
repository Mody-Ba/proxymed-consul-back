--liquibase formatted sql

-- Les ids des referentiels et medecins ne sont pas fixes : on laisse l'identity les generer
-- (sinon la sequence reste a 1 et les POST suivants via l'API echouent en doublon de cle).
-- Les UUID des patients et de la consultation sont fixes pour pouvoir les utiliser directement dans Swagger.

--changeset Mody:007-1-insert-structures context:donnees-initiales
INSERT INTO structure (libelle) VALUES
    ('Hôpital Principal de Dakar'),
    ('Centre Hospitalier National de Pikine'),
    ('Hôpital Abass Ndao'),
    ('Centre de Santé Philippe Maguilen Senghor');

--changeset Mody:007-2-insert-maladies-chroniques context:donnees-initiales
INSERT INTO maladie_chronique (libelle) VALUES
    ('Diabète de type 2'),
    ('Hypertension artérielle'),
    ('Insuffisance cardiaque'),
    ('Asthme'),
    ('Insuffisance rénale chronique'),
    ('Bronchopneumopathie chronique obstructive (BPCO)');

--changeset Mody:007-3-insert-facteurs-de-risque context:donnees-initiales
INSERT INTO facteur_de_risque (libelle) VALUES
    ('Tabagisme'),
    ('Sédentarité'),
    ('Obésité'),
    ('Consommation excessive d''alcool'),
    ('Alimentation riche en sel'),
    ('Antécédents familiaux cardiovasculaires');

--changeset Mody:007-4-insert-situations-sociales context:donnees-initiales
INSERT INTO situation_sociale (libelle) VALUES
    ('Vit seul(e)'),
    ('Sans emploi'),
    ('Sans couverture maladie'),
    ('Logement précaire'),
    ('Aidant familial disponible');

--changeset Mody:007-5-insert-medecins-test context:test-data
INSERT INTO medecin (nom, prenom, numero_ordre, role, structure_rattachement_id) VALUES
    ('Diop', 'Aminata', 'ONMS-2011-0457', 'SENIOR',
     (SELECT id FROM structure WHERE libelle = 'Hôpital Principal de Dakar')),
    ('Ndiaye', 'Ousmane', 'ONMS-2022-1893', 'JUNIOR',
     (SELECT id FROM structure WHERE libelle = 'Hôpital Principal de Dakar'));

--changeset Mody:007-6-insert-patients-test context:test-data
INSERT INTO patient (id, numero_dossier_proxymed, numero_dmi, nom_complet, date_naissance, sexe, telephone,
                     adresse_domicile, commune, quartier, personne_a_contacter_nom,
                     personne_a_contacter_telephone, couverture_sociale) VALUES
    ('11111111-1111-1111-1111-111111111111', 'PXM-2026-0001', 'DMI-458712', 'Fatou Sow', '1954-03-12', 'F',
     '+221 77 512 34 56', 'Villa 124, Cité Soprim', 'Parcelles Assainies', 'Unité 15',
     'Mamadou Sow (fils)', '+221 78 432 10 98', 'CMU'),
    ('22222222-2222-2222-2222-222222222222', 'PXM-2026-0002', 'DMI-458713', 'Abdoulaye Fall', '1968-11-27', 'M',
     '+221 76 221 45 67', 'Rue 10 x 13, Médina', 'Dakar Plateau', 'Médina',
     'Awa Fall (épouse)', '+221 77 998 76 54', 'IPM'),
    ('33333333-3333-3333-3333-333333333333', 'PXM-2026-0003', NULL, 'Mariama Ba', '1991-07-04', 'F',
     '+221 70 345 67 89', 'Quartier Thiaroye Azur', 'Pikine', 'Thiaroye',
     'Ibrahima Ba (frère)', '+221 76 555 12 34', 'AUCUNE');

--changeset Mody:007-7-insert-consultation-brouillon-test context:test-data
INSERT INTO consultation_initiale (id, patient_id, medecin_senior_id, date_consultation, heure_consultation, statut,
                                   origine_demande, motif_principal_consultation,
                                   precisions_maladies_chroniques, allergies_connues,
                                   hospitalisations_anterieures, chirurgies_anterieures,
                                   details_hospitalisations_chirurgies)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111',
        (SELECT id FROM medecin WHERE numero_ordre = 'ONMS-2011-0457'),
        '2026-09-25', '09:30:00', 'BROUILLON',
        'FAMILLE', 'Céphalées et vertiges depuis une semaine, mauvaise observance du traitement antihypertenseur',
        'HTA connue depuis 2015, traitée par amlodipine 5 mg', FALSE,
        TRUE, FALSE,
        'Hospitalisée en 2021 à l''Hôpital Principal pour poussée hypertensive (5 jours)');

INSERT INTO constante_vitale (consultation_id, type, valeur, heure, est_normal, est_alerte, commentaire) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TA', 165, '09:40:00', FALSE, FALSE, 'TA systolique, bras gauche, au repos'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'FC', 88, '09:40:00', TRUE, FALSE, NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'SPO2', 97, '09:42:00', TRUE, FALSE, 'Air ambiant'),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'TEMPERATURE', 36.8, '09:42:00', TRUE, FALSE, NULL),
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'GLYCEMIE_CAPILLAIRE', 182, '09:45:00', FALSE, FALSE, 'Non à jeun');

INSERT INTO antecedent_maladie (consultation_id, maladie_chronique_id, precision) VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
     (SELECT id FROM maladie_chronique WHERE libelle = 'Hypertension artérielle'),
     'Diagnostiquée en 2015, suivi irrégulier');
