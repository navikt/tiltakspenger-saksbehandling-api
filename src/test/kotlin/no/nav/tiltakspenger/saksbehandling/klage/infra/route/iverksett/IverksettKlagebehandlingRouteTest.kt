package no.nav.tiltakspenger.saksbehandling.klage.infra.route.iverksett

import io.kotest.matchers.equality.shouldBeEqualToIgnoringFields
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode
import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.behandling.infra.route.dto.RammebehandlingResultatTypeDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeRevurderingDTO
import no.nav.tiltakspenger.saksbehandling.behandling.shouldBeSøknadsbehandlingDTO
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.fixedClockAt
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsstatus
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagevedtak
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.GenerererKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.infra.repo.KlagevedtakPostgresRepo
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagebehandlingDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.route.shouldBeKlagevedtakJson
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettKlagebehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortvedtakOgKlagebehandlingTilAvvisning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOpprettBehandlingForKlage
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
                journalpostId = klagevedtak.journalpostId,
                journalføringstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(1),
                distribusjonId = klagevedtak.distribusjonId,
                distribusjonstidspunkt = klagevedtak.behandling.iverksattTidspunkt.plusSeconds(2),
                vedtaksdato = LocalDate.parse("2025-01-01"),
                sendtTilDatadeling = null,
            )
            klagevedtak.shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
            tac.sessionFactory.withSession {
                KlagevedtakPostgresRepo.hentForSakId(sak.id, it).single()
                    .shouldBeEqualToIgnoringLocalDateTime(expectedKlagevedtak)
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
                      "distribusjonId": "${klagevedtak.distribusjonId}",
                      "sakId": "${sak.id}",
                      "klagevedtakId": "${klagevedtak.id}",
                      "vedtaksdato": "2025-01-01",
                      "journalpostId": "${klagevedtak.journalpostId}",
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
            val (sak, rammebehandlingMedKlagebehandling, _) = iverksettSøknadsbehandlingOgOpprettBehandlingForKlage(
                tac = tac,
            )!!
            val klagebehandling = rammebehandlingMedKlagebehandling.klagebehandling!!
            val saksbehandler = ObjectMother.saksbehandler(klagebehandling.saksbehandler!!)
            oppdaterSøknadsbehandlingInnvilgelse(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id as RammebehandlingId,
                saksbehandler = saksbehandler,
            )
            val beslutter = ObjectMother.beslutter()
            taBehandling(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id as RammebehandlingId,
                saksbehandler = beslutter,
            )
            val (_, rammevedtak, _, json) = iverksettForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandlingMedKlagebehandling.id as RammebehandlingId,
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
                klagebehandling::åpenRammebehandlingId,
                Klagebehandling::resultat,
            )
            rammevedtak.klagebehandlingsresultat!!.shouldBeEqualToIgnoringFields(
                klagebehandling.resultat!!,
                // denne er fortsatt satt når den er åpen, men fjernes ved iverksettelse av omgjøring, så vi ignorerer den i sammenligningen
                Klagebehandlingsresultat::åpenRammebehandlingId,
            )
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
            `oppdaterSøknadsbehandlingInnvilgelse`(
                tac = tac,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
                saksbehandler = saksbehandler,
            )
            `sendSøknadsbehandlingTilBeslutningForBehandlingId`(
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

            iverksattRammebehandlingJson.toString().`shouldBeSøknadsbehandlingDTO`(
                rammevedtakId = rammevedtak.id,
                sakId = sak.id,
                behandlingId = iverksattRammebehandling.id,
                resultat = RammebehandlingResultatTypeDTO.INNVILGELSE,
                søknadId = (iverksattRammebehandling as Søknadsbehandling).søknad.id,
                klagebehandlingId = klagebehandling.id,
                saksbehandler = "saksbehandlerKlagebehandling",
                attesteringer = listOf(
                    """{"begrunnelse": null, "endretAv": "B12345", "endretTidspunkt": "2025-01-01T01:03:04.456789", "status": "GODKJENT"}""",
                ),
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

            iverksattRammebehandlingJson.toString().shouldBeRevurderingDTO(
                klagebehandlingId = klagebehandling.id,
                rammevedtakId = rammevedtak.id,
                sakId = sak.id,
                behandlingId = iverksattRammebehandling.id,
                resultat = RammebehandlingResultatTypeDTO.REVURDERING_INNVILGELSE,
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
            `oppdaterOmgjøringInnvilgelse`(
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

            iverksattRammebehandlingJson.toString().shouldBeRevurderingDTO(
                klagebehandlingId = klagebehandling.id,
                omgjørVedtak = klagebehandling.formkrav.vedtakDetKlagesPå!!,
                rammevedtakId = rammevedtak.id,
                sakId = sak.id,
                behandlingId = rammebehandling.id,
            )

            // verifiserer at vi kan hente sak igjen uten at det blir kastet noen exception mellom behandling + klage
            hentSakForSaksnummer(tac, sak.saksnummer)!!
        }
    }

    @Test
    fun `kan iverksette avvist klagebehandling der vedtaket er utbetalingsvedtak`() {
        val clock = TikkendeKlokke(fixedClockAt(1.januar(2026)))
        withTestApplicationContextAndPostgres(clock = clock, runIsolated = true) { tac ->
            val (sak, meldekortvedtak, klagevedtak, klagevedtakJson) = iverksettMeldekortvedtakOgKlagebehandlingTilAvvisning(
                tac = tac,
            )!!

            klagevedtakJson.toString().shouldBeKlagevedtakJson(
                klagebehandlingId = klagevedtak.behandling.id,
                sakId = sak.id,
                vedtakDetKlagesPå = meldekortvedtak.id,
                behandlingDetKlagesPå = klagevedtak.behandling.formkrav.behandlingDetKlagesPå!!,
            )
        }
    }
}
