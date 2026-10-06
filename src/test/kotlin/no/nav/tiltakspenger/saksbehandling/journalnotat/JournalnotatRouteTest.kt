package no.nav.tiltakspenger.saksbehandling.journalnotat

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.felles.createOrThrow
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortvedtak.Meldekortvedtak
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.routes.JobberEtterIverksettelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendSøknadsbehandlingTilBeslutningForBehandlingId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taRammebehandlinger
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import org.json.JSONObject
import org.junit.jupiter.api.Test

/**
 * Saksbehandler velger om et notat om vedtaket skal journalføres i Joark.
 * Valget lagres på behandlingen, krever begrunnelse ved sending til beslutning, og notatet journalføres av en egen jobb etter iverksettelse.
 */
class JournalnotatRouteTest {

    private val saksbehandler = ObjectMother.saksbehandler()
    private val beslutter = ObjectMother.beslutter()
    private val begrunnelse = "Begrunnelse for vilkårsvurderingen."

    @Test
    fun `rammebehandling som skal journalføre notat uten begrunnelse kan ikke sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                saksbehandler = saksbehandler,
                begrunnelseVilkårsvurdering = null,
                skalJournalføreNotat = true,
            )

            sendSøknadsbehandlingTilBeslutningForBehandlingId(
                tac = tac,
                sakId = sak.id,
                behandlingId = behandling.id,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Begrunnelsen må være utfylt når notatet skal journalføres","kode":"må_ha_begrunnelse_for_å_journalføre_notat"}""",
                    ),
                ),
            )
        }
    }

    /**
     * Isolert fordi notatjobben tar hele køen.
     * I den delte databasen kan en annen tests jobb journalføre det samme vedtaket samtidig og overskrive journalføringsnotatet mellom stegene i testen.
     */
    @Test
    @IsolatedDatabaseTest
    fun `valget lagres og vises på rammebehandlingen, og notatet journalføres etter iverksettelse`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                saksbehandler = saksbehandler,
                begrunnelseVilkårsvurdering = Begrunnelse.createOrThrow(begrunnelse),
                skalJournalføreNotat = true,
            )
            behandling.skalJournalføreNotat shouldBe true

            val json = sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id, saksbehandler)!!
            JSONObject(json).getBoolean("skalJournalføreNotat") shouldBe true

            val vedtak = iverksettRammebehandling(tac, sak.id, behandling, JobberEtterIverksettelse(journalførNotat = false))
            vedtak.journalføringsnotat.shouldBeNull()

            tac.journalførJournalnotatService.journalførNotater()

            val journalført = tac.hentRammevedtak(sak.id, vedtak)
            journalført.journalføringsnotat.shouldNotBeNull()

            tac.journalførJournalnotatService.journalførNotater()
            tac.hentRammevedtak(sak.id, vedtak).journalføringsnotat shouldBe journalført.journalføringsnotat
        }
    }

    @Test
    fun `rammebehandling der saksbehandler har valgt bort notatet journalføres ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, _, behandling) = opprettSøknadsbehandlingUnderBehandlingMedInnvilgelse(
                tac = tac,
                saksbehandler = saksbehandler,
                skalJournalføreNotat = false,
            )
            sendSøknadsbehandlingTilBeslutningForBehandlingId(tac, sak.id, behandling.id, saksbehandler)

            val vedtak = iverksettRammebehandling(tac, sak.id, behandling, JobberEtterIverksettelse())

            vedtak.skalJournalføreNotat shouldBe false
            vedtak.journalføringsnotat.shouldBeNull()
        }
    }

    @Test
    fun `meldekortbehandling som skal journalføre notat uten begrunnelse kan ikke sendes til beslutning`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sak, meldekortId) = oppdatertMeldekortbehandling(tac, begrunnelse = null, skalJournalføreNotat = true)

            sendMeldekortbehandlingTilBeslutning(
                tac = tac,
                sakId = sak,
                meldekortId = meldekortId,
                saksbehandler = saksbehandler,
                forventet = ForventetRespons(
                    status = 400,
                    contentType = "application/json; charset=UTF-8",
                    body = ForventetBody.Json(
                        """{"melding":"Begrunnelsen må være utfylt når notatet skal journalføres","kode":"må_ha_begrunnelse_for_å_journalføre_notat"}""",
                    ),
                ),
            )
        }
    }

    /**
     * Isolert fordi notatjobben tar hele køen.
     * I den delte databasen kan en annen tests jobb journalføre det samme vedtaket samtidig og overskrive journalføringsnotatet mellom stegene i testen.
     */
    @Test
    @IsolatedDatabaseTest
    fun `valget lagres og vises på meldekortbehandlingen, og notatet journalføres etter iverksettelse`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sakId, meldekortId) = oppdatertMeldekortbehandling(tac, begrunnelse = begrunnelse, skalJournalføreNotat = true)

            val (_, sendt, json) = sendMeldekortbehandlingTilBeslutning(tac, sakId, meldekortId, saksbehandler)!!
            sendt.skalJournalføreNotat shouldBe true
            // Responsen er hele saken, der meldekortbehandlingen er den eneste med valget satt.
            json.toString() shouldContain "\"skalJournalføreNotat\":true"

            val vedtak = iverksettMeldekort(tac, sakId, meldekortId, JobberEtterIverksettelse(journalførNotat = false))
            vedtak.journalføringsnotat.shouldBeNull()

            tac.journalførJournalnotatService.journalførNotater()

            val journalført = tac.hentMeldekortvedtak(sakId, vedtak)
            journalført.journalføringsnotat.shouldNotBeNull()

            tac.journalførJournalnotatService.journalførNotater()
            tac.hentMeldekortvedtak(sakId, vedtak).journalføringsnotat shouldBe journalført.journalføringsnotat
        }
    }

    @Test
    fun `meldekortbehandling der saksbehandler har valgt bort notatet journalføres ikke`() {
        withTestApplicationContextAndPostgres { tac ->
            val (sakId, meldekortId) = oppdatertMeldekortbehandling(tac, begrunnelse = begrunnelse, skalJournalføreNotat = false)
            sendMeldekortbehandlingTilBeslutning(tac, sakId, meldekortId, saksbehandler)

            val vedtak = iverksettMeldekort(tac, sakId, meldekortId, JobberEtterIverksettelse())

            vedtak.skalJournalføreNotat shouldBe false
            tac.hentMeldekortvedtak(sakId, vedtak).journalføringsnotat.shouldBeNull()
        }
    }

    private suspend fun ApplicationTestBuilder.iverksettRammebehandling(
        tac: TestApplicationContext,
        sakId: SakId,
        behandling: Rammebehandling,
        jobber: JobberEtterIverksettelse,
    ): Rammevedtak {
        taRammebehandlinger(tac, behandlinger = listOf(sakId to behandling.id), saksbehandler = beslutter)
        return iverksettForBehandlingId(
            tac = tac,
            sakId = sakId,
            behandlingId = behandling.id,
            beslutter = beslutter,
            jobber = jobber,
        )!!.second
    }

    private suspend fun ApplicationTestBuilder.oppdatertMeldekortbehandling(
        tac: TestApplicationContext,
        begrunnelse: String?,
        skalJournalføreNotat: Boolean,
    ): Pair<SakId, MeldekortId> {
        val (sak, _, _, meldekortbehandling) = iverksettSøknadsbehandlingOgOppdaterMeldekortbehandling(
            tac = tac,
            saksbehandler = saksbehandler,
            begrunnelse = begrunnelse,
            skalJournalføreNotat = skalJournalføreNotat,
        )!!
        meldekortbehandling.skalJournalføreNotat shouldBe skalJournalføreNotat
        return sak.id to meldekortbehandling.id
    }

    private suspend fun ApplicationTestBuilder.iverksettMeldekort(
        tac: TestApplicationContext,
        sakId: SakId,
        meldekortId: MeldekortId,
        jobber: JobberEtterIverksettelse,
    ): Meldekortvedtak {
        taMeldekortbehanding(tac, sakId, meldekortId, saksbehandlerEllerBeslutter = beslutter)
        return iverksettMeldekortbehandling(
            tac = tac,
            sakId = sakId,
            meldekortId = meldekortId,
            beslutter = beslutter,
            jobber = jobber,
        )!!.second
    }

    private fun TestApplicationContext.hentRammevedtak(sakId: SakId, vedtak: Rammevedtak): Rammevedtak =
        sakContext.sakRepo.hentForSakId(sakId)!!.vedtaksliste.rammevedtaksliste.single { it.id == vedtak.id }

    private fun TestApplicationContext.hentMeldekortvedtak(sakId: SakId, vedtak: Meldekortvedtak): Meldekortvedtak =
        sakContext.sakRepo.hentForSakId(sakId)!!.meldekortvedtaksliste.single { it.id == vedtak.id }
}
