package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.service.VisaFichesLotService;
import com.proxymed.service.model.ResultatDecisionModel;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Liste du senior, visa et renvoi en lot (GET /api/fiches-signees, POST /api/fiches-signees/visa et
 * /renvoi). Volontairement sans @Transactional : une transaction de test commune masquerait
 * l'independance des fiches et empecherait le test de concurrence. Ce qui est enregistre est relu en
 * base apres la requete ; les donnees sont nettoyees apres chaque test.
 * Les fiches du test sont importees en 1990-1991 : plus anciennes que celles que d'autres classes de
 * test auraient laissees, elles sont donc en tete de la liste triee par date d'import croissante.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@ExtendWith(OutputCaptureExtension.class)
class VisaFichesSeniorIntegrationTest {

    private static final String URL_LISTE = "/api/fiches-signees";
    private static final String URL_VISA = "/api/fiches-signees/visa";
    private static final String URL_RENVOI = "/api/fiches-signees/renvoi";
    private static final String NOM_PATIENT = "Awa Diop Visa";
    private static final String EMAIL_SENIOR = "medecin_senior@proxymed.test";
    private static final byte[] SCAN = "%PDF-1.4 scan de test".getBytes(StandardCharsets.US_ASCII);
    private static final Instant JANVIER_1990 = Instant.parse("1990-01-01T08:00:00Z");

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
    private VisaFichesLotService visaFichesLotService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID patientId;
    private String numeroDossier;
    private UUID seniorId;
    private String bearerSenior;

    @BeforeEach
    void setUp() {
        numeroDossier = "DOS-VISA-" + System.nanoTime();
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed(numeroDossier).nomComplet(NOM_PATIENT)
                .dateNaissance(LocalDate.of(1950, 3, 4)).sexe(Sexe.F).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
        seniorId = UUID.randomUUID();
        bearerSenior = fabriqueBadge.bearer("MEDECIN_SENIOR", seniorId);
    }

    @AfterEach
    void nettoyer() {
        jdbc.update("DELETE FROM fiche_signee WHERE consultation_id IN "
                + "(SELECT id FROM consultation_initiale WHERE patient_id = ?)", patientId);
        jdbc.update("DELETE FROM consultation_initiale WHERE patient_id = ?", patientId);
        jdbc.update("DELETE FROM patient WHERE id = ?", patientId);
    }

    // --- Liste ---

