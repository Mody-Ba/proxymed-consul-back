package com.proxymed.service;

import com.proxymed.enums.TypeConstanteVitale;
import com.proxymed.service.model.ConstanteVitaleModel;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Bornes normales/alerte par constante vitale (cahier des charges section 5.1 et 9).
 * Valeurs par defaut a confirmer avec le medecin referent du programme avant mise en
 * production - non fournies dans le cahier des charges V1.
 *
 * Pour chaque type : {alerteBas, normalBas, normalHaut, alerteHaut}.
 * En dessous de alerteBas ou au-dessus de alerteHaut -> alerte.
 * Entre normalBas et normalHaut (inclus) -> normal.
 * Entre les deux (zone intermediaire) -> ni normal ni alerte (badge neutre).
 *
 * TA est traitee comme une valeur unique (systolique) : le modele ne distingue pas
 * systolique/diastolique pour l'instant (a revoir si besoin).
 * POIDS et TAILLE n'ont pas de notion d'alerte (mesures brutes, pas de norme universelle).
 *
 * Opere sur ConstanteVitaleModel (couche interne neutre) : c'est une regle de gestion,
 * pas une preoccupation de persistance.
 */
final class BornesConstantesVitales {

    private record Bornes(double alerteBas, double normalBas, double normalHaut, double alerteHaut) {
    }

    private static final Map<TypeConstanteVitale, Bornes> BORNES = Map.of(
            TypeConstanteVitale.TA, new Bornes(70, 90, 140, 180),
            TypeConstanteVitale.FC, new Bornes(40, 60, 100, 130),
            TypeConstanteVitale.FR, new Bornes(8, 12, 20, 30),
            TypeConstanteVitale.SPO2, new Bornes(90, 95, 100, 100),
            TypeConstanteVitale.TEMPERATURE, new Bornes(35.0, 36.1, 37.5, 39.0),
            TypeConstanteVitale.GLYCEMIE_CAPILLAIRE, new Bornes(50, 70, 140, 250),
            TypeConstanteVitale.IMC, new Bornes(16, 18.5, 24.9, 35)
    );

    private BornesConstantesVitales() {
    }

    static ConstanteVitaleModel appliquer(ConstanteVitaleModel constante) {
        Bornes bornes = BORNES.get(constante.type());
        if (bornes == null || constante.valeur() == null) {
            return constante.toBuilder().estNormal(null).estAlerte(null).build();
        }
        double valeur = constante.valeur().doubleValue();
        if (valeur < bornes.alerteBas() || valeur > bornes.alerteHaut()) {
            return constante.toBuilder().estNormal(false).estAlerte(true).build();
        }
        if (valeur >= bornes.normalBas() && valeur <= bornes.normalHaut()) {
            return constante.toBuilder().estNormal(true).estAlerte(false).build();
        }
        return constante.toBuilder().estNormal(false).estAlerte(false).build();
    }

    /**
     * IMC = poids (kg) / taille (m)^2. TAILLE est supposee saisie en centimetres.
     */
    static BigDecimal calculerImc(BigDecimal poidsKg, BigDecimal tailleCm) {
        BigDecimal tailleM = tailleCm.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal tailleM2 = tailleM.multiply(tailleM);
        return poidsKg.divide(tailleM2, 1, java.math.RoundingMode.HALF_UP);
    }
}
