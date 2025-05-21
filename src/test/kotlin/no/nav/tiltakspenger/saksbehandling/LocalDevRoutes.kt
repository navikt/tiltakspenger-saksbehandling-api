package no.nav.tiltakspenger.saksbehandling

import io.github.serpro69.kfaker.faker
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.soknad.BarnetilleggDTO
import no.nav.tiltakspenger.saksbehandling.infra.repo.dto.PeriodeDbJson
import no.nav.tiltakspenger.saksbehandling.infra.repo.withBody
import no.nav.tiltakspenger.saksbehandling.infra.setup.ApplicationContext
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.SøknadDTOMapper.mapBarnetilleggManuelle
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySakMedNySøknad
import no.nav.tiltakspenger.saksbehandling.søknad.infra.route.nySøknadForFnr

internal const val DEV_ROUTE = "/dev"

internal fun Route.localDevRoutes(applicationContext: ApplicationContext) {
    data class NySøknadBody(
        val fnr: String?,
        val deltakelsesperiode: PeriodeDbJson?,
        val barnetillegg: List<BarnetilleggDTO>,
    )

    val faker = faker {
        fakerConfig {
            randomSeed = Fnr.random().verdi.toLong()
            locale = "nb-NO"
        }
    }

    post("$DEV_ROUTE/soknad/ny") {
        call.withBody<NySøknadBody> { body ->
            val fnr = body.fnr?.let { Fnr.tryFromString(it) }
            val barnetillegg = body.barnetillegg.map {
                val fødselsdato = it.fødselsdato ?: faker.person.birthDate(13)
                val fornavn = it.fornavn ?: faker.name.firstName()
                val etternavn = it.etternavn ?: faker.name.lastName()
                BarnetilleggDTO(
                    fødselsdato = fødselsdato,
                    fornavn = fornavn,
                    mellomnavn = null,
                    etternavn = etternavn,
                    oppholderSegIEØS = it.oppholderSegIEØS,
                )
            }.map { mapBarnetilleggManuelle(it) }

            if (fnr == null) {
                val saksnummer = nySakMedNySøknad(
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                    applicationContext = applicationContext,
                    barnetillegg = barnetillegg,
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            } else {
                val saksnummer = nySøknadForFnr(
                    fnr = fnr,
                    applicationContext = applicationContext,
                    deltakelsesperiode = body.deltakelsesperiode?.toDomain(),
                    barnetillegg = barnetillegg,
                )
                call.respond(HttpStatusCode.OK, saksnummer.toString())
            }
        }
    }
}
