package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.entity.Patient;
import com.proxymed.exception.FicheSigneeExistanteException;
import com.proxymed.exception.QrCodeIntrouvableException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.service.mappers.FicheSigneeMapper;
import com.proxymed.service.model.ConsultationModel;
import com.proxymed.service.model.ContenuQrModel;
import com.proxymed.service.model.FicheSigneeModel;
import com.proxymed.service.model.PatientModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Teste FicheSigneeService en isolation : repositories, services et detecteur de QR mockes
 * (la detection dans un vrai PDF est couverte par DetecteurQrPdfTest et le test d'integration),
 * lecteur de QR et mapper DB reels.
 */
@ExtendWith(MockitoExtension.class)
class FicheSigneeServiceTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private FicheSigneeRepository ficheSigneeRepository;
    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private ConsultationService consultationService;
    @Mock
    private PatientService patientService;
    @Mock
    private DetecteurQrPdf detecteurQrPdf;
    @Mock
    private DemandeConsultationService demandeConsultationService;

    private final GenerateurContenuQr generateurContenuQr = new GenerateurContenuQr();

    private FicheSigneeService ficheSigneeService;

    private ConsultationInitiale consultation;
    private UUID patientId;

    @BeforeEach
    void setUp() {
        ficheSigneeService = new FicheSigneeServiceImpl(ficheSigneeRepository, consultationService, patientService,
                demandeConsultationService, new FicheSigneeMapper(consultationRepository), detecteurQrPdf, new LecteurContenuQr());

        patientId = UUID.randomUUID();
        consultation = ConsultationInitiale.builder().id(UUID.randomUUID())
                .dateConsultation(LocalDate.of(2026, 9, 20))
                .patient(Patient.builder().id(patientId).build()).build();
        lenient().when(consultationRepository.getReferenceById(consultation.getId())).thenReturn(consultation);
        lenient().when(consultationService.findById(consultation.getId())).thenReturn(ConsultationModel.builder()
                .id(consultation.getId()).patient(PatientModel.builder().id(patientId).build()).build());
        lenient().when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientId, consultation.getId())));
        lenient().when(ficheSigneeRepository.saveAndFlush(any())).thenAnswer(inv -> {
            FicheSignee fiche = inv.getArgument(0);
            fiche.setId(UUID.randomUUID());
            return fiche;
        });
    }

    @Test
    void importer_rangeLePdfDansLaConsultationDuQrCode_avecSaDateDImport() {
        FicheSigneeModel resultat = ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null);

        ArgumentCaptor<FicheSignee> captor = ArgumentCaptor.forClass(FicheSignee.class);
        verify(ficheSigneeRepository).saveAndFlush(captor.capture());
        FicheSignee enregistree = captor.getValue();
        assertThat(enregistree.getConsultation()).isSameAs(consultation);
        assertThat(enregistree.getNomFichier()).isEqualTo("fiche.pdf");
        assertThat(enregistree.getTypeContenu()).isEqualTo("application/pdf");
        assertThat(enregistree.getTailleOctets()).isEqualTo(PDF.length);
        assertThat(enregistree.getContenu()).isEqualTo(PDF);
        assertThat(enregistree.getDateImport()).isNotNull();

        assertThat(resultat.id()).isNotNull();
        assertThat(resultat.consultationId()).isEqualTo(consultation.getId());
        assertThat(resultat.patientId()).isEqualTo(patientId);
        assertThat(resultat.dateConsultation()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(resultat.dateImport()).isEqualTo(enregistree.getDateImport());
        verify(detecteurQrPdf).detecter(PDF);
    }

    @Test
    void importer_sansJuniorAControler_neVerifiePasLAssignation() {
        ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null);

        verifyNoInteractions(demandeConsultationService);
        verify(ficheSigneeRepository).saveAndFlush(any());
    }

    @Test
    void importer_verifieLAssignationDuJunior_surLaConsultationDuQr() {
        UUID junior = UUID.randomUUID();

        ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), junior);

        verify(demandeConsultationService).verifierJuniorAssigne(consultation.getId(), junior);
        verify(ficheSigneeRepository).saveAndFlush(any());
    }

    /** Refus avant le controle du patient et de la fiche existante : rien n'est enregistre. */
    @Test
    void importer_rejette_siConsultationAssigneeAUnAutreJunior() {
        UUID autreJunior = UUID.randomUUID();
        doThrow(new AccessDeniedException("Consultation assignee a un autre medecin junior"))
                .when(demandeConsultationService).verifierJuniorAssigne(consultation.getId(), autreJunior);

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), autreJunior))
                .isInstanceOf(AccessDeniedException.class);
        verify(ficheSigneeRepository, never()).existsByConsultation_Id(any());
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_siLeTypeNEstPasPdf_ouSiLeContenuNeCommencePasParPdf() {
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "image/png", PDF), null))
                .isInstanceOf(RegleGestionException.class).hasMessageContaining("PDF");
        assertThatThrownBy(() -> ficheSigneeService.importer(
                fichier("photo.pdf", "application/pdf", "ceci n'est pas un PDF".getBytes(StandardCharsets.US_ASCII)), null))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("vide.pdf", "application/pdf", new byte[0]), null))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("sans-type.pdf", null, PDF), null))
                .isInstanceOf(RegleGestionException.class);
        verifyNoInteractions(detecteurQrPdf);
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_auDelaDe10Mo_etAccepteExactement10Mo() {
        byte[] trop = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS + 1);
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("gros.pdf", "application/pdf", trop), null))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("10 Mo");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());

        byte[] limite = pdfDeTaille(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
        assertThat(ficheSigneeService.importer(fichier("limite.pdf", "application/pdf", limite), null).tailleOctets())
                .isEqualTo(FicheSigneeServiceImpl.TAILLE_MAX_OCTETS);
    }

    @Test
    void importer_rejette_sansQrCode() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of());

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(QrCodeIntrouvableException.class)
                .hasMessageContaining("Aucun code QR");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_unQrQuiNEstPasProxymed_ouDUnAutreTypeDeFiche() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of("https://exemple.com"));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage("Le code QR detecte n'est pas un code QR PROXYMED");

        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(generateurContenuQr.construire(
                new ContenuQrModel("FICHE_2_SUIVI", patientId, consultation.getId()))));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("Type de fiche non pris en charge");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_unQrDAncienFormat_sansCompatibilite() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(consultation.getId().toString()));

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(RegleGestionException.class)
                .hasMessage(LecteurContenuQr.MESSAGE_ANCIEN_FORMAT);
        verifyNoInteractions(consultationService, patientService);
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_retientLePremierQrProxymed_quandUneAutrePageEnPorteUnEtranger() {
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of("https://exemple.com", qr(patientId, consultation.getId())));

        assertThat(ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null).consultationId())
                .isEqualTo(consultation.getId());
    }

    @Test
    void importer_rejette_siPatientOuConsultationIntrouvable() {
        UUID patientInconnu = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientInconnu, consultation.getId())));
        when(patientService.findById(patientInconnu))
                .thenThrow(new ResourceNotFoundException("Patient introuvable : " + patientInconnu));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Patient introuvable");

        UUID consultationInconnue = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(patientId, consultationInconnue)));
        when(consultationService.findById(consultationInconnue))
                .thenThrow(new ResourceNotFoundException("Consultation introuvable : " + consultationInconnue));
        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Consultation introuvable");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_siLePatientDuQrNEstPasCeluiDeLaConsultation() {
        UUID autrePatient = UUID.randomUUID();
        when(detecteurQrPdf.detecter(any())).thenReturn(List.of(qr(autrePatient, consultation.getId())));

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(RegleGestionException.class)
                .hasMessageContaining("ne correspond pas au patient de la consultation");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_rejette_siLaConsultationADejaUneFicheSignee() {
        when(ficheSigneeRepository.existsByConsultation_Id(consultation.getId())).thenReturn(true);

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(FicheSigneeExistanteException.class)
                .hasMessageContaining("Une fiche signee existe deja pour cette consultation");
        verify(ficheSigneeRepository, never()).saveAndFlush(any());
    }

    @Test
    void importer_renvoieLeConflit_siUnImportSimultaneAGagneLaCourse() {
        // La verification passe pour les deux imports ; la contrainte d'unicite refuse le second.
        doThrow(new DataIntegrityViolationException("uk_fiche_signee_consultation"))
                .when(ficheSigneeRepository).saveAndFlush(any());

        assertThatThrownBy(() -> ficheSigneeService.importer(fichier("fiche.pdf", "application/pdf", PDF), null))
                .isInstanceOf(FicheSigneeExistanteException.class)
                .hasMessageContaining("Une fiche signee existe deja pour cette consultation");
    }

    @Test
    void telecharger_rejette_siConsultationIntrouvable() {
        UUID consultationId = UUID.randomUUID();
        when(consultationService.findById(consultationId))
                .thenThrow(new ResourceNotFoundException("Consultation introuvable : " + consultationId));

        assertThatThrownBy(() -> ficheSigneeService.telecharger(consultationId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ficheSigneeRepository, never()).findByConsultation_Id(any());
    }

    @Test
    void telecharger_rejette_siAucuneFicheImportee() {
        when(ficheSigneeRepository.findByConsultation_Id(consultation.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ficheSigneeService.telecharger(consultation.getId()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Aucune fiche signee");
    }

    @Test
    void telecharger_renvoieLaFicheDeLaConsultation() {
        FicheSignee fiche = FicheSignee.builder()
                .id(UUID.randomUUID()).consultation(consultation)
                .nomFichier("fiche.pdf").typeContenu("application/pdf")
                .tailleOctets(PDF.length).dateImport(Instant.parse("2026-10-09T12:00:00Z")).contenu(PDF)
                .build();
        when(ficheSigneeRepository.findByConsultation_Id(consultation.getId())).thenReturn(Optional.of(fiche));

        FicheSigneeModel resultat = ficheSigneeService.telecharger(consultation.getId());

        verify(consultationService).findById(consultation.getId());
        assertThat(resultat.id()).isEqualTo(fiche.getId());
        assertThat(resultat.consultationId()).isEqualTo(consultation.getId());
        assertThat(resultat.nomFichier()).isEqualTo("fiche.pdf");
        assertThat(resultat.dateImport()).isEqualTo(fiche.getDateImport());
        assertThat(resultat.contenu()).isEqualTo(PDF);
    }

    @Test
    void findByPatient_rejette_siPatientIntrouvable() {
        UUID inconnu = UUID.randomUUID();
        when(patientService.findById(inconnu)).thenThrow(new ResourceNotFoundException("Patient introuvable : " + inconnu));

        assertThatThrownBy(() -> ficheSigneeService.findByPatient(inconnu))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ficheSigneeRepository, never()).findResumesByPatientId(any());
    }

    private String qr(UUID patient, UUID consultationId) {
        return generateurContenuQr.construire(new ContenuQrModel(
                GenerateurContenuQr.TYPE_FICHE_1_CONSULTATION_INITIALE, patient, consultationId));
    }

    private FicheSigneeModel fichier(String nom, String type, byte[] contenu) {
        return FicheSigneeModel.builder().nomFichier(nom).typeContenu(type).tailleOctets(contenu.length).contenu(contenu).build();
    }

    private byte[] pdfDeTaille(long taille) {
        byte[] contenu = new byte[Math.toIntExact(taille)];
        Arrays.fill(contenu, (byte) ' ');
        System.arraycopy(PDF, 0, contenu, 0, PDF.length);
        return contenu;
    }
}
