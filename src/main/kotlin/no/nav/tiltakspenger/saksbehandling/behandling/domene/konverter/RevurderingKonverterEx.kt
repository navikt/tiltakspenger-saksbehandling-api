package no.nav.tiltakspenger.saksbehandling.behandling.domene.konverter

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandlingsstatus.UNDER_BEHANDLING
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.forlengelse.ForlengerRammevedtak
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.felles.krevSaksbehandlerRolle
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import java.time.Clock

/**
 * Konverterer revurderingen i [kommando] til en annen revurderingstype.
 * Resultatet nullstilles til det en ny revurdering av typen ville startet med, og beregning og simulering fjernes.
 * Saksopplysningene, begrunnelsen og fritekst til vedtaksbrevet beholdes.
 * Krever at saksbehandleren har rollen saksbehandler, og kaster [no.nav.tiltakspenger.saksbehandling.felles.exceptions.TilgangException] ellers.
 * Forutsetningene håndheves av [kanKonvertere], og feilene derfra returneres som venstre-verdi.
 */
fun Sak.konverterRevurdering(
    kommando: KonverterRevurderingKommando,
    clock: Clock,
): Either<KanIkkeKonvertereRevurdering, Pair<Sak, Revurdering>> {
    krevSaksbehandlerRolle(kommando.saksbehandler)
    val behandling = hentRammebehandling(kommando.behandlingId)!!
    behandling.kanKonvertere(kommando.saksbehandler).onLeft { return it.left() }
    behandling as Revurdering

    if (behandling.resultat.harSammeTypeSom(kommando)) {
        return KanIkkeKonvertereRevurdering.HarAlleredeTypen.left()
    }

    val nyttResultat: Revurderingsresultat = when (kommando) {
        is KonverterRevurderingKommando.TilStans -> Revurderingsresultat.Stans.empty

        is KonverterRevurderingKommando.TilInnvilgelse -> Revurderingsresultat.Innvilgelse.empty

        is KonverterRevurderingKommando.TilOmgjøring -> Omgjøringsresultat.OmgjøringIkkeValgt(OmgjørRammevedtak.empty)

        is KonverterRevurderingKommando.TilForlengelse -> {
            val vedtakId = kommando.vedtakIdSomForlenges
            val forlengelseskommando = rammevedtaksliste.find { it.id == vedtakId }
                ?.let { rammevedtaksliste.gyldigForlengelseskommando(it) }
                ?: return KanIkkeKonvertereRevurdering.VedtaketKanIkkeForlenges(vedtakId).left()

            Revurderingsresultat.Forlengelse.opprett(
                ForlengerRammevedtak(
                    rammevedtakId = vedtakId,
                    forlengesFraOgMed = forlengelseskommando.forlengesFraOgMed,
                ),
            )
        }
    }

    val konvertert = behandling.copy(
        resultat = nyttResultat,
        utbetaling = null,
        utbetalingskontroll = null,
        sistEndret = nå(clock),
    )
    return Pair(oppdaterRammebehandling(konvertert), konvertert).right()
}

/**
 * Avgjør om [saksbehandler] kan konvertere behandlingen til en annen revurderingstype.
 * Bare revurderinger [UNDER_BEHANDLING] kan konverteres, og bare av den tildelte saksbehandleren.
 * En behandling som er satt på vent kan ikke konverteres, heller ikke etter at den er tatt igjen uten å være gjenopptatt.
 * Revurderinger som er knyttet til en klagebehandling kan ikke konverteres.
 * Kalles også fra DTO-mappingen for å avgjøre om kommandoen skal tilbys, og må derfor ikke kaste.
 */
fun Rammebehandling.kanKonvertere(saksbehandler: Saksbehandler): Either<KanIkkeKonvertereRevurdering, Unit> {
    return when {
        this !is Revurdering -> KanIkkeKonvertereRevurdering.ErIkkeRevurdering.left()
        !saksbehandler.erSaksbehandler -> KanIkkeKonvertereRevurdering.MåVæreSaksbehandler.left()
        status != UNDER_BEHANDLING -> KanIkkeKonvertereRevurdering.MåVæreUnderBehandling(status).left()
        this.saksbehandler != saksbehandler.navIdent -> KanIkkeKonvertereRevurdering.EiesAvAnnenSaksbehandler(this.saksbehandler).left()
        ventestatus.erSattPåVent -> KanIkkeKonvertereRevurdering.ErSattPåVent.left()
        klagebehandling != null -> KanIkkeKonvertereRevurdering.HarKlagebehandling.left()
        else -> Unit.right()
    }
}

private fun Revurderingsresultat.harSammeTypeSom(kommando: KonverterRevurderingKommando): Boolean = when (kommando) {
    is KonverterRevurderingKommando.TilStans -> this is Revurderingsresultat.Stans
    is KonverterRevurderingKommando.TilInnvilgelse -> this is Revurderingsresultat.Innvilgelse
    is KonverterRevurderingKommando.TilOmgjøring -> this is Omgjøringsresultat
    is KonverterRevurderingKommando.TilForlengelse -> this is Revurderingsresultat.Forlengelse
}
