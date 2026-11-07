package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.TypeVisite;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.DetecteurQrPdf;
import com.proxymed.support.FabriqueBadge;
import com.proxymed.support.PdfDeTest;
import jakarta.persistence.EntityManager;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/visites : visite vierge creee par l'admin a l'impression, pour un patient existant.
 * Pas de BadgeMedecinParDefaut : chaque requete pose son propre badge.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@Transactional
class VisiteViergeIntegrationTest {

    private static final String URL = "/api/visites";

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
    private EntityManager entityManager;

    private UUID patientId;

    @BeforeEach
    void setUp() {
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime()).nomComplet("Patient Visite")
                .dateNaissance(LocalDate.of(1950, 3, 2)).sexe(Sexe.F).couvertureSociale(CouvertureSociale.CMU)
                .telephone("770000000")
                .build()).getId();
    }

    @ParameterizedTest
    @EnumSource(TypeVisite.class)
    void admin_creeUneVisiteVierge_201_sansSeniorNiJunior(TypeVisite type) throws Exception {
        String reponse = creer("ADMIN", corps(patientId, type.name()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.typeVisite", is(type.name())))
                .andReturn().getResponse().getContentAsString();

        // Identifiants et type seulement : ni identite du patient, ni donnee medicale.
        JsonNode json = objectMapper.readTree(reponse);
        assertThat(json.propertyNames()).containsExactlyInAnyOrder("id", "patientId", "typeVisite");
        assertThat(reponse).doesNotContain("Patient Visite").doesNotContain("770000000");

        entityManager.flush();
        entityManager.clear();
        ConsultationInitiale visite = consultationRepository.findById(UUID.fromString(json.get("id").asString())).orElseThrow();
        assertThat(visite.getPatient().getId()).isEqualTo(patientId);
        assertThat(visite.getTypeVisite()).isEqualTo(type);
        assertThat(visite.getStatut()).isEqualTo(StatutConsultation.BROUILLON);
        assertThat(visite.getMedecinSenior()).isNull();
        assertThat(visite.getMedecinJuniorAffecte()).isNull();
        assertThat(visite.getDateConsultation()).isNull();
        assertThat(visite.getHeureConsultation()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEDECIN_JUNIOR", "MEDECIN_SENIOR", "UTILISATEUR"})
    void autreRoleQuAdmin_403(String role) throws Exception {
        creer(role, corps(patientId, "INITIALE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.erreur", is("Acces refuse")))
                .andExpect(jsonPath("$.message", is("Votre role ne permet pas d'acceder a cette ressource")));
        assertThat(consultationRepository.count()).isZero();
    }

    @Test
    void sansBadge_401() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(corps(patientId, "INITIALE")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.erreur", is("Non authentifie")));
        assertThat(consultationRepository.count()).isZero();
    }

    @Test
    void patientInexistant_404() throws Exception {
        UUID inconnu = UUID.randomUUID();
        creer("ADMIN", corps(inconnu, "ROUTINE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erreur", is("Ressource introuvable")))
                .andExpect(jsonPath("$.message", is("Patient introuvable : " + inconnu)));
        assertThat(consultationRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AUTRE", "initiale", ""})
    void typeInvalide_400(String type) throws Exception {
        creer("ADMIN", corps(patientId, type))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Requete invalide")))
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le champ \"typeVisite\"")));
        assertThat(consultationRepository.count()).isZero();
    }

    @Test
    void typeOuPatientManquant_400() throws Exception {
        creer("ADMIN", "{\"patientId\":\"" + patientId + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Validation echouee")))
                .andExpect(jsonPath("$.details[0]", is("typeVisite : le type de visite est obligatoire (INITIALE ou ROUTINE)")));
        creer("ADMIN", "{\"typeVisite\":\"INITIALE\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("patientId : le patient est obligatoire")));
    }

    @Test
    void champEnPlus_400() throws Exception {
        // Le senior n'est jamais choisi a l'impression : un champ en plus est rejete, pas ignore.
        creer("ADMIN", "{\"patientId\":\"" + patientId + "\",\"typeVisite\":\"INITIALE\",\"medecinSeniorId\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : medecinSeniorId")));
    }

    /**
     * Le QR de la fiche d'une visite vierge porte le type de visite, et l'import actuel le relit
     * sans changement : la fiche scannee est rangee dans la visite, EN_ATTENTE.
     */
    @Test
    void qrDeLaVisiteVierge_porteLeType_etEstReluParLImportActuel() throws Exception {
        String reponse = creer("ADMIN", corps(patientId, "ROUTINE"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String visiteId = objectMapper.readTree(reponse).get("id").asString();
        String senior = fabriqueBadge.bearer("MEDECIN_SENIOR");

        byte[] pdf = mockMvc.perform(get("/api/consultations/" + visiteId + "/pdf").header(HttpHeaders.AUTHORIZATION, senior))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        List<String> qr = detecteurQrPdf.detecter(pdf);
        assertThat(qr).hasSize(1);
        JsonNode contenu = objectMapper.readTree(qr.get(0));
        assertThat(contenu.get("type").asString()).isEqualTo("FICHE_1_CONSULTATION_INITIALE");
        assertThat(contenu.get("patientId").asString()).isEqualTo(patientId.toString());
        assertThat(contenu.get("consultationId").asString()).isEqualTo(visiteId);
        assertThat(contenu.get("typeVisite").asString()).isEqualTo("ROUTINE");

        byte[] scan = PdfDeTest.numeriser(pdf, 200);
        mockMvc.perform(multipart("/api/fiches-signees/import")
                        .file(new MockMultipartFile("fichiers", "scan.pdf", "application/pdf", scan))
                        .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultats[0].code", is(201)))
                .andExpect(jsonPath("$.resultats[0].patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.resultats[0].consultationId", is(visiteId)))
                .andExpect(jsonPath("$.resultats[0].statut", is("EN_ATTENTE")));
    }

    private ResultActions creer(String role, String corps) throws Exception {
        return mockMvc.perform(post(URL)
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role))
                .contentType(MediaType.APPLICATION_JSON)
                .content(corps));
    }

    private static String corps(UUID patient, String type) {
        return "{\"patientId\":\"" + patient + "\",\"typeVisite\":\"" + type + "\"}";
    }
}
