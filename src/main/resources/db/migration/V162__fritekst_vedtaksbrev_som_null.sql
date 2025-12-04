/*
 Det er ønskelig at vi lagrer null for brevtekst i vedtaksbrev istedenfor tom streng.
 */
UPDATE behandling
set fritekst_vedtaksbrev = null
WHERE fritekst_vedtaksbrev = '';


