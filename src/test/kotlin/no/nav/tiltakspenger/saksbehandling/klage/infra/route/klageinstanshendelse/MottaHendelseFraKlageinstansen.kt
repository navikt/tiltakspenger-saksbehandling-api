package no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse

import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import java.util.UUID

/**
 * simulerer at Klageinstansen har mottatt klagen, og sender en hendelse tilbake til saksbehandlingstjenesten.
 *
 * @param hendelse - String som representerer hendelsen fra klageinstansen. Du kan bruke [GenerererKlageinstanshendelse] for å generere hendelse
 */
fun TestApplicationContext.mottaHendelseFraKlageinstansen(
    hendelse: String,
) {
    val nyHendelse = NyKlagehendelse(
        klagehendelseId = KlagehendelseId.random(),
        opprettet = nå(clock),
        sistEndret = nå(clock),
        eksternKlagehendelseId = UUID.randomUUID().toString(),
        key = "mottaHendelseFraKlageinstansen-${UUID.randomUUID()}",
        value = hendelse,
        sakId = null,
        klagebehandlingId = null,
    )

    this.klagebehandlingContext.klagehendelseRepo.lagreNyHendelse(nyHendelse)
}
