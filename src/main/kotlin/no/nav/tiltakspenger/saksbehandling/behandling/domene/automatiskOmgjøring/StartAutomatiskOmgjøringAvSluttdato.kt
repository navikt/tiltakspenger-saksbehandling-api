package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring

import no.nav.tiltakspenger.libs.common.CorrelationId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Revurdering
import no.nav.tiltakspenger.saksbehandling.behandling.domene.saksopplysninger.HentSaksopplysninger
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.domene.AutomatiskOpprettetRevurderingGrunn
import java.time.Clock

/**
 * Starter en omgjøring av vedtaket [omgjørVedtakId] som behandles av den automatiske saksbehandleren, se [Revurdering.opprettAutomatiskBehandletOmgjøring].
 * Vedtaket er bare utgangspunktet, og vedtaksperioden som utledes kan også omgjøre senere vedtak, se [finnOmgjøringsperiodeVedAvkortetSluttdato].
 * Brukes når sluttdatoen for en tiltaksdeltakelse er avkortet, mens deltakelsen fortsatt har status deltar, se [utledAutomatiskOmgjøringAvSluttdato].
 */
suspend fun Sak.startAutomatiskOmgjøringAvSluttdato(
    omgjørVedtakId: VedtakId,
    automatiskOpprettetGrunn: AutomatiskOpprettetRevurderingGrunn,
    correlationId: CorrelationId,
    clock: Clock,
    hentSaksopplysninger: HentSaksopplysninger,
): Pair<Sak, Revurdering> {
    val saksopplysninger = hentSaksopplysninger(
        fnr,
        correlationId,
        tiltaksdeltakelserDetErSøktTiltakspengerFor,
        tiltaksdeltakelserDetErSøktTiltakspengerFor.map { it.søknadstiltak.tiltaksdeltakerId }.distinct(),
        false,
        id,
    )
    val revurdering = Revurdering.opprettAutomatiskBehandletOmgjøring(
        saksopplysninger = saksopplysninger,
        omgjørRammevedtak = rammevedtaksliste.hentRammevedtakForId(omgjørVedtakId),
        opprettet = nå(clock),
        automatiskOpprettetGrunn = automatiskOpprettetGrunn,
    )
    return Pair(leggTilRevurdering(revurdering), revurdering)
}
