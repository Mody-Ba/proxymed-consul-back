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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import en lot par l'admin (POST /api/fiches-signees/import, partie "fichiers" repetee) : un
 * resultat par fichier, une transaction par fichier. Volontairement sans @Transactional : une
 * transaction de test commune masquerait l'independance des fichiers (ce qui est enregistre est
 * relu en base apres la requete). Les donnees sont nettoyees apres chaque test.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(FabriqueBadge.class)
@ExtendWith(OutputCaptureExtension.class)
class ImportFichesLotIntegrationTest {

    private static final String URL = "/api/fiches-signees/import";
    private static final String NOM_PATIENT = "Ndeye Fall Lot";
    private static final byte[] PDF_SANS_QR = PdfDeTest.pdfAvecQr((String) null);
    private static final byte[] PAS_UN_PDF = "ceci n'est pas un PDF".getBytes(StandardCharsets.US_ASCII);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private FabriqueBadge fabriqueBadge;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @MockitoSpyBean
    private DetecteurQrPdf detecteurQrPdf;

    private final List<UUID> patientsCrees = new ArrayList<>();
    private UUID patientId;
    private UUID visiteA;
    private UUID visiteB;

    @BeforeEach
    void setUp() {
        patientId = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-LOT-" + System.nanoTime()).nomComplet(NOM_PATIENT)
                .dateNaissance(LocalDate.of(1948, 5, 6)).sexe(Sexe.F).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
        patientsCrees.add(patientId);
        visiteA = creerVisite(TypeVisite.INITIALE);
        visiteB = creerVisite(TypeVisite.ROUTINE);
    }

    @AfterEach
    void nettoyer() {
        for (UUID patient : patientsCrees) {
            jdbc.update("DELETE FROM fiche_signee WHERE consultation_id IN "
                    + "(SELECT id FROM consultation_initiale WHERE patient_id = ?)", patient);
            jdbc.update("DELETE FROM consultation_initiale WHERE patient_id = ?", patient);
            jdbc.update("DELETE FROM patient WHERE id = ?", patient);
        }
    }

    @Test
    void admin_troisFichiers_unReussi_unQrIllisible_unNonPdf_resultatsFicheParFiche(CapturedOutput sortie) throws Exception {
        byte[] fiche = ficheDe(visiteA);

        String reponse = importer("ADMIN",
                fichier("scan-a.pdf", "application/pdf", fiche),
                fichier("scan-sans-qr.pdf", "application/pdf", PDF_SANS_QR),
                fichier("photo.jpg", "image/jpeg", PAS_UN_PDF))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreFichiers", is(3)))
                .andExpect(jsonPath("$.nombreImportees", is(1)))
                .andExpect(jsonPath("$.nombreErreurs", is(2)))

                .andExpect(jsonPath("$.resultats[0].index", is(0)))
                .andExpect(jsonPath("$.resultats[0].nomFichier", is("scan-a.pdf")))
                .andExpect(jsonPath("$.resultats[0].resultat", is("IMPORTEE")))
                .andExpect(jsonPath("$.resultats[0].code", is(201)))
                .andExpect(jsonPath("$.resultats[0].consultationId", is(visiteA.toString())))
                .andExpect(jsonPath("$.resultats[0].patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.resultats[0].statut", is("EN_ATTENTE")))
                .andExpect(jsonPath("$.resultats[0].reimportee", is(false)))
                .andExpect(jsonPath("$.resultats[0].erreur").doesNotExist())

                .andExpect(jsonPath("$.resultats[1].index", is(1)))
                .andExpect(jsonPath("$.resultats[1].nomFichier", is("scan-sans-qr.pdf")))
                .andExpect(jsonPath("$.resultats[1].resultat", is("ERREUR")))
                .andExpect(jsonPath("$.resultats[1].code", is(422)))
                .andExpect(jsonPath("$.resultats[1].erreur", is("Code QR introuvable")))
                .andExpect(jsonPath("$.resultats[1].ficheId").doesNotExist())

                .andExpect(jsonPath("$.resultats[2].nomFichier", is("photo.jpg")))
                .andExpect(jsonPath("$.resultats[2].resultat", is("ERREUR")))
                .andExpect(jsonPath("$.resultats[2].code", is(415)))
                .andExpect(jsonPath("$.resultats[2].erreur", is("Format de fichier non supporte")))
                .andExpect(jsonPath("$.resultats[2].message", is("Le fichier importe doit etre un PDF")))
                .andReturn().getResponse().getContentAsString();

        // Le premier fichier est enregistre (et valide en base : sa transaction est terminee).
        Map<String, Object> enregistree = jdbc.queryForMap(
                "SELECT statut, nom_fichier, contenu FROM fiche_signee WHERE consultation_id = ?", visiteA);
        assertThat(enregistree.get("STATUT")).isEqualTo("EN_ATTENTE");
        assertThat(enregistree.get("NOM_FICHIER")).isEqualTo("scan-a.pdf");
        assertThat((byte[]) enregistree.get("CONTENU")).isEqualTo(fiche);
        assertThat(nombreDeFiches()).isEqualTo(1);

        // Ni identite ni donnee medicale dans la reponse ; ni nom de fichier ni identite dans les journaux.
        assertThat(reponse).doesNotContain(NOM_PATIENT).doesNotContain("contenu");
        assertThat(sortie.getAll()).doesNotContain("scan-a.pdf", "scan-sans-qr.pdf", "photo.jpg", NOM_PATIENT);
    }

