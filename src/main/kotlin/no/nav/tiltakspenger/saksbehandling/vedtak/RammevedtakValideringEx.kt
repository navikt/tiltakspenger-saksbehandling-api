package no.nav.tiltakspenger.saksbehandling.vedtak

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.libs.common.VedtakId
import no.nav.tiltakspenger.libs.periode.overlappendePerioder
import no.nav.tiltakspenger.libs.periodisering.toTidslinje
import no.nav.tiltakspenger.saksbehandling.behandling.domene.resultat.Søknadsbehandlingsresultat
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjortAvRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.OmgjørRammevedtak
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsgrad
import no.nav.tiltakspenger.saksbehandling.omgjøring.Omgjøringsperiode

sealed interface RammevedtakValideringFeil {
    val vedtakId: VedtakId

    data class UgyldigOmgjørRammevedtak(
        override val vedtakId: VedtakId,
        val forventet: OmgjørRammevedtak,
        val faktisk: OmgjørRammevedtak,
    ) : RammevedtakValideringFeil {
        override fun toString(): String =
            "Ugyldig [omgjørRammevedtak] på vedtak $vedtakId. Forventet omgjøringsdata: $forventet, men fant: $faktisk"
    }

    data class UgyldigOmgjortAvRammevedtak(
        override val vedtakId: VedtakId,
        val forventet: OmgjortAvRammevedtak,
        val faktisk: OmgjortAvRammevedtak,
    ) : RammevedtakValideringFeil {
        override fun toString(): String =
            "Ugyldig [omgjortAvRammevedtak] på vedtak $vedtakId. Forventet: $forventet, men fant: $faktisk"
    }
}

fun Rammevedtaksliste.validerOmgjøringerVedNyttVedtak(vedtak: Rammevedtak): Either<RammevedtakValideringFeil, Unit> {
    if (vedtak.rammebehandlingsresultat is Søknadsbehandlingsresultat.Avslag) {
        return Unit.right()
    }

    return this.vedtakUtenAvslag.leggTil(vedtak).validerOmgjøringer()
}

fun List<Rammevedtak>.validerOmgjøringer(): Either<RammevedtakValideringFeil, Unit> {
    this.forEachIndexed { index, vedtak ->
        val tidslinjeFørDetteVedtaket = this.take(index).toTidslinje()
        val overlapp = tidslinjeFørDetteVedtaket.overlappendePeriode(vedtak.periode)
        val omgjør = OmgjørRammevedtak(
            overlapp.perioderMedVerdi.map {
                Omgjøringsperiode(
                    rammevedtakId = it.verdi.id,
                    periode = it.periode,
                    omgjøringsgrad = if (it.verdi.periode == it.periode) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                )
            },
        )
        if (vedtak.omgjørRammevedtak != omgjør) {
            return RammevedtakValideringFeil.UgyldigOmgjørRammevedtak(
                vedtakId = vedtak.id,
                forventet = omgjør,
                faktisk = vedtak.omgjørRammevedtak,
            ).left()
        }

        val alleSenereVedtak = this.drop(index + 1)

        val omgjortAvRammevedtak = alleSenereVedtak.fold(OmgjortAvRammevedtak.empty) { acc, senereVedtak ->
            val perioderSomOmgjøres = senereVedtak.periode
                .trekkFra(acc.perioder)
                .overlappendePerioder(listOf(vedtak.periode))

            perioderSomOmgjøres.map {
                Omgjøringsperiode(
                    rammevedtakId = senereVedtak.id,
                    periode = it,
                    omgjøringsgrad = if (vedtak.periode == it) Omgjøringsgrad.HELT else Omgjøringsgrad.DELVIS,
                )
            }.let { acc.leggTil(it) }
        }

        if (vedtak.omgjortAvRammevedtak != omgjortAvRammevedtak) {
            return RammevedtakValideringFeil.UgyldigOmgjortAvRammevedtak(
                vedtakId = vedtak.id,
                forventet = omgjortAvRammevedtak,
                faktisk = vedtak.omgjortAvRammevedtak,
            ).left()
        }
    }

    return Unit.right()
}
