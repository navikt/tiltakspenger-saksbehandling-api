package no.nav.tiltakspenger.meldekort.domene

import no.nav.tiltakspenger.felles.nå
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.HendelseVersjon
import no.nav.tiltakspenger.libs.common.MeldeperiodeId
import no.nav.tiltakspenger.libs.common.MeldeperiodeKjedeId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.saksbehandling.domene.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.domene.vilkår.Utfallsperiode
import java.time.LocalDate
import java.time.LocalDateTime

data class Meldeperiode(
    val id: MeldeperiodeId,
    // TODO Anders: Rename meldeperiodeKjedeId til kjedeId
    val meldeperiodeKjedeId: MeldeperiodeKjedeId,
    val versjon: HendelseVersjon,
    val periode: Periode,
    val opprettet: LocalDateTime,
    val sakId: SakId,
    val saksnummer: Saksnummer,
    val fnr: Fnr,
    /** Dette gjelder hele perioden. TODO rename: Noen med fungerende IDE, kan rename denne til maksAntallDagerForPeriode */
    val antallDagerForPeriode: Int,
    val girRett: Map<LocalDate, Boolean>,
    val sendtTilMeldekortApi: LocalDateTime?,
) {
    fun helePeriodenErSperret(): Boolean {
        return girRett.values.toList().all { !it }
    }

    // TODO Anders: når skal vi tillate at meldekortet fylles ut? Siste fredag i perioden?
    fun erKlarTilUtfylling(): Boolean {
        return periode.fraOgMed <= nå().toLocalDate()
    }

    val ingenDagerGirRett = girRett.values.none { it }

    companion object {
        fun opprettMeldeperiode(
            periode: Periode,
            utfallsperioder: Periodisering<Utfallsperiode>,
            fnr: Fnr,
            saksnummer: Saksnummer,
            sakId: SakId,
            antallDagerForPeriode: Int,
            versjon: HendelseVersjon = HendelseVersjon.ny(),
        ): Meldeperiode {
            val meldeperiode = Meldeperiode(
                meldeperiodeKjedeId = MeldeperiodeKjedeId.fraPeriode(periode),
                id = MeldeperiodeId.random(),
                fnr = fnr,
                saksnummer = saksnummer,
                sakId = sakId,
                antallDagerForPeriode = antallDagerForPeriode,
                periode = periode,
                opprettet = nå(),
                versjon = versjon,
                girRett = periode.tilDager().associateWith {
                    (utfallsperioder.hentVerdiForDag(it) == Utfallsperiode.RETT_TIL_TILTAKSPENGER)
                },
                sendtTilMeldekortApi = null,
            )

            return meldeperiode
        }
    }
}
