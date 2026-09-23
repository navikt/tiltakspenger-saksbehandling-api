package no.nav.tiltakspenger.saksbehandling.oppgave

import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.persistering.domene.SessionContext

interface EksternOppgaveRepo {
    /**
     * Bevarer første registrering og grunnlag dersom oppgave-ID-en allerede finnes.
     * Oppgavetjenesten kan returnere en eksisterende ID ved duplikatsøk.
     */
    fun lagre(eksternOppgave: EksternOppgave, sessionContext: SessionContext? = null)

    /** Henter sakens referanser sortert stigende på registreringstidspunkt og deretter oppgave-ID. */
    fun hentForSakId(sakId: SakId): List<LagretEksternOppgave>
}
