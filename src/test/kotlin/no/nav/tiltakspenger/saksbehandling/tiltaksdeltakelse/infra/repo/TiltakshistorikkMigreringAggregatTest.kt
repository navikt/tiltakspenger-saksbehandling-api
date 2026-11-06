package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.libs.tid.zoneIdOslo
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Kildestatus
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.hendelse.TiltaksdeltakerHendelseKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilLokalKilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.http.tilTiltakDeltakerstatus
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Gjenskaper rader jobben tidligere skrev til `tiltaksdeltaker_endring` med SQL, og kjører de faktiske migreringsfilene på nytt.
 * DDL-en i V274 er idempotent, så det er kopieringen av radene som testes her, og deretter slettingen i V275.
 * Konverteringen i V276 testes mot innslag skrevet rett i `tiltakshistorikk` i formatet V274 kopierte.
 */
class TiltakshistorikkMigreringAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `kopierer nå-tilstandene fra tiltakshistorikk, og sletter dem deretter fra tiltaksdeltaker_endring`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val tidspunkt = nå(tac.clock).withNano(0)
            val behandlingId = sak.rammebehandlinger.single().id.toString()
            val medBehandling = tac.lagreHistoriskRad(
                sak = sak,
                kilde = "Tiltakshistorikk",
                verdi = """{"versjon":1}""",
                endring = """[{"type":"FORLENGELSE"}]""",
                behandlingId = behandlingId,
                opprettet = tidspunkt.minusMinutes(30),
                behandletTidspunkt = tidspunkt.minusMinutes(20),
            )
            val medOppgave = tac.lagreHistoriskRad(
                sak = sak,
                kilde = "Tiltakshistorikk",
                verdi = """{"versjon":2}""",
                oppgaveId = "oppgave-1",
                opprettet = tidspunkt.minusMinutes(10),
            )
            val utenVerdi = tac.lagreHistoriskRad(sak = sak, kilde = "Tiltakshistorikk", verdi = null, opprettet = tidspunkt.minusMinutes(5))
            val komet = tac.lagreHistoriskRad(sak = sak, kilde = "Komet", verdi = """{"komet":true}""", opprettet = tidspunkt.minusMinutes(40))

            tac.kjørMigrering("V274__tiltakshistorikk.sql")

            val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
            val historikk = tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId)
            historikk shouldHaveSize 2
            historikk[0].also {
                it.id shouldBe medBehandling.replace("tiltaksdeltakerhendelse_", "tiltakshistorikk_")
                it.sakId shouldBe sak.id.toString()
                it.eksternDeltakerId shouldBe tiltak.id
                it.innhentet shouldBe tidspunkt.minusMinutes(20)
                it.verdi shouldEqualJson """{"versjon":1}"""
                it.endring.shouldNotBeNull() shouldEqualJson """[{"type":"FORLENGELSE"}]"""
                it.behandlingId shouldBe behandlingId
                it.oppgaveId.shouldBeNull()
            }
            historikk[1].also {
                it.id shouldBe medOppgave.replace("tiltaksdeltakerhendelse_", "tiltakshistorikk_")
                it.innhentet shouldBe tidspunkt.minusMinutes(10)
                it.verdi shouldEqualJson """{"versjon":2}"""
                it.endring.shouldBeNull()
                it.behandlingId.shouldBeNull()
                it.oppgaveId shouldBe "oppgave-1"
            }
            tac.hentKilderITiltaksdeltakerEndring(tiltak.id) shouldBe listOf(
                komet to "Komet",
                medBehandling to "Tiltakshistorikk",
                medOppgave to "Tiltakshistorikk",
                utenVerdi to "Tiltakshistorikk",
            )

            tac.kjørMigrering("V275__slett_tiltakshistorikk_fra_tiltaksdeltaker_endring.sql")

            tac.hentKilderITiltaksdeltakerEndring(tiltak.id) shouldBe listOf(komet to "Komet")
            tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId) shouldBe historikk
        }
    }

    /**
     * Hver kombinasjon av kilde og gammel deltakerstatus skal enten tolkes tilbake til samme status, eller leses som en ukjent kildestatus når kilden ikke har noen slik status.
     * Ett innslag pinnes i sin helhet, og innslag som allerede har dagens format skal ikke røres.
     */
    @Test
    @IsolatedDatabaseTest
    fun `konverterer innslag i utdatert format til dagens format, og sier hvilke verdier som kan være unøyaktige`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
            val repo = tac.tiltakContext.tiltakshistorikkRepo
            val innhentet = nå(tac.clock).withNano(0).minusDays(1)
            val gamle = Tiltakskilde.entries.flatMap { kilde ->
                TiltakDeltakerstatus.entries.map { status ->
                    Triple(tac.lagreInnslagIUtdatertFormat(sak, kilde, status, innhentet), kilde, status)
                }
            }
            val pinnet = gamle.single { (_, kilde, status) -> kilde == Tiltakskilde.Komet && status == TiltakDeltakerstatus.Deltar }.first
            tac.sessionFactory.withTransactionContext { tx ->
                repo.lagre(tac.innslagIDagensFormat(sak, innhentet), tx)
            }
            val gamleIder = gamle.map { it.first }.toSet()
            val iDagensFormat = tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId).filterNot { it.id in gamleIder }

            tac.kjørMigrering("V276__tiltakshistorikk_konverter_utdatert_format.sql")

            iDagensFormat.shouldNotBeEmpty()
            tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId).filterNot { it.id in gamleIder } shouldBe iDagensFormat
            val innslag = repo.hentForSakId(sak.id).associateBy { it.id.toString() }
            innslag.keys shouldContainAll gamleIder
            gamle.forEach { (id, kilde, status) ->
                val verdi = innslag.getValue(id).verdi
                verdi.kildestatus.kilde.tilLokalKilde() shouldBe kilde
                val tolket = verdi.kildestatus.tilTiltakDeltakerstatus(verdi.fraOgMed, tac.clock)
                if (verdi.kildestatus is Kildestatus.Ukjent) {
                    verdi.kildestatus.kodeIKontrakten shouldBe status.name
                    tolket.shouldBeNull()
                } else {
                    tolket shouldBe status
                }
            }
            val opprettet = innhentet.atZone(ZoneId.systemDefault()).withZoneSameInstant(zoneIdOslo).toLocalDateTime()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            tac.sessionFactory.hentTiltakshistorikk(tiltak.tiltaksdeltakerId).single { it.id == pinnet }.verdi shouldEqualJson """
                {
                  "eksternDeltakelseId": "${tiltak.id}",
                  "gjennomføringId": "gjennomføring-id",
                  "tiltakstype": "GRUPPE_AMO",
                  "tiltakstypenavn": "Gruppe AMO",
                  "tiltakskodeFraKilden": "GRUPPE_AMO",
                  "fraOgMed": "2026-01-01",
                  "tilOgMed": "2026-03-31",
                  "kildestatus": {
                    "kilde": "Komet",
                    "kodeIKontrakten": "DELTAR",
                    "årsak": null,
                    "opprettet": "$opprettet"
                  },
                  "deltakelsesprosent": 50.0,
                  "dagerPerUke": 2.5,
                  "deltidsprosentPåGjennomføring": 80.0,
                  "migrertFraTidligereFormat": {
                    "migrering": "V276",
                    "beskrivelse": "Konvertert fra formatet nå-tilstanden ble lagret i før 2026-10-06. Feltene i kanVæreUnøyaktige ble ikke lagret i det gamle formatet, og er utledet. De riktige verdiene finnes i kildesystemet.",
                    "kanVæreUnøyaktige": [
                      "tiltakskodeFraKilden",
                      "kildestatus.kodeIKontrakten",
                      "kildestatus.årsak",
                      "kildestatus.opprettet"
                    ],
                    "opprinneligVerdi": ${utdatertVerdi(tiltak.id, Tiltakskilde.Komet, TiltakDeltakerstatus.Deltar)}
                  }
                }
            """.trimIndent()
        }
    }

    private fun TestApplicationContextMedPostgres.innslagIDagensFormat(sak: Sak, innhentet: LocalDateTime): Tiltakshistorikkinnslag {
        val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        return Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.random(),
            sakId = sak.id,
            tiltaksdeltakerId = tiltak.tiltaksdeltakerId,
            eksternDeltakerId = tiltak.id,
            innhentet = innhentet,
            verdi = deserialize<TiltaksdeltakelseNåtilstandDbJson>(
                """
                    {
                      "eksternDeltakelseId": "${tiltak.id}",
                      "gjennomføringId": null,
                      "tiltakstype": "GRUPPE_AMO",
                      "tiltakstypenavn": "Gruppe AMO",
                      "tiltakskodeFraKilden": "GRUPPEAMO",
                      "fraOgMed": "2026-01-01",
                      "tilOgMed": null,
                      "kildestatus": { "kilde": "Arena", "kodeIKontrakten": "GJENNOMFORES", "årsak": null, "opprettet": null },
                      "deltakelsesprosent": null,
                      "dagerPerUke": null,
                      "deltidsprosentPåGjennomføring": null
                    }
                """.trimIndent(),
            ).tilDomene(),
            endring = null,
            behandlingId = null,
            oppgaveId = null,
        )
    }

    /** Lagrer et innslag med nå-tilstanden i formatet som ble lagret i `tiltaksdeltaker_endring` før 2026-10-06, og returnerer id-en. */
    private fun TestApplicationContextMedPostgres.lagreInnslagIUtdatertFormat(
        sak: Sak,
        kilde: Tiltakskilde,
        status: TiltakDeltakerstatus,
        innhentet: LocalDateTime,
    ): String {
        val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        val id = TiltakshistorikkId.random().toString()
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        INSERT INTO tiltakshistorikk (id, sak_id, tiltaksdeltaker_id, ekstern_deltaker_id, innhentet, verdi)
                        VALUES (:id, :sak_id, :tiltaksdeltaker_id, :ekstern_deltaker_id, :innhentet, :verdi::jsonb)
                    """.trimIndent(),
                    "id" to id,
                    "sak_id" to sak.id.toString(),
                    "tiltaksdeltaker_id" to tiltak.tiltaksdeltakerId.toString(),
                    "ekstern_deltaker_id" to tiltak.id,
                    "innhentet" to innhentet,
                    "verdi" to utdatertVerdi(tiltak.id, kilde, status),
                ).asUpdate,
            )
        }
        return id
    }

    private fun utdatertVerdi(eksternDeltakelseId: String, kilde: Tiltakskilde, status: TiltakDeltakerstatus) = """
        {
          "eksternDeltakelseId": "$eksternDeltakelseId",
          "gjennomføringId": "gjennomføring-id",
          "typeNavn": "Gruppe AMO",
          "typeKode": "GRUPPE_AMO",
          "rettPåTiltakspenger": true,
          "deltakelseFraOgMed": "2026-01-01",
          "deltakelseTilOgMed": "2026-03-31",
          "deltakelseStatus": "${status.name}",
          "deltakelseProsent": 50.0,
          "antallDagerPerUke": 2.5,
          "kilde": "${kilde.name}",
          "deltidsprosentGjennomforing": 80.0
        }
    """.trimIndent()

    private fun TestApplicationContextMedPostgres.kjørMigrering(filnavn: String) {
        val migrering = javaClass.getResource("/db/migration/$filnavn").shouldNotBeNull().readText()
        sessionFactory.withSession { session -> session.run(sqlQuery(migrering).asExecute) }
    }

    /**
     * Id og kilde for radene i `tiltaksdeltaker_endring`, sortert etter når de ble opprettet.
     * Kilden leses som tekst, siden `Tiltakshistorikk` ikke lenger er en [TiltaksdeltakerHendelseKilde].
     */
    private fun TestApplicationContextMedPostgres.hentKilderITiltaksdeltakerEndring(eksternDeltakerId: String): List<Pair<String, String>> =
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    "SELECT id, kilde FROM tiltaksdeltaker_endring WHERE ekstern_deltaker_id = :ekstern_deltaker_id ORDER BY opprettet",
                    "ekstern_deltaker_id" to eksternDeltakerId,
                ).map { row -> row.string("id") to row.string("kilde") }.asList,
            )
        }

    /** Skriver en rad slik den gamle jobben og consumerne gjorde, og returnerer id-en. */
    private fun TestApplicationContextMedPostgres.lagreHistoriskRad(
        sak: Sak,
        kilde: String,
        verdi: String?,
        opprettet: LocalDateTime,
        endring: String? = null,
        behandlingId: String? = null,
        oppgaveId: String? = null,
        behandletTidspunkt: LocalDateTime? = null,
    ): String {
        val tiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        val id = TiltaksdeltakerHendelseId.random().toString()
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        INSERT INTO tiltaksdeltaker_endring (
                            id, ekstern_deltaker_id, tiltaksdeltaker_id, sak_id, kilde, verdi, endring,
                            behandling_id, oppgave_id, opprettet, behandlet_tidspunkt
                        ) VALUES (
                            :id, :ekstern_deltaker_id, :tiltaksdeltaker_id, :sak_id, :kilde, CAST(:verdi AS jsonb), CAST(:endring AS jsonb),
                            :behandling_id, :oppgave_id, :opprettet, :behandlet_tidspunkt
                        )
                    """.trimIndent(),
                    "id" to id,
                    "ekstern_deltaker_id" to tiltak.id,
                    "tiltaksdeltaker_id" to tiltak.tiltaksdeltakerId.toString(),
                    "sak_id" to sak.id.toString(),
                    "kilde" to kilde,
                    "verdi" to verdi,
                    "endring" to endring,
                    "behandling_id" to behandlingId,
                    "oppgave_id" to oppgaveId,
                    "opprettet" to opprettet,
                    "behandlet_tidspunkt" to behandletTidspunkt,
                ).asUpdate,
            )
        }
        return id
    }
}
