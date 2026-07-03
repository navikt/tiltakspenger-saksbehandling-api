package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.getOrFail
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.objectmothers.ObjectMother
import no.nav.tiltakspenger.saksbehandling.objectmothers.tilBeslutning
import no.nav.tiltakspenger.saksbehandling.vedtak.opprettRammevedtak
import org.junit.jupiter.api.Test
import java.time.Clock

/**
 * Verifiserer at [genererMeldeperioderForValidering] (kjøres på en ikke-vedtatt behandling) og
 * [genererMeldeperioderOgOppdaterKjeder] (kjøres på saken etter at behandlingen er iverksatt) produserer
 * de samme meldeperiodene.
 *
 * De eneste forventede forskjellene er vedtak-id-ene: under validering brukes en tilfeldig [VedtakId] for
 * behandlingens periode, mens den faktiske genereringen bruker vedtak-id-en til det iverksatte rammevedtaket.
 * [no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldeperiode.Meldeperiode.erLik] ignorerer derfor
 * `rammevedtak` (samt `id`, `versjon` og `opprettet`) og brukes til selve sammenligningen.
 */
class GenererMeldeperioderSammenligningTest {

    @Test
    fun `gir like meldeperioder for en enkel vedtaksperiode`() {
        val grunnlag = byggSammenligningsgrunnlag(
            vedtaksperiode = Periode(1.januar(2023), 31.januar(2023)),
        )

        grunnlag.valideringMeldeperioder.isNotEmpty() shouldBe true
        assertMeldeperioderErLike(
            forventet = grunnlag.valideringMeldeperioder,
            faktisk = grunnlag.vedtattMeldeperioder,
        )
    }

    @Test
    fun `gir like meldeperioder for en vedtaksperiode som strekker seg over flere meldeperioder`() {
        val grunnlag = byggSammenligningsgrunnlag(
            vedtaksperiode = Periode(1.januar(2023), 31.mars(2023)),
        )

        // En periode på 3 måneder skal gi flere meldeperioder
        (grunnlag.valideringMeldeperioder.size > 1) shouldBe true
        assertMeldeperioderErLike(
            forventet = grunnlag.valideringMeldeperioder,
            faktisk = grunnlag.vedtattMeldeperioder,
        )
    }

    @Test
    fun `meldeperiodene er like bortsett fra vedtak-id-ene`() {
        val grunnlag = byggSammenligningsgrunnlag(
            vedtaksperiode = Periode(1.januar(2023), 31.januar(2023)),
        )

        // De faktiske meldeperiodene peker på det iverksatte rammevedtaket ...
        val vedtattVedtakIder = grunnlag.vedtattMeldeperioder.flatMap { it.rammevedtak.verdier }.toSet()
        vedtattVedtakIder shouldBe setOf(grunnlag.rammevedtakId)

        // ... mens valideringsmeldeperiodene bruker tilfeldige vedtak-id-er
        val valideringVedtakIder = grunnlag.valideringMeldeperioder.flatMap { it.rammevedtak.verdier }.toSet()
        valideringVedtakIder shouldNotContain grunnlag.rammevedtakId

        // Alt annet enn vedtak-id-ene skal være likt
        assertMeldeperioderErLike(
            forventet = grunnlag.valideringMeldeperioder,
            faktisk = grunnlag.vedtattMeldeperioder,
        )
    }

    private data class Sammenligningsgrunnlag(
        val valideringMeldeperioder: List<Meldeperiode>,
        val vedtattMeldeperioder: List<Meldeperiode>,
        val rammevedtakId: VedtakId,
    )

    /**
     * Bygger opp en sak med én søknadsbehandling, og kjører begge genereringsfunksjonene:
     *  1. [genererMeldeperioderForValidering] mens behandlingen er under beslutning (ikke vedtatt)
     *  2. [genererMeldeperioderOgOppdaterKjeder] etter at den samme behandlingen er iverksatt og lagt på saken
     */
    private fun byggSammenligningsgrunnlag(
        vedtaksperiode: Periode,
        clock: Clock = fixedClock,
    ): Sammenligningsgrunnlag {
        val saksbehandler = ObjectMother.saksbehandler()
        val beslutter = ObjectMother.beslutter()

        val (sak, behandling) = ObjectMother.sakMedOpprettetBehandling(
            vedtaksperiode = vedtaksperiode,
            saksbehandler = saksbehandler,
            clock = clock,
        )

        // Sett behandlingen i en pre-vedtatt tilstand (under beslutning)
        val behandlingUnderBeslutning = behandling
            .tilBeslutning(saksbehandler = saksbehandler, clock = clock)
            .taBehandling(beslutter, clock)
            .getOrFail().first

        // 1. Generer meldeperioder for validering ut fra den ikke-vedtatte behandlingen
        val valideringMeldeperioder = sak
            .genererMeldeperioderForValidering(behandlingUnderBeslutning, clock)
            .getOrFail()

        // Iverksett behandlingen (gir status VEDTATT) og opprett rammevedtaket på saken
        val iverksattBehandling = behandlingUnderBeslutning.iverksett(
            utøvendeBeslutter = beslutter,
            attestering = ObjectMother.godkjentAttestering(beslutter, clock),
            correlationId = CorrelationId.generate(),
            clock = clock,
        ).first

        val (sakMedVedtak, rammevedtak) = sak.oppdaterRammebehandling(iverksattBehandling)
            .opprettRammevedtak(iverksattBehandling, clock)
            .getOrFail()

        // 2. Generer de faktiske meldeperiodene ut fra rammevedtaket på saken
        val vedtattMeldeperioder = sakMedVedtak.meldeperiodeKjeder
            .genererMeldeperioderOgOppdaterKjeder(sakMedVedtak.rammevedtaksliste, clock)
            .getOrFail()
            .second

        return Sammenligningsgrunnlag(
            valideringMeldeperioder = valideringMeldeperioder,
            vedtattMeldeperioder = vedtattMeldeperioder,
            rammevedtakId = rammevedtak.id,
        )
    }

    private fun assertMeldeperioderErLike(
        forventet: List<Meldeperiode>,
        faktisk: List<Meldeperiode>,
    ) {
        faktisk.size shouldBe forventet.size

        forventet.zip(faktisk).forEach { (forventetMp, faktiskMp) ->
            withClue("Meldeperiode for ${forventetMp.periode} skal være lik (bortsett fra vedtak-id-er)") {
                faktiskMp.erLik(forventetMp) shouldBe true
                faktiskMp.periode shouldBe forventetMp.periode
                faktiskMp.maksAntallDagerForMeldeperiode shouldBe forventetMp.maksAntallDagerForMeldeperiode
                faktiskMp.girRett shouldBe forventetMp.girRett
            }
        }
    }
}
