package com.proxymed.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Changelog 012 : une ligne ecrite comme avant (sans les nouvelles colonnes) recoit des valeurs
 * par defaut sures, et les contraintes de statut et de visa sont tenues par la base.
 */
@SpringBootTest
@Transactional
class MigrationVisiteStatutVisaIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private UUID consultationId;
    private UUID ficheId;

    @BeforeEach
    void setUp() {
        UUID patientId = UUID.randomUUID();
        jdbc.update("INSERT INTO patient (id, numero_dossier_proxymed, nom_complet, date_naissance, sexe, couverture_sociale) "
                + "VALUES (?, ?, 'Patient Migration', DATE '1950-01-01', 'M', 'CMU')", patientId, "DOS-" + System.nanoTime());
        // Ni type_visite ni medecin_senior_id : la visite vierge n'a pas de senior.
        consultationId = UUID.randomUUID();
        jdbc.update("INSERT INTO consultation_initiale (id, patient_id) VALUES (?, ?)", consultationId, patientId);
        ficheId = UUID.randomUUID();
        jdbc.update("INSERT INTO fiche_signee (id, consultation_id, nom_fichier, type_contenu, taille_octets, date_import, contenu) "
                + "VALUES (?, ?, 'scan.pdf', 'application/pdf', 1, CURRENT_TIMESTAMP, X'25')", ficheId, consultationId);
    }

    @Test
    void lignesExistantes_recoiventDesValeursParDefautSures() {
        assertThat(jdbc.queryForObject("SELECT type_visite FROM consultation_initiale WHERE id = ?", String.class, consultationId))
                .isEqualTo("INITIALE");
        Map<String, Object> fiche = jdbc.queryForMap("SELECT statut, senior_visa_id, nom_senior_visa, date_visa, motif_renvoi, version "
                + "FROM fiche_signee WHERE id = ?", ficheId);
        assertThat(fiche.get("STATUT")).isEqualTo("EN_ATTENTE");
        assertThat(fiche.get("SENIOR_VISA_ID")).isNull();
        assertThat(fiche.get("NOM_SENIOR_VISA")).isNull();
        assertThat(fiche.get("DATE_VISA")).isNull();
        assertThat(fiche.get("MOTIF_RENVOI")).isNull();
        assertThat(((Number) fiche.get("VERSION")).longValue()).isZero();
    }

    @Test
    void typeVisiteEtStatutInconnus_refuses() {
        assertThatThrownBy(() -> jdbc.update("UPDATE consultation_initiale SET type_visite = 'AUTRE' WHERE id = ?", consultationId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE fiche_signee SET statut = 'SIGNEE' WHERE id = ?", ficheId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void ficheVisee_exigeSeniorNomEtDate_ficheRenvoyee_exigeUnMotif() {
        assertThatThrownBy(() -> jdbc.update("UPDATE fiche_signee SET statut = 'VISEE' WHERE id = ?", ficheId))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE fiche_signee SET statut = 'RENVOYEE' WHERE id = ?", ficheId))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("UPDATE fiche_signee SET statut = 'VISEE', senior_visa_id = ?, nom_senior_visa = 'Dr Diop', "
                + "date_visa = CURRENT_TIMESTAMP WHERE id = ?", UUID.randomUUID(), ficheId);
        jdbc.update("UPDATE fiche_signee SET statut = 'RENVOYEE', motif_renvoi = 'Scan illisible' WHERE id = ?", ficheId);
        assertThat(jdbc.queryForObject("SELECT statut FROM fiche_signee WHERE id = ?", String.class, ficheId)).isEqualTo("RENVOYEE");
    }
}
