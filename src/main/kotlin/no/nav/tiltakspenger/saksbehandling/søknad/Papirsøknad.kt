package no.nav.tiltakspenger.saksbehandling.søknad

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.SøknadId
import no.nav.tiltakspenger.saksbehandling.felles.Avbrutt
import no.nav.tiltakspenger.saksbehandling.sak.Sak
import no.nav.tiltakspenger.saksbehandling.sak.Saksnummer
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad.FraOgMedDatoSpm
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad.JaNeiSpm
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad.PeriodeSpm
import no.nav.tiltakspenger.saksbehandling.søknad.Søknad.Personopplysninger
import java.time.LocalDateTime

data class Papirsøknad(
    val versjon: String = "1",
    override val id: SøknadId,
    override val journalpostId: String,
    override val personopplysninger: Personopplysninger,
    val tiltak: Søknadstiltak?,
    val barnetillegg: List<BarnetilleggFraSøknad>,
    override val opprettet: LocalDateTime,
    override val tidsstempelHosOss: LocalDateTime,
    val kvp: PeriodeSpm?,
    val intro: PeriodeSpm?,
    val institusjon: PeriodeSpm?,
    val etterlønn: JaNeiSpm?,
    val gjenlevendepensjon: PeriodeSpm?,
    val alderspensjon: FraOgMedDatoSpm?,
    val sykepenger: PeriodeSpm?,
    val supplerendeStønadAlder: PeriodeSpm?,
    val supplerendeStønadFlyktning: PeriodeSpm?,
    val jobbsjansen: PeriodeSpm?,
    val trygdOgPensjon: PeriodeSpm?,
    val vedlegg: Int,
    override val sakId: SakId,
    override val saksnummer: Saksnummer,
    override val avbrutt: Avbrutt?,
) : Søknad {
    override val fnr: Fnr = personopplysninger.fnr
    override val erAvbrutt: Boolean by lazy { avbrutt != null }

    fun tilDigitalSøknad(journalpostId: String, sak: Sak): Søknad {
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

        return Digitalsøknad(
            id = Søknad.randomId(),
            journalpostId = journalpostId,
            personopplysninger = personopplysninger,
            tiltak = tiltak,
            barnetillegg = barnetillegg,
            opprettet = opprettet,
            tidsstempelHosOss = LocalDateTime.now(),
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
            sakId = sak.id,
            saksnummer = sak.saksnummer,
            avbrutt = avbrutt,
        )
    }
}
