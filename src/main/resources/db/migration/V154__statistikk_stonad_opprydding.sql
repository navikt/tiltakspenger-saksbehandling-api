alter table statistikk_stonad
    drop column gyldig_fra_dato;

alter table statistikk_stonad
    drop column gyldig_til_dato;

delete from statistikk_stonad where resultat = 'Avslag';
