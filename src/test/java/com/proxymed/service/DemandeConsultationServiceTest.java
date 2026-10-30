package com.proxymed.service;

import com.proxymed.entity.DemandeConsultation;
import com.proxymed.enums.StatutDemandeConsultation;
import com.proxymed.exception.DemandeDejaTraiteeException;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.DemandeConsultationRepository;
import com.proxymed.service.mappers.DemandeConsultationMapper;
import com.proxymed.service.model.DemandeConsultationModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandeConsultationServiceTest {

    @Mock
    private DemandeConsultationRepository demandeConsultationRepository;

    private DemandeConsultationService service;

    @BeforeEach
    void setUp() {
        service = new DemandeConsultationServiceImpl(demandeConsultationRepository, new DemandeConsultationMapper());
        lenient().when(demandeConsultationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(demandeConsultationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void creer_forceEnAttenteEtIgnoreLesChampsDeDecision() {
        UUID demandeur = UUID.randomUUID();
        DemandeConsultationModel model = DemandeConsultationModel.builder()
                .id(UUID.randomUUID())
                .demandeurId(demandeur)
                .nomPatient("Awa Diop")
                .pourProche(true)
                .motif("Toux")
                .statut(StatutDemandeConsultation.ACCEPTEE)
                .medecinSeniorId(UUID.randomUUID())
                .juniorId(UUID.randomUUID())
                .motifRefus("x")
                .dateDecision(Instant.now())
                .build();

        DemandeConsultationModel cree = service.creer(model);

        assertThat(cree.id()).isNull();
        assertThat(cree.demandeurId()).isEqualTo(demandeur);
        assertThat(cree.pourProche()).isTrue();
        assertThat(cree.statut()).isEqualTo(StatutDemandeConsultation.EN_ATTENTE);
        assertThat(cree.medecinSeniorId()).isNull();
        assertThat(cree.juniorId()).isNull();
        assertThat(cree.motifRefus()).isNull();
        assertThat(cree.dateDecision()).isNull();
        assertThat(cree.dateCreation()).isNotNull();
    }

    @Test
    void findAll_filtreParStatut_etTriePlusRecentesDabord() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(demandeConsultationRepository.findByStatut(any(), pageable.capture())).thenReturn(Page.empty());

        service.findAll(StatutDemandeConsultation.REFUSEE, 2, 10);

        verify(demandeConsultationRepository).findByStatut(StatutDemandeConsultation.REFUSEE, pageable.getValue());
        verify(demandeConsultationRepository, never()).findAll(any(Pageable.class));
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("dateCreation").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_sansStatut_renvoieTout() {
        when(demandeConsultationRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(enAttente(UUID.randomUUID()))));

        assertThat(service.findAll(null, 0, 20).getContent()).hasSize(1);
    }

    @Test
    void pagination_rejetteValeursHorsBornes() {
        assertThatThrownBy(() -> service.findAll(null, -1, 20)).isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> service.findByDemandeur(UUID.randomUUID(), 0, 0))
                .isInstanceOf(RegleGestionException.class);
        assertThatThrownBy(() -> service.findAssigneesAuJunior(UUID.randomUUID(), 0, 101))
                .isInstanceOf(RegleGestionException.class);
        verifyNoInteractions(demandeConsultationRepository);
    }

    @Test
    void findAssigneesAuJunior_neChercheQueLesAcceptees() {
        UUID junior = UUID.randomUUID();
        when(demandeConsultationRepository.findByJuniorIdAndStatut(any(), any(), any())).thenReturn(Page.empty());

        service.findAssigneesAuJunior(junior, 0, 20);

        verify(demandeConsultationRepository)
                .findByJuniorIdAndStatut(eq(junior), eq(StatutDemandeConsultation.ACCEPTEE), any(Pageable.class));
    }

    @Test
    void findById_rejette_siInexistante() {
        UUID id = UUID.randomUUID();
        when(demandeConsultationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Demande de consultation introuvable : " + id);
    }

    @Test
    void accepter_enregistreSeniorJuniorEtDate() {
        UUID id = UUID.randomUUID();
        UUID senior = UUID.randomUUID();
        UUID junior = UUID.randomUUID();
        when(demandeConsultationRepository.findById(id)).thenReturn(Optional.of(enAttente(id)));

        DemandeConsultationModel resultat = service.accepter(id, senior, junior);

        assertThat(resultat.statut()).isEqualTo(StatutDemandeConsultation.ACCEPTEE);
        assertThat(resultat.medecinSeniorId()).isEqualTo(senior);
        assertThat(resultat.juniorId()).isEqualTo(junior);
        assertThat(resultat.dateDecision()).isNotNull();
    }

    @Test
    void refuser_enregistreMotifSansEspacesDeBord() {
        UUID id = UUID.randomUUID();
        when(demandeConsultationRepository.findById(id)).thenReturn(Optional.of(enAttente(id)));

        DemandeConsultationModel resultat = service.refuser(id, UUID.randomUUID(), "  Hors zone  ");

        assertThat(resultat.statut()).isEqualTo(StatutDemandeConsultation.REFUSEE);
        assertThat(resultat.motifRefus()).isEqualTo("Hors zone");
        assertThat(resultat.juniorId()).isNull();
        assertThat(resultat.dateDecision()).isNotNull();
    }

    @Test
    void accepterOuRefuser_rejette_siDejaTraitee() {
        for (StatutDemandeConsultation statut : List.of(StatutDemandeConsultation.ACCEPTEE, StatutDemandeConsultation.REFUSEE)) {
            UUID id = UUID.randomUUID();
            DemandeConsultation demande = enAttente(id);
            demande.setStatut(statut);
            when(demandeConsultationRepository.findById(id)).thenReturn(Optional.of(demande));

            assertThatThrownBy(() -> service.accepter(id, UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(DemandeDejaTraiteeException.class);
            assertThatThrownBy(() -> service.refuser(id, UUID.randomUUID(), "motif"))
                    .isInstanceOf(DemandeDejaTraiteeException.class);
        }
        verify(demandeConsultationRepository, never()).saveAndFlush(any());
    }

    /** Deux seniors decident en meme temps : le perdant echoue au controle de version -> 409. */
    @Test
    void accepter_rejette_siDecisionSimultanee() {
        UUID id = UUID.randomUUID();
        when(demandeConsultationRepository.findById(id)).thenReturn(Optional.of(enAttente(id)));
        when(demandeConsultationRepository.saveAndFlush(any()))
                .thenThrow(new ObjectOptimisticLockingFailureException(DemandeConsultation.class, id));

        assertThatThrownBy(() -> service.accepter(id, UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(DemandeDejaTraiteeException.class);
    }

    @Test
    void verifierJuniorAssigne_accepte_leJuniorDeLaDemandeLiee() {
        UUID consultationId = UUID.randomUUID();
        UUID junior = UUID.randomUUID();
        when(demandeConsultationRepository.findByConsultationId(consultationId))
                .thenReturn(Optional.of(liee(consultationId, junior)));

        service.verifierJuniorAssigne(consultationId, junior);
    }

    @Test
    void verifierJuniorAssigne_rejette_unAutreJunior() {
        UUID consultationId = UUID.randomUUID();
        when(demandeConsultationRepository.findByConsultationId(consultationId))
                .thenReturn(Optional.of(liee(consultationId, UUID.randomUUID())));

        assertThatThrownBy(() -> service.verifierJuniorAssigne(consultationId, UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    /** Consultation creee sans demande (ou avant les demandes) : aucune assignation a controler. */
    @Test
    void verifierJuniorAssigne_neRestreintPas_sansDemandeLiee() {
        UUID consultationId = UUID.randomUUID();
        when(demandeConsultationRepository.findByConsultationId(consultationId)).thenReturn(Optional.empty());

        service.verifierJuniorAssigne(consultationId, UUID.randomUUID());
    }

    private static DemandeConsultation liee(UUID consultationId, UUID junior) {
        DemandeConsultation demande = enAttente(UUID.randomUUID());
        demande.setStatut(StatutDemandeConsultation.ACCEPTEE);
        demande.setJuniorId(junior);
        demande.setConsultationId(consultationId);
        return demande;
    }

    private static DemandeConsultation enAttente(UUID id) {
        return DemandeConsultation.builder()
                .id(id)
                .demandeurId(UUID.randomUUID())
                .nomPatient("Awa Diop")
                .motif("Toux")
                .statut(StatutDemandeConsultation.EN_ATTENTE)
                .dateCreation(Instant.now())
                .version(0L)
                .build();
    }
}
