package com.proxymed.integration;

import com.proxymed.entity.DemandeConsultation;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.DemandeConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.FabriqueBadge;
import com.proxymed.support.PdfDeTest;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controle d'assignation sur le PDF de la Fiche 1, l'import de la fiche signee et le
 * telechargement du scan : un MEDECIN_JUNIOR n'accede qu'aux consultations liees a une demande
 * qui lui est assignee (compte lu dans le badge). Le senior garde l'acces a tout ; une consultation
 * sans demande liee reste accessible comme avant a tout medecin junior ou senior.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class ControleAssignationConsultationIntegrationTest {

    private static final String URL_IMPORT = "/api/fiches-signees/import";
    private static final String TYPE = "FICHE_1_CONSULTATION_INITIALE";
    private static final String MESSAGE_ROLE = "Votre role ne permet pas d'acceder a cette ressource";
    private static final String NOM_PATIENT = "Mariama Sow Assignation";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private DemandeConsultationRepository demandeConsultationRepository;
    @Autowired
    private FicheSigneeRepository ficheSigneeRepository;

    private final UUID juniorAssigne = UUID.randomUUID();
    private final UUID autreJunior = UUID.randomUUID();
    private final UUID senior = UUID.randomUUID();

    private Medecin medecinSenior;
    private UUID patientId;
    /** Consultation commencee a partir d'une demande assignee a juniorAssigne. */
    private String consultationAssignee;

    @BeforeEach
    void setUp() throws Exception {
        medecinSenior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Awa").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime()).nomComplet(NOM_PATIENT)
                .dateNaissance(LocalDate.of(1955, 1, 1)).sexe(Sexe.F).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
        consultationAssignee = creerConsultation();
        lierADemandeAssignee(consultationAssignee, juniorAssigne);
    }

    // ---------- Junior assigne ----------

    @Test
    void juniorAssigne_accedeAuxTroisEndpoints(CapturedOutput sortie) throws Exception {
        pdf("MEDECIN_JUNIOR", juniorAssigne, consultationAssignee)
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));

        byte[] fiche = ficheAvecQr(consultationAssignee);
        importer("MEDECIN_JUNIOR", juniorAssigne, fiche)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultationId", is(consultationAssignee)));

        ficheSignee("MEDECIN_JUNIOR", juniorAssigne, consultationAssignee)
                .andExpect(status().isOk())
                .andExpect(content().bytes(fiche));

        // Aucune donnee d'identite dans les journaux.
        assertThat(sortie.getAll()).doesNotContain(NOM_PATIENT);
    }

    // ---------- Autre junior : 403 ----------

    @Test
    void autreJunior_403_surLePdf() throws Exception {
        verifierRefus(pdf("MEDECIN_JUNIOR", autreJunior, consultationAssignee));
    }

    @Test
    void autreJunior_403_aLImport_etRienNestEnregistre(CapturedOutput sortie) throws Exception {
        verifierRefus(importer("MEDECIN_JUNIOR", autreJunior, ficheAvecQr(consultationAssignee)));

        assertThat(ficheSigneeRepository.existsByConsultation_Id(UUID.fromString(consultationAssignee))).isFalse();
        assertThat(sortie.getAll()).doesNotContain(NOM_PATIENT);
    }

    /** Le refus passe avant le 409 : un autre junior n'apprend pas qu'une fiche existe deja. */
    @Test
    void autreJunior_403_aLImport_memeSiUneFicheExisteDeja() throws Exception {
        enregistrerFiche(consultationAssignee);

        verifierRefus(importer("MEDECIN_JUNIOR", autreJunior, ficheAvecQr(consultationAssignee)));
    }

    @Test
    void autreJunior_403_surLaFicheSignee() throws Exception {
        enregistrerFiche(consultationAssignee);

        verifierRefus(ficheSignee("MEDECIN_JUNIOR", autreJunior, consultationAssignee));
    }

    /** Sans fiche importee, le 403 passe aussi avant le 404 « aucune fiche ». */
    @Test
    void autreJunior_403_surLaFicheSignee_memeSansFicheImportee() throws Exception {
        verifierRefus(ficheSignee("MEDECIN_JUNIOR", autreJunior, consultationAssignee));
    }

    // ---------- Senior : comme avant ----------

    @Test
    void senior_accedeAuxTroisEndpoints_sansEtreLeJuniorAssigne() throws Exception {
        pdf("MEDECIN_SENIOR", senior, consultationAssignee).andExpect(status().isOk());

        byte[] fiche = ficheAvecQr(consultationAssignee);
        importer("MEDECIN_SENIOR", senior, fiche).andExpect(status().isCreated());

        ficheSignee("MEDECIN_SENIOR", senior, consultationAssignee)
                .andExpect(status().isOk())
                .andExpect(content().bytes(fiche));
    }

    // ---------- Sans badge : 401 ----------

    @Test
    void sansBadge_401_surLesTroisEndpoints() throws Exception {
        enregistrerFiche(consultationAssignee);
        MockMultipartFile fichier = new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                ficheAvecQr(consultationAssignee));

        for (ResultActions resultat : List.of(
                mockMvc.perform(get("/api/consultations/" + consultationAssignee + "/pdf")),
                mockMvc.perform(multipart(URL_IMPORT).file(fichier)),
                mockMvc.perform(get("/api/consultations/" + consultationAssignee + "/fiche-signee")))) {
            verifierErreur(resultat, 401, "Non authentifie", "Badge d'authentification manquant");
        }
    }

    // ---------- Consultation sans demande liee : acces d'avant ----------

    @Test
    void consultationSansDemandeLiee_resteAccessibleATousLesJuniors() throws Exception {
        String sansDemande = creerConsultation();

        pdf("MEDECIN_JUNIOR", autreJunior, sansDemande).andExpect(status().isOk());

        byte[] fiche = ficheAvecQr(sansDemande);
        importer("MEDECIN_JUNIOR", autreJunior, fiche)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultationId", is(sansDemande)));

        ficheSignee("MEDECIN_JUNIOR", UUID.randomUUID(), sansDemande)
                .andExpect(status().isOk())
                .andExpect(content().bytes(fiche));
    }

    /** Consultation inexistante : le 404 d'avant est conserve pour le junior. */
    @Test
    void consultationInexistante_404_pourUnJunior() throws Exception {
        String absente = UUID.randomUUID().toString();

        pdf("MEDECIN_JUNIOR", autreJunior, absente).andExpect(status().isNotFound());
        ficheSignee("MEDECIN_JUNIOR", autreJunior, absente).andExpect(status().isNotFound());
        importer("MEDECIN_JUNIOR", autreJunior, ficheAvecQr(absente)).andExpect(status().isNotFound());
    }

    // ---------- Outils ----------

    private String creerConsultation() throws Exception {
        String reponse = mockMvc.perform(en("MEDECIN_SENIOR", senior, post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patientId,
                                "medecinSeniorId", medecinSenior.getId())))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asString();
    }

    /** Etat laisse par « commencer » : demande ACCEPTEE, assignee au junior, liee a la consultation. */
    private void lierADemandeAssignee(String consultationId, UUID junior) {
        demandeConsultationRepository.saveAndFlush(DemandeConsultation.builder()
                .demandeurId(UUID.randomUUID())
                .nomPatient(NOM_PATIENT)
                .motif("Douleurs thoraciques")
                .statut(StatutDemandeConsultation.ACCEPTEE)
                .medecinSeniorId(senior)
                .juniorId(junior)
                .dateCreation(Instant.now())
                .dateDecision(Instant.now())
                .consultationId(UUID.fromString(consultationId))
                .build());
    }

    private void enregistrerFiche(String consultationId) {
        byte[] contenu = ficheAvecQr(consultationId);
        ficheSigneeRepository.saveAndFlush(FicheSignee.builder()
                .consultation(consultationRepository.getReferenceById(UUID.fromString(consultationId)))
                .nomFichier("deja-importee.pdf")
                .typeContenu("application/pdf")
                .tailleOctets(contenu.length)
                .dateImport(Instant.now())
                .contenu(contenu)
                .build());
    }

    private byte[] ficheAvecQr(String consultationId) {
        return PdfDeTest.pdfAvecQr("{\"type\":\"" + TYPE + "\",\"patientId\":\"" + patientId
                + "\",\"consultationId\":\"" + consultationId + "\"}");
    }

    private ResultActions pdf(String role, UUID compte, String consultationId) throws Exception {
        return mockMvc.perform(en(role, compte, get("/api/consultations/" + consultationId + "/pdf")));
    }

    private ResultActions importer(String role, UUID compte, byte[] pdf) throws Exception {
        return mockMvc.perform(multipart(URL_IMPORT)
                .file(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role, compte)));
    }

    private ResultActions ficheSignee(String role, UUID compte, String consultationId) throws Exception {
        return mockMvc.perform(en(role, compte, get("/api/consultations/" + consultationId + "/fiche-signee")));
    }

    private MockHttpServletRequestBuilder en(String role, UUID compte, MockHttpServletRequestBuilder requete) {
        return requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role, compte));
    }

    private static void verifierRefus(ResultActions resultat) throws Exception {
        verifierErreur(resultat, 403, "Acces refuse", MESSAGE_ROLE);
    }

    private static void verifierErreur(ResultActions resultat, int statut, String erreur, String message)
            throws Exception {
        resultat.andExpect(status().is(statut))
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(statut)))
                .andExpect(jsonPath("$.erreur", is(erreur)))
                .andExpect(jsonPath("$.message", is(message)))
                .andExpect(jsonPath("$.horodatage", notNullValue()));
    }
}
