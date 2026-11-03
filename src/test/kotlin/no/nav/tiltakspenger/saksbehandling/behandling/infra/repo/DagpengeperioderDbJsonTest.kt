package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Dagpengeperioder
import no.nav.tiltakspenger.saksbehandling.dagpenger.domene.Dagpengeperiode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * **Enhetstest framfor e2e, bevisst valgt.**
 * `BehandletFørFeature` finnes per definisjon kun på rader som ble skrevet før funksjonaliteten kom, og kan ikke produseres av noen prodsti i dag.
 * Mappingen rører ikke postgres.
 *
 * Testen pinner **de faktiske databaseverdiene**, ikke bare rundturen.
 * Et navnebytte i begge retninger samtidig ville ikke slått ut i en ren rundtur.
 */
class DagpengeperioderDbJsonTest {

    private val oppslagstidspunkt = LocalDateTime.parse("2025-01-20T10:00:00")
    private val oppslagsperiode = 1.januar(2025) til 31.mars(2025)

    private fun treff(
        ytelsetype: Dagpengeperiode.Ytelsetype = Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER,
        kilde: Dagpengeperiode.Kilde = Dagpengeperiode.Kilde.ARENA,
        tilOgMed: LocalDate? = 31.mars(2025),
    ) = Dagpengeperioder.Treff(
        value = nonEmptyListOf(
            Dagpengeperiode(
                fraOgMed = 1.januar(2025),
                tilOgMed = tilOgMed,
                ytelsetype = ytelsetype,
                kilde = kilde,
            ),
        ),
        oppslagsperiode = oppslagsperiode,
        oppslagstidspunkt = oppslagstidspunkt,
    )

    private fun lagretPeriode(ytelsetype: String = "ORDINÆRE_DAGPENGER", kilde: String = "ARENA") =
        DagpengeperioderDbJson.DagpengeperiodeDbJson(
            fraOgMed = 1.januar(2025),
            tilOgMed = null,
            ytelsetype = ytelsetype,
            kilde = kilde,
        )

    @Test
    fun `typene lagres med sitt avtalte navn`() {
        treff().toDbJson().type.name shouldBe "Treff"
        Dagpengeperioder.IngenTreff(oppslagsperiode, oppslagstidspunkt).toDbJson().type.name shouldBe "IngenTreff"
        Dagpengeperioder.IkkeBehandlingsgrunnlag.toDbJson().type.name shouldBe "IkkeBehandlingsgrunnlag"
        Dagpengeperioder.BehandletFørFeature.toDbJson().type.name shouldBe "BehandletFørFeature"
    }

    @Test
    fun `ytelsetypene lagres med sitt avtalte navn`() {
        Dagpengeperiode.Ytelsetype.entries.associateWith {
            treff(ytelsetype = it).toDbJson().perioder.single().ytelsetype
        } shouldBe mapOf(
            Dagpengeperiode.Ytelsetype.ORDINÆRE_DAGPENGER to "ORDINÆRE_DAGPENGER",
            Dagpengeperiode.Ytelsetype.PERMITTERING to "PERMITTERING",
            Dagpengeperiode.Ytelsetype.PERMITTERING_FISKEINDUSTRI to "PERMITTERING_FISKEINDUSTRI",
        )
    }

    @Test
    fun `kildene lagres med sitt avtalte navn`() {
        Dagpengeperiode.Kilde.entries.associateWith {
            treff(kilde = it).toDbJson().perioder.single().kilde
        } shouldBe mapOf(
            Dagpengeperiode.Kilde.ARENA to "ARENA",
            Dagpengeperiode.Kilde.DP_SAK to "DP_SAK",
        )
    }

    @Test
    fun `typene leses tilbake fra lagret verdi`() {
        listOf(
            treff(),
            treff(tilOgMed = null),
            Dagpengeperioder.IngenTreff(oppslagsperiode, oppslagstidspunkt),
            Dagpengeperioder.IkkeBehandlingsgrunnlag,
            Dagpengeperioder.BehandletFørFeature,
        ).forEach {
            it.toDbJson().toDomain() shouldBe it
        }
    }

    @Test
    fun `ytelsetypene og kildene leses tilbake fra lagret verdi`() {
        Dagpengeperiode.Ytelsetype.entries.forEach { ytelsetype ->
            treff(ytelsetype = ytelsetype).toDbJson().toDomain().single().ytelsetype shouldBe ytelsetype
        }
        Dagpengeperiode.Kilde.entries.forEach { kilde ->
            treff(kilde = kilde).toDbJson().toDomain().single().kilde shouldBe kilde
        }
    }

    @Test
    fun `ukjent ytelsetype i databasen kaster`() {
        shouldThrow<IllegalStateException> { lagretPeriode(ytelsetype = "UKJENT").toDomain() }
    }

    @Test
    fun `ukjent kilde i databasen kaster`() {
        shouldThrow<IllegalStateException> { lagretPeriode(kilde = "UKJENT").toDomain() }
    }
}
