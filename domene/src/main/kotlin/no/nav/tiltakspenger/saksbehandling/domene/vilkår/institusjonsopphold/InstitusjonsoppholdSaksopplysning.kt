package no.nav.tiltakspenger.saksbehandling.domene.vilkår.institusjonsopphold

import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import java.time.LocalDateTime

sealed interface InstitusjonsoppholdSaksopplysning {
    fun oppdaterPeriode(periode: Periode): InstitusjonsoppholdSaksopplysning

    val opphold: Periodisering<Opphold>
    val tidsstempel: LocalDateTime
    val totalePeriode: Periode

    val navIdent: String?

    data class Søknad(
        override val opphold: Periodisering<Opphold>,
        override val tidsstempel: LocalDateTime,
    ) : InstitusjonsoppholdSaksopplysning {
        override val navIdent = null

        init {
            require(
                opphold.isNotEmpty(),
            ) { "InstitusjonsoppholdSaksopplysning må ha minst én periode, men var tom." }
        }

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Søknad {
            return copy(opphold = opphold.krymp(periode))
        }

        override val totalePeriode: Periode = opphold.totalePeriode
    }

    data class Saksbehandler(
        override val opphold: Periodisering<Opphold>,
        override val tidsstempel: LocalDateTime,
        override val navIdent: String,
    ) : InstitusjonsoppholdSaksopplysning {
        init {
            require(
                opphold.isNotEmpty(),
            ) { "InstitusjonsoppholdSaksopplysning må ha minst én periode, men var tom." }
        }

        override val totalePeriode: Periode = opphold.totalePeriode

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Saksbehandler {
            return copy(opphold = opphold.krymp(periode))
        }
    }
}
