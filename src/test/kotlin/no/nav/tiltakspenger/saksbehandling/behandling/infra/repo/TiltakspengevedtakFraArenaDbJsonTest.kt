package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.arenavedtak.domene.ArenaTPVedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.TiltakspengevedtakFraArena
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * `BehandletFørFeature` finnes per definisjon kun på rader som ble skrevet før funksjonaliteten kom, og kan ikke produseres av noen prodsti i dag.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * `type` og `rettighet` lagres som navn, og et navnebytte i begge retninger samtidig ville ikke slått ut i en ren rundtur.
 */
class TiltakspengevedtakFraArenaDbJsonTest {

    private val oppslagstidspunkt = LocalDateTime.parse("2025-01-20T10:00:00")
    private val oppslagsperiode = 1.januar(2025) til 31.mars(2025)

    private fun treff(rettighet: ArenaTPVedtak.Rettighet) = TiltakspengevedtakFraArena.Treff(
        value = nonEmptyListOf(
            ArenaTPVedtak(
                fraOgMed = 1.januar(2025),
                tilOgMed = 31.mars(2025),
                rettighet = rettighet,
                vedtakId = 4711L,
            ),
        ),
        oppslagsperiode = oppslagsperiode,
        oppslagstidspunkt = oppslagstidspunkt,
    )

    @Test
    fun `typene lagres med sitt avtalte navn`() {
        treff(ArenaTPVedtak.Rettighet.TILTAKSPENGER).toDbJson().type.name shouldBe "Treff"
        TiltakspengevedtakFraArena.IngenTreff(oppslagsperiode, oppslagstidspunkt).toDbJson().type.name shouldBe "IngenTreff"
        TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag.toDbJson().type.name shouldBe "IkkeBehandlingsgrunnlag"
        TiltakspengevedtakFraArena.BehandletFørFeature.toDbJson().type.name shouldBe "BehandletFørFeature"
    }

    @Test
    fun `rettighetene lagres med sitt avtalte navn`() {
        ArenaTPVedtak.Rettighet.entries.associateWith {
            treff(it).toDbJson().tiltakspengevedtakFraArena.single().rettighet
        } shouldBe mapOf(
            ArenaTPVedtak.Rettighet.TILTAKSPENGER to "TILTAKSPENGER",
            ArenaTPVedtak.Rettighet.BARNETILLEGG to "BARNETILLEGG",
            ArenaTPVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG to "TILTAKSPENGER_OG_BARNETILLEGG",
            ArenaTPVedtak.Rettighet.INGENTING to "INGENTING",
        )
    }

    @Test
    fun `typene leses tilbake fra lagret verdi`() {
        listOf(
            treff(ArenaTPVedtak.Rettighet.TILTAKSPENGER),
            TiltakspengevedtakFraArena.IngenTreff(oppslagsperiode, oppslagstidspunkt),
            TiltakspengevedtakFraArena.IkkeBehandlingsgrunnlag,
            TiltakspengevedtakFraArena.BehandletFørFeature,
        ).forEach {
            it.toDbJson().toDomain() shouldBe it
        }
    }

    @Test
    fun `rettighetene leses tilbake fra lagret verdi`() {
        ArenaTPVedtak.Rettighet.entries.forEach { rettighet ->
            treff(rettighet).toDbJson().toDomain().single().rettighet shouldBe rettighet
        }
    }
}
