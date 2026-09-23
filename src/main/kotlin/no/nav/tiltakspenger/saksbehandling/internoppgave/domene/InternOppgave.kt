package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import java.time.Clock
import java.time.LocalDateTime

/**
 * @param dialog Løpende dialog om oppgaven, eldste innlegg først.
 * Dialogen kan bare utvides, og er ikke en del av versjoneringen, slik at et innlegg ikke gjør andres beslutningsgrunnlag utdatert.
 */
data class InternOppgave(
    val id: InternOppgaveId,
    val sakId: SakId,
    val grunnlag: InternOppgaveGrunnlag,
    val opprettet: LocalDateTime,
    val sistEndret: LocalDateTime,
    val versjon: Long,
    val saksbehandler: String?,
    val løsning: InternOppgaveløsning?,
    val dialog: List<Dialoginnlegg>,
) {
    val erLøst: Boolean get() = løsning != null
    val type: InternOppgavetype get() = grunnlag.type
    val nøkkel: String get() = grunnlag.nøkkel

    init {
        require(versjon >= 0) { "Versjonen kan ikke være negativ" }
        require(sistEndret >= opprettet) { "Oppgaven kan ikke være endret før den ble opprettet" }
        require(saksbehandler == null || saksbehandler.isNotBlank()) { "Saksbehandler kan ikke være blank" }
        require(!erLøst || saksbehandler != null) { "En løst oppgave må ha en saksbehandler" }
        require(løsning == null || løsning.løst == sistEndret) { "Løst må være lik sistEndret" }
        when (type) {
            InternOppgavetype.ENDRET_TILTAKSDELTAKELSE -> when (val utfall = løsning?.utfall) {
                null, InternOppgaveløsning.Utfall.Forkastet -> Unit

                is InternOppgaveløsning.Utfall.Revurdering -> require(utfall.type in type.tillatteRevurderingstyper) {
                    "Revurderingstypen er ikke tillatt for oppgavetypen"
                }
            }
        }
    }

    fun ta(saksbehandler: Saksbehandler, clock: Clock): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != null) return InternOppgaveFeil.AlleredeTildelt.left()
        return copy(
            saksbehandler = saksbehandler.navIdent,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    /**
     * Overtar en oppgave som er tildelt en annen saksbehandler.
     * En ufordelt oppgave må tas med [ta].
     */
    fun overta(saksbehandler: Saksbehandler, clock: Clock): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler == null) return InternOppgaveFeil.IkkeTildelt.left()
        if (this.saksbehandler == saksbehandler.navIdent) return InternOppgaveFeil.KanIkkeOvertaFraSegSelv.left()
        return copy(
            saksbehandler = saksbehandler.navIdent,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    fun leggTilbake(saksbehandler: Saksbehandler, clock: Clock): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != saksbehandler.navIdent) return InternOppgaveFeil.IkkeEier.left()
        return copy(
            saksbehandler = null,
            sistEndret = endringstidspunkt(clock),
            versjon = versjon + 1,
        ).right()
    }

    fun løs(
        saksbehandler: Saksbehandler,
        utfall: InternOppgaveløsning.Utfall,
        begrunnelse: Løsningsbegrunnelse,
        clock: Clock,
    ): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        if (this.saksbehandler != saksbehandler.navIdent) return InternOppgaveFeil.IkkeEier.left()
        val tidspunkt = endringstidspunkt(clock)
        return copy(
            løsning = InternOppgaveløsning(utfall, begrunnelse, tidspunkt),
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

    /**
     * Alle saksbehandlere kan skrive i dialogen så lenge oppgaven er åpen, uavhengig av hvem den er tildelt.
     * Innlegget endrer verken versjon eller sistEndret.
     */
    fun leggTilDialoginnlegg(
        saksbehandler: Saksbehandler,
        tekst: NonBlankString,
        clock: Clock,
    ): Either<InternOppgaveFeil, InternOppgave> {
        if (erLøst) return InternOppgaveFeil.OppgavenErLøst.left()
        return copy(dialog = dialog + Dialoginnlegg(saksbehandler.navIdent, nå(clock), tekst)).right()
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
                dialog = emptyList(),
            )
        }
    }
}
