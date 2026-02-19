package no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder

import io.kotest.assertions.json.shouldEqualJson
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.KlagehjemmelDto
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
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:37.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${rammevedtakSøknadsbehandling.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": "PROSESSUELL_FEIL",
                  "begrunnelse": "Begrunnelse for omgjøring",
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null
                }
                """.trimIndent(),
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
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:37.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OPPRETTHOLDT",
                  "vedtakDetKlagesPå": "${rammevedtakSøknadsbehandling.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": null,
                  "begrunnelse":null,
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": [
                    "ARBEIDSMARKEDSLOVEN_17",
                    "TILTAKSPENGEFORSKRIFTEN_2"
                   ],
                   "iverksattOpprettholdelseTidspunkt": null,
                   "journalføringstidspunktInnstillingsbrev": null,
                   "distribusjonstidspunktInnstillingsbrev": null,
                   "oversendtKlageinstansenTidspunkt": null
                }
                """.trimIndent(),
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
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678911",
                  "opprettet": "2025-01-01T01:02:36.456789",
                  "sistEndret": "2025-01-01T01:02:45.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "12345",
                  "journalpostOpprettet": "2025-01-01T01:02:35.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "${vedtakDetKlagesPå.id}",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": true,
                  "erUnntakForKlagefrist": null,
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": null,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": "ANNET",
                  "begrunnelse": "oppdatert begrunnelse for omgjøring",
                  "rammebehandlingId": "${rammebehandlingMedKlagebehandling.id}",
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null
                }
                """.trimIndent(),
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
