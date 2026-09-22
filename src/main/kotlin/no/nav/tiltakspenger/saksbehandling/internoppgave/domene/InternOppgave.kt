package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock
import java.time.LocalDateTime

data class InternOppgave(
    val id: InternOppgaveId,
    val sakId: SakId,
    val grunnlag: InternOppgaveGrunnlag,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val versjon: Long,
    val saksbehandler: String?,
    val løsning: InternOppgaveløsning?,
    val løst: LocalDateTime?,
) {
    val erLøst: Boolean get() = løsning != null
    val type: InternOppgavetype get() = grunnlag.type
    val nøkkel: String get() = grunnlag.nøkkel

    init {
        require(versjon >= 0) { "Versjonen kan ikke være negativ" }
        require(sistEndret >= opprettet) { "Oppgaven kan ikke være endret før den ble opprettet" }
        require((løsning == null) == (løst == null)) { "Løsning og løst må være satt sammen" }
        require(saksbehandler == null || saksbehandler.isNotBlank()) { "Saksbehandler kan ikke være blank" }
        require(!erLøst || saksbehandler != null) { "En løst oppgave må ha en saksbehandler" }
        require(løst == null || løst == sistEndret) { "Løst må være lik sistEndret" }
        when (type) {
            InternOppgavetype.ENDRET_TILTAKSDELTAKELSE -> when (løsning) {
                null, InternOppgaveløsning.Forkastet -> Unit

                is InternOppgaveløsning.Revurdering -> require(løsning.type in type.tillatteRevurderingstyper) {
                    "Revurderingstypen er ikke tillatt for oppgavetypen"
                }
            }
        }
    }

    fun ta(saksbehandler: String, clock: Clock): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != null) return InternOppgaveFeil.AlleredeTildelt.left()
        require(saksbehandler.isNotBlank()) { "Saksbehandler kan ikke være blank" }
        return copy(
            saksbehandler = saksbehandler,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    fun leggTilbake(saksbehandler: String, clock: Clock): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != saksbehandler) return InternOppgaveFeil.IkkeEier.left()
        return copy(
            saksbehandler = null,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    fun løs(
        saksbehandler: String,
        løsning: InternOppgaveløsning,
        clock: Clock,
    ): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != saksbehandler) return InternOppgaveFeil.IkkeEier.left()
        val tidspunkt = endringstidspunkt(clock)
        return copy(
            løsning = løsning,
            løst = tidspunkt,
            sistEndret = tidspunkt,
            versjon = versjon + 1,
        ).right()
    }

    fun oppdaterGrunnlag(
        grunnlag: InternOppgaveGrunnlag,
        clock: Clock,
    ): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (grunnlag.type != type || grunnlag.nøkkel != nøkkel) return InternOppgaveFeil.AnnetGrunnlag.left()
        return copy(
            grunnlag = grunnlag,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    private fun endringstidspunkt(clock: Clock): LocalDateTime = nå(clock).also {
        require(it >= sistEndret) { "Endringstidspunktet kan ikke være før forrige endring" }
    }

    companion object {
        fun opprett(sakId: SakId, grunnlag: InternOppgaveGrunnlag, clock: Clock): InternOppgave {
            val tidspunkt = nå(clock)
            return InternOppgave(
                id = InternOppgaveId.random(),
                sakId = sakId,
                grunnlag = grunnlag,
                opprettet = tidspunkt,
                sistEndret = tidspunkt,
                versjon = 0,
                saksbehandler = null,
                løsning = null,
                løst = null,
            )
        }
    }
}
