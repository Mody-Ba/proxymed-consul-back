package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.FabriqueBadge;
import com.proxymed.support.PdfDeTest;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rapport PDF d'une fiche visee (GET /api/fiches-signees/{consultationId}/rapport) : page de garde
 * puis scan signe. Donnees nettoyees apres chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
class RapportFicheIntegrationTest {

    private static final String NOM_PATIENT = "Fatou Sarr Rapport";
    private static final String NOM_SENIOR = "senior.rapport@proxymed.test";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private FicheSigneeRepository ficheSigneeRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID patientId;
    private String numeroDossier;

    @BeforeEach
    void setUp() {
        numeroDossier = "DOS-RAP-" + System.nanoTime();
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed(numeroDossier).nomComplet(NOM_PATIENT)
                .dateNaissance(LocalDate.of(1952, 7, 14)).sexe(Sexe.F).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM fiche_signee WHERE consultation_id IN "
                + "(SELECT id FROM consultation_initiale WHERE patient_id = ?)", patientId);
        jdbc.update("DELETE FROM consultation_initiale WHERE patient_id = ?", patientId);
        jdbc.update("DELETE FROM patient WHERE id = ?", patientId);
    }

    @Test
    void senior_ficheVisee_200_pdfAvecPageDeGardePuisScan() throws Exception {
        // Scan de 2 pages : le rapport en compte 3, et le scan est repris tel quel apres la page de garde.
        byte[] scan = PdfDeTest.pdfAvecQr(null, null);
        UUID visite = visite(TypeVisite.ROUTINE, StatutFicheSignee.VISEE, scan);

        byte[] pdf = mockMvc.perform(get(url(visite)).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"rapport-" + visite + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(3);
            PDFTextStripper lecteur = new PDFTextStripper();
            lecteur.setStartPage(1);
            lecteur.setEndPage(1);
            String pageDeGarde = lecteur.getText(document);
            assertThat(pageDeGarde)
                    .contains("Rapport de consultation")
                    .contains(NOM_PATIENT)
                    .contains(numeroDossier)
                    .contains(NOM_SENIOR)
                    .contains("Visite de routine")
                    .contains("05/03/2026");
            lecteur.setStartPage(2);
            lecteur.setEndPage(2);
            assertThat(lecteur.getText(document)).contains("Fiche de test").doesNotContain("Rapport de consultation");
        }
    }

    @Test
    void senior_scanImage_placeSurUnePage() throws Exception {
        BufferedImage image = new BufferedImage(400, 600, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", png);
        UUID visite = visite(TypeVisite.INITIALE, StatutFicheSignee.VISEE, png.toByteArray());

        byte[] pdf = mockMvc.perform(get(url(visite)).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertThat(document.getNumberOfPages()).isEqualTo(2);
            assertThat(new PDFTextStripper().getText(document)).contains("Visite initiale");
        }
    }

    @ParameterizedTest
    @EnumSource(value = StatutFicheSignee.class, names = {"EN_ATTENTE", "RENVOYEE"})
    void senior_ficheNonVisee_409(StatutFicheSignee statut) throws Exception {
        UUID visite = visite(TypeVisite.INITIALE, statut, PdfDeTest.pdfAvecQr((String) null));

        mockMvc.perform(get(url(visite)).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.erreur", is("Action incompatible avec le statut de la fiche")))
                .andExpect(jsonPath("$.message", is("Le rapport n'est disponible que pour une fiche VISEE : la fiche de la visite "
                        + visite + " est " + statut)));
    }

    @Test
    void senior_visiteInexistante_404() throws Exception {
        mockMvc.perform(get(url(UUID.randomUUID())).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erreur", is("Ressource introuvable")));
    }

    @Test
    void senior_visiteSansFiche_404() throws Exception {
        UUID visite = consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientId))
                .statut(StatutConsultation.BROUILLON).typeVisite(TypeVisite.INITIALE).build()).getId();

        mockMvc.perform(get(url(visite)).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("MEDECIN_SENIOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Aucune fiche signee importee")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "MEDECIN_JUNIOR", "UTILISATEUR"})
    void autresRoles_403(String role) throws Exception {
        UUID visite = visite(TypeVisite.INITIALE, StatutFicheSignee.VISEE, PdfDeTest.pdfAvecQr((String) null));

        mockMvc.perform(get(url(visite)).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Acces refuse")));
    }

    @Test
    void sansBadge_401() throws Exception {
        UUID visite = visite(TypeVisite.INITIALE, StatutFicheSignee.VISEE, PdfDeTest.pdfAvecQr((String) null));

        mockMvc.perform(get(url(visite))).andExpect(status().isUnauthorized());
    }

    private static String url(UUID consultationId) {
        return "/api/fiches-signees/" + consultationId + "/rapport";
    }

    private UUID visite(TypeVisite type, StatutFicheSignee statut, byte[] scan) {
        ConsultationInitiale visite = consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientId))
                .statut(StatutConsultation.BROUILLON)
                .typeVisite(type)
                .dateConsultation(LocalDate.of(2026, 3, 5))
                .build());
        FicheSignee.FicheSigneeBuilder fiche = FicheSignee.builder()
                .consultation(visite)
                .nomFichier("scan.pdf")
                .typeContenu("application/pdf")
                .tailleOctets(scan.length)
                .dateImport(Instant.parse("2026-03-06T09:00:00Z"))
                .contenu(scan)
                .statut(statut);
        if (statut == StatutFicheSignee.VISEE) {
            fiche.seniorVisaId(UUID.randomUUID()).nomSeniorVisa(NOM_SENIOR).dateVisa(Instant.parse("2026-03-07T10:30:00Z"));
        } else if (statut == StatutFicheSignee.RENVOYEE) {
            fiche.motifRenvoi("Scan illisible");
        }
        ficheSigneeRepository.save(fiche.build());
        return visite.getId();
    }
}
