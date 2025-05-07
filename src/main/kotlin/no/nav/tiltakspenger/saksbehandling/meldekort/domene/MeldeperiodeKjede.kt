package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder,
    Comparable<MeldeperiodeKjede> {
    constructor(meldeperiode: Meldeperiode) : this(nonEmptyListOf(meldeperiode))

    // Disse fungerer også som validering, hvis du fjerner må du legge de inn som init.
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val saksnummer: Saksnummer = meldeperioder.map { it.saksnummer }.distinct().single()
    val fnr: Fnr = meldeperioder.map { it.fnr }.distinct().single()
    val kjedeId: MeldeperiodeKjedeId = meldeperioder.map { it.kjedeId }.distinct().single()

    val siste: Meldeperiode = meldeperioder.last()
    val sisteVersjon = siste.versjon

    init {
        meldeperioder.nonDistinctBy { it.id }.also {
            require(it.isEmpty()) {
                "Meldeperiodekjeden $kjedeId har duplikate meldeperioder - $it"
            }
        }

        meldeperioder.zipWithNext { a, b ->
            require(a.versjon < b.versjon) {
                "Meldeperiodene må være sortert på versjon - ${a.id} og ${b.id} var i feil rekkefølge"
            }
            require(a.opprettet != b.opprettet) {
                "Meldeperodene må ha ulik opprettelses tidspunkt - ${a.id} og ${b.id} var like"
            }
            require(a.id != b.id)
            require(!a.erLik(b)) {
                "Meldeperiodene må være unike - ${a.id} og ${b.id} var like"
            }
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperioder.last()
    }

    fun erLikSiste(meldeperiode: Meldeperiode): Boolean {
        return siste.erLik(meldeperiode)
    }

    /**
     * Endrer bare kjeden dersom siste meldeperiode i kjeden har en differense med nye meldeperioden.
     * @return Par av [MeldeperiodeKjede] med [Meldeperiode] hvis kjeden har blitt endret, ellers nåværende kjede og null.
     */
    fun leggTilMeldeperiode(meldeperiode: Meldeperiode): Pair<MeldeperiodeKjede, Meldeperiode?> {
        require(meldeperiode.versjon == sisteVersjon.inc()) { "Den innkommende meldeperioden sin versjon må være 1 versjon høyere enn kjedens siste versjon" }
        if (erLikSiste(meldeperiode)) {
            return this to null
        }
        return MeldeperiodeKjede(meldeperioder + meldeperiode) to meldeperiode
    }

    fun nesteVersjon(): HendelseVersjon = this.meldeperioder.last().versjon.inc()

    override fun compareTo(other: MeldeperiodeKjede): Int {
        require(!this.periode.overlapperMed(other.periode)) { "Meldeperiodekjedene kan ikke overlappe" }
        return this.periode.fraOgMed.compareTo(other.periode.fraOgMed)
    }
}
