package no.nav.tiltakspenger.saksbehandling.domene.vilkår.alder

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.periodisering.Periode
import java.time.LocalDate
import java.time.LocalDateTime

sealed interface AlderSaksopplysning {

    val fødselsdato: LocalDate
    val tidsstempel: LocalDateTime
    val navIdent: String?

    fun oppdaterPeriode(periode: Periode): AlderSaksopplysning

    data class Register(
        override val fødselsdato: LocalDate,
        override val tidsstempel: LocalDateTime,
    ) : AlderSaksopplysning {
        override val navIdent = null

        companion object {
            fun opprett(fødselsdato: LocalDate): Register =
                Register(fødselsdato = fødselsdato, tidsstempel = nå())
        }

        init {
            require(fødselsdato.isBefore(LocalDate.now())) { "Kan ikke ha fødselsdag frem i tid" }
        }

        /** NOOP - men åpner for muligheten å periodisere denne */
        override fun oppdaterPeriode(periode: Periode): Register {
            return this
        }
    }

    data class Saksbehandler(
        override val fødselsdato: LocalDate,
        override val tidsstempel: LocalDateTime,
        override val navIdent: String,
    ) : AlderSaksopplysning {
        init {
            require(fødselsdato.isBefore(LocalDate.now())) { "Kan ikke ha fødselsdag frem i tid" }
        }

        /** NOOP - men åpner for muligheten å periodisere denne */
        override fun oppdaterPeriode(periode: Periode): Saksbehandler {
            return this
        }
    }
}
