package no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling

import arrow.core.Either
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.saksbehandling.behandling.domene.FritekstTilVedtaksbrev
import no.nav.tiltakspenger.saksbehandling.felles.Attesteringer
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.felles.Begrunnelse
import no.nav.tiltakspenger.saksbehandling.meldekort.domene.meldekortbehandling.overta.KunneIkkeOvertaMeldekortbehandling
import no.nav.tiltakspenger.saksbehandling.oppfølgingsenhet.Navkontor
import no.nav.tiltakspenger.saksbehandling.utbetaling.domene.Simulering
import java.time.Clock
import java.time.LocalDateTime

/**
 * En meldekortbehandling som er avbrutt av saksbehandler,
 * eller automatisk som følge av at meldeperiodene som behandles ikke lengre gir rett til tiltakspenger.
 *
 * En avbrutt meldekortbehandling fører til at meldekort fra bruker på det tidspunktet ansees som "behandlet".
 * (TODO: bedre/mer eksplisitt måte å behandle meldekort fra bruker)
 */
data class MeldekortbehandlingAvbrutt(
    override val id: MeldekortId,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val fnr: Fnr,
    override val opprettet: LocalDateTime,
    override val simulering: Simulering?,
    override val type: MeldekortbehandlingType,
    override val saksbehandler: String?,
    override val navkontor: Navkontor,
    override val begrunnelse: Begrunnelse?,
    override val attesteringer: Attesteringer,
    override val avbrutt: Avbrutt?,
    override val sistEndret: LocalDateTime,
    override val fritekstTilVedtaksbrev: FritekstTilVedtaksbrev?,
    override val meldeperioder: Meldeperiodebehandlinger,
    override val skalSendeVedtaksbrev: Boolean,
) : Meldekortbehandling {
    override val iverksattTidspunkt = null
    override val sendtTilBeslutning = null

    override val status = MeldekortbehandlingStatus.AVBRUTT

    override val beslutter = null

    override val beløpTotal = beregning?.totalBeløp
    override val ordinærBeløp = beregning?.ordinærBeløp
    override val barnetilleggBeløp = beregning?.barnetilleggBeløp

    override fun overta(
        saksbehandler: Saksbehandler,
        clock: Clock,
    ): Either<KunneIkkeOvertaMeldekortbehandling, Meldekortbehandling> {
        throw IllegalStateException("Kan ikke overta avbrutt meldekortbehandling")
    }

    override fun taMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        throw IllegalStateException("Kan ikke tildele avbrutt meldekortbehandling")
    }

    override fun leggTilbakeMeldekortbehandling(saksbehandler: Saksbehandler, clock: Clock): Meldekortbehandling {
        throw IllegalStateException("Kan ikke legge tilbake avbrutt meldekortbehandling")
    }

    override fun oppdaterSimulering(simulering: Simulering?): Meldekortbehandling {
        throw IllegalStateException("Kan ikke oppdatere simulering på avbrutt meldekortbehandling")
    }
}
