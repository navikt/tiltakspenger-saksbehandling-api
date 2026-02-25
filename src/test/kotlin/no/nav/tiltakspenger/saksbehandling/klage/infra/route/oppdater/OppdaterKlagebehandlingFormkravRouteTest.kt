package no.nav.tiltakspenger.saksbehandling.klage.infra.route.oppdater

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
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
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
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678912",
                journalpostId = "123456",
                resultat = "AVVIST",
                vedtakDetKlagesPå = "vedtak_01KEYFMDNGXAFAYW1CD1X47CND",
                erKlagerPartISaken = false,
                klagesDetPåKonkreteElementerIVedtaket = false,
                erKlagefristenOverholdt = false,
                erUnntakForKlagefrist = "NEI",
                erKlagenSignert = false,
            )
        }
    }

    @Test
    fun `oppdatering av formkrav endrer ikke resultatet hvis resultat er omgjøring, og oppdatering er omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val fnr = Fnr.fromString("12345678912")
            val (sak, klagebehandling, _) = opprettSakOgOppdaterKlagebehandlingFormkrav(
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

            val (_, _, json) = oppdaterKlagebehandlingFormkravForSakId(
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

            )!!
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678912",
                journalpostId = "123456",
                resultat = "OMGJØR",
                vedtakDetKlagesPå = "vedtak_01KEYFMDNGXAFAYW1CD1X47CND",
                erKlagefristenOverholdt = false,
                erUnntakForKlagefrist = "JA_KLAGER_KAN_IKKE_LASTES_FOR_Å_HA_SENDT_INN_ETTER_FRISTEN",
                årsak = "FEIL_LOVANVENDELSE",
                begrunnelse = "Begrunnelse for omgjøring",
            )
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
