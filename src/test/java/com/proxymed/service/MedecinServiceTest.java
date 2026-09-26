package com.proxymed.service;

import com.proxymed.entity.Medecin;
import com.proxymed.entity.Structure;
import com.proxymed.enums.RoleMedecin;
import com.proxymed.exception.RegleGestionException;
import com.proxymed.exception.ResourceNotFoundException;
import com.proxymed.repository.ConsultationRepository;
import com.proxymed.repository.MedecinRepository;
import com.proxymed.repository.StructureRepository;
import com.proxymed.service.mappers.MedecinMapper;
import com.proxymed.service.model.MedecinModel;
import com.proxymed.service.model.StructureModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedecinServiceTest {

    @Mock
    private MedecinRepository medecinRepository;
    @Mock
    private ConsultationRepository consultationRepository;
    @Mock
    private StructureRepository structureRepository;
    @Mock
    private StructureService structureService;

    private MedecinService medecinService;

    @BeforeEach
    void setUp() {
        medecinService = new MedecinServiceImpl(medecinRepository, consultationRepository, structureService,
                new MedecinMapper(structureRepository));
        lenient().when(medecinRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void findAll_filtreParRole_siRoleFourni() {
        Medecin senior = Medecin.builder().id(1L).nom("Diop").prenom("Awa").numeroOrdre("SEN01").role(RoleMedecin.SENIOR).build();
        when(medecinRepository.findByRole(RoleMedecin.SENIOR)).thenReturn(List.of(senior));

        var resultat = medecinService.findAll(RoleMedecin.SENIOR);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).nom()).isEqualTo("Diop");
    }

    @Test
    void findAll_retourneTout_siRoleAbsent() {
        Medecin junior = Medecin.builder().id(2L).nom("Fall").prenom("Omar").numeroOrdre("JUN01").role(RoleMedecin.JUNIOR).build();
        when(medecinRepository.findAll()).thenReturn(List.of(junior));

        var resultat = medecinService.findAll(null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).nom()).isEqualTo("Fall");
    }

    @Test
    void findById_rejette_siInexistant() {
        when(medecinRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medecinService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_verifieLExistenceDeLaStructureDeRattachement() {
        StructureModel structure = StructureModel.builder().id(10L).libelle("CHU").actif(true).build();
        when(structureService.findById(10L)).thenReturn(structure);
        when(structureRepository.getReferenceById(10L)).thenReturn(Structure.builder().id(10L).libelle("CHU").actif(true).build());

        MedecinModel model = MedecinModel.builder()
                .nom("Ndiaye").prenom("Fatou").numeroOrdre("NDI01")
                .role(RoleMedecin.SENIOR).structureRattachementId(10L).build();

        var resultat = medecinService.create(model);

        assertThat(resultat.nom()).isEqualTo("Ndiaye");
        assertThat(resultat.structureRattachementId()).isEqualTo(10L);
    }

    @Test
    void create_rejette_siStructureIntrouvable() {
        when(structureService.findById(404L)).thenThrow(new ResourceNotFoundException("Structure introuvable : 404"));

        MedecinModel model = MedecinModel.builder()
                .nom("Sow").prenom("Ibra").numeroOrdre("SOW01")
                .role(RoleMedecin.JUNIOR).structureRattachementId(404L).build();

        assertThatThrownBy(() -> medecinService.create(model))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_sansStructure_neVerifieRien() {
        MedecinModel model = MedecinModel.builder()
                .nom("Gueye").prenom("Aida").numeroOrdre("GUE01")
                .role(RoleMedecin.JUNIOR).build();

        var resultat = medecinService.create(model);

        assertThat(resultat.structureRattachementId()).isNull();
    }

    @Test
    void update_modifieLesChampsDuMedecinExistant() {
        Medecin existant = Medecin.builder().id(3L).nom("Ancien").prenom("Nom").numeroOrdre("ANC01").role(RoleMedecin.JUNIOR).build();
        when(medecinRepository.findById(3L)).thenReturn(Optional.of(existant));

        MedecinModel model = MedecinModel.builder()
                .nom("Nouveau").prenom("Nom").numeroOrdre("ANC01").role(RoleMedecin.SENIOR).build();
        var resultat = medecinService.update(3L, model);

        assertThat(resultat.nom()).isEqualTo("Nouveau");
        assertThat(resultat.role()).isEqualTo(RoleMedecin.SENIOR);
    }

    @Test
    void update_rejette_siMedecinIntrouvable() {
        when(medecinRepository.findById(99L)).thenReturn(Optional.empty());

        MedecinModel model = MedecinModel.builder().nom("X").prenom("Y").numeroOrdre("Z").role(RoleMedecin.JUNIOR).build();

        assertThatThrownBy(() -> medecinService.update(99L, model))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_rejette_siMedecinIntrouvable() {
        when(medecinRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> medecinService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_rejette_siMedecinRattacheAUneConsultation() {
        Medecin medecin = Medecin.builder().id(4L).nom("Diallo").prenom("Kine").numeroOrdre("DIA01").role(RoleMedecin.SENIOR).build();
        when(medecinRepository.findById(4L)).thenReturn(Optional.of(medecin));
        when(consultationRepository.existsByMedecinSeniorIdOrMedecinJuniorAffecteId(4L, 4L)).thenReturn(true);

        assertThatThrownBy(() -> medecinService.delete(4L))
                .isInstanceOf(RegleGestionException.class);
    }

    @Test
    void delete_supprimeLeMedecin_siAucuneConsultationRattachee() {
        Medecin medecin = Medecin.builder().id(5L).nom("Ba").prenom("Mody").numeroOrdre("BA01").role(RoleMedecin.JUNIOR).build();
        when(medecinRepository.findById(5L)).thenReturn(Optional.of(medecin));
        when(consultationRepository.existsByMedecinSeniorIdOrMedecinJuniorAffecteId(5L, 5L)).thenReturn(false);

        medecinService.delete(5L);

        org.mockito.Mockito.verify(medecinRepository).delete(medecin);
    }
}
