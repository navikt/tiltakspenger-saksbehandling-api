package no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.repo

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.Tiltaksdeltakelse
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.TiltakstypeSomGirRett
import no.nav.tiltakspenger.libs.tiltaksdeltakelse.testStatusOpprettet
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.oppgave.OppgaveId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltakDeltakerstatus
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.Tiltakskilde
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.TiltakshistorikkId
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.Tiltakshistorikkinnslag
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.tiltakshistorikk.utenTittelOgArrangør
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.infra.jobb.TiltaksdeltakerEndring
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.medPeriode
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.tilLibsDeltakelse
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TiltakshistorikkPostgresRepoTest {

    /** Formatet låses her, siden verdien sammenlignes med neste nå-tilstand og en endring ellers ville gitt nye innslag for alle deltakere. */
    @Test
    fun `nå-tilstanden lagres med eksplisitte felter`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)

            val verdi = tac.lagreOgHentVerdi(sak, nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))

            verdi shouldEqualJson forventetVerdi(fraOgMed = "\"2026-01-01\"", tilOgMed = "\"2026-03-31\"")
        }
    }

    @Test
    fun `nå-tilstand uten datoer lagres med null-datoer`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)

            val verdi = tac.lagreOgHentVerdi(sak, nåtilstand(fraOgMed = null, tilOgMed = null))

            verdi shouldEqualJson forventetVerdi(fraOgMed = "null", tilOgMed = "null")
        }
    }

    @Test
    fun `henter verdien fra siste innslag for deltakeren`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)
            val repo = tac.tiltakContext.tiltakshistorikkRepo
            val deltakerId = sak.søknader.single().tiltak.shouldNotBeNull().tiltaksdeltakerId
            val opprinnelig = nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))
            val forlenget = nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))

            tac.sessionFactory.withTransactionContext { tx ->
                repo.hentSisteVerdi(deltakerId, tx) shouldBe null
                repo.lagre(tac.innslag(sak, opprinnelig), tx)
                repo.hentSisteVerdi(deltakerId, tx) shouldBe opprinnelig.utenTittelOgArrangør()
                repo.lagre(tac.innslag(sak, forlenget), tx)
                repo.hentSisteVerdi(deltakerId, tx) shouldBe forlenget.utenTittelOgArrangør()
            }
        }
    }

    @Test
    fun `henter ikke verdien fra innslag for andre deltakere`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)
            val annenSak = opprettSak(tac)
            val repo = tac.tiltakContext.tiltakshistorikkRepo
            val deltakerId = sak.søknader.single().tiltak.shouldNotBeNull().tiltaksdeltakerId
            val verdi = nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))

            tac.sessionFactory.withTransactionContext { tx ->
                repo.lagre(tac.innslag(sak, verdi), tx)
                repo.lagre(tac.innslag(annenSak, nåtilstand(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 30))), tx)
                repo.hentSisteVerdi(deltakerId, tx) shouldBe verdi.utenTittelOgArrangør()
            }
        }
    }

    @Test
    fun `innslagene hentes for saken sortert etter innhentet, og følger med saken`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)
            val repo = tac.tiltakContext.tiltakshistorikkRepo
            val førsteInnhentet = nå(tac.clock)
            val første = tac.innslag(sak, nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31))).copy(
                innhentet = førsteInnhentet,
                behandlingId = sak.rammebehandlinger.single().id,
            )
            val andre = tac.innslag(sak, nåtilstand(fraOgMed = null, tilOgMed = null)).copy(
                innhentet = førsteInnhentet.plusMinutes(1),
                endring = TiltaksdeltakerEndring.AvbruttDeltakelse,
                oppgaveId = OppgaveId("oppgave-id"),
            )

            tac.sessionFactory.withTransactionContext { tx ->
                repo.lagre(andre, tx)
                repo.lagre(første, tx)
            }

            val forventet = listOf(første, andre).map { it.copy(verdi = it.verdi.tilNåtilstandDbJson().tilDomene()) }
            repo.hentForSakId(sak.id) shouldBe forventet
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.tiltakshistorikk shouldBe forventet
        }
    }

    /** Et innslag i et format vi ikke kan lese, her formatet V276 konverterer fra. */
    @Test
    fun `innslag som ikke kan leses hoppes over, slik at saken fortsatt kan hentes`() {
        withTestApplicationContextAndPostgres { tac ->
            val sak = opprettSak(tac)
            val repo = tac.tiltakContext.tiltakshistorikkRepo
            val lesbart = tac.innslag(sak, nåtilstand(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31)))
            tac.sessionFactory.withTransactionContext { tx -> repo.lagre(lesbart, tx) }
            tac.lagreInnslagIUtdatertFormat(sak)

            val forventet = listOf(lesbart.copy(verdi = lesbart.verdi.tilNåtilstandDbJson().tilDomene()))
            repo.hentForSakId(sak.id) shouldBe forventet
            tac.sakContext.sakRepo.hentForSakId(sak.id)!!.tiltakshistorikk shouldBe forventet
        }
    }

    private fun TestApplicationContextMedPostgres.lagreInnslagIUtdatertFormat(sak: Sak) {
        val søknadstiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        sessionFactory.withSession { session ->
            session.run(
                sqlQuery(
                    """
                        INSERT INTO tiltakshistorikk (id, sak_id, tiltaksdeltaker_id, ekstern_deltaker_id, innhentet, verdi)
                        VALUES (:id, :sak_id, :tiltaksdeltaker_id, :ekstern_deltaker_id, :innhentet, :verdi::jsonb)
                    """.trimIndent(),
                    "id" to TiltakshistorikkId.random().toString(),
                    "sak_id" to sak.id.toString(),
                    "tiltaksdeltaker_id" to søknadstiltak.tiltaksdeltakerId.toString(),
                    "ekstern_deltaker_id" to søknadstiltak.id,
                    "innhentet" to nå(clock).minusDays(1),
                    "verdi" to """
                        {
                          "eksternDeltakelseId": "${søknadstiltak.id}",
                          "gjennomføringId": null,
                          "typeNavn": "Arbeidsmarkedsopplæring",
                          "typeKode": "ARBEIDSMARKEDSOPPLAERING",
                          "rettPåTiltakspenger": true,
                          "deltakelseFraOgMed": "2026-01-01",
                          "deltakelseTilOgMed": "2026-03-31",
                          "deltakelseStatus": "Deltar",
                          "deltakelseProsent": 50.0,
                          "antallDagerPerUke": 2.5,
                          "kilde": "Komet",
                          "deltidsprosentGjennomforing": 80.0
                        }
                    """.trimIndent(),
                ).asUpdate,
            )
        }
    }

    private suspend fun ApplicationTestBuilder.opprettSak(tac: TestApplicationContextMedPostgres): Sak =
        iverksettSøknadsbehandling(tac = tac).first

    private fun nåtilstand(fraOgMed: LocalDate?, tilOgMed: LocalDate?): Tiltaksdeltakelse.GirRett =
        ObjectMother.tiltaksdeltakelseTac(
            eksternTiltaksdeltakelseId = "ekstern-id",
            tiltakstype = TiltakstypeSomGirRett.ARBEIDSMARKEDSOPPLAERING,
            typeNavn = "Arbeidsmarkedsopplæring",
            eksternTiltaksgjennomføringsId = "gjennomføring-id",
            fom = LocalDate.of(2026, 1, 1),
            tom = LocalDate.of(2026, 3, 31),
            status = TiltakDeltakerstatus.Deltar,
            dagerPrUke = 2.5f,
            prosent = 50f,
            kilde = Tiltakskilde.Komet,
            deltidsprosentGjennomforing = 80.0,
        ).medPeriode(fraOgMed = fraOgMed, tilOgMed = tilOgMed).tilLibsDeltakelse()

    private fun TestApplicationContextMedPostgres.innslag(sak: Sak, verdi: Tiltaksdeltakelse.GirRett): Tiltakshistorikkinnslag {
        val søknadstiltak = sak.søknader.single().tiltak.shouldNotBeNull()
        return Tiltakshistorikkinnslag(
            id = TiltakshistorikkId.random(),
            sakId = sak.id,
            tiltaksdeltakerId = søknadstiltak.tiltaksdeltakerId,
            eksternDeltakerId = søknadstiltak.id,
            innhentet = nå(clock),
            verdi = verdi,
            endring = null,
            behandlingId = null,
            oppgaveId = null,
        )
    }

    /** Lagrer et innslag for en ekte sak og deltaker, og returnerer verdien slik den ble skrevet. */
    private fun TestApplicationContextMedPostgres.lagreOgHentVerdi(sak: Sak, verdi: Tiltaksdeltakelse.GirRett): String {
        val innslag = innslag(sak, verdi)
        sessionFactory.withTransactionContext { tx -> tiltakContext.tiltakshistorikkRepo.lagre(innslag, tx) }
        return sessionFactory.hentTiltakshistorikk(innslag.tiltaksdeltakerId).single { it.id == innslag.id.toString() }.verdi
    }

    private fun forventetVerdi(fraOgMed: String, tilOgMed: String) = """
        {
          "eksternDeltakelseId": "ekstern-id",
          "gjennomføringId": "gjennomføring-id",
          "tiltakstype": "ARBEIDSMARKEDSOPPLAERING",
          "tiltakstypenavn": "Arbeidsmarkedsopplæring",
          "tiltakskodeFraKilden": "ARBEIDSMARKEDSOPPLAERING",
          "fraOgMed": $fraOgMed,
          "tilOgMed": $tilOgMed,
          "kildestatus": {
            "kilde": "Komet",
            "kodeIKontrakten": "DELTAR",
            "årsak": null,
            "opprettet": "$testStatusOpprettet"
          },
          "deltakelsesprosent": 50.0,
          "dagerPerUke": 2.5,
          "deltidsprosentPåGjennomføring": 80.0
        }
    """.trimIndent()
}
