package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.json.serialize
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.toDbJson
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelse
import no.nav.tiltakspenger.saksbehandling.ytelser.domene.Ytelsetype
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * Ytelsene lagres som jsonb på saksopplysningene, og hver variant av [Ytelser] har sin egen gren i begge retninger.
 * Én behandling har nøyaktig én variant, så det ville tatt fire behandlinger å nå alle grenene gjennom prodstien — og de to siste variantene er historiske tilstander prodkoden ikke lenger produserer.
 * Mappingen er en ren funksjon og testes uttømmende her.
 */
class YtelserDbJsonTest {

    private val oppslagsperiode = Periode(1.januar(2025), 31.mars(2025))
    private val oppslagstidspunkt = LocalDateTime.of(2025, 4, 1, 12, 0)

    @Test
    fun `treff med ytelser overlever rundturen`() {
        val treff = Ytelser.Treff(
            value = nonEmptyListOf(
                Ytelse.Kjent(ytelsetype = Ytelsetype.AAP, kildetekst = "Arbeidsavklaringspenger", perioder = listOf(oppslagsperiode)),
                Ytelse.Kjent(ytelsetype = Ytelsetype.SPK, kildetekst = "Statens pensjonskasse", perioder = listOf(oppslagsperiode)),
                Ytelse.Ukjent(kildetekst = "Økonomisk sosialhjelp", perioder = listOf(oppslagsperiode)),
                Ytelse.Ukjent(kildetekst = null, perioder = listOf(oppslagsperiode)),
            ),
            oppslagsperiode = oppslagsperiode,
            oppslagstidspunkt = oppslagstidspunkt,
        )

        treff.toDbJson().toDomain() shouldBe treff
    }

    @Test
    fun `ingen treff overlever rundturen`() {
        val ingenTreff = Ytelser.IngenTreff(
            oppslagsperiode = oppslagsperiode,
            oppslagstidspunkt = oppslagstidspunkt,
        )

        ingenTreff.toDbJson().toDomain() shouldBe ingenTreff
    }

    @Test
    fun `variantene uten oppslag overlever rundturen`() {
        Ytelser.IkkeBehandlingsgrunnlag.toDbJson().toDomain() shouldBe Ytelser.IkkeBehandlingsgrunnlag
        Ytelser.BehandletFørFeature.toDbJson().toDomain() shouldBe Ytelser.BehandletFørFeature
    }

    @Test
    fun `lagres med vår egen kode`() {
        val treff = Ytelser.Treff(
            value = nonEmptyListOf(Ytelse.Kjent(ytelsetype = Ytelsetype.SPK, kildetekst = "Statens pensjonskasse", perioder = listOf(oppslagsperiode))),
            oppslagsperiode = oppslagsperiode,
            oppslagstidspunkt = oppslagstidspunkt,
        )

        serialize(treff.toDbJson()) shouldEqualJson """
            {
              "ytelser": [{"ytelsetype": "SPK", "kildetekst": "Statens pensjonskasse", "perioder": [{"fraOgMed": "2025-01-01", "tilOgMed": "2025-03-31"}]}],
              "type": "Treff",
              "oppslagstidspunkt": "2025-04-01T12:00:00",
              "oppslagsperiode": {"fraOgMed": "2025-01-01", "tilOgMed": "2025-03-31"}
            }
        """.trimIndent()
    }
}
