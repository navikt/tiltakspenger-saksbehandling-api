package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

import io.kotest.assertions.json.shouldEqualJson
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.formkrav.KlagefristUnntakSvarord
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.KlageOmgjøringsårsak
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.vurder.Vurderingstype
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterKlagebehandlingFormkravForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingFormkrav
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.vurderKlagebehandling
import org.junit.jupiter.api.Test

class OppdaterKlagebehandlingFormkravRouteTest {
    @Test
    fun `kan oppdatere klagebehandling - formkrav`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, json) =
                opprettSakOgOppdaterKlagebehandlingFormkrav(
                    tac = tac,
                    fnr = fnr,
                    erKlagerPartISaken = false,
                    klagesDetPåKonkreteElementerIVedtaket = false,
                    erKlagefristenOverholdt = false,
                    erKlagenSignert = false,
                    erUnntakForKlagefrist = KlagefristUnntakSvarord.NEI,
                    journalpostId = JournalpostId("123456"),
                    vedtakDetKlagesPå = VedtakId.fromString("vedtak_01KEYFMDNGXAFAYW1CD1X47CND"),
                )!!
            json.toString().shouldEqualJson(
                """
                {
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678912",
                  "opprettet": "2025-01-01T01:02:07.456789",
                  "sistEndret": "2025-01-01T01:02:10.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "123456",
                  "journalpostOpprettet": "2025-01-01T01:02:09.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "AVVIST",
                  "vedtakDetKlagesPå": "vedtak_01KEYFMDNGXAFAYW1CD1X47CND",
                  "erKlagerPartISaken": false,
                  "klagesDetPåKonkreteElementerIVedtaket": false,
                  "erKlagefristenOverholdt": false,
                  "erUnntakForKlagefrist": "NEI",
                  "erKlagenSignert": false,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": null,
                  "begrunnelse": null,
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": null,
                  "ferdigstiltTidspunkt": null
                }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `oppdatering av formkrav endrer ikke resultatet hvis resultat er omgjøring, og oppdatering er omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, json) = opprettSakOgOppdaterKlagebehandlingFormkrav(
                tac = tac,
                fnr = fnr,
                erKlagerPartISaken = true,
                klagesDetPåKonkreteElementerIVedtaket = true,
                erKlagefristenOverholdt = true,
                erKlagenSignert = true,
                erUnntakForKlagefrist = null,
                journalpostId = JournalpostId("123456"),
                vedtakDetKlagesPå = VedtakId.fromString("vedtak_01KEYFMDNGXAFAYW1CD1X47CND"),
            )!!

            vurderKlagebehandling(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                begrunnelse = Begrunnelse.createOrThrow("Begrunnelse for omgjøring"),
                årsak = KlageOmgjøringsårsak.FEIL_LOVANVENDELSE,
                vurderingstype = Vurderingstype.OMGJØR,
                hjemler = null,
            )!!

            oppdaterKlagebehandlingFormkravForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                erKlagerPartISaken = true,
                klagesDetPåKonkreteElementerIVedtaket = true,
                erKlagefristenOverholdt = false,
                erKlagenSignert = true,
                erUnntakForKlagefrist = KlagefristUnntakSvarord.JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN,
                journalpostId = JournalpostId("123456"),
                vedtakDetKlagesPå = VedtakId.fromString("vedtak_01KEYFMDNGXAFAYW1CD1X47CND"),
                forventetJsonBody = {
                    //language=json
                    """{
                  "id": "${klagebehandling.id}",
                  "sakId": "${sak.id}",
                  "saksnummer": "${sak.saksnummer}",
                  "fnr": "12345678912",
                  "opprettet": "2025-01-01T01:02:07.456789",
                  "sistEndret": "2025-01-01T01:02:13.456789",
                  "iverksattTidspunkt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "journalpostId": "123456",
                  "journalpostOpprettet": "2025-01-01T01:02:12.456789",
                  "status": "UNDER_BEHANDLING",
                  "resultat": "OMGJØR",
                  "vedtakDetKlagesPå": "vedtak_01KEYFMDNGXAFAYW1CD1X47CND",
                  "erKlagerPartISaken": true,
                  "klagesDetPåKonkreteElementerIVedtaket": true,
                  "erKlagefristenOverholdt": false,
                  "erUnntakForKlagefrist": "JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN",
                  "erKlagenSignert": true,
                  "innsendingsdato": "2026-02-16",
                  "innsendingskilde": "DIGITAL",
                  "brevtekst": [],
                  "avbrutt": null,
                  "kanIverksetteVedtak": false,
                  "kanIverksetteOpprettholdelse": false,
                  "årsak": "FEIL_LOVANVENDELSE",
                  "begrunnelse": "Begrunnelse for omgjøring",
                  "rammebehandlingId": null,
                  "ventestatus": null,
                  "hjemler": null,
                  "iverksattOpprettholdelseTidspunkt": null,
                  "journalføringstidspunktInnstillingsbrev": null,
                  "distribusjonstidspunktInnstillingsbrev": null,
                  "oversendtKlageinstansenTidspunkt": null,
                  "klageinstanshendelser": null,
                  "ferdigstiltTidspunkt": null
                }
                    """.trimIndent()
                },
            )!!
        }
    }

    @Test
    fun `kan ikke oppdatere formkrav til avvist dersom klage er omgjøring med rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage(
                tac = tac,
                type = "REVURDERING_INNVILGELSE",
            )!!

            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            oppdaterKlagebehandlingFormkravForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                vedtakDetKlagesPå = null,
                erKlagerPartISaken = true,
                klagesDetPåKonkreteElementerIVedtaket = true,
                erKlagefristenOverholdt = true,
                erUnntakForKlagefrist = null,
                erKlagenSignert = true,
                forventetStatus = HttpStatusCode.BadRequest,
            )
        }
    }
}
