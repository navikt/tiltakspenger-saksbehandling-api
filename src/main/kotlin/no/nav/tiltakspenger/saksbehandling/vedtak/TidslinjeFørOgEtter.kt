package no.nav.tiltakspenger.saksbehandling.vedtak

import no.nav.tiltakspenger.libs.common.RammebehandlingId
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periodisering.PeriodeMedVerdi
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.tilPeriodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.saksbehandling.barnetillegg.AntallBarn
import no.nav.tiltakspenger.saksbehandling.behandling.domene.AntallDagerForMeldeperiode
import no.nav.tiltakspenger.saksbehandling.behandling.domene.Rammebehandling
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Omgjøringsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Revurderingsresultat
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat

/**
 * Vedtakstidslinjen innenfor vedtaksperioden til en rammebehandling, før og etter at behandlingen er iverksatt.
 * Gjør det mulig for saksbehandler å se konsekvensene av behandlingen.
 * Dager uten verdi i [før] eller [etter] gir ikke rett til tiltakspenger.
 *
 * @param periode Vedtaksperioden til behandlingen.
 * @param før Tidslinjen slik den var før behandlingen, krympet til [periode].
 * @param etter Tidslinjen slik den blir (eller ble) etter at behandlingen er iverksatt, krympet til [periode].
 */
data class TidslinjeFørOgEtter(
    val periode: Periode,
    val før: Periodisering<Tidslinjeverdi>,
    val etter: Periodisering<Tidslinjeverdi>,
) {
    /**
     * True dersom behandlingen endrer retten til tiltakspenger, antall dager per meldeperiode eller antall barn for minst én dag.
     * Hvilket vedtak eller resultat som gir retten, regnes ikke som en endring i seg selv.
     */
    val harEndring: Boolean by lazy {
        periode.tilDager().any { dag ->
            før.hentVerdiForDag(dag)?.rett != etter.hentVerdiForDag(dag)?.rett
        }
    }
}

/**
 * En sammenhengende del av tidslinjen som stammer fra én rammebehandling.
 *
 * @param rammevedtakId Null dersom behandlingen ikke er iverksatt.
 * @param antallDagerPerMeldeperiode Kun satt ved innvilgelse.
 * @param antallBarn Kun satt ved innvilgelse.
 */
data class Tidslinjeverdi(
    val rammebehandlingId: RammebehandlingId,
    val rammevedtakId: VedtakId?,
    val resultat: Tidslinjeresultat,
    val antallDagerPerMeldeperiode: AntallDagerForMeldeperiode?,
    val antallBarn: AntallBarn?,
) {
    /** Null dersom dagen ikke gir rett til tiltakspenger. */
    val rett: Pair<AntallDagerForMeldeperiode, AntallBarn>? by lazy {
        when (resultat) {
            Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE,
            Tidslinjeresultat.REVURDERING_INNVILGELSE,
            Tidslinjeresultat.OMGJØRING_INNVILGELSE,
            -> Pair(antallDagerPerMeldeperiode!!, antallBarn!!)

            Tidslinjeresultat.OMGJØRING_OPPHØR,
            Tidslinjeresultat.STANS,
            -> null
        }
    }
}

enum class Tidslinjeresultat {
    SØKNADSBEHANDLING_INNVILGELSE,
    REVURDERING_INNVILGELSE,
    OMGJØRING_INNVILGELSE,
    OMGJØRING_OPPHØR,
    STANS,
}

/**
 * Lager tidslinjen før og etter [rammebehandling] innenfor behandlingens vedtaksperiode.
 * For en iverksatt behandling er «før» basert på vedtakene som var iverksatt før behandlingens eget vedtak.
 * For en behandling under arbeid er «før» basert på alle iverksatte vedtak, og «etter» på behandlingens nåværende resultat.
 * Behandlingen overstyrer hele vedtaksperioden, unntatt ved avslag, som ikke påvirker tidslinjen.
 *
 * @return Null dersom behandlingen er avbrutt eller ikke har et resultat med vedtaksperiode.
 */
