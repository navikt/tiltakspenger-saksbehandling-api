package no.nav.tiltakspenger.meldekort.domene

import arrow.core.NonEmptyList
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nonDistinctBy
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer

data class MeldeperiodeKjede(
    private val meldeperioder: NonEmptyList<Meldeperiode>,
) : List<Meldeperiode> by meldeperioder {

    // Disse fungerer også som validering, hvis du fjerner må du legge de inn som init.
    val sakId: SakId = meldeperioder.map { it.sakId }.distinct().single()
    val periode: Periode = meldeperioder.map { it.periode }.distinct().single()
    val saksnummer: Saksnummer = meldeperioder.map { it.saksnummer }.distinct().single()
    val fnr: Fnr = meldeperioder.map { it.fnr }.distinct().single()
    val kjedeId: MeldeperiodeKjedeId = meldeperioder.map { it.kjedeId }.distinct().single()

    val siste = meldeperioder.last()

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
            Pair(this.leggTilMeldeperiode(oppdatertMeldeperiode), oppdatertMeldeperiode)
        } else {
            Pair(this, null)
        }
    }

    fun hentSisteMeldeperiode(): Meldeperiode {
        return meldeperioder.last()
    }

    fun leggTilMeldeperiode(meldeperiode: Meldeperiode): MeldeperiodeKjede {
        return MeldeperiodeKjede(meldeperioder + meldeperiode)
    }
}
