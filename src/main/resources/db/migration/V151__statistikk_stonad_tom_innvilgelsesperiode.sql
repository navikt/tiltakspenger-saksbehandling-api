update statistikk_stonad
set innvilgelsesperioder='[]'
where innvilgelsesperioder is null
  and (resultat = 'Avslag' or resultat = 'Stans');
