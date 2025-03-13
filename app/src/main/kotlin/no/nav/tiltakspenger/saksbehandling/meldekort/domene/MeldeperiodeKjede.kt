package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder,
    Comparable<MeldeperiodeKjede> {

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
            require(a.id != b.id)
            require(!a.erLik(b)) {
                "Meldeperiodene må være unike - ${a.id} og ${b.id} var like"
            }
        }
    }

    /**
     * Legger til en ny meldeperiode i kjeden dersom den overlapper med stansperioden.
     * Setter [Meldeperiode.antallDagerForPeriode] til 0 dersom ingen av dagene i meldeperioden gir rett.
     * Oppdaterer [Meldeperiode.girRett] basert på [stansperiode]
     * Inkrementerer [Meldeperiode.versjon] med 1.
     */
    fun oppdaterMedNyStansperiode(stansperiode: Periode): Pair<MeldeperiodeKjede, Meldeperiode?> {
        val erFullstendigStans = stansperiode.inneholderHele(this.periode)
        return if (stansperiode.overlapperMed(this.periode)) {
            val oppdatertMeldeperiode = Meldeperiode(
                kjedeId = kjedeId,
                id = MeldeperiodeId.random(),
                versjon = siste.versjon.inc(),
                periode = this.periode,
                opprettet = nå(),
                sakId = sakId,
                saksnummer = saksnummer,
                fnr = fnr,
                // Kommentar jah: Småplukk. Dersom dager som gir rett < siste.antallDagerForPeriode, bør vi krympet antallDagerForPeriode?
                // Meldeperiode burde isåfall ha noe i init-en sin dersom det er en slik avhengighet mellom typene.
                antallDagerForPeriode = if (erFullstendigStans) 0 else siste.antallDagerForPeriode,
                girRett = siste.girRett.mapValues { (dag, girRett) -> if (stansperiode.inneholder(dag)) false else girRett },
                sendtTilMeldekortApi = null,
            )
            this.leggTilMeldeperiode(oppdatertMeldeperiode)
        } else {
            Pair(this, null)
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
