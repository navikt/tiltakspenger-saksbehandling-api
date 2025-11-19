package no.nav.tiltakspenger.saksbehandling.omgjøring

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import java.time.LocalDate

data class OmgjortAvRammevedtak(
    val omgjøringsperioder: Omgjøringsperioder,
) : List<Omgjøringsperiode> by omgjøringsperioder {

    constructor(vararg omgjøringsperiode: Omgjøringsperiode) : this(
        Omgjøringsperioder(omgjøringsperiode.toList()),
    )

    constructor(omgjøringsperioder: List<Omgjøringsperiode>) : this(
        Omgjøringsperioder(omgjøringsperioder),
    )

    constructor(vararg omgjøringsperioder: Omgjøringsperioder) : this(
        Omgjøringsperioder(omgjøringsperioder.toList().flatMap { it.omgjøringsperiode }),
    )

    val rammevedtakIDer: List<VedtakId> = omgjøringsperioder.rammevedtakIDer
    val perioder: List<Periode> = omgjøringsperioder.perioder
    val fraOgMed: LocalDate? = omgjøringsperioder.fraOgMed
    val tilOgMed: LocalDate? = omgjøringsperioder.tilOgMed
    val totalPeriode: Periode? = omgjøringsperioder.totalPeriode

    fun leggTil(omgjøringsperioder: List<Omgjøringsperiode>): OmgjortAvRammevedtak {
        return OmgjortAvRammevedtak(
            Omgjøringsperioder(this.omgjøringsperioder + omgjøringsperioder),
        )
    }

    companion object {
        val empty: OmgjortAvRammevedtak = OmgjortAvRammevedtak(Omgjøringsperioder.empty)
    }

    init {
        if (omgjøringsperioder.any { it.omgjøringsgrad == Omgjøringsgrad.HELT }) {
            require(size == 1) {
                "Et vedtak som er omgjort i sin helhet, kan ikke ha flere omgjøringsperioder."
            }
        }
    }
}
