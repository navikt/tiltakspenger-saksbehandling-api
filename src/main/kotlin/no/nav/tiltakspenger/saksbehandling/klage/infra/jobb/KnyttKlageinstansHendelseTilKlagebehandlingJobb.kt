package no.nav.tiltakspenger.saksbehandling.klage.infra.jobb

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.json.deserialize
import no.nav.tiltakspenger.saksbehandling.felles.ErrorEveryNLogger
import no.nav.tiltakspenger.saksbehandling.klage.domene.KlagebehandlingId
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.EnkelKabalKlagehendelseDTO
import no.nav.tiltakspenger.saksbehandling.klage.infra.kafka.toKlageinstanshendelse
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagebehandlingRepo
import no.nav.tiltakspenger.saksbehandling.klage.ports.KlagehendelseRepo
import java.time.Clock

class KnyttKlageinstansHendelseTilKlagebehandlingJobb(
    private val klagehendelseRepo: KlagehendelseRepo,
    private val klagebehandlingRepo: KlagebehandlingRepo,
    private val clock: Clock,
) {
    private val log = KotlinLogging.logger {}
    private val errorEveryNLogger = ErrorEveryNLogger(log, 3)

    fun knyttHendelser() {
        Either.catch {
            klagehendelseRepo.hentUbehandledeHendelser().forEach { nyKlagehendelse ->
                val loggkontekst =
                    "klagehendelseId: ${nyKlagehendelse.klagehendelseId}, eksternKlagehendelseId: ${nyKlagehendelse.eksternKlagehendelseId}"
                Either.catch {
                    log.info { "Prøver å knytte klageinstanshendelse til klagebehandling. $loggkontekst" }
                    val enkelKabalKlagehendelseDTO = deserialize<EnkelKabalKlagehendelseDTO>(nyKlagehendelse.value)
                    val klageinstanshendelse =
                        nyKlagehendelse.value.toKlageinstanshendelse(nyKlagehendelse.opprettet, nå(clock))
                    val klagebehandlingId = KlagebehandlingId.fromString(enkelKabalKlagehendelseDTO.kabalReferanse)
                    val klagebehandling = klagebehandlingRepo.hentForKlagebehandlingId(klagebehandlingId)!!

                    val oppdatertKlagebehandling = klagebehandling.leggTilKlageinstanshendelse(klageinstanshendelse)
                    klagebehandlingRepo.lagreKlagebehandling(oppdatertKlagebehandling)
                    log.info { "Klageinstanshendelse knyttet til klagebehandling. klagebehandlingId: ${klagebehandling.id}, $loggkontekst" }
                    errorEveryNLogger.reset()
                }.onLeft {
                    errorEveryNLogger.log(it) { "Feil ved knytting av klageinstanshendelse til klagebehandling. $loggkontekst" }
                }
            }
        }.onLeft {
            errorEveryNLogger.log(it) { "Ukjent feil skjedde under knytting av klageinstanshendelser til klagebehandlinger." }
        }
    }
}
