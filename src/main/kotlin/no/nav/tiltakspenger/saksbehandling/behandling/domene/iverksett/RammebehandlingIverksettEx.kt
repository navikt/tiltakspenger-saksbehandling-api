package no.nav.tiltakspenger.saksbehandling.behandling.domene.iverksett

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.AVBRUTT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.KLAR_TIL_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_AUTOMATISK_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BESLUTNING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.VEDTATT
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Søknadsbehandling
import no.nav.tiltakspenger.saksbehandling.felles.Attestering
import no.nav.tiltakspenger.saksbehandling.felles.krevBeslutterRolle
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandling
import no.nav.tiltakspenger.saksbehandling.klage.domene.Klagebehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettOmgjøringKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.IverksettOpprettholdelseKommando
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.iverksettOmgjøring
import no.nav.tiltakspenger.saksbehandling.klage.domene.iverksett.iverksettOpprettholdelse
import no.nav.tiltakspenger.saksbehandling.statistikk.Statistikkhendelser
import java.time.Clock
import java.time.LocalDateTime

/**
 * Iverksetter rammebehandlingen.
 * Forutsetningene håndheves av [kanIverksette], og feilene derfra returneres som venstre-verdi.
 *
 * @return Oppdatert [Rammebehandling] som eventuelt også har en oppdatert [Klagebehandling] dersom det finnes en slik knyttet til behandlingen.
 */
fun Rammebehandling.iverksett(
    utøvendeBeslutter: Saksbehandler,
    attestering: Attestering,
    correlationId: CorrelationId,
    clock: Clock,
): Either<KanIkkeIverksetteBehandling, Pair<Rammebehandling, Statistikkhendelser>> {
    kanIverksette(utøvendeBeslutter).onLeft { return it.left() }

    val attesteringer = attesteringer.leggTil(attestering)
    val iverksattTidspunkt = nå(clock)

    val (oppdatertKlagebehandling, klagestatistikk) = iverksettKlagebehandling(correlationId, iverksattTidspunkt)

    val oppdatertRammebehandling = when (this) {
        is Søknadsbehandling -> this.copy(
            status = VEDTATT,
            attesteringer = attesteringer,
            iverksattTidspunkt = iverksattTidspunkt,
            sistEndret = iverksattTidspunkt,
            klagebehandling = oppdatertKlagebehandling,
        )

        is Revurdering -> this.copy(
            status = VEDTATT,
            attesteringer = attesteringer,
            iverksattTidspunkt = iverksattTidspunkt,
            sistEndret = iverksattTidspunkt,
            klagebehandling = oppdatertKlagebehandling,
        )
    }
    return (oppdatertRammebehandling to klagestatistikk).right()
}

/**
 * Kalles kun fra [iverksett], som allerede har verifisert forutsetningene via [kanIverksette].
 */
private fun Rammebehandling.iverksettKlagebehandling(
    correlationId: CorrelationId,
    iverksattTidspunkt: LocalDateTime,
): Pair<Klagebehandling?, Statistikkhendelser> {
    if (klagebehandling?.erFerdigstilt == true) {
        // man har mulighet til å opprette rammebehandling på en ferdigstilt klagebehandling.
        // oppdaterer resultatets rammebehandling knyttninger
        return klagebehandling!!.nullstillÅpenBehandlingId() to Statistikkhendelser.empty()
    }
    return when (klagebehandling?.resultat) {
        is Klagebehandlingsresultat.Avvist -> throw IllegalStateException("Klagebehandling med avvist resultat skal ikke være knyttet til en rammebehandling. Dette skjedde for sakId: $sakId, saksnummer: $saksnummer, behandling: ${this.id}, klagebehandlingId: ${klagebehandling!!.id}")

        is Klagebehandlingsresultat.Omgjør -> klagebehandling?.iverksettOmgjøring(
            IverksettOmgjøringKommando(
                sakId = sakId,
                klagebehandlingId = klagebehandling!!.id,
                correlationId = correlationId,
                iverksattTidspunkt = iverksattTidspunkt,
            ),
        )
            ?.getOrElse { throw IllegalStateException("Feil ved iverksetting av rammebehandling $id knyttet til klagebehandling ${klagebehandling!!.id}. Underliggende feil: $it, sakId: $sakId, saksnummer: $saksnummer") }
            ?: (null to Statistikkhendelser.empty())

        is Klagebehandlingsresultat.Opprettholdt -> klagebehandling?.iverksettOpprettholdelse(
            IverksettOpprettholdelseKommando(
                sakId = sakId,
                klagebehandlingId = klagebehandling!!.id,
                correlationId = correlationId,
                iverksattTidspunkt = iverksattTidspunkt,
            ),
        )
            ?.getOrElse { throw IllegalStateException("Feil ved iverksetting av rammebehandling $id knyttet til klagebehandling ${klagebehandling!!.id}. Underliggende feil: $it, sakId: $sakId, saksnummer: $saksnummer") }
            ?: (null to Statistikkhendelser.empty())

        null -> (null to Statistikkhendelser.empty())
    }
}

/**
 * Avgjør om [utøvendeBeslutter] kan iverksette behandlingen.
 * Krever at [utøvendeBeslutter] har rollen beslutter, og kaster [no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException] ellers.
 * Krever også at behandlingen har en vedtaksperiode, og kaster ellers, fordi det ikke er noe en saksbehandler kan treffe fra saksbehandlingsflyten.
 *
 * Betingelsene speiler hvilke tilstander [iverksett] faktisk håndterer, og sjekkes i denne rekkefølgen:
 *  - behandlingen må være [UNDER_BESLUTNING]
 *  - [utøvendeBeslutter] må være beslutteren på behandlingen
 *  - behandlingen kan ikke allerede være godkjent
 *  - behandlingen kan ikke stå på vent
 *
 * Statusen sjekkes først, fordi saksbehandleren kan ha angret sendingen til beslutning i mellomtiden.
 * Da er beslutteren fjernet fra behandlingen, og beslutteren skal få vite at behandlingen ikke lenger er under beslutning.
 */
private fun Rammebehandling.kanIverksette(utøvendeBeslutter: Saksbehandler): Either<KanIkkeIverksetteBehandling, Unit> {
    krevBeslutterRolle(utøvendeBeslutter)
    require(vedtaksperiode != null) { "vedtaksperiode må være satt ved iverksetting" }

    when (status) {
        UNDER_BESLUTNING -> Unit

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> return KanIkkeIverksetteBehandling.BehandlingenErIkkeUnderBeslutning(status).left()
    }

    if (this.beslutter != utøvendeBeslutter.navIdent) {
        return KanIkkeIverksetteBehandling.BehandlingenEiesAvAnnenBeslutter(eiesAvBeslutter = this.beslutter).left()
    }
    if (this.attesteringer.any { it.isGodkjent() }) {
        return KanIkkeIverksetteBehandling.BehandlingenErAlleredeGodkjent.left()
    }
    if (ventestatus.erSattPåVent) {
        return KanIkkeIverksetteBehandling.BehandlingenErSattPåVent.left()
    }
    return Unit.right()
}
