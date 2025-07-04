package no.nav.tiltakspenger.saksbehandling.felles

import no.nav.tiltakspenger.libs.common.GenerellSystembruker
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerrolle
import no.nav.tiltakspenger.libs.common.GenerellSystembrukerroller

data class Systembruker(
    override val roller: Systembrukerroller,
    override val klientId: String,
    override val klientnavn: String,
) : GenerellSystembruker<Systembrukerrolle, Systembrukerroller>

enum class Systembrukerrolle : GenerellSystembrukerrolle {
    HENT_ELLER_OPPRETT_SAK,
    LAGRE_SOKNAD,
    LAGRE_MELDEKORT,
}

data class Systembrukerroller(
    override val value: Set<Systembrukerrolle>,
) : GenerellSystembrukerroller<Systembrukerrolle>,
    Set<Systembrukerrolle> by value {

    constructor(vararg roller: Systembrukerrolle) : this(roller.toSet())
    constructor(roller: Collection<Systembrukerrolle>) : this(roller.toSet())

    override fun harRolle(rolle: Systembrukerrolle): Boolean = contains(rolle)

    fun harHentEllerOpprettSak(): Boolean = value.contains(Systembrukerrolle.HENT_ELLER_OPPRETT_SAK)
    fun harLagreSoknad(): Boolean = value.contains(Systembrukerrolle.LAGRE_SOKNAD)
    fun harLagreMeldekort(): Boolean = value.contains(Systembrukerrolle.LAGRE_MELDEKORT)
}