    @Test
    void ficheDejaImportee_409_pourCeFichierSeulement_lesAutresSontImportes() throws Exception {
        importer("ADMIN", fichier("premier.pdf", "application/pdf", ficheDe(visiteA)))
                .andExpect(jsonPath("$.resultats[0].code", is(201)));

        importer("ADMIN",
                fichier("doublon.pdf", "application/pdf", ficheDe(visiteA)),
                fichier("autre.pdf", "application/pdf", ficheDe(visiteB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreImportees", is(1)))
                .andExpect(jsonPath("$.resultats[0].code", is(409)))
                .andExpect(jsonPath("$.resultats[0].erreur", is("Fiche signee deja importee")))
                .andExpect(jsonPath("$.resultats[0].message",
                        is("Une fiche signee existe deja pour cette consultation : " + visiteA)))
                .andExpect(jsonPath("$.resultats[1].code", is(201)))
                .andExpect(jsonPath("$.resultats[1].consultationId", is(visiteB.toString())));

        // La premiere fiche n'a pas ete remplacee.
        assertThat(jdbc.queryForObject("SELECT nom_fichier FROM fiche_signee WHERE consultation_id = ?",
                String.class, visiteA)).isEqualTo("premier.pdf");
    }

    @Test
    void memeFicheDeuxFoisDansLeMemeLot_laSecondeRecoit409() throws Exception {
        byte[] fiche = ficheDe(visiteA);
        importer("ADMIN", fichier("a.pdf", "application/pdf", fiche), fichier("a-bis.pdf", "application/pdf", fiche))
                .andExpect(jsonPath("$.resultats[0].code", is(201)))
                .andExpect(jsonPath("$.resultats[1].code", is(409)));
        assertThat(nombreDeFiches()).isEqualTo(1);
    }

    @Test
    void ficheVisee_409() throws Exception {
        importer("ADMIN", fichier("premier.pdf", "application/pdf", ficheDe(visiteA)));
        jdbc.update("UPDATE fiche_signee SET statut = 'VISEE', senior_visa_id = ?, nom_senior_visa = 'Dr Diop', "
                + "date_visa = CURRENT_TIMESTAMP WHERE consultation_id = ?", UUID.randomUUID(), visiteA);

        importer("ADMIN", fichier("second.pdf", "application/pdf", ficheDe(visiteA)))
                .andExpect(jsonPath("$.resultats[0].code", is(409)));
        assertThat(jdbc.queryForObject("SELECT statut FROM fiche_signee WHERE consultation_id = ?",
                String.class, visiteA)).isEqualTo("VISEE");
    }

    @Test
    void ficheRenvoyee_reimportee_memeLigne_statutEnAttente_motifConserve() throws Exception {
        String reponse = importer("ADMIN", fichier("premier.pdf", "application/pdf", ficheDe(visiteA)))
                .andReturn().getResponse().getContentAsString();
        UUID ficheId = UUID.fromString(com.jayway.jsonpath.JsonPath.read(reponse, "$.resultats[0].ficheId"));
        jdbc.update("UPDATE fiche_signee SET statut = 'RENVOYEE', motif_renvoi = 'Page 2 illisible' "
                + "WHERE consultation_id = ?", visiteA);
        byte[] nouveauScan = PdfDeTest.pdfAvecQr(qr(visiteA), null);

        importer("ADMIN", fichier("nouveau.pdf", "application/pdf", nouveauScan))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultats[0].code", is(201)))
                .andExpect(jsonPath("$.resultats[0].ficheId", is(ficheId.toString())))
                .andExpect(jsonPath("$.resultats[0].statut", is("EN_ATTENTE")))
                .andExpect(jsonPath("$.resultats[0].reimportee", is(true)));

        Map<String, Object> fiche = jdbc.queryForMap("SELECT id, statut, nom_fichier, contenu, motif_renvoi, version "
                + "FROM fiche_signee WHERE consultation_id = ?", visiteA);
        assertThat(fiche.get("ID")).isEqualTo(ficheId);
        assertThat(fiche.get("STATUT")).isEqualTo("EN_ATTENTE");
        assertThat(fiche.get("NOM_FICHIER")).isEqualTo("nouveau.pdf");
        assertThat((byte[]) fiche.get("CONTENU")).isEqualTo(nouveauScan);
        assertThat(fiche.get("MOTIF_RENVOI")).isEqualTo("Page 2 illisible");
        assertThat(((Number) fiche.get("VERSION")).longValue()).isPositive();
        assertThat(nombreDeFiches()).isEqualTo(1);
    }

    @Test
    void visiteOuPatientIntrouvable_404_etFichierTropGros_413_pourCeFichierSeulement() throws Exception {
        UUID inconnue = UUID.randomUUID();
        byte[] gros = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF_SANS_QR, 0, gros, 0, PDF_SANS_QR.length);

        importer("ADMIN",
                fichier("inconnue.pdf", "application/pdf", PdfDeTest.pdfAvecQr(qr(patientId, inconnue))),
                fichier("gros.pdf", "application/pdf", gros),
                fichier("ok.pdf", "application/pdf", ficheDe(visiteB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultats[0].code", is(404)))
                .andExpect(jsonPath("$.resultats[0].erreur", is("Ressource introuvable")))
                .andExpect(jsonPath("$.resultats[0].message", is("Consultation introuvable : " + inconnue)))
                .andExpect(jsonPath("$.resultats[1].code", is(413)))
                .andExpect(jsonPath("$.resultats[1].erreur", is("Fichier trop volumineux")))
                .andExpect(jsonPath("$.resultats[1].message", is("Le fichier importe depasse la taille maximale de 10 Mo")))
                .andExpect(jsonPath("$.resultats[2].code", is(201)));
        assertThat(nombreDeFiches()).isEqualTo(1);
    }

    /** Une erreur imprevue sur un fichier : 500 pour lui seul, journal sans son nom, les autres importes. */
    @Test
    void erreurImprevueSurUnFichier_500_pourLuiSeul_journalSansNomDeFichier(CapturedOutput sortie) throws Exception {
        byte[] piege = PdfDeTest.pdfAvecQr(qr(visiteA), null, null);
        doThrow(new IllegalStateException("panne " + NOM_PATIENT))
                .when(detecteurQrPdf).detecter(argThat(contenu -> Arrays.equals(contenu, piege)));

        importer("ADMIN",
                fichier("piege-confidentiel.pdf", "application/pdf", piege),
                fichier("ok.pdf", "application/pdf", ficheDe(visiteB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultats[0].code", is(500)))
                .andExpect(jsonPath("$.resultats[0].erreur", is("Erreur interne")))
                .andExpect(jsonPath("$.resultats[0].message", containsString("raison imprevue")))
                .andExpect(jsonPath("$.resultats[1].code", is(201)));

        assertThat(nombreDeFiches()).isEqualTo(1);
        assertThat(sortie.getAll()).contains("Import en lot : erreur imprevue sur le fichier n° 1 (java.lang.IllegalStateException)");
        assertThat(sortie.getAll()).doesNotContain("piege-confidentiel.pdf", NOM_PATIENT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MEDECIN_JUNIOR", "MEDECIN_SENIOR", "UTILISATEUR"})
    void autreRoleQuAdmin_403_etRienNestEnregistre(String role) throws Exception {
        importer(role, fichier("fiche.pdf", "application/pdf", ficheDe(visiteA)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.erreur", is("Acces refuse")))
                .andExpect(jsonPath("$.message", is("Votre role ne permet pas d'acceder a cette ressource")));
        assertThat(nombreDeFiches()).isZero();
    }

    @Test
    void sansBadge_401() throws Exception {
        mockMvc.perform(multipart(URL).file(fichier("fiche.pdf", "application/pdf", ficheDe(visiteA))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erreur", is("Non authentifie")));
        assertThat(nombreDeFiches()).isZero();
    }

    @Test
    void plusDeVingtFichiers_400_etRienNestEnregistre() throws Exception {
        MockMultipartFile[] fichiers = new MockMultipartFile[21];
        fichiers[0] = fichier("fiche.pdf", "application/pdf", ficheDe(visiteA));
        for (int i = 1; i < fichiers.length; i++) {
            fichiers[i] = fichier("vide-" + i + ".pdf", "application/pdf", PDF_SANS_QR);
        }
        importer("ADMIN", fichiers)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur", is("Regle de gestion non respectee")))
                .andExpect(jsonPath("$.message", is("Au plus 20 fichiers par import (21 recus) : rien n'a ete importe")));
        assertThat(nombreDeFiches()).isZero();
    }

    @Test
    void vingtFichiers_acceptes() throws Exception {
        MockMultipartFile[] fichiers = new MockMultipartFile[20];
        for (int i = 0; i < fichiers.length; i++) {
            fichiers[i] = fichier("vide-" + i + ".pdf", "application/pdf", PDF_SANS_QR);
        }
        importer("ADMIN", fichiers)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreFichiers", is(20)))
                .andExpect(jsonPath("$.nombreErreurs", is(20)));
    }

    @Test
    void sansFichier_400() throws Exception {
        mockMvc.perform(multipart(URL).header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("La partie \"fichiers\" (fichier PDF) est obligatoire")));
    }

    private ResultActions importer(String role, MockMultipartFile... fichiers) throws Exception {
        MockMultipartHttpServletRequestBuilder requete = multipart(URL);
        for (MockMultipartFile fichier : fichiers) {
            requete.file(fichier);
        }
        return mockMvc.perform(requete.header(HttpHeaders.AUTHORIZATION, fabriqueBadge.bearer(role)));
    }

    private static MockMultipartFile fichier(String nom, String type, byte[] contenu) {
        return new MockMultipartFile("fichiers", nom, type, contenu);
    }

    private byte[] ficheDe(UUID visite) {
        return PdfDeTest.pdfAvecQr(qr(visite));
    }

    private String qr(UUID visite) {
        return qr(patientId, visite);
    }

    private static String qr(UUID patient, UUID visite) {
        return "{\"type\":\"FICHE_1_CONSULTATION_INITIALE\",\"patientId\":\"" + patient
                + "\",\"consultationId\":\"" + visite + "\",\"typeVisite\":\"INITIALE\"}";
    }

    private UUID creerVisite(TypeVisite type) {
        return consultationRepository.save(ConsultationInitiale.builder()
                .patient(patientRepository.getReferenceById(patientId))
                .statut(StatutConsultation.BROUILLON)
                .typeVisite(type)
                .build()).getId();
    }

    private long nombreDeFiches() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM fiche_signee f JOIN consultation_initiale c "
                + "ON c.id = f.consultation_id WHERE c.patient_id = ?", Long.class, patientId);
    }
}
