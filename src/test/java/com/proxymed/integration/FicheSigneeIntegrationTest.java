package com.proxymed.integration;

import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import com.proxymed.support.BadgeMedecinParDefaut;
import com.proxymed.support.PdfDeTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Import sans id (POST /api/fiches-signees/import) : la consultation est retrouvee grace au QR
 * code du PDF scanne. Telechargement par consultation et liste du dossier patient.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BadgeMedecinParDefaut.class)
@Transactional
class FicheSigneeIntegrationTest {

    private static final String URL_IMPORT = "/api/fiches-signees/import";
    private static final String TYPE = "FICHE_1_CONSULTATION_INITIALE";
    private static final byte[] PDF_SANS_QR = PdfDeTest.pdfAvecQr((String) null);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private MedecinRepository medecinRepository;
    @Autowired
    private ConsultationRepository consultationRepository;
    @Autowired
    private FicheSigneeRepository ficheSigneeRepository;
    @Autowired
    private EntityManager entityManager;

    private Medecin senior;
    private UUID patientId;
    private String consultationId;

    @BeforeEach
    void setUp() throws Exception {
        senior = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Awa").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        patientId = creerPatient("Patient Test");
        consultationId = creerConsultation(patientId);
    }

    @Test
    void allerRetour_pdfGenere_puisScanne_estRangeSansIdDansLaBonneConsultation() throws Exception {
        // Le patient a deja une consultation (setUp) : on importe la fiche d'une seconde.
        String deuxieme = creerConsultation(patientId);
        byte[] pdfGenere = mockMvc.perform(get("/api/consultations/" + deuxieme + "/pdf"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        byte[] scan = PdfDeTest.numeriser(pdfGenere, 200);

        String reponse = importer(new MockMultipartFile("fichier", "scan-0001.pdf", "application/pdf", scan))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.patientId", is(patientId.toString())))
                .andExpect(jsonPath("$.consultationId", is(deuxieme)))
                .andExpect(jsonPath("$.nomFichier", is("scan-0001.pdf")))
                .andExpect(jsonPath("$.tailleOctets", is(scan.length)))
                .andExpect(jsonPath("$.dateImport", notNullValue()))
                // Metadonnees seulement : jamais le contenu du fichier dans le JSON.
                .andExpect(jsonPath("$.contenu").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID ficheId = UUID.fromString(objectMapper.readTree(reponse).get("id").asString());

        entityManager.flush();
        entityManager.clear();

        FicheSignee enregistree = ficheSigneeRepository.findById(ficheId).orElseThrow();
        assertThat(enregistree.getConsultation().getId()).isEqualTo(UUID.fromString(deuxieme));
        assertThat(enregistree.getTypeContenu()).isEqualTo("application/pdf");
        assertThat(enregistree.getContenu()).isEqualTo(scan);
        assertThat(ficheSigneeRepository.findByConsultation_Id(UUID.fromString(consultationId))).isEmpty();

        mockMvc.perform(get("/api/consultations/" + deuxieme + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"scan-0001.pdf\""))
                .andExpect(content().bytes(scan));
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].consultationId", is(deuxieme)))
                .andExpect(jsonPath("$[0].nomFichier", is("scan-0001.pdf")));
    }

    @Test
    void import_trouveLeQrSurUnePageSuivante() throws Exception {
        byte[] pdf = PdfDeTest.pdfAvecQr(null, null, qr(TYPE, patientId, consultationId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consultationId", is(consultationId)));
    }

    @Test
    void deuxiemeImport_surLaMemeConsultation_renvoie409_etLaPremiereFicheEstConservee() throws Exception {
        byte[] premier = PdfDeTest.pdfAvecQr(qr(TYPE, patientId, consultationId));
        byte[] second = PdfDeTest.pdfAvecQr(qr(TYPE, patientId, consultationId), null);

        importer(new MockMultipartFile("fichier", "premier.pdf", "application/pdf", premier))
                .andExpect(status().isCreated());
        importer(new MockMultipartFile("fichier", "second.pdf", "application/pdf", second))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.erreur", is("Fiche signee deja importee")))
                .andExpect(jsonPath("$.message", containsString("Une fiche signee existe deja pour cette consultation")));

        mockMvc.perform(get("/api/consultations/" + consultationId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(premier));
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nomFichier", is("premier.pdf")));
    }

    @Test
    void memeFichierImporteDeuxFois_renvoie409() throws Exception {
        MockMultipartFile fichier = new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr(TYPE, patientId, consultationId)));

        importer(fichier).andExpect(status().isCreated());
        importer(fichier)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("Une fiche signee existe deja pour cette consultation")));

        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void contrainteDUnicite_refuseUneSecondeFichePourLaMemeConsultation_enBase() {
        // Garde-fou contre deux imports simultanes, qui passeraient tous deux la verification du service.
        UUID id = UUID.fromString(consultationId);
        enregistrerFiche(id, "premiere.pdf", Instant.parse("2026-10-08T12:00:00Z"), PDF_SANS_QR);
        entityManager.flush();

        assertThatThrownBy(() -> {
            enregistrerFiche(id, "seconde.pdf", Instant.parse("2026-10-09T12:00:00Z"), PDF_SANS_QR);
            entityManager.flush();
        }).rootCause().message().containsIgnoringCase("uk_fiche_signee_consultation");
    }

    @Test
    void dossierPatient_listeLesFichesDeToutesSesConsultations_plusRecenteDAbord() throws Exception {
        UUID premiere = UUID.fromString(consultationId);
        UUID deuxieme = UUID.fromString(creerConsultation(patientId));
        enregistrerFiche(premiere, "consultation-1.pdf", Instant.parse("2026-09-05T10:00:00Z"), PDF_SANS_QR);
        enregistrerFiche(deuxieme, "consultation-2.pdf", Instant.parse("2026-10-05T10:00:00Z"), PDF_SANS_QR);
        // Fiche d'un autre patient : absente du dossier.
        enregistrerFiche(UUID.fromString(creerConsultation(creerPatient("Awa Ndiaye"))), "autre.pdf",
                Instant.parse("2026-10-06T10:00:00Z"), PDF_SANS_QR);

        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].consultationId", is(deuxieme.toString())))
                .andExpect(jsonPath("$[0].nomFichier", is("consultation-2.pdf")))
                .andExpect(jsonPath("$[0].dateImport", is("2026-10-05T10:00:00Z")))
                .andExpect(jsonPath("$[0].patientId", is(patientId.toString())))
                .andExpect(jsonPath("$[1].consultationId", is(premiere.toString())))
                .andExpect(jsonPath("$[1].nomFichier", is("consultation-1.pdf")))
                .andExpect(jsonPath("$[1].dateImport", is("2026-09-05T10:00:00Z")))
                .andExpect(jsonPath("$[0].contenu").doesNotExist());
    }

    @Test
    void dossierPatient_videOu404() throws Exception {
        mockMvc.perform(get("/api/patients/" + patientId + "/fiches-signees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/patients/" + UUID.randomUUID() + "/fiches-signees"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Patient introuvable")));
    }

    @Test
    void import_sansQrCode_renvoie422() throws Exception {
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", PDF_SANS_QR))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.erreur", is("Code QR introuvable")))
                .andExpect(jsonPath("$.message", containsString("Aucun code QR n'a ete trouve dans le PDF importe")));

        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_qrQuiNEstPasProxymed_renvoie400() throws Exception {
        for (String texte : new String[]{"https://exemple.com", "{\"type\":\"" + TYPE + "\"}"}) {
            importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", PdfDeTest.pdfAvecQr(texte)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", is("Le code QR detecte n'est pas un code QR PROXYMED")));
        }
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_ancienPdf_qrAvecSeulementLIdDeLaConsultation_renvoie400() throws Exception {
        importer(new MockMultipartFile("fichier", "ancienne-fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(consultationId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ancienne fiche")))
                .andExpect(jsonPath("$.message", containsString("regenerez le PDF")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_autreTypeDeFiche_renvoie400() throws Exception {
        byte[] pdf = PdfDeTest.pdfAvecQr(qr("FICHE_2_SUIVI", patientId, consultationId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Type de fiche non pris en charge : FICHE_2_SUIVI")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_patientOuConsultationInconnus_renvoie404() throws Exception {
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr(TYPE, UUID.randomUUID(), consultationId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Patient introuvable")));
        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf",
                PdfDeTest.pdfAvecQr(qr(TYPE, patientId, UUID.randomUUID().toString()))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Consultation introuvable")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_patientDuQrDifferentDuPatientDeLaConsultation_renvoie400() throws Exception {
        UUID autrePatient = creerPatient("Awa Ndiaye");
        byte[] pdf = PdfDeTest.pdfAvecQr(qr(TYPE, autrePatient, consultationId));

        importer(new MockMultipartFile("fichier", "fiche.pdf", "application/pdf", pdf))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("ne correspond pas au patient de la consultation")));
        assertThat(ficheSigneeRepository.findAll()).isEmpty();
    }

    @Test
    void import_rejetteCeQuiNEstPasUnVraiPdf_en415() throws Exception {
        verifierNonPdf(importer(new MockMultipartFile("fichier", "photo.pdf", "application/pdf",
                "pas un pdf".getBytes(StandardCharsets.US_ASCII))));
        verifierNonPdf(importer(new MockMultipartFile("fichier", "fiche.png", "image/png", PDF_SANS_QR)));
        verifierNonPdf(importer(new MockMultipartFile("fichier", "vide.pdf", "application/pdf", new byte[0])));
        // Un PDF endommage reste un PDF : 400, et non 415.
        importer(new MockMultipartFile("fichier", "tronque.pdf", "application/pdf",
                "%PDF-1.7\nfiche tronquee".getBytes(StandardCharsets.US_ASCII)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Le fichier PDF est illisible ou endommage")));

        assertThat(ficheSigneeRepository.count()).isZero();
    }

    /** Vrai debut de JPEG (marqueurs SOI + APP0 JFIF), declare image/jpeg ou faussement application/pdf. */
    @Test
    void import_fichierJpeg_renvoie415() throws Exception {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 'J', 'F', 'I', 'F', 0x00};

        verifierNonPdf(importer(new MockMultipartFile("fichier", "scan.jpg", "image/jpeg", jpeg)));
        verifierNonPdf(importer(new MockMultipartFile("fichier", "scan.pdf", "application/pdf", jpeg)));

        assertThat(ficheSigneeRepository.count()).isZero();
    }

    /** Controle du service (MockMvc ne passe pas par la limite du serveur, testee a part en HTTP). */
    @Test
    void import_rejetteUnFichierDePlusDe10Mo_en413() throws Exception {
        byte[] gros = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PDF_SANS_QR, 0, gros, 0, PDF_SANS_QR.length);

        importer(new MockMultipartFile("fichier", "gros.pdf", "application/pdf", gros))
                .andExpect(status().isContentTooLarge())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(413)))
                .andExpect(jsonPath("$.erreur", is("Fichier trop volumineux")))
                .andExpect(jsonPath("$.message", is("Le fichier importe depasse la taille maximale de 10 Mo")))
                .andExpect(jsonPath("$.horodatage", notNullValue()))
                .andExpect(jsonPath("$.details", hasSize(0)));
        assertThat(ficheSigneeRepository.count()).isZero();
    }

    @Test
    void import_sansFichier_renvoie400() throws Exception {
        mockMvc.perform(multipart(URL_IMPORT))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("fichier")));
    }

