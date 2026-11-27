package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import kotliquery.queryOf
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.common.fixedClockAt
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.persistering.infra.PostgresSessionFactory
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContextMedPostgres
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.brukersmeldekort.BrukersMeldekort
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother.innvilgelsesperioder
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilOppdatertMeldeperiodeDTO
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.avbrytMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettRevurderingStans
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.mottaMeldekortRequest
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.oppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettMeldekortbehandlingForSakId
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgIverksettMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettOgOppdaterMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.sendMeldekortbehandlingTilBeslutning
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.taMeldekortbehanding
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.tilUtfyltFraBruker
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import org.junit.jupiter.api.Test

/**
 * Tester V287, som knytter manuelle meldekortbehandlingene fra før koblingene ble innført til meldekortene fra bruker de behandlet.
 * Tester også V288, som markerer meldekortene som behandlet av de iverksatte og avbrutte behandlingene de er knyttet til.
 * Tilstanden bygges gjennom prodstiene, og koblingene og markeringene fjernes etterpå med SQL for å etterligne data fra før de ble innført.
 * Migreringene oppdaterer hele tabeller, så testene kjører isolert.
 */
class BrukersMeldekortKoblingMigreringTest {

    @Test
    @IsolatedDatabaseTest
    fun `knytter meldekortene mottatt før behandlingen ble sendt til beslutning, som ikke er behandlet tidligere`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1 til 31.januar(2025)),
            )
            val førsteKjede = sak.meldeperiodeKjeder[0].kjedeId
            val andreKjede = sak.meldeperiodeKjeder[1].kjedeId
            val førsteMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            val andreMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 1)

            val (_, førsteBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeIder = listOf(førsteKjede, andreKjede),
            )!!
            oppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                meldeperioder = sak.meldeperiodeKjeder.take(2).map {
                    it.hentSisteMeldeperiode().tilOppdatertMeldeperiodeDTO()
                },
            )!!
            val mottattFørSendingTilBeslutning = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            sendMeldekortbehandlingTilBeslutning(tac = tac, sakId = sak.id, meldekortId = førsteBehandling.id)!!
            val mottattFørIverksettelse = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            taMeldekortbehanding(
                tac = tac,
                sakId = sak.id,
                meldekortId = førsteBehandling.id,
                saksbehandlerEllerBeslutter = ObjectMother.beslutter("beslutter"),
            )!!
            iverksettMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = førsteBehandling.id)!!

            val (_, _, korrigering) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = førsteKjede,
            )!!
            val ubehandlet = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)

            tac.fjernKoblinger(førsteBehandling.id)
            tac.fjernKoblinger(korrigering.id)
            tac.fjernMarkeringer()
            tac.kjørMigrering()

            val forventetFørste = mapOf(
                førsteKjede to listOf(førsteMeldekort.id, mottattFørSendingTilBeslutning.id),
                andreKjede to listOf(andreMeldekort.id),
            )
            val forventetKorrigering = mapOf(førsteKjede to listOf(mottattFørIverksettelse.id))
            val forventetBehandlingId = mapOf(
                førsteMeldekort.id to førsteBehandling.id,
                andreMeldekort.id to førsteBehandling.id,
                mottattFørSendingTilBeslutning.id to førsteBehandling.id,
                mottattFørIverksettelse.id to korrigering.id,
                ubehandlet.id to null,
            )
            tac.hentKoblinger(førsteBehandling.id) shouldBe forventetFørste
            tac.hentKoblinger(korrigering.id) shouldBe forventetKorrigering
            tac.hentBehandlingId(forventetBehandlingId.keys) shouldBe forventetBehandlingId

            // Meldekortet mottatt etter korrigeringen er ikke behandlet, og migreringene endrer ingenting når de kjøres på nytt.
            tac.kjørMigrering()
            tac.hentKoblinger(førsteBehandling.id) shouldBe forventetFørste
            tac.hentKoblinger(korrigering.id) shouldBe forventetKorrigering
            tac.hentBehandlingId(forventetBehandlingId.keys) shouldBe forventetBehandlingId
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `beholder eksisterende koblinger, hopper over automatiske behandlinger, og knytter avbrutte og åpne behandlinger til meldekortene som ikke er behandlet tidligere`() {
        // Automatisk behandling krever en hverdag innenfor økonomisystemets åpningstider.
        withTestApplicationContextAndPostgres(
            clock = TikkendeKlokke(fixedClockAt(2.mai(2025).atTime(12, 0))),
            runIsolated = true,
        ) { tac ->
            val (sak, automatiskBehandletMeldekort) = this.iverksettSøknadsbehandlingOgBehandleMeldekortAutomatisk(tac)
            val kjedeId = sak.meldeperiodeKjeder[0].kjedeId
            val automatiskBehandling = tac.meldekortContext.meldekortbehandlingRepo.hentForSakId(sak.id)!!.single()

            val korrigertMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            val (_, avbruttBehandling, _) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!
            avbrytMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = avbruttBehandling.id)!!

            val (_, åpenBehandling, _) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!
            val mottattEtterLagring = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)

            tac.fjernKoblinger(avbruttBehandling.id)
            tac.fjernKoblinger(åpenBehandling.id)
            tac.fjernMarkeringer()
            tac.kjørMigrering()

            tac.hentKoblinger(automatiskBehandling.id) shouldBe mapOf(kjedeId to listOf(automatiskBehandletMeldekort.id))
            tac.hentKoblinger(avbruttBehandling.id) shouldBe mapOf(kjedeId to listOf(korrigertMeldekort.id))
            tac.hentKoblinger(åpenBehandling.id) shouldBe mapOf(kjedeId to listOf(mottattEtterLagring.id))
            // Den avbrutte behandlingen markerer meldekortene sine som behandlet, mens den åpne ikke gjør det.
            tac.hentBehandlingId(listOf(automatiskBehandletMeldekort.id, korrigertMeldekort.id, mottattEtterLagring.id)) shouldBe mapOf(
                automatiskBehandletMeldekort.id to automatiskBehandling.id,
                korrigertMeldekort.id to avbruttBehandling.id,
                mottattEtterLagring.id to null,
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `knytter en avbrutt behandling til meldekortene mottatt før den ble avbrutt, som ikke er behandlet tidligere`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(
                tac = tac,
                innvilgelsesperioder = innvilgelsesperioder(1 til 31.januar(2025)),
            )
            val kjedeId = sak.meldeperiodeKjeder[0].kjedeId
            val førsteMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            val (_, _, førsteBehandling) = opprettOgIverksettMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!

            val korrigertMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            val (_, avbruttBehandling, _) = opprettOgOppdaterMeldekortbehandling(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!
            sendMeldekortbehandlingTilBeslutning(tac = tac, sakId = sak.id, meldekortId = avbruttBehandling.id)!!
            // Appen nuller tidspunktet for sendingen ved avbrytelse, så migreringen kan bare bruke avbrytelsestidspunktet.
            val mottattEtterSending = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            avbrytMeldekortbehandling(tac = tac, sakId = sak.id, meldekortId = avbruttBehandling.id)!!
            val mottattEtterAvbrytelse = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)

            tac.fjernKoblinger(førsteBehandling.id)
            tac.fjernKoblinger(avbruttBehandling.id)
            tac.fjernMarkeringer()
            tac.kjørMigrering()

            tac.hentKoblinger(førsteBehandling.id) shouldBe mapOf(kjedeId to listOf(førsteMeldekort.id))
            tac.hentKoblinger(avbruttBehandling.id) shouldBe mapOf(kjedeId to listOf(korrigertMeldekort.id, mottattEtterSending.id))
            tac.hentBehandlingId(listOf(førsteMeldekort.id, korrigertMeldekort.id, mottattEtterSending.id, mottattEtterAvbrytelse.id)) shouldBe mapOf(
                førsteMeldekort.id to førsteBehandling.id,
                korrigertMeldekort.id to avbruttBehandling.id,
                mottattEtterSending.id to avbruttBehandling.id,
                mottattEtterAvbrytelse.id to null,
            )
        }
    }

    @Test
    @IsolatedDatabaseTest
    fun `knytter en behandling som ble avbrutt av en stans før avbrytelsen ble lagret, til meldekortene mottatt før den sist ble endret`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val (sak, _, _) = this.iverksettSøknadsbehandling(tac = tac)
            val kjedeId = sak.meldeperiodeKjeder[0].kjedeId
            val brukersMeldekort = mottaMeldekortFraBruker(tac, sak, kjedeIndeks = 0)
            val (_, avbruttBehandling, _) = opprettMeldekortbehandlingForSakId(
                tac = tac,
                sakId = sak.id,
                kjedeId = kjedeId,
            )!!
            iverksettRevurderingStans(tac = tac, sakId = sak.id)

            tac.fjernKoblinger(avbruttBehandling.id)
            tac.fjernAvbrutt(avbruttBehandling.id)
            tac.fjernMarkeringer()
            tac.kjørMigrering()

            tac.hentKoblinger(avbruttBehandling.id) shouldBe mapOf(kjedeId to listOf(brukersMeldekort.id))
            tac.hentBehandlingId(listOf(brukersMeldekort.id)) shouldBe mapOf(brukersMeldekort.id to avbruttBehandling.id)
        }
    }

    private fun TestApplicationContextMedPostgres.kjørMigrering() {
        listOf(
            "/db/migration/V287__meldekortbehandling_knytt_brukers_meldekort.sql",
            "/db/migration/V288__meldekort_bruker_behandling_id.sql",
        ).forEach { migrering ->
            val sql = javaClass.getResource(migrering)!!.readText()
            (sessionFactory as PostgresSessionFactory).withSession { session ->
                session.run(queryOf(sql).asExecute)
            }
        }
    }

    private fun TestApplicationContextMedPostgres.fjernMarkeringer() {
        (sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(queryOf("update meldekort_bruker set behandling_id = null").asUpdate)
        }
    }

    /**
     * Rammeiverksettelsen lagret tidligere ikke avbrytelsen når en stans avbrøt en åpen meldekortbehandling.
     */
    private fun TestApplicationContextMedPostgres.fjernAvbrutt(meldekortbehandlingId: MeldekortId) {
        (sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                queryOf(
                    "update meldekortbehandling set avbrutt = null where id = :id",
                    mapOf("id" to meldekortbehandlingId.toString()),
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContext.hentBehandlingId(meldekortIder: Collection<MeldekortId>): Map<MeldekortId, MeldekortId?> =
        meldekortIder.associateWith { meldekortContext.brukersMeldekortRepo.hentForMeldekortId(it)!!.behandlingId }

    private fun TestApplicationContextMedPostgres.fjernKoblinger(meldekortbehandlingId: MeldekortId) {
        (sessionFactory as PostgresSessionFactory).withSession { session ->
            session.run(
                queryOf(
                    """
                    update meldekortbehandling
                    set meldeperioder = (
                        select jsonb_agg(meldeperiode || '{"brukersMeldekortIder": []}'::jsonb order by ordinalitet)
                        from jsonb_array_elements(meldeperioder) with ordinality as t(meldeperiode, ordinalitet)
                    )
                    where id = :id
                    """,
                    mapOf("id" to meldekortbehandlingId.toString()),
                ).asUpdate,
            )
        }
    }

    private fun TestApplicationContext.hentKoblinger(meldekortbehandlingId: MeldekortId): Map<MeldeperiodeKjedeId, List<MeldekortId>> =
        meldekortContext.meldekortbehandlingRepo.hent(meldekortbehandlingId)!!.meldeperioder.associate { meldeperiode ->
            meldeperiode.kjedeId to meldeperiode.brukersMeldekort.map { it.id }
        }

    private suspend fun ApplicationTestBuilder.mottaMeldekortFraBruker(
        tac: TestApplicationContext,
        sak: Sak,
        kjedeIndeks: Int,
    ): BrukersMeldekort {
        val meldeperiode = sak.meldeperiodeKjeder[kjedeIndeks].hentSisteMeldeperiode()
        val (_, brukersMeldekort) = mottaMeldekortRequest(
            tac = tac,
            meldeperiodeId = meldeperiode.id,
            sakId = sak.id,
            dager = meldeperiode.tilUtfyltFraBruker(kanSendeInnHelgForMeldekort = sak.kanSendeInnHelgForMeldekort),
        )
        return brukersMeldekort!!
    }
}
