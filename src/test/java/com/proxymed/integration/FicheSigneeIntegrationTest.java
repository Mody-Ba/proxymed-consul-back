package com.proxymed.integration;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Medecin;
import com.proxymed.entity.Patient;
import com.proxymed.enums.CouvertureSociale;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.enums.Sexe;
import com.proxymed.enums.StatutConsultation;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FicheSigneeIntegrationTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

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

    private UUID consultationId;

    @BeforeEach
    void setUp() {
        Patient patient = patientRepository.save(Patient.builder()
                .numeroDossierProxymed("DOS-" + System.nanoTime()).nomComplet("Patient Test")
                .dateNaissance(LocalDate.of(1955, 1, 1)).sexe(Sexe.M).couvertureSociale(CouvertureSociale.CMU).build());
        Medecin medecin = medecinRepository.save(Medecin.builder()
                .nom("Diop").prenom("Awa").numeroOrdre("SEN-" + System.nanoTime()).role(RoleMedecin.SENIOR).build());
        ConsultationInitiale consultation = consultationRepository.save(ConsultationInitiale.builder()
                .patient(patient).medecinSenior(medecin).statut(StatutConsultation.BROUILLON).build());
        consultationId = consultation.getId();
    }

    @Test
    void importer_enregistreLePdf_etRenvoieUniquementLesMetadonnees() throws Exception {
        String reponse = mockMvc.perform(multipart("/api/consultations/" + consultationId + "/fiche-signee")
                        .file(new MockMultipartFile("fichier", "fiche-signee.pdf", "application/pdf", PDF)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.nomFichier", is("fiche-signee.pdf")))
                .andExpect(jsonPath("$.tailleOctets", is(PDF.length)))
                .andExpect(jsonPath("$.dateImport", notNullValue()))
                .andExpect(jsonPath("$.contenu").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        UUID ficheId = UUID.fromString(objectMapper.readTree(reponse).get("id").asString());

        ficheSigneeRepository.flush();
        FicheSignee enregistree = ficheSigneeRepository.findById(ficheId).orElseThrow();
        assertThat(enregistree.getConsultation().getId()).isEqualTo(consultationId);
        assertThat(enregistree.getTypeContenu()).isEqualTo("application/pdf");
        assertThat(enregistree.getContenu()).isEqualTo(PDF);
    }

    @Test
    void importer_renvoie404_siConsultationInconnue() throws Exception {
        mockMvc.perform(multipart("/api/consultations/" + UUID.randomUUID() + "/fiche-signee")
                        .file(new MockMultipartFile("fichier", "fiche-signee.pdf", "application/pdf", PDF)))
                .andExpect(status().isNotFound());
    }

    @Test
    void importer_renvoie400_siFichierNonPdf() throws Exception {
        mockMvc.perform(multipart("/api/consultations/" + consultationId + "/fiche-signee")
                        .file(new MockMultipartFile("fichier", "photo.pdf", "application/pdf",
                                "pas un pdf".getBytes(StandardCharsets.US_ASCII))))
                .andExpect(status().isBadRequest());
        assertThat(ficheSigneeRepository.count()).isZero();
    }

    @Test
    void telecharger_renvoieLePdfLePlusRecent_avecLesEnTetesDeTelechargement() throws Exception {
        byte[] ancien = "%PDF-1.7\nancienne fiche".getBytes(StandardCharsets.US_ASCII);
        byte[] recent = "%PDF-1.7\nfiche la plus recente".getBytes(StandardCharsets.US_ASCII);
        enregistrerFiche("fiche-recente.pdf", Instant.parse("2026-10-09T12:00:00Z"), recent);
        enregistrerFiche("fiche-ancienne.pdf", Instant.parse("2026-10-08T12:00:00Z"), ancien);

        byte[] corps = mockMvc.perform(get("/api/consultations/" + consultationId + "/fiche-signee"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fiche-recente.pdf\""))
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(new String(corps, StandardCharsets.US_ASCII)).startsWith("%PDF-");
        assertThat(corps).isEqualTo(recent);
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

    private void enregistrerFiche(String nomFichier, Instant dateImport, byte[] contenu) {
        ficheSigneeRepository.save(FicheSignee.builder()
                .consultation(consultationRepository.getReferenceById(consultationId))
                .nomFichier(nomFichier).typeContenu("application/pdf")
                .tailleOctets(contenu.length).dateImport(dateImport).contenu(contenu)
                .build());
    }
}
