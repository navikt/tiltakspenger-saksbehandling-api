package no.nav.tiltakspenger.saksbehandling.søknad.infra.route

import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.ManueltRegistrertSøknadBody.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.ManueltRegistrertSøknadBody.SøknadsTiltakDTO

data class ManueltRegistrertSøknadSvarDTO(
    val tiltak: SøknadsTiltakDTO?,
    val barnetilleggPdl: List<BarnetilleggDTO> = emptyList(),
    val barnetilleggManuelle: List<BarnetilleggDTO> = emptyList(),
    val harSøktPåTiltak: ManueltRegistrertSøknadBody.JaNeiSpmDTO,
    val harSøktOmBarnetillegg: ManueltRegistrertSøknadBody.JaNeiSpmDTO,
    val kvp: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val intro: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val institusjon: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val etterlønn: ManueltRegistrertSøknadBody.JaNeiSpmDTO,
    val gjenlevendepensjon: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val alderspensjon: ManueltRegistrertSøknadBody.FraOgMedDatoSpmDTO,
    val sykepenger: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val supplerendeStønadAlder: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val supplerendeStønadFlyktning: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val jobbsjansen: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
    val trygdOgPensjon: ManueltRegistrertSøknadBody.PeriodeSpmDTO,
)