    @Test
    void telecharger_renvoie404_siConsultationInconnue() throws Exception {
        mockMvc.perform(get("/api/consultations/" + UUID.randomUUID() + "/fiche-signee"))
                .andExpect(status().isNotFound());
    }

    @Test
    void telecharger_renvoie404_siAucuneFicheImportee() throws Exception {
        mockMvc.perform(get("/api/consultations/" + consultationId + "/fiche-signee"))
                .andExpect(status().isNotFound());
    }

    private static void verifierNonPdf(ResultActions resultat) throws Exception {
        resultat.andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status", is(415)))
                .andExpect(jsonPath("$.erreur", is("Format de fichier non supporte")))
                .andExpect(jsonPath("$.message", is("Le fichier importe doit etre un PDF")))
                .andExpect(jsonPath("$.horodatage", notNullValue()))
                .andExpect(jsonPath("$.details", hasSize(0)));
    }

    private ResultActions importer(MockMultipartFile fichier) throws Exception {
        return mockMvc.perform(multipart(URL_IMPORT).file(fichier));
    }

    /**
     * Texte de QR ecrit a la main (et non via GenerateurContenuQr) : verifie aussi le format attendu.
     */
    private String qr(String type, UUID patient, String consultation) {
        return "{\"type\":\"" + type + "\",\"patientId\":\"" + patient + "\",\"consultationId\":\"" + consultation + "\"}";
    }

    private void enregistrerFiche(UUID id, String nom, Instant dateImport, byte[] contenu) {
        ficheSigneeRepository.save(FicheSignee.builder()
                .consultation(consultationRepository.getReferenceById(id))
                .nomFichier(nom)
                .typeContenu("application/pdf")
                .tailleOctets(contenu.length)
                .dateImport(dateImport)
                .contenu(contenu)
                .build());
    }

    private UUID creerPatient(String nom) {
        return patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime()).nomComplet(nom)
                .dateNaissance(LocalDate.of(1955, 1, 1)).sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU)
                .build()).getId();
    }

    private String creerConsultation(UUID patient) throws Exception {
        String reponse = mockMvc.perform(post("/api/consultations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(Map.of(
                                "patientId", patient,
                                "medecinSeniorId", senior.getId()))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(reponse).get("id").asString();
    }
}
