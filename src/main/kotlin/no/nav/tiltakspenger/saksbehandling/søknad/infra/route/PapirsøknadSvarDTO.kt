package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.PapirsøknadBody.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.PapirsøknadBody.SøknadsTiltakDTO

data class PapirsøknadSvarDTO(
    val tiltak: SøknadsTiltakDTO?,
    val barnetilleggPdl: List<BarnetilleggDTO> = emptyList(),
    val barnetilleggManuelle: List<BarnetilleggDTO> = emptyList(),
    val kvp: PapirsøknadBody.PeriodeSpmDTO,
    val intro: PapirsøknadBody.PeriodeSpmDTO,
    val institusjon: PapirsøknadBody.PeriodeSpmDTO,
    val etterlønn: PapirsøknadBody.JaNeiSpmDTO,
    val gjenlevendepensjon: PapirsøknadBody.PeriodeSpmDTO,
    val alderspensjon: PapirsøknadBody.FraOgMedDatoSpmDTO,
    val sykepenger: PapirsøknadBody.PeriodeSpmDTO,
    val supplerendeStønadAlder: PapirsøknadBody.PeriodeSpmDTO,
    val supplerendeStønadFlyktning: PapirsøknadBody.PeriodeSpmDTO,
    val jobbsjansen: PapirsøknadBody.PeriodeSpmDTO,
    val trygdOgPensjon: PapirsøknadBody.PeriodeSpmDTO,
)
