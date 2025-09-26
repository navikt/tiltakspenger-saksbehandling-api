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
    override val tiltak: Søknadstiltak?,
    override val barnetillegg: List<BarnetilleggFraSøknad>,
    override val opprettet: LocalDateTime,
    override val tidsstempelHosOss: LocalDateTime,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val avbrutt: Avbrutt?,
    override val kvp: Søknad.PeriodeSpm?,
    override val intro: Søknad.PeriodeSpm?,
    override val institusjon: Søknad.PeriodeSpm?,
    override val etterlønn: Søknad.JaNeiSpm?,
    override val gjenlevendepensjon: Søknad.PeriodeSpm?,
    override val alderspensjon: Søknad.FraOgMedDatoSpm?,
    override val sykepenger: Søknad.PeriodeSpm?,
    override val supplerendeStønadAlder: Søknad.PeriodeSpm?,
    override val supplerendeStønadFlyktning: Søknad.PeriodeSpm?,
    override val jobbsjansen: Søknad.PeriodeSpm?,
    override val trygdOgPensjon: Søknad.PeriodeSpm?,
    override val vedlegg: Int,
    override val manueltSattSøknadsperiode: Periode,
    override val søknadstype: Søknadstype,
) : Søknad {
    override val fnr: Fnr = personopplysninger.fnr
    override val erAvbrutt: Boolean by lazy { avbrutt != null }

    // TODO Ta høyde for at tiltak er satt og bruk den i stedet?
    override fun tiltaksdeltagelseperiodeDetErSøktOm(): Periode =
        Periode(manueltSattSøknadsperiode.fraOgMed, manueltSattSøknadsperiode.tilOgMed)

    fun tilInnvilgbarSøknad(): InnvilgbarSøknad {
        requireNotNull(tiltak) { "Tiltak mangler" }
        requireNotNull(kvp) { "Mangler å ta stilling til kvp" }
        requireNotNull(intro) { "Mangler å ta stilling til intro" }
        requireNotNull(institusjon) { "Mangler å ta stilling til institusjon" }
        requireNotNull(etterlønn) { "Mangler å ta stilling til etterlønn" }
        requireNotNull(gjenlevendepensjon) { "Mangler å ta stilling til gjenlevendepensjon" }
        requireNotNull(alderspensjon) { "Mangler å ta stilling til alderspensjon" }
        requireNotNull(sykepenger) { "Mangler å ta stilling til sykepenger" }
        requireNotNull(supplerendeStønadAlder) { "Mangler å ta stilling til supplerendeStønadAlder" }
        requireNotNull(supplerendeStønadFlyktning) { "Mangler å ta stilling til supplerendeStønadFlyktning" }
        requireNotNull(jobbsjansen) { "Mangler å ta stilling til jobbsjansen" }
        requireNotNull(trygdOgPensjon) { "Mangler å ta stilling til trygdOgPensjon" }

        return InnvilgbarSøknad(
            id = SøknadId.random(),
            journalpostId = journalpostId,
            personopplysninger = personopplysninger,
            tiltak = tiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = tidsstempelHosOss,
            kvp = kvp,
            intro = intro,
            institusjon = institusjon,
            etterlønn = etterlønn,
            gjenlevendepensjon = gjenlevendepensjon,
            alderspensjon = alderspensjon,
            sykepenger = sykepenger,
            supplerendeStønadAlder = supplerendeStønadAlder,
            supplerendeStønadFlyktning = supplerendeStønadFlyktning,
            jobbsjansen = jobbsjansen,
            trygdOgPensjon = trygdOgPensjon,
            vedlegg = vedlegg,
            sakId = sakId,
            saksnummer = saksnummer,
            avbrutt = avbrutt,
            søknadstype = søknadstype,
        )
    }
}
