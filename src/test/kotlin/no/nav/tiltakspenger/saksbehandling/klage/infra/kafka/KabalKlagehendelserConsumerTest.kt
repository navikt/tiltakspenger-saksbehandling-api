package no.nav.tiltakspenger.saksbehandling.klage.infra.kafka

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldBeEqualToIgnoringLocalDateTime
import no.nav.tiltakspenger.saksbehandling.infra.route.shouldEqualJsonIgnoringTimestamps
import no.nav.tiltakspenger.saksbehandling.journalføring.JournalpostId
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klageinstanshendelser
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.KlagehendelseId
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.BehandlingFeilregistrert.KlagehendelseFeilregistrertType
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.KlagebehandlingAvsluttet.KlagehendelseKlagebehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet.OmgjøringskravbehandlingAvsluttetUtfall
import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.NyKlagehendelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.hentSakForSaksnummer
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgOpprettholdKlagebehandling
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KabalKlagehendelserConsumerTest {

    @Test
    fun `avsluttet klage hendelse`() {
        KlagehendelseKlagebehandlingAvsluttetUtfall.entries.forEach {
            testKlagebehandlingAvsluttet(
                utfall = it,
            )
        }
    }

    @Test
    fun `avsluttet omgjøringskravbehandling hendelse`() {
        OmgjøringskravbehandlingAvsluttetUtfall.entries.forEach {
            testOmgjøringskravbehandlingAvsluttet(
                utfall = it,
            )
        }
    }

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
        withTestApplicationContextAndPostgres { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val eksternKlagehendelseId = UUID.randomUUID().toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.avsluttetJson(eventId = eksternKlagehendelseId, utfall = utfall, kildeReferanse = kildeReferanse),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetNyKlagebehandlingAvsluttetHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                ),
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelse(klagehendelseId)
            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetNyKlagebehandlingAvsluttetHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                    sakId = sak.id,
                    klagebehandlingId = klagebehandling.id,
                ),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser.shouldBeEqualToIgnoringLocalDateTime(
                    Klageinstanshendelser(
                        listOf(
                            Klageinstanshendelse.KlagebehandlingAvsluttet(
                                klagehendelseId = klagehendelseId,
                                opprettet = LocalDateTime.MIN,
                                sistEndret = LocalDateTime.MIN,
                                eksternKlagehendelseId = eksternKlagehendelseId,
                                avsluttetTidspunkt = LocalDateTime.MIN,
                                utfall = utfall,
                                journalpostreferanser = listOf(123, 456).map { JournalpostId(it.toString()) },
                                klagebehandlingId = klagebehandling.id,
                            ),
                        ),
                    ),
                )
            }
            hentSakForSaksnummer(tac = tac, saksnummer = klagebehandling.saksnummer)!!
                .getJSONArray("klagebehandlinger")
                .getJSONObject(0)
                .getJSONObject("resultat")
                .getJSONArray("klageinstanshendelser")
                .getJSONObject(0).toString().shouldEqualJsonIgnoringTimestamps(
                    """
                    {
                      "klagebehandlingId": "${klagebehandling.id}",
                      "klagehendelseId": "$klagehendelseId",
                      "utfall": "$utfall",
                      "opprettet": "TIMESTAMP",
                      "sistEndret": "TIMESTAMP",
                      "eksternKlagehendelseId": "$eksternKlagehendelseId",
                      "avsluttetTidspunkt": "TIMESTAMP",
                      "journalpostreferanser": ["123","456"],
                      "hendelsestype": "KLAGEBEHANDLING_AVSLUTTET"
                    }
                    """.trimIndent(),
                )
        }
    }

    private fun forventetNyKlagebehandlingAvsluttetHendelse(
        klagehendelseId: KlagehendelseId,
        eksternKlagehendelseId: String,
        utfall: KlagehendelseKlagebehandlingAvsluttetUtfall,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.MIN,
        sistEndret = LocalDateTime.MIN,
        eksternKlagehendelseId = eksternKlagehendelseId,
        key = "some-unused-uuid",
        value = """{"type":"KLAGEBEHANDLING_AVSLUTTET","kilde":"TILTAKSPENGER","eventId":"$eksternKlagehendelseId","detaljer":{"klagebehandlingAvsluttet":{"utfall":"$utfall","avsluttet":"2025-01-01T01:02:03.456789","journalpostReferanser":[123,456]}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )

    private fun testOmgjøringskravbehandlingAvsluttet(
        utfall: OmgjøringskravbehandlingAvsluttetUtfall,
    ) {
        withTestApplicationContextAndPostgres { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val eksternKlagehendelseId = UUID.randomUUID().toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.omgjøringskravbehandlingAvsluttet(
                    eventId = eksternKlagehendelseId,
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                ),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                ),
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelse(klagehendelseId)
            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    utfall = utfall,
                    kildeReferanse = kildeReferanse,
                    sakId = sak.id,
                    klagebehandlingId = klagebehandling.id,
                ),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser.shouldBeEqualToIgnoringLocalDateTime(
                    Klageinstanshendelser(
                        listOf(
                            Klageinstanshendelse.OmgjøringskravbehandlingAvsluttet(
                                klagehendelseId = klagehendelseId,
                                opprettet = LocalDateTime.MIN,
                                sistEndret = LocalDateTime.MIN,
                                eksternKlagehendelseId = eksternKlagehendelseId,
                                avsluttetTidspunkt = LocalDateTime.MIN,
                                utfall = utfall,
                                journalpostreferanser = listOf(123, 456).map { JournalpostId(it.toString()) },
                                klagebehandlingId = klagebehandling.id,
                            ),
                        ),
                    ),
                )
            }
        }
    }

    private fun forventetNyOmgjøringskravbehandlingAvsluttetHendelse(
        klagehendelseId: KlagehendelseId,
        eksternKlagehendelseId: String,
        utfall: OmgjøringskravbehandlingAvsluttetUtfall,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.MIN,
        sistEndret = LocalDateTime.MIN,
        eksternKlagehendelseId = eksternKlagehendelseId,
        key = "some-unused-uuid",
        value = """{"type":"OMGJOERINGSKRAVBEHANDLING_AVSLUTTET","kilde":"TILTAKSPENGER","eventId":"$eksternKlagehendelseId","detaljer":{"omgjoeringskravbehandlingAvsluttet":{"utfall":"$utfall","avsluttet":"2025-01-01T01:02:03.456789","journalpostReferanser":[123,456]}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )

    private fun testBehandlingFeilregistrert(
        type: KlagehendelseFeilregistrertType,
    ) {
        withTestApplicationContextAndPostgres { tac ->
            val klagehendelseRepo = tac.klagebehandlingContext.klagehendelseRepo
            val (sak, klagebehandling) = opprettSakOgOpprettholdKlagebehandling(tac = tac)!!
            val kildeReferanse = klagebehandling.id.toString()
            val eksternKlagehendelseId = UUID.randomUUID().toString()
            val klagehendelseId = KlageinstansKlagehendelseConsumer.consume(
                key = "some-unused-uuid",
                value = GenerererKlageinstanshendelse.behandlingFeilregistrert(
                    eventId = eksternKlagehendelseId,
                    type = type,
                    kildeReferanse = kildeReferanse,
                ),
                clock = tac.clock,
                lagreNyHendelse = klagehendelseRepo::lagreNyHendelse,
            )!!

            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetBehandlingFeilregistrertHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    type = type,
                    kildeReferanse = kildeReferanse,
                ),
            )
            tac.klagebehandlingContext.knyttKlageinstansHendelseTilKlagebehandlingJobb.knyttHendelse(klagehendelseId)
            klagehendelseRepo.hentNyHendelse(klagehendelseId)!!.shouldBeEqualToIgnoringLocalDateTime(
                forventetBehandlingFeilregistrertHendelse(
                    klagehendelseId = klagehendelseId,
                    eksternKlagehendelseId = eksternKlagehendelseId,
                    type = type,
                    kildeReferanse = kildeReferanse,
                    sakId = sak.id,
                    klagebehandlingId = klagebehandling.id,
                ),
            )
            tac.klagebehandlingContext.klagebehandlingRepo.hentForKlagebehandlingId(klagebehandling.id)!!.also {
                val resultat = it.resultat as Klagebehandlingsresultat.Opprettholdt
                resultat.klageinstanshendelser.shouldBeEqualToIgnoringLocalDateTime(
                    Klageinstanshendelser(
                        listOf(
                            Klageinstanshendelse.BehandlingFeilregistrert(
                                klagehendelseId = klagehendelseId,
                                klagebehandlingId = klagebehandling.id,
                                opprettet = LocalDateTime.MIN,
                                sistEndret = LocalDateTime.MIN,
                                eksternKlagehendelseId = eksternKlagehendelseId,
                                feilregistrertTidspunkt = LocalDateTime.MIN,
                                årsak = "Årsaken til at behandlingen endte opp som feilregistrert.",
                                navIdent = "Z123456",
                                type = type,
                            ),
                        ),
                    ),
                )
            }
        }
    }

    private fun forventetBehandlingFeilregistrertHendelse(
        klagehendelseId: KlagehendelseId,
        eksternKlagehendelseId: String,
        type: KlagehendelseFeilregistrertType,
        kildeReferanse: String = "klage_01KJ36CZA345ZM2QWMBVWH8NN8",
        sakId: SakId? = null,
        klagebehandlingId: KlagebehandlingId? = null,
    ): NyKlagehendelse = NyKlagehendelse(
        klagehendelseId = klagehendelseId,
        opprettet = LocalDateTime.MIN,
        sistEndret = LocalDateTime.MIN,
        eksternKlagehendelseId = eksternKlagehendelseId,
        key = "some-unused-uuid",
        value = """{"type":"BEHANDLING_FEILREGISTRERT","kilde":"TILTAKSPENGER","eventId":"$eksternKlagehendelseId","detaljer":{"behandlingFeilregistrert":{"type":"$type","reason":"Årsaken til at behandlingen endte opp som feilregistrert.","navIdent":"Z123456","feilregistrert":"2025-01-01T01:02:03.456789"}},"kabalReferanse":"c0aef33a-da01-4262-ab55-1bbdde157e8a","kildeReferanse":"$kildeReferanse"}""",
        sakId = sakId,
        klagebehandlingId = klagebehandlingId,
    )
}
