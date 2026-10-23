package no.nav.tiltakspenger.saksbehandling.statistikk

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.libs.persistering.infra.sqlQuery
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Forsøkshistorikk
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldekortBehandletAutomatiskStatus
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgBesluttertarMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.underkjennMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.SaksstatistikkDTO
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingAarsak
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingResultat
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingStatus
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkBehandlingType
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkFormat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * Saksstatistikk for manuelle meldekortbehandlinger, bestilt av Team Sak for å koble tilbakekrevinger til en behandling hos oss.
 * Radene leses tilbake med egen SQL, jf. [StatistikkLagringTest].
 */
class SaksstatistikkMeldekortbehandlingTest {

    @Test
    fun `manuell meldekortbehandling gir én rad per hendelse fram til iverksetting`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, rammevedtak) = iverksettSøknadsbehandling(tac = tac)
            val (_, _, meldekortbehandling) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!

            val rader = tac.sessionFactory.hentMeldekortstatistikk(sak.id)

            rader.map { it.hendelse } shouldContainExactly listOf(
                "opprettet_behandling",
                "sendt_til_beslutter",
                "oppdatert_saksbehandler_beslutter",
                "iverksatt_behandling",
            )
            rader.map { it.behandlingStatus } shouldContainExactly listOf(
                StatistikkBehandlingStatus.UNDER_BEHANDLING,
                StatistikkBehandlingStatus.UNDER_BESLUTNING,
                StatistikkBehandlingStatus.UNDER_BESLUTNING,
                StatistikkBehandlingStatus.FERDIG_BEHANDLET,
            )
            rader.forEach {
                it.behandlingId shouldBe meldekortbehandling.id.toString()
                it.relatertBehandlingId shouldBe rammevedtak.rammebehandling.id.toString()
                // Uten meldekort fra bruker er det saksbehandler som starter behandlingen.
                it.mottattTidspunkt shouldBe meldekortbehandling.opprettet
                it.registrertTidspunkt shouldBe meldekortbehandling.opprettet
                it.søknadsformat shouldBe StatistikkFormat.ANNET
                it.behandlingAarsak shouldBe StatistikkBehandlingAarsak.MELDEKORT
            }
            rader.last().let {
                it.behandlingResultat shouldBe StatistikkBehandlingResultat.GODKJENT
                it.vedtakTidspunkt shouldBe meldekortbehandling.iverksattTidspunkt
                it.ferdigBehandletTidspunkt shouldBe meldekortbehandling.iverksattTidspunkt
                it.saksbehandler shouldBe meldekortbehandling.saksbehandler
                it.ansvarligBeslutter shouldBe meldekortbehandling.beslutter
            }
        }
    }

    @Test
    fun `meldekort fra bruker som gikk til manuell behandling gir tidspunktet det gikk til manuell behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            val forsøk = mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, meldeperiode)

            val (_, meldekortbehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
            )!!
            avbrytMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = meldekortbehandling.id)!!

            val rader = tac.sessionFactory.hentMeldekortstatistikk(sak.id)

            rader.map { it.hendelse } shouldContainExactly listOf("opprettet_behandling", "avsluttet_behandling")
            rader.forEach {
                it.mottattTidspunkt shouldBe forsøk.forrigeForsøk
                it.søknadsformat shouldBe StatistikkFormat.DIGITAL
            }
            rader.last().let {
                it.behandlingStatus shouldBe StatistikkBehandlingStatus.AVSLUTTET
                it.behandlingResultat shouldBe StatistikkBehandlingResultat.AVBRUTT
            }
        }
    }

    @Test
    fun `korrigering etter at brukers meldekort er behandlet regnes fra når behandlingen ble opprettet`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, meldeperiode)
            opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = meldeperiode.kjedeId)!!

            val (_, korrigering) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
            )!!

            tac.sessionFactory.hentMeldekortstatistikk(sak.id)
                .filter { it.behandlingId == korrigering.id.toString() }
                .single().let {
                    it.mottattTidspunkt shouldBe korrigering.opprettet
                    it.søknadsformat shouldBe StatistikkFormat.ANNET
                    it.behandlingAarsak shouldBe StatistikkBehandlingAarsak.KORRIGERING
                }
        }
    }

    @Test
    fun `behandling med flere meldeperioder bruker meldekortet som gikk først til manuell behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (første, andre) = sak.meldeperiodeKjeder.take(2).map { it.hentSisteMeldeperiode() }
            // Bare den andre meldeperioden har et meldekort fra bruker, så oppslaget må se forbi den første.
            val forsøk = mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, andre)

            val (_, meldekortbehandling) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeIder = listOf(første.kjedeId, andre.kjedeId),
            )!!

            tac.sessionFactory.hentMeldekortstatistikk(sak.id).single().let {
                it.behandlingId shouldBe meldekortbehandling.id.toString()
                it.mottattTidspunkt shouldBe forsøk.forrigeForsøk
                it.søknadsformat shouldBe StatistikkFormat.DIGITAL
            }
        }
    }

    @Test
    fun `flere meldekort fra bruker på samme meldeperiode bruker det som gikk først til manuell behandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            val første = mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, meldeperiode)
            mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, meldeperiode, journalpostId = "andre")

            opprettMeldekortbehandlingForSakId(tac = tac, sakId = sak.id, kjedeId = meldeperiode.kjedeId)!!

            tac.sessionFactory.hentMeldekortstatistikk(sak.id).single().mottattTidspunkt shouldBe første.forrigeForsøk
        }
    }

    /**
     * Migreringen leser hele databasen, så den kjører isolert.
     * Radene prodkoden lager er fasiten: de slettes, migreringen gjenskaper dem, og feltene skal være like.
     * Unntaket er `endrettidspunkt` for send til beslutter og underkjenning, der prodkoden bruker `sistEndret`, som leses fra klokka rett etter tidspunktet migreringen har.
     */
    @Test
    @IsolatedDatabaseTest
    fun `migreringen gjenskaper radene prodkoden lager`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val (første, andre) = sak.meldeperiodeKjeder.take(2).map { it.hentSisteMeldeperiode() }
            mottaMeldekortSomGikkTilManuellBehandling(tac, sak.id, sak.kanSendeInnHelgForMeldekort, første)
            val (_, _, iverksatt) = opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = første.kjedeId)!!
            val (_, avbrutt) = opprettMeldekortbehandlingForSakId(tac = tac, sakId = sak.id, kjedeId = første.kjedeId)!!
            avbrytMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = avbrutt.id)!!
            val (_, underkjent) = opprettOgBesluttertarMeldekortbehanding(tac = tac, sakId = sak.id, kjedeId = andre.kjedeId)!!
            underkjennMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = underkjent.id)!!

            val fraProdkoden = tac.sessionFactory.hentMeldekortstatistikk(sak.id)
            tac.sessionFactory.slettMeldekortstatistikk()
            tac.sessionFactory.kjørMigrering() shouldBe 8

            val migrert = tac.sessionFactory.hentMeldekortstatistikk(sak.id)
            migrert.groupBy({ it.behandlingId }, { it.hendelse }) shouldBe mapOf(
                iverksatt.id.toString() to listOf("opprettet_behandling", "sendt_til_beslutter", "iverksatt_behandling"),
                avbrutt.id.toString() to listOf("opprettet_behandling", "avsluttet_behandling"),
                underkjent.id.toString() to listOf("opprettet_behandling", "sendt_til_beslutter", "underkjent_behandling"),
            )
            migrert.forEach { rad ->
                val fasit = fraProdkoden.single { it.behandlingId == rad.behandlingId && it.hendelse == rad.hendelse }
                rad.sammenlignbar() shouldBe fasit.sammenlignbar()
            }
            migrert.first { it.behandlingId == iverksatt.id.toString() }.søknadsformat shouldBe StatistikkFormat.DIGITAL
            migrert.first { it.behandlingId == avbrutt.id.toString() }.behandlingAarsak shouldBe StatistikkBehandlingAarsak.KORRIGERING

            tac.sessionFactory.kjørMigrering() shouldBe 0
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `migreringen lager ikke dobbeltrader for behandling som fikk rader etter deploy`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            opprettOgIverksettMeldekortbehandling(tac = tac, sakId = sak.id, kjedeId = sak.meldeperiodeKjeder.first().kjedeId)!!
            val fraProdkoden = tac.sessionFactory.hentMeldekortstatistikk(sak.id)
            // Behandlingen ble opprettet før deploy og sendt til beslutter etterpå.
            tac.sessionFactory.slettMeldekortstatistikk(hendelse = "opprettet_behandling")

            tac.sessionFactory.kjørMigrering() shouldBe 1

            tac.sessionFactory.hentMeldekortstatistikk(sak.id).map { it.hendelse }.sorted() shouldBe fraProdkoden.map { it.hendelse }.sorted()
        }
    }

    @Test
    fun `stans som fjerner all rett avbryter åpen meldekortbehandling`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = sak.meldeperiodeKjeder.first().kjedeId,
            )!!

            iverksettRevurderingStans(tac = tac, sakId = sak.id)

            tac.sessionFactory.hentMeldekortstatistikk(sak.id).let { rader ->
                rader.map { it.hendelse } shouldContainExactly listOf("opprettet_behandling", "avsluttet_behandling")
                rader.last().behandlingResultat shouldBe StatistikkBehandlingResultat.AVBRUTT
            }
        }
    }

    @Test
    fun `stans som endrer meldeperioden sender behandlingen tilbake fra beslutter`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak) = iverksettSøknadsbehandling(tac = tac)
            val meldeperiode = sak.meldeperiodeKjeder.first().hentSisteMeldeperiode()
            opprettOgBesluttertarMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                kjedeId = meldeperiode.kjedeId,
            )!!

            // Første hverdag med rett beholdes, så meldeperioden endres uten at behandlingen avbrytes eller får rett bare i helgen.
            val førsteHverdagMedRett = meldeperiode.girRett
                .filter { (dato, harRett) -> harRett && dato.dayOfWeek < DayOfWeek.SATURDAY }
                .keys.min()
            iverksettRevurderingStans(tac = tac, sakId = sak.id, stansFraOgMed = førsteHverdagMedRett.plusDays(1))

            tac.sessionFactory.hentMeldekortstatistikk(sak.id).last().let {
                it.hendelse shouldBe "automatisk_tilbakesendt_til_saksbehandler"
                it.behandlingStatus shouldBe StatistikkBehandlingStatus.UNDER_BEHANDLING
            }
        }
    }
}

private fun PostgresSessionFactory.hentMeldekortstatistikk(sakId: SakId): List<SaksstatistikkDTO> =
    hentSaksstatistikk(sakId).filter { it.behandlingType == StatistikkBehandlingType.MELDEKORT }

/** Det automatiske forsøket som sender meldekortet til manuell behandling, uten å kjøre jobben, som leser meldekort på tvers av saker. */
private suspend fun ApplicationTestBuilder.mottaMeldekortSomGikkTilManuellBehandling(
    tac: TestApplicationContext,
    sakId: SakId,
    kanSendeInnHelgForMeldekort: Boolean,
    meldeperiode: Meldeperiode,
    journalpostId: String = meldeperiode.id.toString(),
): Forsøkshistorikk {
    val (_, brukersMeldekort) = mottaMeldekortRequest(
        tac = tac,
        meldeperiodeId = meldeperiode.id,
        sakId = sakId,
        dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = kanSendeInnHelgForMeldekort),
        journalpostId = journalpostId,
    )
    val forsøk = brukersMeldekort!!.behandletAutomatiskForsøkshistorikk.inkrementer(clock = tac.clock)
    tac.meldekortContext.brukersMeldekortRepo.oppdaterAutomatiskBehandletStatus(
        meldekortId = brukersMeldekort.id,
        status = MeldekortBehandletAutomatiskStatus.HAR_FEILUTBETALING,
        behandlesAutomatisk = false,
        metadata = forsøk,
    )
    return forsøk
}

private fun PostgresSessionFactory.slettMeldekortstatistikk(hendelse: String? = null) = withSession { session ->
    session.run(
        sqlQuery(
            "delete from statistikk_sak where behandlingtype = 'MELDEKORT' and (cast(:hendelse as varchar) is null or hendelse = :hendelse)",
            "hendelse" to hendelse,
        ).asUpdate,
    )
}

private fun PostgresSessionFactory.kjørMigrering(): Int = withSession { session ->
    val sql = SaksstatistikkMeldekortbehandlingTest::class.java
        .getResource("/db/migration/V264__statistikk_sak_historiske_meldekortbehandlinger.sql")!!
        .readText()
    session.run(sqlQuery(sql).asUpdate)
}

/** Feltene migreringen skal gjenskape likt med prodkoden. */
private fun SaksstatistikkDTO.sammenlignbar(): SaksstatistikkDTO = copy(
    versjon = "",
    endretTidspunkt = if (hendelse in setOf("sendt_til_beslutter", "underkjent_behandling")) LocalDateTime.MIN else endretTidspunkt,
)
