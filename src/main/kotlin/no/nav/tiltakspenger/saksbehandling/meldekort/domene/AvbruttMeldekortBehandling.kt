package no.nav.tiltakspenger.saksbehandling.meldekort.domene

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.MeldekortBehandlingStatus.IKKE_RETT_TIL_TILTAKSPENGER
import no.nav.tiltakspenger.saksbehandling.meldekort.service.overta.KunneIkkeOvertaMeldekortBehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.LocalDateTime

data class AvbruttMeldekortBehandling(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val dager: MeldekortDager,
    override val beregning: MeldekortBeregning?,
    override val simulering: Simulering?,
    override val meldeperiode: Meldeperiode,
    override val type: MeldekortBehandlingType,
    override val brukersMeldekort: BrukersMeldekort?,
    override val saksbehandler: String?,
    override val navkontor: Navkontor,
    override val begrunnelse: MeldekortBehandlingBegrunnelse?,
    override val attesteringer: Attesteringer,
    override val ikkeRettTilTiltakspengerTidspunkt: LocalDateTime?,
    override val avbrutt: Avbrutt?,
) : MeldekortBehandling {
    override val iverksattTidspunkt = null
    override val sendtTilBeslutning = null

    override val status = if (ikkeRettTilTiltakspengerTidspunkt == null) AVBRUTT else IKKE_RETT_TIL_TILTAKSPENGER

    override val beslutter = null

    override val beløpTotal = beregning?.totalBeløp
    override val ordinærBeløp = beregning?.ordinærBeløp
    override val barnetilleggBeløp = beregning?.barnetilleggBeløp

    override fun overta(saksbehandler: Saksbehandler): Either<KunneIkkeOvertaMeldekortBehandling, MeldekortBehandling> {
        throw IllegalStateException("Kan ikke overta avbrutt meldekortbehandling")
    }

    override fun taMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        throw IllegalStateException("Kan ikke tildele avbrutt meldekortbehandling")
    }

    override fun leggTilbakeMeldekortBehandling(saksbehandler: Saksbehandler): MeldekortBehandling {
        throw IllegalStateException("Kan ikke legge tilbake avbrutt meldekortbehandling")
    }
}
