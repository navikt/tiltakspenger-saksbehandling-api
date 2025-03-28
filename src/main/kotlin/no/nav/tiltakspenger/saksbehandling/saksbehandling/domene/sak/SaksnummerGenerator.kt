package no.nav.tiltakspenger.saksbehandling.saksbehandling.domene.sak

import java.time.LocalDate

interface SaksnummerGenerator {
    fun generer(dato: LocalDate = LocalDate.now()): Saksnummer

    data object Prod : SaksnummerGenerator {
        override fun generer(dato: LocalDate): Saksnummer {
            return Saksnummer.genererSaknummer(dato = dato, løpenr = "0001")
        }
    }

    data object Dev : SaksnummerGenerator {
        override fun generer(dato: LocalDate): Saksnummer {
            return Saksnummer.genererSaknummer(dato = dato, løpenr = "1001")
        }
    }

    data object Local : SaksnummerGenerator {
        override fun generer(dato: LocalDate): Saksnummer {
            return Saksnummer.genererSaknummer(dato = dato, løpenr = "1001")
        }
    }
}
