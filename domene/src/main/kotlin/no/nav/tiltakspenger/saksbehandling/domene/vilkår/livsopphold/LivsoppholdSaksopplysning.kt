package no.nav.tiltakspenger.saksbehandling.domene.vilkår.livsopphold

import no.nav.tiltakspenger.felles.exceptions.IkkeImplementertException
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.felles.ÅrsakTilEndring
import java.time.LocalDateTime

sealed interface LivsoppholdSaksopplysning {
    fun oppdaterPeriode(periode: Periode): LivsoppholdSaksopplysning

    val harLivsoppholdYtelser: Boolean
    val tidsstempel: LocalDateTime

    // TODO jah: Periodiser denne. Generelt sett bør alle saksopplysninger kunne periodiseres.
    val periode: Periode

    val årsakTilEndring: ÅrsakTilEndring?
    val navIdent: String?

    data class Søknad(
        override val harLivsoppholdYtelser: Boolean,
        override val tidsstempel: LocalDateTime,
        override val periode: Periode,
    ) : LivsoppholdSaksopplysning {
        override val navIdent = null
        override val årsakTilEndring = null

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Søknad {
            return copy(periode = periode)
        }
    }

    data class Saksbehandler(
        override val harLivsoppholdYtelser: Boolean,
        override val årsakTilEndring: ÅrsakTilEndring?,
        override val tidsstempel: LocalDateTime,
        override val navIdent: String,
        override val periode: Periode,
    ) : LivsoppholdSaksopplysning {
        init {
            if (harLivsoppholdYtelser) {
                throw IkkeImplementertException("Støtter ikke avslag enda.")
            }
        }

        /** Støtter i førsteomgang kun å krympe perioden. Dersom man skulle utvidet den, måtte man gjort en ny vurdering og ville derfor hatt en ny saksopplysning. */
        override fun oppdaterPeriode(periode: Periode): Saksbehandler {
            return copy(periode = periode)
        }
    }
}
