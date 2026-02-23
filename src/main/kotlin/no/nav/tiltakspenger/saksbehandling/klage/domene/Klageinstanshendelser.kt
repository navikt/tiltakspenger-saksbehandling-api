package no.nav.tiltakspenger.saksbehandling.klage.domene

import no.nav.tiltakspenger.saksbehandling.klage.domene.hendelse.Klageinstanshendelse

data class Klageinstanshendelser(
    val value: List<Klageinstanshendelse>,
) : List<Klageinstanshendelse> by value {
    fun leggTil(hendelse: Klageinstanshendelse): Klageinstanshendelser {
        return Klageinstanshendelser(
            value = value + hendelse,
        )
    }

    init {
        value.zipWithNext { a, b -> require(b.opprettet > a.opprettet) }
    }

    companion object {
        fun empty(): Klageinstanshendelser {
            return Klageinstanshendelser(emptyList())
        }
    }
}
