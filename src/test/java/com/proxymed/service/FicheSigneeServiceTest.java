package com.proxymed.service;

import com.proxymed.entity.ConsultationInitiale;
import com.proxymed.entity.FicheSignee;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.FicheSigneeRepository;
import com.proxymed.service.model.FicheSigneeModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FicheSigneeServiceTest {

    private static final byte[] PDF = "%PDF-1.7\nfiche signee".getBytes(StandardCharsets.US_ASCII);

    @Mock
    private FicheSigneeRepository ficheSigneeRepository;
    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private ConsultationService consultationService;

    private FicheSigneeService ficheSigneeService;

    @BeforeEach
    void setUp() {
        ficheSigneeService = new FicheSigneeServiceImpl(
                ficheSigneeRepository, consultationService,
                new com.proxymed.service.mappers.FicheSigneeMapper(consultationRepository));
        lenient().when(ficheSigneeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importer_rejette_siConsultationIntrouvable() {
        UUID consultationId = UUID.randomUUID();
        when(consultationService.findById(consultationId))
                .thenThrow(new ResourceNotFoundException("Consultation introuvable : " + consultationId));

        assertThatThrownBy(() -> ficheSigneeService.importer(consultationId, fichier("application/pdf", PDF)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(ficheSigneeRepository, never()).save(any());
    }

    @Test
    void importer_rejette_siTypeDeContenuNonPdf() {
        UUID consultationId = UUID.randomUUID();

        assertThatThrownBy(() -> ficheSigneeService.importer(consultationId, fichier("image/png", PDF)))
                .isInstanceOf(RegleGestionException.class);
        verify(ficheSigneeRepository, never()).save(any());
    }

    @Test
    void importer_rejette_siContenuNeCommencePasParLaSignaturePdf() {
        UUID consultationId = UUID.randomUUID();
        byte[] fauxPdf = "ceci n'est pas un PDF".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> ficheSigneeService.importer(consultationId, fichier("application/pdf", fauxPdf)))
                .isInstanceOf(RegleGestionException.class);
        verify(ficheSigneeRepository, never()).save(any());
    }

    @Test
    void importer_enregistreLaFicheRattacheeALaConsultation() {
        ConsultationInitiale consultation = ConsultationInitiale.builder().id(UUID.randomUUID()).build();
        when(consultationRepository.getReferenceById(consultation.getId())).thenReturn(consultation);

        FicheSigneeModel resultat = ficheSigneeService.importer(consultation.getId(), fichier("application/pdf", PDF));

        ArgumentCaptor<FicheSignee> captor = ArgumentCaptor.forClass(FicheSignee.class);
        verify(ficheSigneeRepository).save(captor.capture());
        FicheSignee enregistree = captor.getValue();
        assertThat(enregistree.getConsultation()).isSameAs(consultation);
        assertThat(enregistree.getNomFichier()).isEqualTo("fiche.pdf");
        assertThat(enregistree.getTypeContenu()).isEqualTo("application/pdf");
        assertThat(enregistree.getTailleOctets()).isEqualTo(PDF.length);
        assertThat(enregistree.getContenu()).isEqualTo(PDF);
        assertThat(enregistree.getDateImport()).isNotNull();

        assertThat(resultat.consultationId()).isEqualTo(consultation.getId());
        assertThat(resultat.nomFichier()).isEqualTo("fiche.pdf");
        assertThat(resultat.dateImport()).isEqualTo(enregistree.getDateImport());
    }

    private FicheSigneeModel fichier(String typeContenu, byte[] contenu) {
        return FicheSigneeModel.builder()
                .nomFichier("fiche.pdf")
                .typeContenu(typeContenu)
                .tailleOctets(contenu.length)
                .contenu(contenu)
                .build();
    }
}
