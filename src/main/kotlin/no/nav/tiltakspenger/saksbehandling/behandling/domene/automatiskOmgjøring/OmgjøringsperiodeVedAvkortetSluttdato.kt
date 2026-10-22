package no.nav.tiltakspenger.saksbehandling.behandling.domene.automatiskOmgjøring

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.saksbehandling.behandling.domene.ManueltBehandlesGrunn
import no.nav.tiltakspenger.saksbehandling.tiltaksdeltakelse.TiltaksdeltakerId
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtak
import no.nav.tiltakspenger.saksbehandling.vedtak.Rammevedtaksliste
import java.time.LocalDate

/**
 * Perioden en automatisk omgjøring må dekke når sluttdatoen for en tiltaksdeltakelse er avkortet.
 *
 * @param vedtakVedSluttdato Vedtaket som innvilger deltakelsen på ny sluttdato.
 * Omgjøringen startes fra dette vedtaket.
 * @param vedtaksperiode Fra starten av [vedtakVedSluttdato] til slutten av det siste vedtaket som innvilger deltakelsen etter ny sluttdato.
 * Alle vedtakene som gjelder i perioden, omgjøres i sin helhet.
 */
data class OmgjøringsperiodeVedAvkortetSluttdato(
    val vedtakVedSluttdato: Rammevedtak,
    val vedtaksperiode: Periode,
)

/**
 * Finner perioden en omgjøring må dekke for at [tiltaksdeltakerId] skal være innvilget til og med [nySluttdato], og ikke etter.
 * Ny sluttdato kan være innvilget i et kronologisk tidligere vedtak enn de avkortede dagene.
 * Omgjøringen dekker da både det tidligere vedtaket, de senere vedtakene som innvilger deltakelsen etter ny sluttdato, og vedtakene mellom dem.
 * Stanser og opphør mellom dem omgjøres også, og forblir opphørt.
 *
 * Gir grunnen til at omgjøringen må behandles manuelt dersom
 * - deltakelsen ikke er innvilget i noe gjeldende vedtak,
 * - ny sluttdato ikke er før siste innvilgede dag,
 * - ny sluttdato ikke er innvilget for deltakelsen, f.eks. fordi den er før deltakelsen er innvilget eller i en stans,
 * - et vedtak som gjelder i perioden, også gjelder utenfor den, slik at bare deler av det ville blitt omgjort,
 * - eller perioden går over dager uten vedtak, som ikke kan opphøres.
 */
fun Rammevedtaksliste.finnOmgjøringsperiodeVedAvkortetSluttdato(
    tiltaksdeltakerId: TiltaksdeltakerId,
    nySluttdato: LocalDate,
): Either<ManueltBehandlesGrunn, OmgjøringsperiodeVedAvkortetSluttdato> {
    val innvilgetForDeltakelse: List<Periode> = innvilgelsesperioder.perioderMedVerdi
        .filter { it.verdi.valgtTiltaksdeltakelse.internDeltakelseId == tiltaksdeltakerId }
        .map { it.periode }

    val sisteInnvilgedeDag = innvilgetForDeltakelse.lastOrNull()?.tilOgMed
        ?: return ManueltBehandlesGrunn.OMGJORING_DELTAKELSEN_ER_IKKE_INNVILGET.left()

    if (!nySluttdato.isBefore(sisteInnvilgedeDag)) {
        return ManueltBehandlesGrunn.OMGJORING_SLUTTDATO_ER_IKKE_FOR_INNVILGELSEN_SLUTTER.left()
    }

    if (innvilgetForDeltakelse.none { it.inneholder(nySluttdato) }) {
        return ManueltBehandlesGrunn.OMGJORING_SLUTTDATO_ER_IKKE_INNVILGET.left()
    }

    val vedtakVedSluttdato = tidslinje.hentVerdiForDag(nySluttdato)!!
    val vedtakEtterSluttdato = innvilgetForDeltakelse
        .mapNotNull { it.overlappendePeriode(nySluttdato.plusDays(1) til sisteInnvilgedeDag) }
        .flatMap { avkortet -> tidslinje.krymp(avkortet).perioderMedVerdi.map { it.verdi } }

    val vedtaksperiode = vedtakVedSluttdato.gjeldendeTotalPeriode!!.fraOgMed til
        vedtakEtterSluttdato.maxOf { it.gjeldendeTotalPeriode!!.tilOgMed }

    val vedtakSomOmgjøres = finnVedtakSomOmgjøres(vedtaksperiode)

    if (vedtakSomOmgjøres.rammevedtakIDer.any { !vedtaksperiode.inneholderHele(hentRammevedtakForId(it).gjeldendeTotalPeriode!!) }) {
        return ManueltBehandlesGrunn.OMGJORING_OMGJOR_DELER_AV_VEDTAK.left()
    }

    if (vedtaksperiode.trekkFra(vedtakSomOmgjøres.perioder).isNotEmpty()) {
        return ManueltBehandlesGrunn.OMGJORING_VEDTAKSPERIODEN_HAR_DAGER_UTEN_VEDTAK.left()
    }

    return OmgjøringsperiodeVedAvkortetSluttdato(
        vedtakVedSluttdato = vedtakVedSluttdato,
        vedtaksperiode = vedtaksperiode,
    ).right()
}
