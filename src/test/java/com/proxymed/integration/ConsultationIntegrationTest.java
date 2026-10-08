package com.proxymed.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConsultationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private com.proxymed.repository.MaladieChroniqueRepository maladieChroniqueRepository;

    private Patient patient;
    private Medecin medecinSenior;
    private Medecin medecinJunior;

    @BeforeEach
    void setUp() {
        patient = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime())
                .nomComplet("Mamadou Ba")
                .dateNaissance(LocalDate.of(1948, 6, 1))
                .sexe(Sexe.M)
                .couvertureSociale(CouvertureSociale.IPM)
                .build());
        medecinSenior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Awa").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        medecinJunior = medecinRepository.save(Medecin.builder()
                .nom("Fall").prenom("Omar").numeroOrdre("JUN-" + System.nanoTime()).role(RoleMedecin.JUNIOR).build());
    }

    @Test
    void cycleDeVieComplet_brouillon_validee_signee_puisImmuable() throws Exception {
        String consultationId = creerBrouillon();

        // Section 2 : origine=AUTRE sans precision -> rejetee
        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {"origineDemande":"AUTRE"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "origineDemande":"SAMU",
                                  "motifPrincipalConsultation":"Chute a domicile"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origineDemande", is("SAMU")));

        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {
                                  "niveauAutonomie":"PARTIELLEMENT_DEPENDANT",
                                  "risqueIsolement":"MODERE",
                                  "niveauPrecarite":"AUCUNE",
                                  "resumeSyndromiqueCim10":"R296"
                                }
                                """))
                .andExpect(status().isOk());

        // Examen par appareil (section 5, endpoint dedie)
        mockMvc.perform(put("/api/consultations/" + consultationId + "/examen-par-appareil")
                        .contentType("application/json")
                        .content("""
                                {"etatGeneral":"altere", "cardioVasculaire":"RAS"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etatGeneral", is("altere")));

        // Antecedent maladie (section 3, endpoint dedie)
        mockMvc.perform(post("/api/consultations/" + consultationId + "/antecedents-maladies")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "maladieChroniqueId", creerMaladieChronique("Diabete"),
                                "precision", "depuis 2019"))))
                .andExpect(status().isCreated());

        // Decision ELIGIBLE sans medecin junior -> rejetee
        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {"decisionEligibilite":"ELIGIBLE"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decisionEligibilite", "ELIGIBLE",
                                "medecinJuniorAffecteId", medecinJunior.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medecinJuniorAffecte.id", is(medecinJunior.getId().intValue())));

        // Constantes : POIDS + TAILLE -> IMC auto-calcule
        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"POIDS","valeur":70}
                                """))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"TAILLE","valeur":175}
                                """))
                .andExpect(status().isCreated());

        String fiche = mockMvc.perform(get("/api/consultations/" + consultationId))
                        .andExpect(status().isOk())
                .andExpect(jsonPath("$.maladiesChroniques", hasSize(1)))
                .andExpect(jsonPath("$.maladiesChroniques[0].maladieChroniqueLibelle", is("Diabete")))
                .andReturn().getResponse().getContentAsString();
        JsonNode imc = trouverConstante(fiche, "IMC");
        assertThat(imc).isNotNull();
        assertThat(imc.get("valeur").asDouble()).isEqualTo(22.9);
        assertThat(imc.get("estNormal").asBoolean()).isTrue();

        // Signature avant validation -> conflit
        mockMvc.perform(post("/api/consultations/" + consultationId + "/signer"))
                .andExpect(status().isConflict());

        // Validation puis signature
        mockMvc.perform(post("/api/consultations/" + consultationId + "/valider"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("VALIDEE")));

        mockMvc.perform(post("/api/consultations/" + consultationId + "/signer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("SIGNEE")))
                .andExpect(jsonPath("$.signatureMedecinSenior", is(true)));

        // Fiche signee -> non modifiable (consultation, constantes, antecedents)
        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {"delaiRecommande":"1 semaine"}
                                """))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"FC","valeur":80}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void signer_rejette_siFicheIncomplete() throws Exception {
        String consultationId = creerBrouillon();

        mockMvc.perform(post("/api/consultations/" + consultationId + "/valider"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/consultations/" + consultationId + "/signer"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void ajouterConstante_declencheAlerte_horsBornes() throws Exception {
        String consultationId = creerBrouillon();

        mockMvc.perform(post("/api/consultations/" + consultationId + "/constantes")
                        .contentType("application/json")
                        .content("""
                                {"type":"SPO2","valeur":80}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estAlerte", is(true)))
                .andExpect(jsonPath("$.estNormal", is(false)));
    }

    @Test
    void enAttenteValidationDass_listeLesFichesValideesNonSigneesParLaDass() throws Exception {
        String consultationId = creerBrouillon();
        mockMvc.perform(post("/api/consultations/" + consultationId + "/valider"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/consultations/en-attente-dass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + consultationId + "')]").exists());

        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {"signaturePointFocalDass": true, "nomPointFocalDass":"M. Sy"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/consultations/en-attente-dass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='" + consultationId + "')]").doesNotExist());
    }

    @Test
    void rechercher_filtreParNomPatientEtStatut() throws Exception {
        creerBrouillon();

        mockMvc.perform(get("/api/consultations")
                        .param("nomPatient", "Mamadou")
                        .param("statut", "BROUILLON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientNomComplet", is("Mamadou Ba")));

        mockMvc.perform(get("/api/consultations")
                        .param("nomPatient", "Inconnu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void mettreAJour_rejette_examenParAppareilImbrique() throws Exception {
        String consultationId = creerBrouillon();

        // L'examen par appareil se gere via son endpoint dedie : un objet imbrique dans le PUT
        // de la fiche doit etre rejete, et non ignore silencieusement avec un 200.
        mockMvc.perform(put("/api/consultations/" + consultationId)
                        .contentType("application/json")
                        .content("""
                                {"motifPrincipalConsultation":"Chute", "examenParAppareil":{"etatGeneral":"altere"}}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : examenParAppareil")));

        mockMvc.perform(get("/api/consultations/" + consultationId + "/examen-par-appareil"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_renvoie404_siConsultationInconnue() throws Exception {
        mockMvc.perform(get("/api/consultations/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creerBrouillon_prendEnCompteOrigineEtMotifDeLaDemande() throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patient.getId(),
                                "medecinSeniorId", medecinSenior.getId(),
                                "origineDemande", "SAMU",
                                "motifPrincipalConsultation", "Chute a domicile"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origineDemande", is("SAMU")))
                .andExpect(jsonPath("$.motifPrincipalConsultation", is("Chute a domicile")))
                .andReturn().getResponse().getContentAsString();
        String consultationId = objectMapper.readTree(reponse).get("id").asText();

        mockMvc.perform(get("/api/consultations/" + consultationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origineDemande", is("SAMU")))
                .andExpect(jsonPath("$.motifPrincipalConsultation", is("Chute a domicile")));
    }

    @Test
    void creerBrouillon_rejette_siMedecinNestPasSenior() throws Exception {
        mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patient.getId(),
                                "medecinSeniorId", medecinJunior.getId()))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerBrouillon_rejette_siPatientOuMedecinManquant() throws Exception {
        mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creerBrouillon_renvoie404_siPatientInconnu() throws Exception {
        mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", UUID.randomUUID(),
                                "medecinSeniorId", medecinSenior.getId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void telechargerPdf_renvoieUnFichierPdfEnPieceJointe() throws Exception {
        String consultationId = creerBrouillon();

        byte[] pdf = mockMvc.perform(get("/api/consultations/" + consultationId + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"fiche-consultation-" + consultationId + ".pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    private String creerBrouillon() throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patient.getId(),
                                "medecinSeniorId", medecinSenior.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut", is("BROUILLON")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asText();
    }

    private long creerMaladieChronique(String libelle) throws Exception {
        // Cree via le repository plutot que l'API (pas d'endpoint de creation pour ce referentiel).
        return maladieChroniqueRepository.save(
                        com.proxymed.entity.MaladieChronique.builder().libelle(libelle).actif(true).build())
                .getId();
    }

    private JsonNode trouverConstante(String ficheJson, String type) throws Exception {
        JsonNode racine = objectMapper.readTree(ficheJson);
        for (JsonNode c : racine.get("constantesVitales")) {
            if (c.get("type").asText().equals(type)) {
                return c;
            }
        }
        return null;
    }
}
