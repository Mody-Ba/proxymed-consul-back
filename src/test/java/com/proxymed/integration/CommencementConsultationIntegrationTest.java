package com.proxymed.integration;

import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.DemandeConsultationRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.FabriqueBadge;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * POST /api/demandes-consultation/{id}/commencer : le junior assigne cree le patient et le
 * brouillon de Fiche 1 lies a la demande.
 * <p>
 * Volontairement SANS @Transactional : la transaction du service doit valider ou annuler pour de
 * vrai (sinon elle rejoindrait celle du test et l'annulation ne serait pas observable). Chaque test
 * supprime ensuite exactement ce qu'il a cree, pour ne rien laisser aux autres classes de test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@ExtendWith(OutputCaptureExtension.class)
class CommencementConsultationIntegrationTest {

    private static final String URL = "/api/demandes-consultation";
    private static final String MESSAGE_ROLE = "Votre role ne permet pas d'acceder a cette ressource";
    private static final String NOM_PATIENT = "Ndeye Sarr";
    private static final String MOTIF = "Essoufflement a l'effort depuis deux semaines";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private DemandeConsultationRepository demandeConsultationRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;

    private final UUID demandeur = UUID.randomUUID();
    private final UUID senior = UUID.randomUUID();
    private final UUID junior = UUID.randomUUID();

    private final List<UUID> demandesCreees = new ArrayList<>();
    private final List<UUID> consultationsCreees = new ArrayList<>();
    private final List<UUID> patientsCrees = new ArrayList<>();
    private final List<Long> medecinsCrees = new ArrayList<>();

    private Medecin medecinSenior;
    private Medecin medecinJunior;
    private String numeroDossier;

    @BeforeEach
    void setUp() {
        medecinSenior = medecin("Diop", RoleMedecin.SENIOR);
        medecinJunior = medecin("Fall", RoleMedecin.JUNIOR);
        numeroDossier = "PXM-TEST-" + System.nanoTime();
    }

    /** Ordre impose par les cles etrangeres : demande -> consultation -> patient -> medecin. */
    @AfterEach
    void nettoyer() {
        demandeConsultationRepository.findAllById(demandesCreees).forEach(demande -> {
            if (demande.getConsultationId() != null) {
                consultationsCreees.add(demande.getConsultationId());
            }
        });
        demandeConsultationRepository.deleteAllById(demandesCreees);
        consultationRepository.findAllById(consultationsCreees)
                .forEach(consultation -> patientsCrees.add(consultation.getPatient().getId()));
        consultationRepository.deleteAllById(consultationsCreees);
        patientRepository.deleteAllById(patientsCrees);
        medecinRepository.deleteAllById(medecinsCrees);
    }

    // ---------- Succes ----------

    @Test
    void commencer_201_creePatientEtBrouillonLiesALaDemande(CapturedOutput sortie) throws Exception {
        UUID demande = demandeAcceptee();
        lireDemande(demande).andExpect(jsonPath("$.consultationId", nullValue()));

        JsonNode reponse = lireJson(commencer(junior, demande, corps(numeroDossier, medecinSenior.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.demandeId", is(demande.toString())))
                .andExpect(jsonPath("$.patientId", notNullValue()))
                .andExpect(jsonPath("$.consultationId", notNullValue()))
                .andExpect(jsonPath("$.statutConsultation", is("BROUILLON"))));
        String consultationId = reponse.get("consultationId").asString();
        String patientId = reponse.get("patientId").asString();

        // Le lien est visible dans la reponse de la demande (pour le junior comme pour le senior).
        lireDemande(demande).andExpect(jsonPath("$.consultationId", is(consultationId)));
        mockMvc.perform(en("MEDECIN_SENIOR", senior, get(URL + "/" + demande)))
                .andExpect(jsonPath("$.consultationId", is(consultationId)));

        // Patient et brouillon : seules les donnees de la demande et celles saisies par le junior.
        mockMvc.perform(en("MEDECIN_JUNIOR", junior, get("/api/consultations/" + consultationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut", is("BROUILLON")))
                .andExpect(jsonPath("$.patient.id", is(patientId)))
                .andExpect(jsonPath("$.patient.nomComplet", is(NOM_PATIENT)))
                .andExpect(jsonPath("$.patient.numeroDossierProxymed", is(numeroDossier)))
                .andExpect(jsonPath("$.patient.dateNaissance", is("1952-04-18")))
                .andExpect(jsonPath("$.patient.sexe", is("F")))
                .andExpect(jsonPath("$.patient.telephone", nullValue()))
                .andExpect(jsonPath("$.medecinSenior.id", is(medecinSenior.getId().intValue())))
                .andExpect(jsonPath("$.motifPrincipalConsultation", is(MOTIF)))
                .andExpect(jsonPath("$.origineDemande", nullValue()))
                .andExpect(jsonPath("$.medecinJuniorAffecte", nullValue()))
                .andExpect(jsonPath("$.decisionEligibilite", nullValue()));

        // Aucune donnee d'identite ni medicale dans les journaux.
        assertThat(sortie.getAll()).doesNotContain(NOM_PATIENT, MOTIF, numeroDossier);
    }

    // ---------- Droits ----------

    @Test
    void commencer_403_siAutreJunior_etRienNestCree() throws Exception {
        UUID demande = demandeAcceptee();
        long consultationsAvant = consultationRepository.count();

        verifierErreur(commencer(UUID.randomUUID(), demande, corps(numeroDossier, medecinSenior.getId())),
                403, "Acces refuse", MESSAGE_ROLE);

        verifierRienCree(demande, consultationsAvant);
    }

    @Test
    void commencer_403_siPasMedecinJunior() throws Exception {
        UUID demande = demandeAcceptee();
        for (String role : List.of("MEDECIN_SENIOR", "UTILISATEUR", "ADMIN")) {
            verifierErreur(mockMvc.perform(en(role, junior,
                            json(post(URL + "/" + demande + "/commencer"), corps(numeroDossier, medecinSenior.getId())))),
                    403, "Acces refuse", MESSAGE_ROLE);
        }
        verifierRienCree(demande, consultationRepository.count());
    }

    @Test
    void commencer_401_sansBadge() throws Exception {
        UUID demande = demandeAcceptee();
        mockMvc.perform(json(post(URL + "/" + demande + "/commencer"), corps(numeroDossier, medecinSenior.getId())))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Etat de la demande ----------

    @Test
    void commencer_409_siDemandeEnAttente() throws Exception {
        UUID demande = creerDemande();

        verifierErreur(commencer(junior, demande, corps(numeroDossier, medecinSenior.getId())),
                409, "Consultation impossible a commencer",
                "La demande de consultation " + demande + " n'est pas acceptee : seule une demande ACCEPTEE "
                        + "peut donner lieu a une consultation initiale");
        assertThat(patientRepository.existsByNumeroDossierProxymed(numeroDossier)).isFalse();
    }

    @Test
    void commencer_409_siDemandeRefusee() throws Exception {
        UUID demande = creerDemande();
        mockMvc.perform(en("MEDECIN_SENIOR", senior,
                        json(patch(URL + "/" + demande + "/refuser"), Map.of("motifRefus", "Hors perimetre"))))
                .andExpect(status().isOk());

        verifierErreur(commencer(junior, demande, corps(numeroDossier, medecinSenior.getId())),
                409, "Consultation impossible a commencer",
                "La demande de consultation " + demande + " n'est pas acceptee : seule une demande ACCEPTEE "
                        + "peut donner lieu a une consultation initiale");
        assertThat(patientRepository.existsByNumeroDossierProxymed(numeroDossier)).isFalse();
    }

    @Test
    void commencer_409_auDeuxiemeAppel_sansSecondPatient() throws Exception {
        UUID demande = demandeAcceptee();
        String premiere = lireJson(commencer(junior, demande, corps(numeroDossier, medecinSenior.getId()))
                .andExpect(status().isCreated())).get("consultationId").asString();
        long consultationsApres = consultationRepository.count();
        String autreNumero = numeroDossier + "-BIS";

        verifierErreur(commencer(junior, demande, corps(autreNumero, medecinSenior.getId())),
                409, "Consultation impossible a commencer",
                "La consultation initiale de la demande " + demande + " a deja ete commencee");

        assertThat(patientRepository.existsByNumeroDossierProxymed(autreNumero)).isFalse();
        assertThat(consultationRepository.count()).isEqualTo(consultationsApres);
        lireDemande(demande).andExpect(jsonPath("$.consultationId", is(premiere)));
    }

    @Test
    void commencer_404_siDemandeAbsente() throws Exception {
        UUID absente = UUID.randomUUID();
        verifierErreur(commencer(junior, absente, corps(numeroDossier, medecinSenior.getId())),
                404, "Ressource introuvable", "Demande de consultation introuvable : " + absente);
    }

    // ---------- Une erreur au milieu annule tout ----------

    /**
     * Le patient est cree AVANT le controle du medecin senior (fait par la creation du brouillon) :
     * un medecin JUNIOR passe comme senior fait donc echouer la deuxieme etape. Rien ne doit rester.
     */
    @Test
    void commencer_annuleTout_siErreurApresLaCreationDuPatient() throws Exception {
        UUID demande = demandeAcceptee();
        long patientsAvant = patientRepository.count();
        long consultationsAvant = consultationRepository.count();

        verifierErreur(commencer(junior, demande, corps(numeroDossier, medecinJunior.getId())),
                400, "Regle de gestion non respectee",
                "Le medecin senior doit avoir le role SENIOR (medecin " + medecinJunior.getId() + ")");

        assertThat(patientRepository.count()).isEqualTo(patientsAvant);
        verifierRienCree(demande, consultationsAvant);

        // La demande reste commencable : le meme appel corrige reussit.
        commencer(junior, demande, corps(numeroDossier, medecinSenior.getId())).andExpect(status().isCreated());
    }

    @Test
    void commencer_404_siMedecinSeniorAbsent_etRienNestCree() throws Exception {
        UUID demande = demandeAcceptee();
        long consultationsAvant = consultationRepository.count();

        commencer(junior, demande, corps(numeroDossier, Long.MAX_VALUE)).andExpect(status().isNotFound());

        verifierRienCree(demande, consultationsAvant);
    }

    @Test
    void commencer_409_siNumeroDeDossierDejaUtilise() throws Exception {
        Patient existant = patientRepository.save(Patient.builder()
                .numeroDossierProxymed(numeroDossier).nomComplet("Autre patient")
                .dateNaissance(LocalDate.of(1960, 1, 1)).sexe(Sexe.M).build());
        patientsCrees.add(existant.getId());
        UUID demande = demandeAcceptee();
        long consultationsAvant = consultationRepository.count();

        verifierErreur(commencer(junior, demande, corps(numeroDossier, medecinSenior.getId())),
                409, "Patient existant", "Un patient existe deja avec ce numero de dossier PROXYMED");

        assertThat(consultationRepository.count()).isEqualTo(consultationsAvant);
        lireDemande(demande).andExpect(jsonPath("$.consultationId", nullValue()));
    }

    // ---------- Validation du corps ----------

    @Test
    void commencer_400_siChampsObligatoiresManquants() throws Exception {
        UUID demande = demandeAcceptee();
        verifierErreur(commencer(junior, demande, Map.of()),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details", hasSize(4)));

        Map<String, Object> futur = new HashMap<>(corps(numeroDossier, medecinSenior.getId()));
        futur.put("dateNaissance", LocalDate.now().plusDays(1).toString());
        futur.put("numeroDossierProxymed", "N".repeat(101));
        verifierErreur(commencer(junior, demande, futur),
                400, "Validation echouee", "Un ou plusieurs champs sont invalides")
                .andExpect(jsonPath("$.details", hasSize(2)));
        lireDemande(demande).andExpect(jsonPath("$.consultationId", nullValue()));
    }

    /** L'identite du junior et les donnees reprises de la demande ne passent jamais par le corps. */
    @Test
    void commencer_400_siChampInconnuDansLeCorps() throws Exception {
        UUID demande = demandeAcceptee();
        for (String champ : List.of("juniorId", "nomComplet", "motifPrincipalConsultation")) {
            Map<String, Object> corps = new HashMap<>(corps(numeroDossier, medecinSenior.getId()));
            corps.put(champ, "valeur");
            verifierErreur(commencer(junior, demande, corps),
                    400, "Requete invalide", "Champ inconnu dans la requete : " + champ);
        }
        assertThat(patientRepository.existsByNumeroDossierProxymed(numeroDossier)).isFalse();
    }

    // ---------- Outils ----------

    private Medecin medecin(String nom, RoleMedecin role) {
        Medecin medecin = medecinRepository.save(Medecin.builder()
                .nom(nom).prenom("Test").numeroOrdre(role + "-" + System.nanoTime()).role(role).build());
        medecinsCrees.add(medecin.getId());
        return medecin;
    }

    private UUID creerDemande() throws Exception {
        JsonNode reponse = lireJson(mockMvc.perform(en("UTILISATEUR", demandeur, json(post(URL),
                        Map.of("nomPatient", NOM_PATIENT, "pourProche", true, "motif", MOTIF))))
                .andExpect(status().isCreated()));
        UUID id = UUID.fromString(reponse.get("id").asString());
        demandesCreees.add(id);
        return id;
    }

    private UUID demandeAcceptee() throws Exception {
        UUID id = creerDemande();
        mockMvc.perform(en("MEDECIN_SENIOR", senior,
                        json(patch(URL + "/" + id + "/accepter"), Map.of("juniorId", junior.toString()))))
                .andExpect(status().isOk());
        return id;
    }

    private ResultActions commencer(UUID compteJunior, UUID demande, Object corps) throws Exception {
        ResultActions resultat = mockMvc.perform(en("MEDECIN_JUNIOR", compteJunior,
                json(post(URL + "/" + demande + "/commencer"), corps)));
        // Pour le nettoyage, meme si l'assertion du test echoue ensuite.
        String reponse = resultat.andReturn().getResponse().getContentAsString();
        if (resultat.andReturn().getResponse().getStatus() == 201) {
            consultationsCreees.add(UUID.fromString(objectMapper.readTree(reponse).get("consultationId").asString()));
        }
        return resultat;
    }

    private ResultActions lireDemande(UUID demande) throws Exception {
        return mockMvc.perform(en("MEDECIN_JUNIOR", junior, get(URL + "/" + demande))).andExpect(status().isOk());
    }

    private void verifierRienCree(UUID demande, long consultationsAvant) throws Exception {
        assertThat(patientRepository.existsByNumeroDossierProxymed(numeroDossier)).isFalse();
        assertThat(consultationRepository.count()).isEqualTo(consultationsAvant);
        assertThat(demandeConsultationRepository.findById(demande).orElseThrow().getConsultationId()).isNull();
        lireDemande(demande).andExpect(jsonPath("$.consultationId", nullValue()));
    }

    private static Map<String, Object> corps(String numeroDossierProxymed, Long medecinSeniorId) {
        return Map.of(
                "numeroDossierProxymed", numeroDossierProxymed,
                "dateNaissance", "1952-04-18",
                "sexe", "F",
                "medecinSeniorId", medecinSeniorId);
    }

    private JsonNode lireJson(ResultActions resultat) throws Exception {
        return objectMapper.readTree(resultat.andReturn().getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, Object corps) {
        return requete.contentType("application/json").content(objectMapper.writeValueAsString(corps));
    }

    private MockHttpServletRequestBuilder en(String role, UUID compte, MockHttpServletRequestBuilder requete) {
        return requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role, compte));
    }

    private static ResultActions verifierErreur(ResultActions resultat, int statut, String erreur, String message)
            throws Exception {
        return resultat.andExpect(status().is(statut))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(statut)))
                .andExpect(jsonPath("$.erreur", is(erreur)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.horodatage", notNullValue()));
    }
}
