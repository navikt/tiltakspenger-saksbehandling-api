update statistikk_stonad
set innvilgelsesperioder=jsonb_build_array(
        jsonb_build_object(
                'fraOgMed', to_jsonb(fra_og_med),
                'tilOgMed', to_jsonb(til_og_med)
        )
                         )
where innvilgelsesperioder is null
  and resultat = 'Innvilgelse';
