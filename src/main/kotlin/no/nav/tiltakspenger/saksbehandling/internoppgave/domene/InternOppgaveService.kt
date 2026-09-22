package no.nav.tiltakspenger.saksbehandling.internoppgave.domene

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext
import java.time.Clock

/**
 * Intern modulgrense som ennå ikke er koblet til jobber eller routes.
 * Tilgangskontroll og opprettelse av revurdering må håndteres av fremtidige kallere.
 */
class InternOppgaveService(
    private val repo: InternOppgaveRepo,
    private val clock: Clock,
) {
    /**
     * Oppdaterer grunnlaget uten å frata en saksbehandler oppgaven.
     * Ved samtidighetskonflikt må systemkalleren lese på nytt og vurdere oppdateringen igjen.
     */
    fun opprettEllerOppdater(
        sakId: SakId,
        grunnlag: InternOppgaveGrunnlag,
        sessionContext: SessionContext? = null,
    ): Either<InternOppgaveFeil, InternOppgave> {
        val eksisterende = repo.hentÅpen(sakId, grunnlag.type, grunnlag.nøkkel, sessionContext)
        if (eksisterende == null) {
            val ny = InternOppgave.opprett(sakId, grunnlag, clock)
            return if (repo.opprett(ny, sessionContext)) ny.right() else InternOppgaveFeil.OppgavenErEndret.left()
        }
        val oppdatert = eksisterende.oppdaterGrunnlag(grunnlag, clock).getOrElse { return it.left() }
        return lagre(oppdatert, eksisterende.versjon, sessionContext)
    }

    fun ta(
        id: InternOppgaveId,
        forventetVersjon: Long,
        saksbehandler: String,
        sessionContext: SessionContext? = null,
    ): Either<InternOppgaveFeil, InternOppgave> =
        endre(id, forventetVersjon, sessionContext) { it.ta(saksbehandler, clock) }

    fun leggTilbake(
        id: InternOppgaveId,
        forventetVersjon: Long,
        saksbehandler: String,
        sessionContext: SessionContext? = null,
    ): Either<InternOppgaveFeil, InternOppgave> =
        endre(id, forventetVersjon, sessionContext) { it.leggTilbake(saksbehandler, clock) }

    /**
     * Registrerer utfallet, men oppretter ikke en behandling.
     * En fremtidig revurderingsflyt må validere behandlingen og lagre begge endringene i samme transaksjon.
     */
    fun løs(
        id: InternOppgaveId,
        forventetVersjon: Long,
        saksbehandler: String,
        løsning: InternOppgaveløsning,
        sessionContext: SessionContext? = null,
    ): Either<InternOppgaveFeil, InternOppgave> =
        endre(id, forventetVersjon, sessionContext) { it.løs(saksbehandler, løsning, clock) }

    private fun endre(
        id: InternOppgaveId,
        forventetVersjon: Long,
        sessionContext: SessionContext?,
        operasjon: (InternOppgave) -> Either<InternOppgaveFeil, InternOppgave>,
    ): Either<InternOppgaveFeil, InternOppgave> {
        val oppgave = repo.hent(id, sessionContext) ?: return InternOppgaveFeil.FantIkkeOppgave.left()
        if (oppgave.versjon != forventetVersjon) return InternOppgaveFeil.OppgavenErEndret.left()
        val oppdatert = operasjon(oppgave).getOrElse { return it.left() }
        return lagre(oppdatert, forventetVersjon, sessionContext)
    }

    private fun lagre(
        oppgave: InternOppgave,
        forventetVersjon: Long,
        sessionContext: SessionContext?,
    ): Either<InternOppgaveFeil, InternOppgave> =
        if (repo.oppdater(oppgave, forventetVersjon, sessionContext)) {
            oppgave.right()
        } else {
            InternOppgaveFeil.OppgavenErEndret.left()
        }
}
