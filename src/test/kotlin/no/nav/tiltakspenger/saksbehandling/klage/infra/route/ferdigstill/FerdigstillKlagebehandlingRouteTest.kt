package no.nav.tiltakspenger.saksbehandling.klage.infra.route.ferdigstill

import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgFerdigstillKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.`opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling`
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import java.util.UUID

class FerdigstillKlagebehandlingRouteTest {

    @Test
    fun `kan ferdigstille en klagebehandling som ikke har behov for videre behandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagebehandling, json) = opprettSakOgFerdigstillKlagebehandling(tac = tac)!!
            val resultat = klagebehandling.resultat as Klagebehandlingsresultat.Opprettholdt
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                saksbehandler = "saksbehandlerKlagebehandling",
                resultat = "OPPRETTHOLDT",
                vedtakDetKlagesPå = sak.rammevedtaksliste.first().id.toString(),
                status = "FERDIGSTILT",
                kanIverksetteVedtak = null,
                rammebehandlingId = null,
                brevtekst = listOf(
                    """{"tittel":"Hva klagesaken gjelder","tekst":"Vi viser til klage av 2025-01-01 på vedtak av 2025-01-01 der <kort om resultatet i vedtaket>"}""",
                    """{"tittel":"Klagers anførsler","tekst":"<saksbehandler fyller ut>"}""",
                    """{"tittel":"Vurdering av klagen","tekst":"<saksbehandler fyller ut>"}""",
                ),
                hjemler = listOf("ARBEIDSMARKEDSLOVEN_17"),
                iverksattOpprettholdelseTidspunkt = true,
                journalføringstidspunktInnstillingsbrev = true,
                distribusjonstidspunktInnstillingsbrev = true,
                oversendtKlageinstansenTidspunkt = true,
                ferdigstiltTidspunkt = true,
                journalpostIdInnstillingsbrev = "2",
                dokumentInfoIder = listOf("1"),
                klageinstanshendelser = listOf(
                    """
                     {
                      "klagehendelseId": "${resultat.klageinstanshendelser.single().klagehendelseId}",
                      "klagebehandlingId": "${klagebehandling.id}",
                      "opprettet": "TIMESTAMP",
                      "sistEndret": "TIMESTAMP",
                      "eksternKlagehendelseId": "${resultat.klageinstanshendelser.single().eksternKlagehendelseId}",
                      "avsluttetTidspunkt": "TIMESTAMP",
                      "journalpostreferanser": [],
                      "utfall": "STADFESTELSE",
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun `Avsluttet hendelse + visse utfall skal ikke ferdigstilles uten en opprettet rammebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            opprettSakOgFerdigstillKlagebehandling(
                tac = tac,
                forventetStatus = HttpStatusCode.InternalServerError,
                forventetJsonBody = { """{"kode": "server_feil","melding": "Noe gikk galt på serversiden"}""" },
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.UGUNST,
                    )
                },
            ) shouldBe null
        }
    }

    @Test
    fun `omgjør avsluttet hendelse (medhold) og oppretter revurdering-omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling, klagebehandling, rammebehandlingJson) = `opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling`(
                tac = tac,
            )!!

            rammebehandlingJson.toString().shouldEqualJsonIgnoringTimestamps(
                //language=json
                """
                {
                  "avbrutt": null,
                  "attesteringer": [],
                  "saksnummer": "${sak.saksnummer}",
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "utbetalingskontroll": null,
                  "iverksattTidspunkt": null,
                  "vedtaksperiode": null,
                  "fritekstTilVedtaksbrev": null,
                  "resultat": "OMGJØRING_IKKE_VALGT",
                  "type": "REVURDERING",
                  "automatiskOpprettetGrunn": null,
                  "beslutter": null,
                  "begrunnelseVilkårsvurdering": null,
                  "klagebehandlingId": "${klagebehandling.id}",
                  "tilbakekrevingId": null,
                  "utbetaling": null,
                  "ventestatus": null,
                  "omgjørVedtak": "${klagebehandling.formkrav.vedtakDetKlagesPå!!}",
                  "saksopplysninger": {
                    "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
                    "tiltaksdeltagelse": [
                      {
                        "typeKode": "GRUPPE_AMO",
                        "gjennomforingsprosent": null,
                        "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                        "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                        "antallDagerPerUke": 5,
                        "deltakelseStatus": "Deltar",
                        "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                        "deltagelseFraOgMed": "2023-01-01",
                        "deltagelseTilOgMed": "2023-03-31",
                        "kilde": "Komet",
                        "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                        "deltakelseProsent": 100
                      }
                    ],
                    "fødselsdato": "2001-01-01",
                    "ytelser": [],
                    "tiltakspengevedtakFraArena": [],
                    "periode": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    }
                  },
                  "rammevedtakId": null,
                  "sistEndret": "2025-01-01T01:02:51.456789",
                  "sakId": "${sak.id}",
                  "id": "${rammebehandling.id}",
                  "status": "UNDER_BEHANDLING"
                }
                """.trimIndent(),
            )

            klagebehandling.rammebehandlingId shouldBe rammebehandling.id
            klagebehandling.status shouldBe Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
            klagebehandling.resultat.shouldBeInstanceOf<Klagebehandlingsresultat.Opprettholdt>()
            klagebehandling.resultat.ferdigstiltTidspunkt.shouldNotBeNull()
        }
    }

    @Test
    fun `iverksetter klagebehandling med søknadsbehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling, klagebehandling) = opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling(
                tac = tac,
                behandlingstype = "SØKNADSBEHANDLING_INNVILGELSE",
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD,
                    )
                },
            )!!

            klagebehandling.status shouldBe Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
            klagebehandling.resultat.shouldBeInstanceOf<Klagebehandlingsresultat.Opprettholdt>()

            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = beslutter,
            )
            val (_, rammevedtak, iverksattRammebehandling, iverksattRammebehandlingJson) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                beslutter = beslutter,
            )!!

            iverksattRammebehandlingJson.toString().shouldEqualJsonIgnoringTimestamps(
                //language=json
                """
                {
                  "attesteringer": [
                    {
                      "begrunnelse": null,
                      "endretAv": "B12345",
                      "endretTidspunkt": "2025-01-01T01:03:05.456789",
                      "status": "GODKJENT"
                    }
                  ],
                  "saksnummer": "202501011001",
                  "utbetalingskontroll": null,
                  "iverksattTidspunkt": "2025-01-01T01:03:06.456789",
                  "vedtaksperiode": {
                    "fraOgMed": "2023-01-01",
                    "tilOgMed": "2023-03-31"
                  },
                  "type": "SØKNADSBEHANDLING",
                  "utbetaling": null,
                  "manueltBehandlesGrunner": [],
                  "saksopplysninger": {
                    "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
                    "tiltaksdeltagelse": [
                      {
                        "typeKode": "GRUPPE_AMO",
                        "gjennomforingsprosent": null,
                        "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                        "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                        "antallDagerPerUke": 5,
                        "deltakelseStatus": "Deltar",
                        "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                        "deltagelseFraOgMed": "2023-01-01",
                        "deltagelseTilOgMed": "2023-03-31",
                        "kilde": "Komet",
                        "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                        "deltakelseProsent": 100
                      }
                    ],
                    "fødselsdato": "2001-01-01",
                    "ytelser": [],
                    "tiltakspengevedtakFraArena": [],
                    "periode": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    }
                  },
                  "sakId": "${sak.id}",
                  "id": "${rammebehandling.id}",
                  "avbrutt": null,
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "barnetillegg": {
                    "begrunnelse": null,
                    "perioder": [
                      {
                        "antallBarn": 0,
                        "periode": {
                          "fraOgMed": "2023-01-01",
                          "tilOgMed": "2023-03-31"
                        }
                      }
                    ]
                  },
                  "fritekstTilVedtaksbrev": null,
                  "resultat": "INNVILGELSE",
                  "beslutter": "B12345",
                  "begrunnelseVilkårsvurdering": null,
                  "klagebehandlingId": "${klagebehandling.id}",
                  "tilbakekrevingId": null,
                  "kanInnvilges": true,
                  "ventestatus": null,
                  "innvilgelsesperioder": [
                    {
                      "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                      "periode": {
                        "fraOgMed": "2023-01-01",
                        "tilOgMed": "2023-03-31"
                      },
                      "antallDagerPerMeldeperiode": 10
                    }
                  ],
                  "rammevedtakId": "${rammevedtak.id}",
                  "sistEndret": "2025-01-01T01:03:06.456789",
                  "automatiskSaksbehandlet": false,
                  "søknad": {
                    "avbrutt": null,
                    "svar": {
                      "harSøktPåTiltak": { "svar": "JA" },
                      "kvp": { "svar": "NEI", "periode": null },
                      "gjenlevendepensjon": { "svar": "NEI", "periode": null },
                      "harSøktOmBarnetillegg": { "svar": "NEI" },
                      "sykepenger": { "svar": "NEI", "periode": null },
                      "etterlønn": { "svar": "NEI" },
                      "institusjon": { "svar": "NEI", "periode": null },
                      "trygdOgPensjon": { "svar": "NEI", "periode": null },
                      "intro": { "svar": "NEI", "periode": null },
                      "supplerendeStønadAlder": { "svar": "NEI", "periode": null },
                      "jobbsjansen": { "svar": "NEI", "periode": null },
                      "alderspensjon": { "svar": "NEI", "fraOgMed": null },
                      "supplerendeStønadFlyktning": { "svar": "NEI", "periode": null }
                    },
                    "tiltaksdeltakelseperiodeDetErSøktOm": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    },
                    "barnetillegg": [],
                    "opprettet": "2023-01-01T00:00:00",
                    "antallVedlegg": 0,
                    "tiltak": {
                      "fraOgMed": "2023-01-01",
                      "typeKode": "GRUPPEAMO",
                      "tilOgMed": "2023-03-31",
                      "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                      "id": "61328250-7d5d-4961-b70e-5cb727a34371"
                    },
                    "manueltSattTiltak": null,
                    "søknadstype": "DIGITAL",
                    "behandlingsarsak": null,
                    "kanInnvilges": true,
                    "tidsstempelHosOss": "2023-01-01T00:00:00",
                    "id": "${(iverksattRammebehandling as Søknadsbehandling).søknad.id}",
                    "journalpostId": "123456789"
                  },
                  "status": "VEDTATT"
                }
                """.trimIndent(),
            )

            // verifiserer at vi kan hente sak igjen uten at det blir kastet noen exception mellom behandling + klage
            hentSakForSaksnummer(tac, sak.saksnummer)!!
        }
    }

    @Test
    fun `iverksetter klagebehandling og revurdering innvilgelse`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling, klagebehandling, rammebehandlingJson) = opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling(
                tac = tac,
                behandlingstype = "REVURDERING_INNVILGELSE",
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD,
                    )
                },
            )!!

            klagebehandling.status shouldBe Klagebehandlingsstatus.OMGJØRING_ETTER_KLAGEINSTANS
            klagebehandling.resultat.shouldBeInstanceOf<Klagebehandlingsresultat.Opprettholdt>()

            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterRevurderingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = beslutter,
            )
            val (_, rammevedtak, iverksattRammebehandling, iverksattRammebehandlingJson) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                beslutter = beslutter,
            )!!

            iverksattRammebehandlingJson.toString().shouldEqualJsonIgnoringTimestamps(
                //language=json
                """
                {
                  "avbrutt": null,
                  "attesteringer": [
                    {
                      "begrunnelse": null,
                      "endretAv": "B12345",
                      "endretTidspunkt": "2025-01-01T01:03:04.456789",
                      "status": "GODKJENT"
                    }
                  ],
                  "saksnummer": "202501011001",
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "utbetalingskontroll": null,
                  "barnetillegg": {
                    "begrunnelse": null,
                    "perioder": [
                      {
                        "antallBarn": 0,
                        "periode": {
                          "fraOgMed": "2023-01-01",
                          "tilOgMed": "2023-03-31"
                        }
                      }
                    ]
                  },
                  "iverksattTidspunkt": "2025-01-01T01:03:05.456789",
                  "vedtaksperiode": {
                    "fraOgMed": "2023-01-01",
                    "tilOgMed": "2023-03-31"
                  },
                  "fritekstTilVedtaksbrev": null,
                  "resultat": "REVURDERING_INNVILGELSE",
                  "automatiskOpprettetGrunn": null,
                  "type": "REVURDERING",
                  "beslutter": "B12345",
                  "begrunnelseVilkårsvurdering": null,
                  "klagebehandlingId": "${klagebehandling.id}",
                  "tilbakekrevingId": null,
                  "utbetaling": null,
                  "ventestatus": null,
                  "innvilgelsesperioder": [
                    {
                      "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                      "periode": {
                        "fraOgMed": "2023-01-01",
                        "tilOgMed": "2023-03-31"
                      },
                      "antallDagerPerMeldeperiode": 10
                    }
                  ],
                  "saksopplysninger": {
                    "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
                    "tiltaksdeltagelse": [
                      {
                        "typeKode": "GRUPPE_AMO",
                        "gjennomforingsprosent": null,
                        "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                        "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                        "antallDagerPerUke": 5,
                        "deltakelseStatus": "Deltar",
                        "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                        "deltagelseFraOgMed": "2023-01-01",
                        "deltagelseTilOgMed": "2023-03-31",
                        "kilde": "Komet",
                        "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                        "deltakelseProsent": 100
                      }
                    ],
                    "fødselsdato": "2001-01-01",
                    "ytelser": [],
                    "tiltakspengevedtakFraArena": [],
                    "periode": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    }
                  },
                  "rammevedtakId": "${rammevedtak.id}",
                  "sistEndret": "2025-01-01T01:03:05.456789",
                  "sakId": "${sak.id}",
                  "id": "${iverksattRammebehandling.id}",
                  "status": "VEDTATT"
                }
                """.trimIndent(),
            )

            // verifiserer at vi kan hente sak igjen uten at det blir kastet noen exception mellom behandling + klage
            hentSakForSaksnummer(tac, sak.saksnummer)!!
        }
    }

    @Test
    fun `iverksetter klagebehandling og revurdering omgjøring`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, rammebehandling, klagebehandling) = opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling(
                tac = tac,
                hendelseGenerering = { _, klagebehandling ->
                    GenerererKlageinstanshendelse.avsluttetJson(
                        eventId = UUID.randomUUID().toString(),
                        kildeReferanse = klagebehandling.id.toString(),
                        kabalReferanse = UUID.randomUUID().toString(),
                        avsluttetTidspunkt = nå(tac.clock).toString(),
                        utfall = Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall.MEDHOLD,
                    )
                },
            )!!

            sak.vedtaksliste.alle.size shouldBe 1

            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterOmgjøringInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
                vedtaksperiode = (sak.vedtaksliste.alle.first() as Rammevedtak).periode,
            )
            sendRevurderingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = beslutter,
            )
            val (_, rammevedtak, iverksattRammebehandling, iverksattRammebehandlingJson) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                beslutter = beslutter,
            )!!

            iverksattRammebehandlingJson.toString().shouldEqualJsonIgnoringTimestamps(
                //language=json
                """
                {
                  "avbrutt": null,
                  "attesteringer": [
                    {
                      "begrunnelse": null,
                      "endretAv": "B12345",
                      "endretTidspunkt": "2025-01-01T01:03:04.456789",
                      "status": "GODKJENT"
                    }
                  ],
                  "saksnummer": "202501011001",
                  "saksbehandler": "saksbehandlerKlagebehandling",
                  "utbetalingskontroll": null,
                  "barnetillegg": {
                    "begrunnelse": null,
                    "perioder": [
                      {
                        "antallBarn": 0,
                        "periode": {
                          "fraOgMed": "2023-01-01",
                          "tilOgMed": "2023-03-31"
                        }
                      }
                    ]
                  },
                  "iverksattTidspunkt": "2025-01-01T01:03:05.456789",
                  "vedtaksperiode": {
                    "fraOgMed": "2023-01-01",
                    "tilOgMed": "2023-03-31"
                  },
                  "fritekstTilVedtaksbrev": null,
                  "resultat": "OMGJØRING",
                  "automatiskOpprettetGrunn": null,
                  "type": "REVURDERING",
                  "beslutter": "B12345",
                  "begrunnelseVilkårsvurdering": null,
                  "klagebehandlingId": "${klagebehandling.id}",
                  "tilbakekrevingId": null,
                  "utbetaling": null,
                  "ventestatus": null,
                  "innvilgelsesperioder": [
                    {
                      "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                      "periode": {
                        "fraOgMed": "2023-01-01",
                        "tilOgMed": "2023-03-31"
                      },
                      "antallDagerPerMeldeperiode": 10
                    }
                  ],
                  "omgjørVedtak": "${klagebehandling.formkrav.vedtakDetKlagesPå!!}",
                  "saksopplysninger": {
                    "oppslagstidspunkt": "2025-01-01T01:02:52.456789",
                    "tiltaksdeltagelse": [
                      {
                        "typeKode": "GRUPPE_AMO",
                        "gjennomforingsprosent": null,
                        "eksternDeltagelseId": "61328250-7d5d-4961-b70e-5cb727a34371",
                        "gjennomføringId": "358f6fe9-ebbe-4f7d-820f-2c0f04055c23",
                        "antallDagerPerUke": 5,
                        "deltakelseStatus": "Deltar",
                        "typeNavn": "Arbeidsmarkedsoppfølging gruppe",
                        "deltagelseFraOgMed": "2023-01-01",
                        "deltagelseTilOgMed": "2023-03-31",
                        "kilde": "Komet",
                        "internDeltakelseId": "tiltaksdeltaker_01KEYFWFRPZ9F0H446TF8HQFP0",
                        "deltakelseProsent": 100
                      }
                    ],
                    "fødselsdato": "2001-01-01",
                    "ytelser": [],
                    "tiltakspengevedtakFraArena": [],
                    "periode": {
                      "fraOgMed": "2023-01-01",
                      "tilOgMed": "2023-03-31"
                    }
                  },
                  "rammevedtakId": "${rammevedtak.id}",
                  "sistEndret": "2025-01-01T01:03:05.456789",
                  "sakId": "${sak.id}",
                  "id": "${rammebehandling.id}",
                  "status": "VEDTATT"
                }
                """.trimIndent(),
            )

            // verifiserer at vi kan hente sak igjen uten at det blir kastet noen exception mellom behandling + klage
            hentSakForSaksnummer(tac, sak.saksnummer)!!
        }
    }
}