fun Rammevedtaksliste.tidslinjeFørOgEtter(rammebehandling: Rammebehandling): TidslinjeFørOgEtter? {
    if (rammebehandling.erAvbrutt) return null
    val resultat = rammebehandling.resultat ?: return null
    val vedtaksperiode = rammebehandling.vedtaksperiode ?: return null

    val egetVedtak = finnRammevedtakForBehandling(rammebehandling.id)
    val tidligereVedtak = if (egetVedtak == null) {
        vedtakUtenAvslag
    } else {
        vedtakUtenAvslag.filter { it.opprettet < egetVedtak.opprettet }
    }

    val før = tidligereVedtak
        .toTidslinje()
        .krymp(vedtaksperiode)
        .flatMapTilPeriodisering { it.verdi.rammebehandling.tilTidslinjeverdier(it.verdi.id) }
        .slåSammenTilstøtendePerioder()

    val etter = when (resultat) {
        is Søknadsbehandlingsresultat.Avslag -> før

        is Søknadsbehandlingsresultat.Innvilgelse,
        is Revurderingsresultat.Innvilgelse,
        is Revurderingsresultat.Stans,
        is Omgjøringsresultat.OmgjøringInnvilgelse,
        is Omgjøringsresultat.OmgjøringOpphør,
        -> rammebehandling.tilTidslinjeverdier(egetVedtak?.id).krymp(vedtaksperiode)

        is Omgjøringsresultat.OmgjøringIkkeValgt -> return null
    }

    return TidslinjeFørOgEtter(
        periode = vedtaksperiode,
        før = før,
        etter = etter,
    )
}

/**
 * Behandlingens påvirkning på vedtakstidslinjen innenfor vedtaksperioden.
 * Hull mellom innvilgelsesperiodene gir ikke rett, og vises som opphør ved omgjøring, slik som i den ordinære vedtakstidslinjen.
 */
private fun Rammebehandling.tilTidslinjeverdier(rammevedtakId: VedtakId?): Periodisering<Tidslinjeverdi> {
    val vedtaksperiode = this.vedtaksperiode ?: return Periodisering.empty()

    fun verdi(
        resultat: Tidslinjeresultat,
        antallDagerPerMeldeperiode: AntallDagerForMeldeperiode? = null,
        antallBarn: AntallBarn? = null,
    ) = Tidslinjeverdi(
        rammebehandlingId = this.id,
        rammevedtakId = rammevedtakId,
        resultat = resultat,
        antallDagerPerMeldeperiode = antallDagerPerMeldeperiode,
        antallBarn = antallBarn,
    )

    fun innvilgelse(tidslinjeresultat: Tidslinjeresultat): Periodisering<Tidslinjeverdi> {
        val innvilgelsesperioder = this.innvilgelsesperioder ?: return Periodisering.empty()
        return innvilgelsesperioder.periodisering.flatMapTilPeriodisering { (innvilgelsesperiodeVerdi, periode) ->
            (barnetillegg?.periodisering ?: Periodisering.empty())
                .nyPeriode(periode, AntallBarn(0))
                .map { antallBarn, _ ->
                    verdi(tidslinjeresultat, innvilgelsesperiodeVerdi.antallDagerPerMeldeperiode, antallBarn)
                }
        }.slåSammenTilstøtendePerioder()
    }

    return when (this.resultat) {
        is Søknadsbehandlingsresultat.Innvilgelse -> innvilgelse(Tidslinjeresultat.SØKNADSBEHANDLING_INNVILGELSE)

        is Revurderingsresultat.Innvilgelse -> innvilgelse(Tidslinjeresultat.REVURDERING_INNVILGELSE)

        is Omgjøringsresultat.OmgjøringInnvilgelse -> {
            val innvilget = innvilgelse(Tidslinjeresultat.OMGJØRING_INNVILGELSE)
            vedtaksperiode.trekkFra(innvilget.perioder)
                .fold(innvilget) { acc, opphørtPeriode ->
                    acc.setVerdiForDelperiode(verdi(Tidslinjeresultat.OMGJØRING_OPPHØR), opphørtPeriode)
                }
        }

        is Revurderingsresultat.Stans -> Periodisering(verdi(Tidslinjeresultat.STANS), vedtaksperiode)

        is Omgjøringsresultat.OmgjøringOpphør -> Periodisering(verdi(Tidslinjeresultat.OMGJØRING_OPPHØR), vedtaksperiode)

        is Søknadsbehandlingsresultat.Avslag,
        is Omgjøringsresultat.OmgjøringIkkeValgt,
        null,
        -> Periodisering.empty()
    }
}

/**
 * Som [Periodisering.flatMapPeriodisering], men tillater at resultatet får hull.
 * [no.nav.tiltakspenger.libs.periodisering.SammenhengendePeriodisering.flatMapPeriodisering] krever at resultatet er sammenhengende.
 */
private fun <T : Any, U : Any> Periodisering<T>.flatMapTilPeriodisering(
    transform: (PeriodeMedVerdi<T>) -> Periodisering<U>,
): Periodisering<U> {
    return perioderMedVerdi.flatMap { transform(it).krymp(it.periode).perioderMedVerdi }.tilPeriodisering()
}