    @Test
    void senior_listeEnAttenteParDefaut_paginee_laPlusAncienneDabord() throws Exception {
        // Creees dans le desordre pour verifier le tri ; la VISEE, plus ancienne encore, est exclue.
        UUID deuxieme = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990.plusSeconds(60));
        UUID visee = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.VISEE, JANVIER_1990.minusSeconds(60));
        UUID troisieme = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990.plusSeconds(120));
        UUID premiere = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);

        mockMvc.perform(get(URL_LISTE).param("taille", "2").header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.taille", is(2)))
                .andExpect(jsonPath("$.elements.length()", is(2)))
                .andExpect(jsonPath("$.elements[0].visiteId", is(premiere.toString())))
                .andExpect(jsonPath("$.elements[0].patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.elements[0].nomPatient", is(NOM_PATIENT)))
                .andExpect(jsonPath("$.elements[0].numeroDossierProxymed", is(numeroDossier)))
                .andExpect(jsonPath("$.elements[0].typeVisite", is("INITIALE")))
                .andExpect(jsonPath("$.elements[0].statut", is("EN_ATTENTE")))
                .andExpect(jsonPath("$.elements[0].dateImport", is("1990-01-01T08:00:00Z")))
                .andExpect(jsonPath("$.elements[0].ficheId").exists())
                .andExpect(jsonPath("$.elements[0].nomSeniorVisa").doesNotExist())
                .andExpect(jsonPath("$.elements[1].visiteId", is(deuxieme.toString())))
                .andExpect(jsonPath("$.elements[1].typeVisite", is("ROUTINE")));

        mockMvc.perform(get(URL_LISTE).param("page", "1").param("taille", "2")
                        .header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page", is(1)))
                .andExpect(jsonPath("$.elements[0].visiteId", is(troisieme.toString())));

        String reponse = mockMvc.perform(get(URL_LISTE).param("taille", "100")
                        .header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(reponse).doesNotContain(visee.toString());

        mockMvc.perform(get(URL_LISTE).param("statut", "VISEE").param("taille", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elements[0].visiteId", is(visee.toString())))
                .andExpect(jsonPath("$.elements[0].statut", is("VISEE")))
                .andExpect(jsonPath("$.elements[0].nomSeniorVisa", is("Dr Ancien")))
                .andExpect(jsonPath("$.elements[0].dateVisa").exists());
    }

    @Test
    void senior_listeParametresInvalides_400() throws Exception {
        mockMvc.perform(get(URL_LISTE).param("taille", "101").header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("La taille de page doit etre comprise entre 1 et 100")));
        mockMvc.perform(get(URL_LISTE).param("page", "-1").header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL_LISTE).param("statut", "INCONNU").header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Valeur invalide pour le parametre \"statut\"")));
    }

    // --- Visa ---

    @Test
    void senior_viseTroisFiches_uneEnAttente_uneDejaVisee_uneInexistante_resultatsFicheParFiche() throws Exception {
        UUID enAttente = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
        UUID dejaVisee = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.VISEE, JANVIER_1990);
        UUID inexistante = UUID.randomUUID();
        Instant avant = Instant.now();

        mockMvc.perform(json(post(URL_VISA), bearerSenior,
                        "{\"visiteIds\":[\"" + enAttente + "\",\"" + dejaVisee + "\",\"" + inexistante + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreFiches", is(3)))
                .andExpect(jsonPath("$.nombreReussies", is(1)))
                .andExpect(jsonPath("$.nombreErreurs", is(2)))
                .andExpect(jsonPath("$.resultats[0].index", is(0)))
                .andExpect(jsonPath("$.resultats[0].visiteId", is(enAttente.toString())))
                .andExpect(jsonPath("$.resultats[0].resultat", is("VISEE")))
                .andExpect(jsonPath("$.resultats[0].code", is(200)))
                .andExpect(jsonPath("$.resultats[0].statut", is("VISEE")))
                .andExpect(jsonPath("$.resultats[0].erreur").doesNotExist())
                .andExpect(jsonPath("$.resultats[1].visiteId", is(dejaVisee.toString())))
                .andExpect(jsonPath("$.resultats[1].resultat", is("ERREUR")))
                .andExpect(jsonPath("$.resultats[1].code", is(409)))
                .andExpect(jsonPath("$.resultats[1].erreur", is("Action incompatible avec le statut de la fiche")))
                .andExpect(jsonPath("$.resultats[1].message",
                        is("La fiche de la visite " + dejaVisee + " est VISEE : seule une fiche EN_ATTENTE peut etre visee")))
                .andExpect(jsonPath("$.resultats[2].visiteId", is(inexistante.toString())))
                .andExpect(jsonPath("$.resultats[2].resultat", is("ERREUR")))
                .andExpect(jsonPath("$.resultats[2].code", is(404)))
                .andExpect(jsonPath("$.resultats[2].erreur", is("Ressource introuvable")));

        Map<String, Object> visee = ligneFiche(enAttente);
        assertThat(visee.get("STATUT")).isEqualTo("VISEE");
        assertThat(visee.get("SENIOR_VISA_ID")).isEqualTo(seniorId);
        assertThat(visee.get("NOM_SENIOR_VISA")).isEqualTo(EMAIL_SENIOR);
        assertThat(dateVisa(enAttente)).isAfterOrEqualTo(avant.minusSeconds(1)).isBeforeOrEqualTo(Instant.now());
        assertThat(((Number) visee.get("VERSION")).longValue()).isEqualTo(1L);

        Map<String, Object> inchangee = ligneFiche(dejaVisee);
        assertThat(inchangee.get("NOM_SENIOR_VISA")).isEqualTo("Dr Ancien");
        assertThat(((Number) inchangee.get("VERSION")).longValue()).isZero();
    }

    @Test
    void senior_seniorPrisDuBadge_jamaisDuCorps() throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);

        mockMvc.perform(json(post(URL_VISA), bearerSenior,
                        "{\"visiteIds\":[\"" + visite + "\"],\"seniorVisaId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Champ inconnu dans la requete : seniorVisaId")));
        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
    }

    @Test
    void senior_viseListeVideTropLongueOuAvecDoublons_400_rienNestVise() throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);

        mockMvc.perform(json(post(URL_VISA), bearerSenior, "{\"visiteIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Validation echouee")));
        mockMvc.perform(json(post(URL_VISA), bearerSenior, "{}"))
                .andExpect(status().isBadRequest());

        List<String> cinquanteEtUne = IntStream.range(0, 50).mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toCollection(ArrayList::new));
        cinquanteEtUne.add(visite.toString());
        mockMvc.perform(json(post(URL_VISA), bearerSenior, "{\"visiteIds\":" + tableauJson(cinquanteEtUne) + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("visiteIds : au plus 50 fiches par requete")));

        mockMvc.perform(json(post(URL_VISA), bearerSenior,
                        "{\"visiteIds\":[\"" + visite + "\",\"" + visite + "\"]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message",
                        is("La liste contient plusieurs fois la meme visite : aucune fiche n'a ete traitee")));

        mockMvc.perform(json(post(URL_RENVOI), bearerSenior,
                        "{\"visiteIds\":[],\"motif\":\"Scan illisible\"}"))
                .andExpect(status().isBadRequest());

        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
    }

    @Test
    void lot_cinquanteFiches_estAccepte() throws Exception {
        List<String> cinquante = IntStream.range(0, 50).mapToObj(i -> UUID.randomUUID().toString()).toList();
        mockMvc.perform(json(post(URL_VISA), bearerSenior, "{\"visiteIds\":" + tableauJson(cinquante) + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreFiches", is(50)))
                .andExpect(jsonPath("$.nombreErreurs", is(50)));
    }

    @Test
    void service_sansNomDeSenior_refuseToutLeLot() {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);

        assertThatThrownBy(() -> visaFichesLotService.viser(List.of(visite), seniorId, null))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> visaFichesLotService.viser(List.of(visite), seniorId, "  "))
                .isInstanceOf(RegleGestionException.class);
        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
    }

    // --- Renvoi ---

    @Test
    void senior_renvoieAvecMotif_ficheRenvoyee_motifEnregistre(CapturedOutput sortie) throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
        UUID dejaVisee = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.VISEE, JANVIER_1990);
        String motif = "Signature du patient absente en page 2";

        mockMvc.perform(json(post(URL_RENVOI), bearerSenior,
                        "{\"visiteIds\":[\"" + visite + "\",\"" + dejaVisee + "\"],\"motif\":\"  " + motif + " \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreReussies", is(1)))
                .andExpect(jsonPath("$.resultats[0].resultat", is("RENVOYEE")))
                .andExpect(jsonPath("$.resultats[0].code", is(200)))
                .andExpect(jsonPath("$.resultats[0].statut", is("RENVOYEE")))
                .andExpect(jsonPath("$.resultats[1].code", is(409)));

        Map<String, Object> renvoyee = ligneFiche(visite);
        assertThat(renvoyee.get("STATUT")).isEqualTo("RENVOYEE");
        assertThat(renvoyee.get("MOTIF_RENVOI")).isEqualTo(motif);
        assertThat(ligneFiche(dejaVisee).get("STATUT")).isEqualTo("VISEE");

        mockMvc.perform(get(URL_LISTE).param("statut", "RENVOYEE").param("taille", "1")
                        .header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(jsonPath("$.elements[0].visiteId", is(visite.toString())))
                .andExpect(jsonPath("$.elements[0].motifRenvoi", is(motif)));

        assertThat(sortie.getAll()).doesNotContain(NOM_PATIENT).doesNotContain(motif).doesNotContain(visite.toString());
    }

    @Test
    void senior_renvoieSansMotifVideOuTropLong_400_rienNestRenvoye() throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.ROUTINE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
        String liste = "\"visiteIds\":[\"" + visite + "\"]";

        mockMvc.perform(json(post(URL_RENVOI), bearerSenior, "{" + liste + "}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("motif : le motif du renvoi est obligatoire")));
        mockMvc.perform(json(post(URL_RENVOI), bearerSenior, "{" + liste + ",\"motif\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(json(post(URL_RENVOI), bearerSenior, "{" + liste + ",\"motif\":\"" + "x".repeat(1001) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0]", is("motif : le motif du renvoi ne doit pas depasser 1000 caracteres")));

        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
        assertThat(ligneFiche(visite).get("MOTIF_RENVOI")).isNull();
    }

    // --- Concurrence ---

    @Test
    void deuxVisasSimultanes_unSeulPasse_lautreRecoit409() throws Exception {
        for (int essai = 0; essai < 5; essai++) {
            UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
            UUID seniorA = UUID.randomUUID();
            UUID seniorB = UUID.randomUUID();
            CyclicBarrier depart = new CyclicBarrier(2);
            ExecutorService executeur = Executors.newFixedThreadPool(2);
            try {
                Future<ResultatDecisionModel> a = executeur.submit(() -> {
                    depart.await();
                    return visaFichesLotService.viser(List.of(visite), seniorA, "a@proxymed.test").get(0);
                });
                Future<ResultatDecisionModel> b = executeur.submit(() -> {
                    depart.await();
                    return visaFichesLotService.viser(List.of(visite), seniorB, "b@proxymed.test").get(0);
                });
                List<ResultatDecisionModel> resultats = List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS));

                assertThat(resultats).filteredOn(ResultatDecisionModel::reussi).hasSize(1);
                ResultatDecisionModel perdant = resultats.stream().filter(r -> !r.reussi()).findFirst().orElseThrow();
                assertThat(perdant.erreur()).isInstanceOf(com.proxymed.exception.ConflitEtatException.class);
                UUID gagnant = resultats.get(0).reussi() ? seniorA : seniorB;
                Map<String, Object> ligne = ligneFiche(visite);
                assertThat(ligne.get("STATUT")).isEqualTo("VISEE");
                assertThat(ligne.get("SENIOR_VISA_ID")).isEqualTo(gagnant);
                assertThat(((Number) ligne.get("VERSION")).longValue()).isEqualTo(1L);
            } finally {
                executeur.shutdownNow();
            }
        }
    }

    @Test
    void versionPerimee_aucuneModification() {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
        UUID ficheId = ficheSigneeRepository.findEtatByConsultationId(visite).orElseThrow().getId();
        // Un autre traitement a modifie la fiche (version 0 -> 1) apres la lecture d'un premier senior.
        jdbc.update("UPDATE fiche_signee SET version = version + 1 WHERE id = ?", ficheId);

        Integer modifiees = transactionTemplate.execute(tx ->
                ficheSigneeRepository.viserSiVersion(ficheId, 0L, seniorId, EMAIL_SENIOR, Instant.now()));

        assertThat(modifiees).isZero();
        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
    }

    // --- Droits ---

    @Test
    void adminEtJunior_403_sansBadge_401_rienNestModifie() throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);
        String visa = "{\"visiteIds\":[\"" + visite + "\"]}";
        String renvoi = "{\"visiteIds\":[\"" + visite + "\"],\"motif\":\"Scan illisible\"}";

        for (String role : List.of("ADMIN", "MEDECIN_JUNIOR")) {
            String bearer = fabriqueBadge.bearer(role);
            mockMvc.perform(get(URL_LISTE).header(HttpHeaders.AUTHORIZATION, bearer))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.erreur", is("Acces refuse")));
            mockMvc.perform(json(post(URL_VISA), bearer, visa)).andExpect(status().isForbidden());
            mockMvc.perform(json(post(URL_RENVOI), bearer, renvoi)).andExpect(status().isForbidden());
        }

        mockMvc.perform(get(URL_LISTE)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL_VISA).contentType(MediaType.APPLICATION_JSON).content(visa))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(URL_RENVOI).contentType(MediaType.APPLICATION_JSON).content(renvoi))
                .andExpect(status().isUnauthorized());

        assertThat(ligneFiche(visite).get("STATUT")).isEqualTo("EN_ATTENTE");
    }

    @Test
    void senior_litLeScanDeLaFiche() throws Exception {
        UUID visite = visiteAvecFiche(TypeVisite.INITIALE, StatutFicheSignee.EN_ATTENTE, JANVIER_1990);

        mockMvc.perform(get("/api/consultations/" + visite + "/fiche-signee")
                        .header(HttpHeaders.AUTHORIZATION, bearerSenior))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"scan.pdf\""))
                .andExpect(content().bytes(SCAN));
    }

    // --- Outils ---

    /** Une visite du patient avec sa fiche importee a la date donnee ; une fiche VISEE porte un visa ancien. */
    private UUID visiteAvecFiche(TypeVisite type, StatutFicheSignee statut, Instant dateImport) {
        ConsultationInitiale visite = consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientId))
                .statut(StatutConsultation.BROUILLON)
                .typeVisite(type)
                .build());
        FicheSignee.FicheSigneeBuilder fiche = FicheSignee.builder()
                .consultation(visite)
                .nomFichier("scan.pdf")
                .typeContenu("application/pdf")
                .tailleOctets(SCAN.length)
                .dateImport(dateImport)
                .contenu(SCAN)
                .statut(statut);
        if (statut == StatutFicheSignee.VISEE) {
            fiche.seniorVisaId(UUID.randomUUID()).nomSeniorVisa("Dr Ancien").dateVisa(dateImport.plusSeconds(3600));
        }
        ficheSigneeRepository.save(fiche.build());
        return visite.getId();
    }

    private Map<String, Object> ligneFiche(UUID visiteId) {
        return jdbc.queryForMap("SELECT statut, senior_visa_id, nom_senior_visa, motif_renvoi, version "
                + "FROM fiche_signee WHERE consultation_id = ?", visiteId);
    }

    private Instant dateVisa(UUID visiteId) {
        return jdbc.queryForObject("SELECT date_visa FROM fiche_signee WHERE consultation_id = ?",
                java.time.OffsetDateTime.class, visiteId).toInstant();
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String bearer, String corps) {
        return requete.header(HttpHeaders.AUTHORIZATION, bearer).contentType(MediaType.APPLICATION_JSON).content(corps);
    }

    private static String tableauJson(List<String> valeurs) {
        return valeurs.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
