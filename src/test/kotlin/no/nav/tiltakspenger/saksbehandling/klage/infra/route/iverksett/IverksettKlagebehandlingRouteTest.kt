package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.distribusjon.DistribusjonId
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettRammebehandlingForKlage
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgVurderKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterOmgjøringInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterRevurderingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterSøknadsbehandlingInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgAvbrytKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgIverksettKlagebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOmgjørFraKaKlagebehandlingMedNyRammebehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendRevurderingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taBehandling
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class IverksettKlagebehandlingRouteTest {
    @Test
    fun `kan iverksette avvist klagebehandling`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2025)))
        // TODO jah: Fjern runIsolated når vi har fikset at databasetester kan kjøre parallelt (tiltaksdeltakelse og fnr må være garantert unik per test)
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, klagevedtak, json) = opprettSakOgIverksettKlagebehandling(
                tac = tac,
            )!!
            val klagebehandling = klagevedtak.behandling
            val expectedKlagevedtak = Klagevedtak(
                id = klagevedtak.id,
                opprettet = klagevedtak.behandling.iverksattTidspunkt!!,
                behandling = klagebehandling,
                journalpostId = JournalpostId("1"),
                journalføringstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(1),
                distribusjonId = DistribusjonId("1"),
                distribusjonstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(2),
                vedtaksdato = LocalDate.parse("2025-01-01"),
                sendtTilDatadeling = null,
            )
            klagevedtak.shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
            tac.sessionFactory.withSession {
                KlagevedtakPostgresRepo.hentForSakId(sak.id, it).single().shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
            }
            json.toString().shouldBeKlagebehandlingDTO(
                sakId = sak.id,
                saksnummer = sak.saksnummer,
                klagebehandlingId = klagebehandling.id,
                fnr = "12345678911",
                status = "VEDTATT",
                resultat = "AVVIST",
                iverksattTidspunkt = "TIMESTAMP",
                brevtekst = listOf("""{"tittel": "Avvisning av klage","tekst": "Din klage er dessverre avvist."}"""),
            )
            hentSakForSaksnummer(tac = tac, saksnummer = klagebehandling.saksnummer)!!.getJSONArray("alleKlagevedtak")
                .also {
                    it.length() shouldBe 1
                    val hentetKlagevedtakJson = it.getJSONObject(0)
                    hentetKlagevedtakJson.toString().shouldEqualJsonIgnoringTimestamps(
                        """
                    {
                      "klagebehandlingId": "${klagebehandling.id}",
                      "journalføringstidspunkt": "TIMESTAMP",
                      "opprettet": "TIMESTAMP",
                      "distribusjonstidspunkt": "TIMESTAMP",
                      "distribusjonId": "1",
                      "sakId": "${sak.id}",
                      "klagevedtakId": "${klagevedtak.id}",
                      "vedtaksdato": "2025-01-01",
                      "journalpostId": "1",
                      "resultat": "AVVIST"
                    }
                        """.trimIndent(),
                    )
                }
        }
    }

    @Test
    fun `kan ikke iverksette omgjøring fra klageroute`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _, klagebehandling) = iverksettSøknadsbehandlingOgVurderKlagebehandling(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = null,
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette avbrutt klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgAvbrytKlagebehandling(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan kun iverksette klagebehandling med status UNDER_BEHANDLING",
                        "kode": "må_ha_status_under_behandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette allerede iverksatt avvist klagebehandling`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagevedtak, _) = opprettSakOgIverksettKlagebehandling(
                tac = tac,
            )!!
            val klagebehandling = klagevedtak.behandling
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan kun iverksette klagebehandling med status UNDER_BEHANDLING",
                        "kode": "må_ha_status_under_behandling"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette - feil saksbehandler`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgOppdaterKlagebehandlingTilAvvisningBrevtekst(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                saksbehandler = ObjectMother.saksbehandler("annenSaksbehandler"),
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Du kan ikke utføre handlinger på en behandling som ikke er tildelt deg. Behandlingen er tildelt saksbehandlerKlagebehandling",
                        "kode": "behandling_eies_av_annen_saksbehandler"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan ikke iverksette avvist klagebehandling uten brevtekst`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, klagebehandling, _) = opprettSakOgKlagebehandlingTilAvvisning(
                tac = tac,
            )!!
            iverksettKlagebehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                forventetStatus = HttpStatusCode.BadRequest,
                forventetJsonBody = {
                    """
                     {
                        "melding": "Kan ikke iverksette klagebheandling uten brevtekst",
                        "kode": "mangler_brevtekst"
                     }
                    """.trimIndent()
                },
            ) shouldBe null
        }
    }

    @Test
    fun `kan iverksette klagebehandling til omgjøring`() {
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
            val (_, rammevedtak, _, json) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id,
                beslutter = beslutter,
            )!!
            rammevedtak.klagebehandling!!.also {
                it.status shouldBe Klagebehandlingsstatus.VEDTATT
                it.kanIverksetteVedtak shouldBe false
                it.erVedtatt shouldBe true
                it.erAvsluttet shouldBe true
                it.erUnderBehandling shouldBe false
                it.erÅpen shouldBe false
            }
            rammevedtak.klagebehandling.shouldBeEqualToIgnoringFields(
                klagebehandling,
                Klagebehandling::sistEndret,
                Klagebehandling::iverksattTidspunkt,
                Klagebehandling::status,
                Klagebehandling::kanIverksetteVedtak,
                Klagebehandling::erVedtatt,
                Klagebehandling::erAvsluttet,
                Klagebehandling::erUnderBehandling,
                Klagebehandling::erÅpen,
            )
            rammevedtak.klagebehandlingsresultat shouldBe klagebehandling.resultat
            json.getString("klagebehandlingId") shouldBe klagebehandling.id.toString()
        }
    }

    @Test
    fun `iverksetter klagebehandling (opprettholdelse) med søknadsbehandling`() {
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
    fun `iverksetter klagebehandling (opprettholdelse) og revurdering innvilgelse`() {
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
    fun `iverksetter klagebehandling (opprettholdelse) og revurdering omgjøring`() {
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
