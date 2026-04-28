package no.nav.tiltakspenger.saksbehandling.klage.infra.route.avbryt

import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettOmgjøringInnvilgelseForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgFerdigstillOppretholdtKlagebehandlingForSak
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test

class AvbrytKlagebehandlingMedRammebehandlingRouteTest {

    /**
     * Se [Rammebehandlinger.oppdaterRammebehandling] for mer context.
     */
    @Test
    fun `kan avbryte rammebehandling hvor klagebehandlingen har flere rammebehandlinger tilknyttet hvor det finnes flere ulike klager og behandlinger`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, omgjøringsbehandling) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_OMGJØRING",
            )!!
            val saksbehandler = ObjectMother.saksbehandler(omgjøringsbehandling.saksbehandler!!)
            val (_, rammevedtak) = iverksettOmgjøringInnvilgelseForBehandlingId(
                tac = tac,
                sakId = sak.id,
                rammevedtakIdSomOmgjøres = (sak.vedtaksliste.alle.first() as Rammevedtak).id,
                behandlingId = omgjøringsbehandling.id,
                saksbehandler = saksbehandler,
            )

            val (_, ferdigstiltOpprettholdtKlagebehandling, _) = opprettOgFerdigstillOppretholdtKlagebehandlingForSak(
                tac = tac,
                sak = sak,
                vedtakDetKlagesPå = rammevedtak.id,
            )!!

            val (_, opprettetRammebehandling) = opprettRammebehandlingForKlage(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = ferdigstiltOpprettholdtKlagebehandling.id,
                vedtakIdSomOmgjøres = rammevedtak.id.toString(),
                type = "REVURDERING_OMGJØRING",
                saksbehandler = saksbehandler,
            )!!

            val (_, _, avbruttRammebehandling, sakJson) = avbrytRammebehandling(
                tac = tac,
                saksnummer = sak.saksnummer,
                sakId = sak.id,
                rammebehandlingId = opprettetRammebehandling.id,
                saksbehandler = saksbehandler,
            )!!

            val rammebehandlingJson = sakJson.get("behandlinger").last().toString()
            val klagebehandlingJson = sakJson.get("klageBehandlinger").last().toString()

            rammebehandlingJson.shouldBeRevurderingDTO(
                behandlingId = avbruttRammebehandling!!.id,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                rammevedtakId = null,
                saksbehandler = "saksbehandlerKlagebehandling",
                beslutter = null,
                klagebehandlingId = ferdigstiltOpprettholdtKlagebehandling.id,
                omgjørVedtak = rammevedtak.id,
                resultat = RammebehandlingResultatTypeDTO.OMGJØRING_IKKE_VALGT,
                //language=json
                avbrutt = """{"avbruttAv": "saksbehandlerKlagebehandling","avbruttTidspunkt": "TIMESTAMP","begrunnelse": "begrunnelse for avbryt søknad og/eller rammebehandling"}""",
                attesteringer = emptyList(),
                iverksattTidspunkt = null,
                vedtaksperiode = null,
                status = "AVBRUTT",
            )

            klagebehandlingJson.shouldBeFerdigstiltOpprettholdtKlagebehandlingDTO(
                resultat = ferdigstiltOpprettholdtKlagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt,
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = ferdigstiltOpprettholdtKlagebehandling.id,
                fnr = "12345678911",
                vedtakDetKlagesPå = "${rammevedtak.id}",
                behandlingDetKlagesPå = "${rammevedtak.behandlingId}",
                rammebehandlingId = emptyList(),
                åpenRammebehandlingId = null,
                brevtekst = listOf("""{"tittel":"Avvisning av klage","tekst":"Din klage er dessverre avvist."}"""),
            )
        }
    }
}
