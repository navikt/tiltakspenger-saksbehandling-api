package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.`OmgjøringskravbehandlingAvsluttet`.`OmgjøringskravbehandlingAvsluttetUtfall`
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KabalKlagehendelserConsumerTest {

    @Disabled // FIX CLOCK :REEE:
    @Test
    fun `avsluttet klage hendelse`() {
        KlagehendelseKlagebehandlingAvsluttetUtfall.entries.forEach {
            testKlagebehandlingAvsluttet(
                utfall = it,
            )
        }
    }

    @Disabled
    @Test
    fun `avsluttet omgjøringskravbehandling hendelse`() {
        OmgjøringskravbehandlingAvsluttetUtfall.entries.forEach {
            testOmgjøringskravbehandlingAvsluttet(
                utfall = it,
            )
        }
    }

    @Disabled
    @Test
    fun `feilregistrert behandling hendelse`() {
        KlagehendelseFeilregistrertType.entries.forEach {
            testBehandlingFeilregistrert(
                type = it,
            )
        }
    }

    private fun testKlagebehandlingAvsluttet(
        utfall: KlagehendelseKlagebehandlingAvsluttetUtfall,
    ) {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.avsluttetJson(utfall = utfall, kildeReferanse = kildeReferanse),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetNyKlagebehandlingAvsluttetHendelse(
                klagehendelseId = klagehendelseId,
                utfall = utfall,
                kildeReferanse = kildeReferanse,
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetNyKlagebehandlingAvsluttetHendelse(
                klagehendelseId = klagehendelseId,
                utfall = utfall,
                kildeReferanse = kildeReferanse,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser shouldBe Klageinstanshendelser(
                    listOf(
                        Klageinstanshendelse.KlagebehandlingAvsluttet(
                            klagehendelseId = klagehendelseId,
                            opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
                            sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
                            eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
                            avsluttetTidspunkt = LocalDateTime.parse("2025-01-01T01:02:03.456789"),
                            utfall = utfall,
                            journalpostreferanser = listOf(123, 456).map { JournalpostId(it.toString()) },
                            klagebehandlingId = klagebehandling.id,
                        ),
                    ),
                )
            }
            hentSakForSaksnummer(tac = tac, saksnummer = klagebehandling.saksnummer)!!
                .getJSONArray("klageBehandlinger")
                .getJSONObject(0)
                .getJSONArray("klageinstanshendelser")
                .getJSONObject(0).toString().shouldEqualJsonIgnoringTimestamps(
                    """
                    {
                      "klagebehandlingId": "${klagebehandling.id}",
                      "klagehendelseId": "$klagehendelseId",
                      "utfall": "$utfall",
                      "opprettet": "2025-05-01T01:02:45.456789",
                      "sistEndret": "2025-05-01T01:02:46.456789",
                      "eksternKlagehendelseId": "0f4ea0c2-8b44-4266-a1c3-801006b06280",
                      "avsluttetTidspunkt": "2025-01-01T01:02:03.456789",
                      "journalpostreferanser": ["123","456"],
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                )
        }
    }

    private fun forventetNyKlagebehandlingAvsluttetHendelse(
        klagehendelseId: KlagehendelseId,
        utfall: KlagehendelseKlagebehandlingAvsluttetUtfall,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
        sistEndret: LocalDateTime = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
        sistEndret = sistEndret,
        eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        key = "some-unused-uuid",
        value = """{"type":"KLAGEBEHANDLING_AVSLUTTET","kilde":"TIL_TIP","eventId":"0f4ea0c2-8b44-4266-a1c3-801006b06280","detaljer":{"klagebehandlingAvsluttet":{"utfall":"$utfall","avsluttet":"2025-01-01T01:02:03.456789","journalpostReferanser":[123,456]}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )

    private fun testOmgjøringskravbehandlingAvsluttet(
        utfall: OmgjøringskravbehandlingAvsluttetUtfall,
    ) {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.omgjøringskravbehandlingAvsluttet(
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                ),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
                klagehendelseId = klagehendelseId,
                utfall = utfall,
                kildeReferanse = kildeReferanse,
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
                klagehendelseId = klagehendelseId,
                utfall = utfall,
                kildeReferanse = kildeReferanse,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser shouldBe Klageinstanshendelser(
                    listOf(
                        Klageinstanshendelse.`OmgjøringskravbehandlingAvsluttet`(
                            klagehendelseId = klagehendelseId,
                            opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
                            sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
                            eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
                            avsluttetTidspunkt = LocalDateTime.parse("2025-01-01T01:02:03.456789"),
                            utfall = utfall,
                            journalpostreferanser = listOf(123, 456).map { JournalpostId(it.toString()) },
                            klagebehandlingId = klagebehandling.id,
                        ),
                    ),
                )
            }
        }
    }

    private fun forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
        klagehendelseId: KlagehendelseId,
        utfall: `OmgjøringskravbehandlingAvsluttetUtfall`,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
        sistEndret: LocalDateTime = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
        sistEndret = sistEndret,
        eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        key = "some-unused-uuid",
        value = """{"type":"OMGJOERINGSKRAVBEHANDLING_AVSLUTTET","kilde":"TIL_TIP","eventId":"0f4ea0c2-8b44-4266-a1c3-801006b06280","detaljer":{"omgjoeringskravbehandlingAvsluttet":{"utfall":"$utfall","avsluttet":"2025-01-01T01:02:03.456789","journalpostReferanser":[123,456]}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )

    private fun testBehandlingFeilregistrert(
        type: KlagehendelseFeilregistrertType,
    ) {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.behandlingFeilregistrert(
                    type = type,
                    kildeReferanse = kildeReferanse,
                ),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetBehandlingFeilregistrertHendelse(
                klagehendelseId = klagehendelseId,
                type = type,
                kildeReferanse = kildeReferanse,
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelser()
            klagehendelseRepo.hentNyHendelse(klagehendelseId) shouldBe forventetBehandlingFeilregistrertHendelse(
                klagehendelseId = klagehendelseId,
                type = type,
                kildeReferanse = kildeReferanse,
                sakId = sak.id,
                klagebehandlingId = klagebehandling.id,
                sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser shouldBe Klageinstanshendelser(
                    listOf(
                        Klageinstanshendelse.BehandlingFeilregistrert(
                            klagehendelseId = klagehendelseId,
                            klagebehandlingId = klagebehandling.id,
                            opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
                            sistEndret = LocalDateTime.parse("2025-05-01T01:02:46.456789"),
                            eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
                            feilregistrertTidspunkt = LocalDateTime.parse("2025-01-01T01:02:03.456789"),
                            årsak = "Årsaken til at behandlingen endte opp som feilregistrert.",
                            navIdent = "Z123456",
                            type = type,
                        ),
                    ),
                )
            }
        }
    }

    private fun forventetBehandlingFeilregistrertHendelse(
        klagehendelseId: KlagehendelseId,
        type: KlagehendelseFeilregistrertType,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
        sistEndret: LocalDateTime = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.parse("2025-05-01T01:02:45.456789"),
        sistEndret = sistEndret,
        eksternKlagehendelseId = "0f4ea0c2-8b44-4266-a1c3-801006b06280",
        key = "some-unused-uuid",
        value = """{"type":"BEHANDLING_FEILREGISTRERT","kilde":"TIL_TIP","eventId":"0f4ea0c2-8b44-4266-a1c3-801006b06280","detaljer":{"behandlingFeilregistrertDetaljer":{"type":"$type","reason":"Årsaken til at behandlingen endte opp som feilregistrert.","navIdent":"Z123456","feilregistrert":"2025-01-01T01:02:03.456789"}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )
}
