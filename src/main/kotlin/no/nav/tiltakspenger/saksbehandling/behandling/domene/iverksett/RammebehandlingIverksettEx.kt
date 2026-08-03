package no.nav.tiltakspenger.saksbehandling.behandling.domene.iverksett

import arrow.core.getOrElse
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
 * Forutsetningene håndheves av [krevKanIverksette], som kaster dersom de ikke er oppfylt.
 *
 * @return Oppdatert [Rammebehandling] som eventuelt også har en oppdatert [Klagebehandling] dersom det finnes en slik knyttet til behandlingen.
 */
fun Rammebehandling.iverksett(
    utøvendeBeslutter: Saksbehandler,
    attestering: Attestering,
    correlationId: CorrelationId,
    clock: Clock,
): Pair<Rammebehandling, Statistikkhendelser> {
    krevKanIverksette(utøvendeBeslutter)

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
    return oppdatertRammebehandling to klagestatistikk
}

/**
 * Kalles kun fra [iverksett], som allerede har verifisert forutsetningene via [krevKanIverksette].
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
 * Krever at [utøvendeBeslutter] kan iverksette behandlingen, og kaster ellers.
 *
 * Betingelsene speiler hvilke tilstander [iverksett] faktisk håndterer:
 *  - behandlingen må ha en vedtaksperiode
 *  - behandlingen må være [UNDER_BESLUTNING]
 *  - [utøvendeBeslutter] må ha beslutterrollen og være beslutteren på behandlingen
 *  - behandlingen kan ikke allerede være godkjent
 *  - behandlingen kan ikke stå på vent
 *
 * Kaster i stedet for å returnere en venstre-verdi, fordi tilstandene her ikke er noe en saksbehandler kan treffe fra saksbehandlingsflyten.
 */
private fun Rammebehandling.krevKanIverksette(utøvendeBeslutter: Saksbehandler) {
    require(vedtaksperiode != null) { "vedtaksperiode må være satt ved iverksetting" }

    when (status) {
        UNDER_BESLUTNING -> Unit

        KLAR_TIL_BEHANDLING,
        UNDER_BEHANDLING,
        KLAR_TIL_BESLUTNING,
        VEDTATT,
        AVBRUTT,
        UNDER_AUTOMATISK_BEHANDLING,
        -> throw IllegalStateException(
            "Må ha status UNDER_BESLUTNING for å iverksette. Behandlingsstatus: $status",
        )
    }

    krevBeslutterRolle(utøvendeBeslutter)
    check(this.beslutter == utøvendeBeslutter.navIdent) { "Kan ikke iverksette en behandling man ikke er beslutter på" }
    check(!this.attesteringer.any { it.isGodkjent() }) {
        "Behandlingen er allerede godkjent"
    }
    check(!ventestatus.erSattPåVent) { "Behandlingen må gjenopptas før den kan iverksettes." }
}
