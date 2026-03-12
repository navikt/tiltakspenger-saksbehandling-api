CREATE TYPE periode_datoer AS
(
    fra_og_med date,
    til_og_med date
);

CREATE DOMAIN periode AS periode_datoer
    CHECK (
        VALUE IS NULL OR (
            (VALUE).fra_og_med IS NOT NULL AND
            (VALUE).til_og_med IS NOT NULL AND
            (VALUE).fra_og_med <= (VALUE).til_og_med)
        );

CREATE FUNCTION periode_datoer_to_daterange(periode_datoer) RETURNS daterange AS
$$
SELECT daterange($1.fra_og_med, $1.til_og_med, '[]');
$$ LANGUAGE sql IMMUTABLE
                STRICT;

CREATE CAST (periode_datoer AS daterange)
    WITH FUNCTION periode_datoer_to_daterange(periode_datoer)
    AS IMPLICIT;