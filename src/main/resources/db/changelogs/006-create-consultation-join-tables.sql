--liquibase formatted sql

--changeset Mody:006-1-create-consultation-facteur-de-risque
CREATE TABLE consultation_facteur_de_risque
(
    consultation_id      UUID   NOT NULL,
    facteur_de_risque_id BIGINT NOT NULL,
    CONSTRAINT pk_consultation_facteur_de_risque PRIMARY KEY (consultation_id, facteur_de_risque_id),
    CONSTRAINT fk_cfr_consultation FOREIGN KEY (consultation_id) REFERENCES consultation_initiale (id),
    CONSTRAINT fk_cfr_facteur FOREIGN KEY (facteur_de_risque_id) REFERENCES facteur_de_risque (id)
);

--changeset Mody:006-2-create-consultation-situation-sociale
CREATE TABLE consultation_situation_sociale
(
    consultation_id      UUID   NOT NULL,
    situation_sociale_id BIGINT NOT NULL,
    CONSTRAINT pk_consultation_situation_sociale PRIMARY KEY (consultation_id, situation_sociale_id),
    CONSTRAINT fk_css_consultation FOREIGN KEY (consultation_id) REFERENCES consultation_initiale (id),
    CONSTRAINT fk_css_situation FOREIGN KEY (situation_sociale_id) REFERENCES situation_sociale (id)
);
