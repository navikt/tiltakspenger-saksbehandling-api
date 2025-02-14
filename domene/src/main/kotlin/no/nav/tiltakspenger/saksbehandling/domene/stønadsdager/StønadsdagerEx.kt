package no.nav.tiltakspenger.saksbehandling.domene.stønadsdager

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.saksbehandling.domene.tiltak.Tiltaksdeltagelse

fun Tiltaksdeltagelse.tilStønadsdagerRegisterSaksopplysning(
    maksAntallDagerPerMeldeperiode: Int = 14,
): StønadsdagerSaksopplysning.Register {
    return StønadsdagerSaksopplysning.Register(
        tiltakNavn = typeNavn,
        eksternDeltagelseId = eksternDeltagelseId,
        gjennomføringId = gjennomføringId,
        antallDager = maksAntallDagerPerMeldeperiode,
        periode = deltakelsesperiode,
        kilde = kilde,
        tidsstempel = nå(),
    )
}
