package no.nav.tiltakspenger.saksbehandling.meldekort.infra.repo.dbjson

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.MeldeperiodebehandlingType
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.Meldeperiodebehandlinger
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.tilMeldeperiodebehandling
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.tilUtfyltMeldeperiode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import org.junit.jupiter.api.Test

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * Dette er en ren mapping som ikke rører postgres, og poenget er å pinne den lagrede json-en - ikke bare rundturen.
 */
class MeldeperiodebehandlingerDbJsonTest {

    @Test
    fun `meldekort fra bruker lagres som en liste med id-er`() {
        val brukersMeldekort = ObjectMother.brukersMeldekort()
        val meldekortbehandlingId = MeldekortId.random()

        val meldeperiodebehandlinger = Meldeperiodebehandlinger(
            meldeperiode = brukersMeldekort.tilMeldeperiodebehandling(
                type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                meldekortbehandlingId = meldekortbehandlingId,
            ),
            beregning = null,
        )

        meldeperiodebehandlinger.tilDbJson() shouldContain """"brukersMeldekortIder":["${brukersMeldekort.id}"]"""
    }

    @Test
    fun `behandling uten meldekort fra bruker lagres med tom liste`() {
        val meldeperiode = ObjectMother.meldeperiode()
        val meldekortbehandlingId = MeldekortId.random()

        val meldeperiodebehandlinger = Meldeperiodebehandlinger(
            meldeperiode = meldeperiode.tilMeldeperiodebehandling(
                type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
                meldekortbehandlingId = meldekortbehandlingId,
            ),
            beregning = null,
        )

        meldeperiodebehandlinger.tilDbJson() shouldContain """"brukersMeldekortIder":[]"""
    }

    @Test
    fun `lagret liste leses tilbake til de samme meldekortene fra bruker`() {
        val brukersMeldekort = ObjectMother.brukersMeldekort()
        val meldeperiode = brukersMeldekort.meldeperiode
        val meldekortbehandlingId = MeldekortId.random()

        val meldeperiodebehandlinger = Meldeperiodebehandlinger(
            meldeperiode = brukersMeldekort.tilUtfyltMeldeperiode(),
            beregning = null,
            brukersMeldekort = listOf(brukersMeldekort),
            type = MeldeperiodebehandlingType.FØRSTE_BEHANDLING,
            meldekortbehandlingId = meldekortbehandlingId,
        )

        val lest = meldeperiodebehandlinger.tilDbJson().tilMeldeperiodebehandlinger(
            beregning = null,
            hentMeldeperiode = { meldeperiode },
            hentBrukersMeldekort = { brukersMeldekort },
            meldekortbehandlingId = meldekortbehandlingId,
        )

        lest.single().brukersMeldekort shouldBe listOf(brukersMeldekort)
    }
}
