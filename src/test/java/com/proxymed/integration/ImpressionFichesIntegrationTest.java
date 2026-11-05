package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.DecisionEligibilite;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeVisite;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.DetecteurQrPdf;
import com.proxymed.service.PdfGenerationService;
import com.proxymed.support.FabriqueBadge;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/visites/impression : fiches vierges imprimees par l'admin, un seul PDF combine.
 * Volontairement sans @Transactional : le tout-ou-rien doit etre verifie sur la base reelle
 * (une transaction de test commune masquerait le rollback). Les donnees sont nettoyees apres chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
class ImpressionFichesIntegrationTest {

    private static final String URL = "/api/visites/impression";
    /** Le template de la Fiche 1 (8 sections, vides a l'impression) tient sur deux pages A4. */
    private static final int PAGES_PAR_FICHE = 2;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private DetecteurQrPdf detecteurQrPdf;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private PdfGenerationService pdfGenerationService;

    private final List<UUID> patientsCrees = new ArrayList<>();
    private UUID patientA;
    private UUID patientB;

    @BeforeEach
    void setUp() {
        patientA = creerPatient("Awa Ndiaye", "DOS-A-", Sexe.F, "771112233", CouvertureSociale.IPM);
        patientB = creerPatient("Moussa Diallo", "DOS-B-", Sexe.M, "772223344", CouvertureSociale.CMU);
    }

    @AfterEach
    void nettoyer() {
        for (UUID patient : patientsCrees) {
            jdbc.update("DELETE FROM consultation_initiale WHERE patient_id = ?", patient);
            jdbc.update("DELETE FROM patient WHERE id = ?", patient);
        }
    }

    @Test
    void admin_deuxPatients_unSeulPdf_chaqueFicheSurUneNouvellePage_chaqueQrPorteLeBonPatientEtLeBonType() throws Exception {
        MvcResult resultat = imprimer("ADMIN", corps(patientA, "INITIALE", patientB, "ROUTINE"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        matchesPattern("attachment; filename=\"fiches-\\d{4}-\\d{2}-\\d{2}-2\\.pdf\"")))
                .andReturn();
        byte[] pdf = resultat.getResponse().getContentAsByteArray();
        List<UUID> visites = Arrays.stream(resultat.getResponse().getHeader("X-Visites-Ids").split(","))
                .map(UUID::fromString).toList();

        // Une fiche vierge fait PAGES_PAR_FICHE pages ; son QR est en haut de sa premiere page.
        List<List<String>> qrParPage = qrParPage(pdf);
        assertThat(qrParPage).hasSize(2 * PAGES_PAR_FICHE);
        assertThat(qrParPage.get(0)).hasSize(1);
        assertThat(qrParPage.get(PAGES_PAR_FICHE)).hasSize(1);
        assertThat(qrParPage).filteredOn(List::isEmpty).hasSize(2 * PAGES_PAR_FICHE - 2);
        verifierQr(qrParPage.get(0).get(0), patientA, visites.get(0), "INITIALE");
        verifierQr(qrParPage.get(PAGES_PAR_FICHE).get(0), patientB, visites.get(1), "ROUTINE");

        // Les visites sont creees, vierges, dans l'ordre des fiches.
        ConsultationInitiale visiteA = consultationRepository.findById(visites.get(0)).orElseThrow();
        ConsultationInitiale visiteB = consultationRepository.findById(visites.get(1)).orElseThrow();
        assertThat(visiteA.getTypeVisite()).isEqualTo(TypeVisite.INITIALE);
        assertThat(visiteB.getTypeVisite()).isEqualTo(TypeVisite.ROUTINE);
        assertThat(List.of(visiteA, visiteB)).allSatisfy(v -> {
            assertThat(v.getStatut()).isEqualTo(StatutConsultation.BROUILLON);
            assertThat(v.getMedecinSenior()).isNull();
            assertThat(v.getMedecinJuniorAffecte()).isNull();
        });
        assertThat(nombreDeVisites()).isEqualTo(2);
    }

