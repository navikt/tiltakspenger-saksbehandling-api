package no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.repo

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.common.IsolatedDatabaseTest
import no.nav.tiltakspenger.saksbehandling.common.TestApplicationContext
import no.nav.tiltakspenger.saksbehandling.common.withTestApplicationContextAndPostgres
import no.nav.tiltakspenger.saksbehandling.routes.RouteBehandlingBuilder.opprettSakOgSøknad
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseId
import no.nav.tiltakspenger.saksbehandling.tilbakekreving.infra.kafka.konsumerTilbakekrevingshendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

/**
 * Aggregat-test for køen av ubehandlede tilbakekrevingshendelser, jf. testtaksonomien i `AGENTS.md`.
 *
 * Spørringen velger ut på tvers av alle saker (`where behandlet is null`) og sorterer eldst først (`order by opprettet`).
 * Testen bygger flere hendelser og asserter hele køen, uten å filtrere.
 *
 * Hendelsene kommer inn gjennom prodstien, altså consumeren for tilbakekrevingstopicen.
 * Grensen på 100 er hardkodet i spørringen, og testes ikke her — det ville kostet 101 hendelser for å bevise ett tall.
 */
class TilbakekrevingHendelseAggregatTest {

    @Test
    @IsolatedDatabaseTest
    fun `køen tar ubehandlede hendelser, sorterer eldst først og tømmes når hendelsen er behandlet`() {
        withTestApplicationContextAndPostgres(runIsolated = true) { tac ->
            val repo = tac.tilbakekrevingHendelseRepo

            repo.hentUbehandledeHendelseIder().shouldBeEmpty()

            val eldst = mottaHendelseMedUgyldigSaksnummer(tac, saksnummer = "ikke-et-saksnummer-1")
            val nyest = mottaHendelseMedUgyldigSaksnummer(tac, saksnummer = "ikke-et-saksnummer-2")

            repo.hentUbehandledeHendelseIder() shouldBe listOf(eldst, nyest)

            // Jobben markerer hendelsen som behandlet med feil, og da forlater den køen.
            tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(eldst)

            repo.hentUbehandledeHendelseIder() shouldBe listOf(nyest)

            tac.behandleTilbakekrevingHendelserJobb.håndterHendelse(nyest)

            repo.hentUbehandledeHendelseIder().shouldBeEmpty()
        }
    }

    /**
     * Et saksnummer vi ikke klarer å lese gir [no.nav.tiltakspenger.saksbehandling.tilbakekreving.domene.hendelser.TilbakekrevinghendelseFeil.UgyldigSaksnummer].
     * Det er den billigste veien til en ubehandlet hendelse som jobben kan gjøre seg ferdig med, og dekker samtidig `markerSomBehandletMedFeil`.
     */
    private suspend fun ApplicationTestBuilder.mottaHendelseMedUgyldigSaksnummer(
        tac: TestApplicationContext,
        saksnummer: String,
    ): TilbakekrevinghendelseId {
        val (sak) = opprettSakOgSøknad(tac = tac)

        @Language("JSON")
        val json = """
            {
                "hendelsestype": "fagsysteminfo_behov",
                "versjon": 1,
                "eksternFagsakId": "$saksnummer",
                "hendelseOpprettet": "${nå(tac.clock)}",
                "kravgrunnlagReferanse": "ref-$saksnummer"
            }
        """.trimIndent()

        return konsumerTilbakekrevingshendelse(
            key = sak.fnr.verdi,
            value = json,
            tilbakekrevingHendelseRepo = tac.tilbakekrevingHendelseRepo,
            clock = tac.clock,
        )!!
    }
}
