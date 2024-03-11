package no.nav.tiltakspenger.felles

data class Saksbehandler(
    val navIdent: String,
    override val brukernavn: String,
    val epost: String,
    override val roller: List<Rolle>,
) : Bruker {

    fun isAdmin() = roller.contains(Rolle.ADMINISTRATOR)
    fun isSaksbehandler() = roller.contains(Rolle.BESLUTTER)
    fun isBeslutter() = roller.contains(Rolle.BESLUTTER)
}