    @Test
    void pdf_neContientQueLIdentite_aucuneDonneeMedicale() throws Exception {
        // Le patient a deja une consultation remplie : rien n'en doit passer sur la fiche imprimee.
        consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientA))
                .statut(StatutConsultation.VALIDEE)
                .motifPrincipalConsultation("MOTIF-SECRET douleur thoracique")
                .precisionsMaladiesChroniques("ANTECEDENT-SECRET")
                .resumeSyndromiqueCim10("CIM10-SECRET")
                .decisionEligibilite(DecisionEligibilite.NON_ELIGIBLE)
                .motifNonEligibilite("NON-ELIGIBLE-SECRET")
                .traitementPrescrit("TRAITEMENT-SECRET")
                .besoinsSociauxIdentifies("BESOIN-SECRET")
                .build());

        byte[] pdf = imprimer("ADMIN", corps(patientA, "ROUTINE"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();

        String texte;
        try (PDDocument document = Loader.loadPDF(pdf)) {
            texte = new PDFTextStripper().getText(document);
        }
        assertThat(texte).contains("Awa Ndiaye", "771112233", "01/02/1950", "Adresse Awa Ndiaye", "Visite de routine");
        assertThat(texte).doesNotContain("SECRET", "ÉLIGIBLE", "Signé le", "Visé le");
        assertThat(texte).contains("Aucune maladie chronique renseignée", "Aucune constante vitale saisie",
                "Examen par appareil non renseigné");
    }

    @Test
    void patientInexistant_404_etAucuneVisiteEnregistree() throws Exception {
        UUID inconnu = UUID.randomUUID();
        imprimer("ADMIN", corps(patientA, "INITIALE", inconnu, "INITIALE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.erreur", is("Ressource introuvable")))
                .andExpect(jsonPath("$.message", is("Patient introuvable : " + inconnu)));
        assertThat(nombreDeVisites()).isZero();
    }

    @Test
    void echecDeGenerationDuPdf_500_etAucuneVisiteEnregistree() throws Exception {
        doThrow(new UncheckedIOException("Echec de la conversion HTML -> PDF de la fiche", new IOException()))
                .when(pdfGenerationService).genererFichesCombinees(anyList());

        // Aucun gestionnaire ne transforme cette erreur imprevue : MockMvc la relance (500 en production).
        assertThatThrownBy(() -> imprimer("ADMIN", corps(patientA, "INITIALE", patientB, "ROUTINE")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IOException.class);
        assertThat(nombreDeVisites()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEDECIN_JUNIOR", "MEDECIN_SENIOR", "UTILISATEUR"})
    void autreRoleQuAdmin_403(String role) throws Exception {
        imprimer(role, corps(patientA, "INITIALE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Acces refuse")))
                .andExpect(jsonPath("$.message", is("Votre role ne permet pas d'acceder a cette ressource")));
        assertThat(nombreDeVisites()).isZero();
    }

    @Test
    void sansBadge_401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps(patientA, "INITIALE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erreur", is("Non authentifie")));
        assertThat(nombreDeVisites()).isZero();
    }

    @Test
    void listeVideOuAbsente_400() throws Exception {
        for (String corps : List.of("{\"fiches\":[]}", "{}")) {
            imprimer("ADMIN", corps)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.erreur", is("Validation echouee")))
                    .andExpect(jsonPath("$.details[0]", is("fiches : la liste des fiches a imprimer ne doit pas etre vide")));
        }
    }

    @Test
    void plusDeCinquanteFiches_400_etAucuneVisiteEnregistree() throws Exception {
        String fiches = IntStream.range(0, 51)
                .mapToObj(i -> "{\"patientId\":\"" + (i % 2 == 0 ? patientA : patientB) + "\",\"typeVisite\":\"INITIALE\"}")
                .collect(Collectors.joining(","));
        imprimer("ADMIN", "{\"fiches\":[" + fiches + "]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("fiches : au plus 50 fiches par impression")));
        assertThat(nombreDeVisites()).isZero();
    }

    @Test
    void demandeIncompleteOuTypeInconnu_400() throws Exception {
        imprimer("ADMIN", "{\"fiches\":[{\"patientId\":\"" + patientA + "\"}]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]",
                        is("fiches[0].typeVisite : le type de visite est obligatoire (INITIALE ou ROUTINE)")));
        imprimer("ADMIN", "{\"fiches\":[null]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("fiches[0] : une demande d'impression ne peut pas etre vide")));
        imprimer("ADMIN", corps(patientA, "AUTRE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le champ \"typeVisite\"")));
        assertThat(nombreDeVisites()).isZero();
    }

    @Test
    void doublon_400_etAucuneVisiteEnregistree_memePatientAutreType_accepte() throws Exception {
        imprimer("ADMIN", corps(patientA, "INITIALE", patientB, "ROUTINE") .replace("]}", ",{\"patientId\":\""
                + patientA + "\",\"typeVisite\":\"INITIALE\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", is("La fiche n° 3 est en double avec la fiche n° 1 (meme patient, "
                        + "meme type de visite) : chaque fiche ne peut etre demandee qu'une fois")));
        assertThat(nombreDeVisites()).isZero();

        imprimer("ADMIN", corps(patientA, "INITIALE", patientA, "ROUTINE")).andExpect(status().isOk());
        assertThat(nombreDeVisites()).isEqualTo(2);
    }

    private List<List<String>> qrParPage(byte[] pdf) throws IOException {
        List<List<String>> resultat = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdf)) {
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                try (PDDocument page = new PDDocument()) {
                    page.importPage(document.getPage(i));
                    ByteArrayOutputStream octets = new ByteArrayOutputStream();
                    page.save(octets);
                    resultat.add(detecteurQrPdf.detecter(octets.toByteArray()));
                }
            }
        }
        return resultat;
    }

    private void verifierQr(String texte, UUID patient, UUID visite, String typeVisite) {
        JsonNode qr = objectMapper.readTree(texte);
        assertThat(qr.get("type").asString()).isEqualTo("FICHE_1_CONSULTATION_INITIALE");
        assertThat(qr.get("patientId").asString()).isEqualTo(patient.toString());
        assertThat(qr.get("consultationId").asString()).isEqualTo(visite.toString());
        assertThat(qr.get("typeVisite").asString()).isEqualTo(typeVisite);
    }

    private long nombreDeVisites() {
        return patientsCrees.stream()
                .mapToLong(p -> jdbc.queryForObject("SELECT COUNT(*) FROM consultation_initiale WHERE patient_id = ? "
                        + "AND medecin_senior_id IS NULL AND statut = 'BROUILLON'", Long.class, p))
                .sum();
    }

    private ResultActions imprimer(String role, String corps) throws Exception {
        return mockMvc.perform(post(URL)
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private static String corps(Object... patientEtType) {
        List<String> fiches = new ArrayList<>();
        for (int i = 0; i < patientEtType.length; i += 2) {
            fiches.add("{\"patientId\":\"" + patientEtType[i] + "\",\"typeVisite\":\"" + patientEtType[i + 1] + "\"}");
        }
        return "{\"fiches\":[" + String.join(",", fiches) + "]}";
    }

    private UUID creerPatient(String nom, String prefixeDossier, Sexe sexe, String telephone, CouvertureSociale couverture) {
        UUID id = patientRepository.save(Patient.builder()
                .numeroDossierProxymed(prefixeDossier + System.nanoTime()).nomComplet(nom)
                .dateNaissance(LocalDate.of(1950, 2, 1)).sexe(sexe).telephone(telephone)
                .adresseDomicile("Adresse " + nom).commune("Dakar").quartier("Medina")
                .couvertureSociale(couverture)
                .build()).getId();
        patientsCrees.add(id);
        return id;
    }
}
