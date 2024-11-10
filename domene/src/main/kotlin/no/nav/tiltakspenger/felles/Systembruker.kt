package no.nav.tiltakspenger.felles

import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller

/**
 * @param brukernavn Brukernavn til systembruker (azp_name for Entra ID). Kan ikke brukes til autentisering.
 */
data class Systembruker(
    override val brukernavn: String,
    override val roller: Systembrukerroller,
) : GenerellSystembruker<Systembrukerrolle, Systembrukerroller>

enum class Systembrukerrolle : GenerellSystembrukerrolle {
    LAGE_HENDELSER,
    HENTE_DATA,
}

data class Systembrukerroller(
    override val value: Set<Systembrukerrolle>,
) : GenerellSystembrukerroller<Systembrukerrolle>, Set<Systembrukerrolle> by value {

    constructor(vararg roller: Systembrukerrolle) : this(roller.toSet())
    constructor(roller: Collection<Systembrukerrolle>) : this(roller.toSet())

    override fun harRolle(rolle: Systembrukerrolle): Boolean = contains(rolle)

    fun harLageHendelser(): Boolean = value.contains(Systembrukerrolle.LAGE_HENDELSER)
    fun harHenteData(): Boolean = value.contains(Systembrukerrolle.HENTE_DATA)
}
