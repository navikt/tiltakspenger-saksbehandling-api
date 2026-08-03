package no.nav.tiltakspenger.saksbehandling.behandling.domene.avbryt

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.NonBlankString
import no.nav.tiltakspenger.libs.common.Saksbehandler
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
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.klage.domene.vurder.fjernBehandlingId
import java.time.LocalDateTime

/**
 * Avbryter rammebehandlingen.
 * Forutsetningene håndheves av [kanAvbryte], og feilene derfra returneres som venstre-verdi.
 * @param skalAvbryteSøknad Kun relevant for søknadsbehandling.
 * Avbryter også den underliggende søknaden dersom den er true.
 */
fun Rammebehandling.avbryt(
    avbruttAv: Saksbehandler,
    begrunnelse: NonBlankString,
    tidspunkt: LocalDateTime,
    skalAvbryteSøknad: Boolean,
): Either<KunneIkkeAvbryteBehandling, Rammebehandling> {
    kanAvbryte(avbruttAv).onLeft { return it.left() }

    val avbrutt = Avbrutt(
        tidspunkt = tidspunkt,
        saksbehandler = avbruttAv.navIdent,
        begrunnelse = begrunnelse,
    )
    val oppdatertKlagebehandling = klagebehandling?.fjernBehandlingId(
        behandlingId = id,
        saksbehandler = avbruttAv,
        sistEndret = tidspunkt,
    )

    return when (this) {
        is Søknadsbehandling -> this.copy(
            status = AVBRUTT,
            søknad = if (skalAvbryteSøknad) this.søknad.avbryt(avbruttAv, begrunnelse, tidspunkt) else this.søknad,
            avbrutt = avbrutt,
            sistEndret = tidspunkt,
            klagebehandling = oppdatertKlagebehandling,
        )

        is Revurdering -> this.copy(
            status = AVBRUTT,
            avbrutt = avbrutt,
            sistEndret = tidspunkt,
            klagebehandling = oppdatertKlagebehandling,
        )
    }.right()
}

/**
 * Avgjør om [avbruttAv] kan avbryte behandlingen i gjeldende tilstand.
 *
 * Behandlingen kan avbrytes i alle aktive tilstander.
 * I beslutningstilstandene kreves det i tillegg at [avbruttAv] er den som er tildelt behandlingen:
 *  - [KLAR_TIL_BESLUTNING]: må være tildelt saksbehandler.
 *  - [UNDER_BESLUTNING]: må være tildelt beslutter.
 */
fun Rammebehandling.kanAvbryte(avbruttAv: Saksbehandler): Either<KunneIkkeAvbryteBehandling, Unit> {
    return when (status) {
        UNDER_AUTOMATISK_BEHANDLING, KLAR_TIL_BEHANDLING, UNDER_BEHANDLING -> Unit.right()

        KLAR_TIL_BESLUTNING -> {
            if (saksbehandler != avbruttAv.navIdent) {
                KunneIkkeAvbryteBehandling.MåVæreSaksbehandlerPåBehandlingen(id).left()
            } else {
                Unit.right()
            }
        }

        UNDER_BESLUTNING -> {
            if (beslutter != avbruttAv.navIdent) {
                KunneIkkeAvbryteBehandling.MåVæreBeslutterPåBehandlingen(id).left()
            } else {
                Unit.right()
            }
        }

        VEDTATT, AVBRUTT -> KunneIkkeAvbryteBehandling.BehandlingKanIkkeAvbrytesITilstanden(
            behandlingId = id,
            status = status,
        ).left()
    }
}
