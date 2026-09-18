package com.proxymed.service;

import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.service.model.ConstanteVitaleModel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BornesConstantesVitalesTest {

    @Test
    void appliquer_valeurNormale_fc() {
        ConstanteVitaleModel fc = ConstanteVitaleModel.builder().type(TypeConstanteVitale.FC).valeur(BigDecimal.valueOf(80)).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(fc);

        assertThat(resultat.estNormal()).isTrue();
        assertThat(resultat.estAlerte()).isFalse();
    }

    @Test
    void appliquer_valeurAlerteBasse_fc() {
        ConstanteVitaleModel fc = ConstanteVitaleModel.builder().type(TypeConstanteVitale.FC).valeur(BigDecimal.valueOf(30)).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(fc);

        assertThat(resultat.estNormal()).isFalse();
        assertThat(resultat.estAlerte()).isTrue();
    }

    @Test
    void appliquer_valeurAlerteHaute_spo2() {
        ConstanteVitaleModel spo2 = ConstanteVitaleModel.builder().type(TypeConstanteVitale.SPO2).valeur(BigDecimal.valueOf(85)).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(spo2);

        assertThat(resultat.estAlerte()).isTrue();
        assertThat(resultat.estNormal()).isFalse();
    }

    @Test
    void appliquer_zoneIntermediaire_niNormalNiAlerte() {
        // FC : alerteBas=40, normalBas=60, normalHaut=100, alerteHaut=130 -> 115 est entre les deux
        ConstanteVitaleModel fc = ConstanteVitaleModel.builder().type(TypeConstanteVitale.FC).valeur(BigDecimal.valueOf(115)).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(fc);

        assertThat(resultat.estNormal()).isFalse();
        assertThat(resultat.estAlerte()).isFalse();
    }

    @Test
    void appliquer_poidsEtTaille_pasDeBornes() {
        ConstanteVitaleModel poids = ConstanteVitaleModel.builder().type(TypeConstanteVitale.POIDS).valeur(BigDecimal.valueOf(70)).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(poids);

        assertThat(resultat.estNormal()).isNull();
        assertThat(resultat.estAlerte()).isNull();
    }

    @Test
    void calculerImc_poidsEtTailleNormaux() {
        BigDecimal imc = BornesConstantesVitales.calculerImc(BigDecimal.valueOf(70), BigDecimal.valueOf(175));

        // 70 / 1.75^2 = 22.857... arrondi a 22.9
        assertThat(imc).isEqualByComparingTo("22.9");
    }

    @Test
    void appliquer_imcCalculeClasseCorrectement() {
        ConstanteVitaleModel imc = ConstanteVitaleModel.builder().type(TypeConstanteVitale.IMC).valeur(new BigDecimal("22.9")).build();

        ConstanteVitaleModel resultat = BornesConstantesVitales.appliquer(imc);

        assertThat(resultat.estNormal()).isTrue();
        assertThat(resultat.estAlerte()).isFalse();
    }
}
