package com.proxymed.security;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.DemandeConsultation;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.enums.StatutFicheSignee;
import com.proxymed.enums.TypeVisite;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.DemandeConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.FabriqueBadge;
import com.proxymed.support.PdfDeTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/**
 * Matrice role x endpoint : pour chaque endpoint et chaque appelant (ADMIN, MEDECIN_JUNIOR,
 * MEDECIN_SENIOR, UTILISATEUR, sans badge), le statut attendu. Un role autorise obtient un 2xx (ou
 * le code metier indique), un role non autorise 403, un appel sans badge 401. Le junior n'est
 * autorise nulle part. Chaque cas tourne dans sa propre transaction, annulee a la fin.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@Transactional
class MatriceAccesIntegrationTest {

    private static final String ADMIN = "ADMIN";
    private static final String JUNIOR = "MEDECIN_JUNIOR";
    private static final String SENIOR = "MEDECIN_SENIOR";
    private static final String UTILISATEUR = "UTILISATEUR";
    private static final String SANS_BADGE = "SANS_BADGE";
    private static final List<String> APPELANTS = List.of(ADMIN, JUNIOR, SENIOR, UTILISATEUR, SANS_BADGE);

    /** Statut attendu pour un role autorise : n'importe quel 2xx. */
    private static final int SUCCES = 0;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private FicheSigneeRepository ficheSigneeRepository;
    @Autowired
    private DemandeConsultationRepository demandeConsultationRepository;

    private UUID patientId;
    private UUID visiteBrouillon;
    private UUID visiteVisee;
    private UUID visiteEnAttente;
    private UUID visiteARenvoyer;
    private Long medecinId;
    private UUID demandeId;

    /**
     * Une ligne de la matrice : la requete (construite sur les donnees du test), les roles autorises
     * et le statut qu'ils obtiennent.
     */
    private record Ligne(String nom, Function<MatriceAccesIntegrationTest, AbstractMockHttpServletRequestBuilder<?>> requete,
                         Set<String> autorises, int statutAutorise) {
        @Override
        public String toString() {
            return nom;
        }
    }

    private static Stream<Ligne> lignes() {
        return Stream.of(
                // --- Donnees medicales : SENIOR seul ---
                new Ligne("GET /api/consultations", t -> get("/api/consultations"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/consultations/en-attente-dass", t -> get("/api/consultations/en-attente-dass"),
                        Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/consultations/{id}", t -> get("/api/consultations/" + t.visiteBrouillon),
                        Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/consultations/{id}/pdf", t -> get("/api/consultations/" + t.visiteBrouillon + "/pdf"),
                        Set.of(SENIOR), SUCCES),
                new Ligne("POST /api/consultations (fermee, 409)", t -> json(post("/api/consultations"), "{}"),
                        Set.of(SENIOR), 409),
                new Ligne("PUT /api/consultations/{id}", t -> json(put("/api/consultations/" + t.visiteBrouillon),
                        "{\"motifPrincipalConsultation\":\"Chute\"}"), Set.of(SENIOR), SUCCES),
                new Ligne("POST /api/consultations/{id}/signer (fiche incomplete, 409)",
                        t -> post("/api/consultations/" + t.visiteBrouillon + "/signer"), Set.of(SENIOR), 409),
                new Ligne("GET /api/consultations/{id}/antecedents-maladies",
                        t -> get("/api/consultations/" + t.visiteBrouillon + "/antecedents-maladies"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/consultations/{id}/fiche-signee (scan)",
                        t -> get("/api/consultations/" + t.visiteVisee + "/fiche-signee"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/patients/{id}/fiches-signees",
                        t -> get("/api/patients/" + t.patientId + "/fiches-signees"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/fiches-signees (a viser)", t -> get("/api/fiches-signees"), Set.of(SENIOR), SUCCES),
                new Ligne("POST /api/fiches-signees/visa", t -> json(post("/api/fiches-signees/visa"),
                        "{\"visiteIds\":[\"" + t.visiteEnAttente + "\"]}"), Set.of(SENIOR), SUCCES),
                new Ligne("POST /api/fiches-signees/renvoi", t -> json(post("/api/fiches-signees/renvoi"),
                        "{\"visiteIds\":[\"" + t.visiteARenvoyer + "\"],\"motif\":\"Scan illisible\"}"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/fiches-signees/{id}/rapport",
                        t -> get("/api/fiches-signees/" + t.visiteVisee + "/rapport"), Set.of(SENIOR), SUCCES),
                new Ligne("GET /api/referentiels/structures", t -> get("/api/referentiels/structures"), Set.of(SENIOR), SUCCES),

                // --- Administratif : ADMIN seul ---
                new Ligne("POST /api/fiches-signees/import", t -> multipart("/api/fiches-signees/import")
                        .file(new MockMultipartFile("fichiers", "scan.pdf", "application/pdf", PdfDeTest.pdfAvecQr((String) null))),
                        Set.of(ADMIN), SUCCES),
                new Ligne("POST /api/visites", t -> json(post("/api/visites"),
                        "{\"patientId\":\"" + t.patientId + "\",\"typeVisite\":\"ROUTINE\"}"), Set.of(ADMIN), SUCCES),
                new Ligne("POST /api/visites/impression", t -> json(post("/api/visites/impression"),
                        "{\"fiches\":[{\"patientId\":\"" + t.patientId + "\",\"typeVisite\":\"INITIALE\"}]}"), Set.of(ADMIN), SUCCES),
                new Ligne("POST /api/medecins", t -> json(post("/api/medecins"),
                        "{\"nom\":\"Ndiaye\",\"prenom\":\"Ibou\",\"numeroOrdre\":\"MAT-" + UUID.randomUUID() + "\",\"role\":\"SENIOR\"}"),
                        Set.of(ADMIN), SUCCES),
                new Ligne("PUT /api/medecins/{id}", t -> json(put("/api/medecins/" + t.medecinId),
                        "{\"nom\":\"Ndiaye\",\"prenom\":\"Ibou\",\"numeroOrdre\":\"MAT-" + UUID.randomUUID() + "\",\"role\":\"SENIOR\"}"),
                        Set.of(ADMIN), SUCCES),
                new Ligne("DELETE /api/medecins/{id}", t -> delete("/api/medecins/" + t.medecinId), Set.of(ADMIN), SUCCES),

                // --- Identite et annuaire : SENIOR et ADMIN ---
                new Ligne("GET /api/patients", t -> get("/api/patients"), Set.of(SENIOR, ADMIN), SUCCES),
                new Ligne("GET /api/patients/{id}", t -> get("/api/patients/" + t.patientId), Set.of(SENIOR, ADMIN), SUCCES),
                new Ligne("POST /api/patients", t -> json(post("/api/patients"),
                        "{\"numeroDossierProxymed\":\"DOS-MAT-" + UUID.randomUUID() + "\",\"nomComplet\":\"Coumba Ka\","
                                + "\"dateNaissance\":\"1950-01-01\",\"sexe\":\"F\"}"), Set.of(SENIOR, ADMIN), SUCCES),
                new Ligne("PUT /api/patients/{id}", t -> json(put("/api/patients/" + t.patientId),
                        "{\"numeroDossierProxymed\":\"DOS-MAT-" + UUID.randomUUID() + "\",\"nomComplet\":\"Coumba Ka\","
                                + "\"dateNaissance\":\"1950-01-01\",\"sexe\":\"F\"}"), Set.of(SENIOR, ADMIN), SUCCES),
                new Ligne("GET /api/medecins", t -> get("/api/medecins"), Set.of(SENIOR, ADMIN), SUCCES),

                // --- Demandes de consultation ---
                new Ligne("POST /api/demandes-consultation", t -> json(post("/api/demandes-consultation"),
                        "{\"nomPatient\":\"Awa\",\"pourProche\":false,\"motif\":\"Fievre\"}"), Set.of(UTILISATEUR), SUCCES),
                new Ligne("GET /api/demandes-consultation/mes-demandes", t -> get("/api/demandes-consultation/mes-demandes"),
                        Set.of(UTILISATEUR), SUCCES),
                new Ligne("GET /api/demandes-consultation", t -> get("/api/demandes-consultation"), Set.of(SENIOR), SUCCES),
                new Ligne("PATCH /api/demandes-consultation/{id}/accepter",
                        t -> patch("/api/demandes-consultation/" + t.demandeId + "/accepter"), Set.of(SENIOR), SUCCES),
                new Ligne("PATCH /api/demandes-consultation/{id}/refuser",
                        t -> json(patch("/api/demandes-consultation/" + t.demandeId + "/refuser"), "{\"motifRefus\":\"Hors zone\"}"),
                        Set.of(SENIOR), SUCCES));
    }

    static Stream<Arguments> matrice() {
        return lignes().flatMap(ligne -> APPELANTS.stream().map(appelant -> Arguments.of(ligne, appelant)));
    }

    @BeforeEach
    void setUp() {
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-MAT-" + System.nanoTime()).nomComplet("Patient Matrice")
                .dateNaissance(LocalDate.of(1951, 2, 3)).sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
        Medecin senior = medecinRepository.save(Medecin.builder().nom("Diop").prenom("Awa")
                .numeroOrdre("SEN-MAT-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        medecinId = medecinRepository.save(Medecin.builder().nom("Sow").prenom("Ali")
                .numeroOrdre("MED-MAT-" + System.nanoTime()).role(RoleMedecin.SENIOR).build()).getId();
        visiteBrouillon = visite(senior);
        visiteVisee = visite(senior);
        visiteEnAttente = visite(senior);
        visiteARenvoyer = visite(senior);
        fiche(visiteVisee, StatutFicheSignee.VISEE);
        fiche(visiteEnAttente, StatutFicheSignee.EN_ATTENTE);
        fiche(visiteARenvoyer, StatutFicheSignee.EN_ATTENTE);
        demandeId = demandeConsultationRepository.save(DemandeConsultation.builder()
                .demandeurId(UUID.randomUUID()).nomPatient("Awa").pourProche(false).motif("Fievre")
                .statut(StatutDemandeConsultation.EN_ATTENTE).dateCreation(Instant.now()).build()).getId();
    }

    @ParameterizedTest(name = "{0} / {1}")
    @MethodSource("matrice")
    void statutSelonLeRole(Ligne ligne, String appelant) throws Exception {
        AbstractMockHttpServletRequestBuilder<?> requete = ligne.requete().apply(this);
        if (!SANS_BADGE.equals(appelant)) {
            requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(appelant));
        }

        int statut = mockMvc.perform(requete).andReturn().getResponse().getStatus();

        if (SANS_BADGE.equals(appelant)) {
            assertThat(statut).as("sans badge").isEqualTo(401);
        } else if (!ligne.autorises().contains(appelant)) {
            assertThat(statut).as(appelant + " non autorise").isEqualTo(403);
        } else if (ligne.statutAutorise() == SUCCES) {
            assertThat(statut).as(appelant + " autorise").isBetween(200, 299);
        } else {
            assertThat(statut).as(appelant + " autorise").isEqualTo(ligne.statutAutorise());
        }
    }

    /** Endpoints supprimes : plus aucun 2xx, meme pour un role qui y avait acces. */
    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("endpointsSupprimes")
    void endpointsSupprimes_nexistentPlus(String methode, String adresse, String role) throws Exception {
        MockHttpServletRequestBuilder requete = "POST".equals(methode) ? json(post(adresse), "{}") : get(adresse);
        int statut = mockMvc.perform(requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role)))
                .andReturn().getResponse().getStatus();
        assertThat(statut).isBetween(400, 499).isNotIn(401);
    }

    static Stream<Arguments> endpointsSupprimes() {
        return Stream.of(
                Arguments.of("GET", "/api/consultations/download-pdf", SENIOR),
                Arguments.of("GET", "/api/demandes-consultation/assignees-a-moi", JUNIOR),
                Arguments.of("POST", "/api/demandes-consultation/" + UUID.randomUUID() + "/commencer", JUNIOR));
    }

    private UUID visite(Medecin senior) {
        return consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientId))
                .medecinSenior(senior)
                .statut(StatutConsultation.BROUILLON)
                .typeVisite(TypeVisite.INITIALE)
                .dateConsultation(LocalDate.now())
                .build()).getId();
    }

    private void fiche(UUID visiteId, StatutFicheSignee statut) {
        byte[] scan = PdfDeTest.pdfAvecQr((String) null);
        FicheSignee.FicheSigneeBuilder fiche = FicheSignee.builder()
                .consultation(consultationRepository.getReferenceById(visiteId))
                .nomFichier("scan.pdf").typeContenu("application/pdf").tailleOctets(scan.length)
                .dateImport(Instant.now()).contenu(scan).statut(statut);
        if (statut == StatutFicheSignee.VISEE) {
            fiche.seniorVisaId(UUID.randomUUID()).nomSeniorVisa("senior@proxymed.test").dateVisa(Instant.now());
        }
        ficheSigneeRepository.save(fiche.build());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder requete, String corps) {
        return requete.contentType(MediaType.APPLICATION_JSON).content(corps);
    }
}
