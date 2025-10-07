UPDATE behandling
SET beregning = jsonb_build_object('beregninger', beregning)
WHERE beregning is not null;