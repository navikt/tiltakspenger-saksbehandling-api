package no.nav.tiltakspenger.saksbehandling.omgjøring

import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import java.time.LocalDate

data class OmgjørRammevedtak(
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

    companion object {
        fun create(omgjørRammevedtak: Rammevedtak): OmgjørRammevedtak {
            return OmgjørRammevedtak(Omgjøringsperioder.create(omgjørRammevedtak))
        }

        val empty: OmgjørRammevedtak = OmgjørRammevedtak(Omgjøringsperioder.empty)
    }

    init {
        omgjøringsperioder.groupBy { it.rammevedtakId }.forEach { (rammevedtakId, perioder) ->
            if (perioder.any { it.omgjøringsgrad == Omgjøringsgrad.HELT }) {
                require(perioder.size == 1) {
                    "Et vedtak som er omgjort i sin helhet, kan ikke ha flere omgjøringsperioder. Fant flere for vedtakId=$rammevedtakId"
                }
            }
        }
    }
}
