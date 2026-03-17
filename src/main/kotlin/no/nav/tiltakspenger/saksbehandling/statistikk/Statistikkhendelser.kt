package no.nav.tiltakspenger.saksbehandling.statistikk

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.saksbehandling.statistikk.saksstatistikk.StatistikkDTO
import java.time.Clock

data class Statistikkhendelser(
    val hendelser: List<Statistikkhendelse>,
) : List<Statistikkhendelse> by hendelser {
    constructor(vararg hendelser: Statistikkhendelse) : this(hendelser.toList())

    fun leggTil(hendelse: Statistikkhendelse): Statistikkhendelser {
        return copy(hendelser = hendelser + hendelse)
    }

    fun leggTil(hendelser: List<Statistikkhendelse>): Statistikkhendelser {
        return copy(hendelser = this.hendelser + hendelser)
    }

    fun leggTil(vararg hendelser: Statistikkhendelse): Statistikkhendelser {
        return copy(hendelser = this.hendelser + hendelser)
    }

    operator fun plus(hendelse: Statistikkhendelse): Statistikkhendelser {
        return copy(hendelser = this.hendelser + hendelse)
    }

    operator fun plus(hendelser: Statistikkhendelser): Statistikkhendelser {
        return copy(hendelser = this.hendelser + hendelser)
    }

    suspend fun tilStatistikkDto(
        gjelderKode6: suspend (Fnr) -> Boolean,
        versjon: String,
        clock: Clock,
    ): StatistikkDTO {
        val saksstatistikk = hendelser.filterIsInstance<GenererSaksstatistikk>().map {
            it.genererSaksstatistikk(
                gjelderKode6 = gjelderKode6,
                versjon = versjon,
                clock = clock,
            )
        }
        val stønadsstatistikk = hendelser.filterIsInstance<GenererStønadsstatistikk>().map {
            it.genererStønadsstatistikk()
        }
        val meldekortstatistikk = hendelser.filterIsInstance<GenererMeldekortstatistikk>().map {
            it.genererMeldekortstatistikk()
        }
        val utbetalingsstatistikk = hendelser.filterIsInstance<GenererUtbetalingsstatistikk>().map {
            it.genererUtbetalingsstatistikk()
        }
        return StatistikkDTO(
            saksstatistikk = saksstatistikk,
            stønadsstatistikk = stønadsstatistikk,
            meldekortstatistikk = meldekortstatistikk,
            utbetalingsstatistikk = utbetalingsstatistikk,
        )
    }

    companion object {
        fun empty(): Statistikkhendelser = Statistikkhendelser(emptyList())
    }
}
