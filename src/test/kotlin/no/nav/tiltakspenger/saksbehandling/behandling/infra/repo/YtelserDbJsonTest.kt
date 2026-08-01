package no.nav.tiltakspenger.saksbehandling.behandling.infra.repo

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.Ytelser
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
                Ytelse(ytelsetype = Ytelsetype.AAP, perioder = listOf(oppslagsperiode)),
                Ytelse(ytelsetype = Ytelsetype.DAGPENGER, perioder = listOf(oppslagsperiode)),
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

    /**
     * Ytelsetypen lagres som `tekstverdi`, ikke som enum-navnet, og leses tilbake ved å søke opp tekstverdien.
     * Endres en tekstverdi uten migrering, smeller lesestien på gammel data — derfor sjekkes alle typene.
     */
    @Test
    fun `alle ytelsetypene overlever rundturen`() {
        val alle = Ytelsetype.entries.map { Ytelse(ytelsetype = it, perioder = listOf(oppslagsperiode)) }
        val treff = Ytelser.Treff(
            value = alle.toTypedArray().let { nonEmptyListOf(it.first(), *it.drop(1).toTypedArray()) },
            oppslagsperiode = oppslagsperiode,
            oppslagstidspunkt = oppslagstidspunkt,
        )

        treff.toDbJson().toDomain() shouldBe treff
    }
}
