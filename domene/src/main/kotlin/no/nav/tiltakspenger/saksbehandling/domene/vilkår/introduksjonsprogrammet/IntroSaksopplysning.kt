package no.nav.tiltakspenger.saksbehandling.domene.vilkår.introduksjonsprogrammet

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.Deltagelse
import java.time.LocalDateTime

sealed interface IntroSaksopplysning {
    val deltar: Periodisering<Deltagelse>
    val tidsstempel: LocalDateTime
    val totalePeriode: Periode

    val navIdent: String?

    fun oppdaterPeriode(periode: Periode): IntroSaksopplysning

    data class Søknad(
        override val deltar: Periodisering<Deltagelse>,
        override val tidsstempel: LocalDateTime,
    ) : IntroSaksopplysning {
        override val navIdent = null

        init {
            require(deltar.isNotEmpty()) { "IntroSaksopplysning må ha minst én periode, men var tom." }
        }

        override val totalePeriode: Periode = deltar.totalePeriode

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Søknad {
            return copy(deltar = deltar.krymp(periode))
        }
    }

    data class Saksbehandler(
        override val deltar: Periodisering<Deltagelse>,
        override val tidsstempel: LocalDateTime,
        override val navIdent: String,
    ) : IntroSaksopplysning {
        init {
            require(deltar.isNotEmpty()) { "IntroSaksopplysning må ha minst én periode, men var tom." }
        }

        override val totalePeriode: Periode = deltar.totalePeriode

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Saksbehandler {
            return copy(deltar = deltar.krymp(periode))
        }
    }
}
