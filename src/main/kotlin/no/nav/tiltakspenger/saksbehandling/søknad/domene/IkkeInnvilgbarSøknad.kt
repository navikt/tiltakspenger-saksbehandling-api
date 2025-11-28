package no.nav.tiltakspenger.saksbehandling.søknad.domene

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import java.time.LocalDateTime

data class IkkeInnvilgbarSøknad(
    override val versjon: String = "1",
    override val id: SøknadId,
    override val journalpostId: String,
    override val personopplysninger: Søknad.Personopplysninger,
    override val harSøktPåTiltak: Søknad.JaNeiSpm,
    override val tiltak: Søknadstiltak?,
    override val harSøktOmBarnetillegg: Søknad.JaNeiSpm,
    override val barnetillegg: List<BarnetilleggFraSøknad>,
    override val opprettet: LocalDateTime,
    override val tidsstempelHosOss: LocalDateTime,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val avbrutt: Avbrutt? = null,
    override val kvp: Søknad.PeriodeSpm,
    override val intro: Søknad.PeriodeSpm,
    override val institusjon: Søknad.PeriodeSpm,
    override val etterlønn: Søknad.JaNeiSpm,
    override val gjenlevendepensjon: Søknad.PeriodeSpm,
    override val alderspensjon: Søknad.FraOgMedDatoSpm,
    override val sykepenger: Søknad.PeriodeSpm,
    override val supplerendeStønadAlder: Søknad.PeriodeSpm,
    override val supplerendeStønadFlyktning: Søknad.PeriodeSpm,
    override val jobbsjansen: Søknad.PeriodeSpm,
    override val trygdOgPensjon: Søknad.PeriodeSpm,
    override val vedlegg: Int,
    override val manueltSattSøknadsperiode: Periode?,
    override val manueltSattTiltak: String?,
    override val søknadstype: Søknadstype,
) : Søknad {
    override val fnr: Fnr = personopplysninger.fnr
    override val erAvbrutt: Boolean by lazy { avbrutt != null }

    override fun tiltaksdeltakelseperiodeDetErSøktOm(): Periode? {
        return manueltSattSøknadsperiode
            ?: tiltak?.let { Periode(it.deltakelseFom, it.deltakelseTom) }
    }
}
