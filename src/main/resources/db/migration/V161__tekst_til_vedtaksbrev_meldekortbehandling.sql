/*
Brevteksten for meldekortbehandling har vært gjennom noen faser.
    Først var ikke begrunnelsen med i vedtaksrebevet.
    Så ble det deretter tatt med kun for korrigeringer.
    Etter det igjen, tok vi det alltid med.

Så har vi nå valgt å splitte begrunnelses feltet for behandlingen, og det som skal være vedtaksbrevteksten (tekst_til_vedtaksbrev).

Denne migreringen prøver på sitt beste måte å ta stilling til endringene over.

Merk at noen begrunnelsesfelt er tomme strenger, disse skal være null i tekst_til_vedtaksbrev.
 */
ALTER TABLE meldekortbehandling
    ADD COLUMN IF NOT EXISTS tekst_til_vedtaksbrev VARCHAR;

/*
    meldekortbehandlinger før 11.04.2025 13:48 skal ikke ha tekst til vedtaksbrev
    vedtaksbrev teksten ble tatt med først etter denne datoen 11.04.2025 13:48
 */
UPDATE meldekortbehandling mb
set tekst_til_vedtaksbrev = null
from meldekortvedtak mv
where mb.id = mv.meldekort_id
  and mv.journalføringstidspunkt < '2025-04-11 13:48:00';

/*
    meldekortbehandlinger i denne tidsperioden er korrigeringer som har fått vedtaksbrev-teksten sin som begrunnelse
 */
UPDATE meldekortbehandling mb
set tekst_til_vedtaksbrev = begrunnelse
from meldekortvedtak mv
where mb.id = mv.meldekort_id
  and mb.tekst_til_vedtaksbrev is null
  and mb.begrunnelse is not null
  and mb.begrunnelse != ''
  and mv.journalføringstidspunkt > '2025-04-11 13:48:00'
  and mv.journalføringstidspunkt < '2025-11-13 12:48:00'
  and mb.type = 'KORRIGERING';

/*
  Meldekortbehandlinger etter 13.11.2025 12:48 skal alltid ha vedtaksbrev-teksten sin som begrunnelse
 */
UPDATE meldekortbehandling mb
set tekst_til_vedtaksbrev = begrunnelse
from meldekortvedtak mv
where mb.id = mv.meldekort_id
  and mb.tekst_til_vedtaksbrev is null
  and mb.begrunnelse is not null
  and mb.begrunnelse != ''
  and mv.journalføringstidspunkt > '2025-11-13 12:48:00'


