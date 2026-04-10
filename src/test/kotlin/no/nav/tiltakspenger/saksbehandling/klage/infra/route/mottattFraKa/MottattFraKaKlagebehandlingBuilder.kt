package no.nav.tiltakspenger.saksbehandling.klage.infra.route.mottattFraKa

import io.kotest.assertions.json.CompareJsonOptions
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.infra.route.KlagebehandlingDTOJson
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.klageinstanshendelse.mottaHendelseFraKlageinstansen
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.util.UUID

/**
 * Route: [no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett.iverksettAvvistKlagebehandlingRoute]
 */
interface MottattFraKaKlagebehandlingBuilder {
    suspend fun ApplicationTestBuilder.opprettSakOgMottaOppretholdtKlagebehandlingFraKa(
        tac: TestApplicationContext,
        fnr: Fnr = ObjectMother.gyldigFnr(),
        saksbehandler: Saksbehandler = ObjectMother.saksbehandler("saksbehandlerKlagebehandling"),
        forventetStatus: HttpStatusCode? = HttpStatusCode.OK,
        forventetJsonBody: (CompareJsonOptions.() -> String)? = null,
        utførJobber: Boolean = true,
        hendelseGenerering: (
            sak: Sak,
            klagebehandling: Klagebehandling,
        ) -> String = { sak, klagebehandling ->
            GenerererKlageinstanshendelse.avsluttetJson(
                eventId = UUID.randomUUID().toString(),
                kildeReferanse = klagebehandling.id.toString(),
                kabalReferanse = UUID.randomUUID().toString(),
                avsluttetTidspunkt = nå(tac.clock).toString(),
                utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.STADFESTELSE,
                journalpostReferanser = emptyList(),
            )
        },
    ): Triple<Sak, Klagebehandling, KlagebehandlingDTOJson>? {
        val (sak, klagebehandling, json) = this.opprettSakOgOpprettholdKlagebehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            fnr = fnr,
        ) ?: return null

        if (utførJobber) {
            tac.mottaHendelseFraKlageinstansen(hendelseGenerering(sak, klagebehandling))
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
        }

        return Triple(sak, klagebehandling, json)
    }
}
