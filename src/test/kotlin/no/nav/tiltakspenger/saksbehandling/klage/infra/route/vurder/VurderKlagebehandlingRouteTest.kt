package no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder

import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import org.junit.jupiter.api.Test

class VurderKlagebehandlingRouteTest {
    @Test
    fun `kan omgjøre klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                status = "UNDER_BEHANDLING",
                årsak = "PROSESSUELL_FEIL",
                begrunnelse = "Begrunnelse for omgjøring",
            )
        }
    }

    @Test
    fun `kan opprettholde klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, _, rammevedtakSøknadsbehandling, klagebehandling, json) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
                vurderingstype = Vurderingstype.OPPRETTHOLD,
                årsak = null,
                begrunnelse = null,
                hjemler = listOf(
                    KlagehjemmelDto.ARBEIDSMARKEDSLOVEN_17,
                    KlagehjemmelDto.TILTAKSPENGEFORSKRIFTEN_2,
                ),
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = "${rammevedtakSøknadsbehandling.id}",
                status = "UNDER_BEHANDLING",
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17", "TILTAKSPENGEFORSKRIFTEN_2"),
            )
        }
    }

    @Test
    fun `kan endre årsak og begrunnelse ved rammebehandling under behandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            rammebehandlingMedKlagebehandling as Søknadsbehandling
            val vedtakDetKlagesPå = sak.rammevedtaksliste.first()
            val (_, _, json) = vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!),
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "${vedtakDetKlagesPå.id}",
                status = "UNDER_BEHANDLING",
                kanIverksetteVedtak = null,
                årsak = "ANNET",
                begrunnelse = "oppdatert begrunnelse for omgjøring",
                rammebehandlingId = "${rammebehandlingMedKlagebehandling.id}",
            )
        }
    }

    @Test
    fun `kan ikke vurdere klage når rammebehandling er klar til beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                      {
                         "melding": "Feil rammebehandlingsstatus. Forventet: [UNDER_BEHANDLING], faktisk: KLAR_TIL_BESLUTNING",
                         "kode": "feil_rammebehandlingsstatus"
                      }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan ikke vurdere klage når rammebehandling er under beslutning`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                saksbehandler = beslutter,
            )
            vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                      {
                         "melding": "Feil rammebehandlingsstatus. Forventet: [UNDER_BEHANDLING], faktisk: UNDER_BESLUTNING",
                         "kode": "feil_rammebehandlingsstatus"
                      }
                    """.trimIndent()
                },
            )
        }
    }

    @Test
    fun `kan ikke vurdere klage når rammebehandling er iverksatt`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandlingg, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandlingg.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandlingg.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandlingg.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandlingg.id,
                saksbehandler = beslutter,
            )
            iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandlingg.id,
                beslutter = beslutter,
            )
            vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = saksbehandler,
                begrunnelse = Begrunnelse.createOrThrow("oppdatert begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.ANNET,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                      {
                        "melding": "Feil klagebehandlingsstatus. Forventet: [UNDER_BEHANDLING], faktisk: VEDTATT",
                        "kode": "feil_klagebehandlingsstatus"
                      }
                    """.trimIndent()
                },
            )
        }
    }
}
