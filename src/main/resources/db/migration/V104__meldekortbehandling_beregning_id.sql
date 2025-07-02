UPDATE meldekortbehandling
SET beregninger = (SELECT jsonb_agg(elem || jsonb_build_object('beregningId', gen_random_uuid()))
                   FROM jsonb_array_elements(beregninger) as t(elem))
