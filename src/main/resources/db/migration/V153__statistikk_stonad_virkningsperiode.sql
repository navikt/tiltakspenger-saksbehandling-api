update statistikk_stonad
set virkningsperiode_fra_og_med = fra_og_med
where virkningsperiode_fra_og_med is null;

update statistikk_stonad
set virkningsperiode_til_og_med = til_og_med
where virkningsperiode_til_og_med is null;
