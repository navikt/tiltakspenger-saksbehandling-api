-- V274 kopierte nå-tilstander fra tiltaksdeltaker_endring som ble lagret før formatet fikk eksplisitte felter fra libs-domenet.
-- De har typeKode, deltakelseStatus osv. i stedet for tiltakstype, kildestatus osv., og kan ikke leses som TiltaksdeltakelseNåtilstandDbJson.
-- Her konverteres de til dagens format.
-- Id-er, datoer, typen og omfanget kopieres uendret.
-- Kildens tiltakskode, kildens statuskode, Komet-årsaken og tidspunktet Komet-statusen ble opprettet ble ikke lagret i det gamle formatet.
-- De utledes derfor så godt det lar seg gjøre, og feltet migrertFraTidligereFormat sier hvilke verdier som kan være unøyaktige.
-- Tidspunktet Komet-statusen ble opprettet settes til tidspunktet nå-tilstanden ble innhentet, i norsk tid slik appen kjører.
-- De riktige verdiene finnes i kildesystemene, og den opprinnelige verdien beholdes i feltet.
-- Statuskoden velges slik at den tolkes tilbake til den gamle deltakerstatusen.
-- Finnes det ingen slik kode hos kilden, beholdes den gamle statusen, og den leses da som en ukjent kildestatus.
UPDATE tiltakshistorikk
SET verdi = jsonb_build_object(
        'eksternDeltakelseId', verdi -> 'eksternDeltakelseId',
        'gjennomføringId', verdi -> 'gjennomføringId',
        'tiltakstype', verdi -> 'typeKode',
        'tiltakstypenavn', verdi -> 'typeNavn',
        'tiltakskodeFraKilden', verdi -> 'typeKode',
        'fraOgMed', verdi -> 'deltakelseFraOgMed',
        'tilOgMed', verdi -> 'deltakelseTilOgMed',
        'kildestatus', jsonb_build_object(
                'kilde', verdi -> 'kilde',
                'kodeIKontrakten', CASE verdi ->> 'kilde'
                    WHEN 'Komet' THEN CASE verdi ->> 'deltakelseStatus'
                        WHEN 'VenterPåOppstart' THEN 'VENTER_PA_OPPSTART'
                        WHEN 'Deltar' THEN 'DELTAR'
                        WHEN 'HarSluttet' THEN 'HAR_SLUTTET'
                        WHEN 'Avbrutt' THEN 'AVBRUTT'
                        WHEN 'Fullført' THEN 'FULLFORT'
                        WHEN 'IkkeAktuell' THEN 'IKKE_AKTUELL'
                        WHEN 'Feilregistrert' THEN 'FEILREGISTRERT'
                        WHEN 'PåbegyntRegistrering' THEN 'PABEGYNT_REGISTRERING'
                        WHEN 'SøktInn' THEN 'SOKT_INN'
                        WHEN 'Venteliste' THEN 'VENTELISTE'
                        WHEN 'Vurderes' THEN 'VURDERES'
                        ELSE verdi ->> 'deltakelseStatus'
                        END
                    WHEN 'Arena' THEN CASE verdi ->> 'deltakelseStatus'
                        WHEN 'VenterPåOppstart' THEN 'TILBUD'
                        WHEN 'Deltar' THEN 'TAKKET_JA_TIL_TILBUD'
                        WHEN 'Avbrutt' THEN 'DELTAKELSE_AVBRUTT'
                        WHEN 'Fullført' THEN 'FULLFORT'
                        WHEN 'IkkeAktuell' THEN 'IKKE_AKTUELL'
                        WHEN 'Feilregistrert' THEN 'FEILREGISTRERT'
                        WHEN 'SøktInn' THEN 'AKTUELL'
                        WHEN 'Venteliste' THEN 'VENTELISTE'
                        ELSE verdi ->> 'deltakelseStatus'
                        END
                    WHEN 'TeamTiltak' THEN CASE verdi ->> 'deltakelseStatus'
                        WHEN 'VenterPåOppstart' THEN 'KLAR_FOR_OPPSTART'
                        WHEN 'Deltar' THEN 'GJENNOMFORES'
                        WHEN 'HarSluttet' THEN 'AVSLUTTET'
                        WHEN 'Avbrutt' THEN 'AVBRUTT'
                        WHEN 'IkkeAktuell' THEN 'ANNULLERT'
                        WHEN 'PåbegyntRegistrering' THEN 'PAABEGYNT'
                        WHEN 'SøktInn' THEN 'MANGLER_GODKJENNING'
                        ELSE verdi ->> 'deltakelseStatus'
                        END
                    END,
                'årsak', NULL,
                'opprettet', CASE
                    WHEN verdi ->> 'kilde' = 'Komet' THEN replace((innhentet AT TIME ZONE 'Europe/Oslo')::text, ' ', 'T')
                    END
            ),
        'deltakelsesprosent', verdi -> 'deltakelseProsent',
        'dagerPerUke', verdi -> 'antallDagerPerUke',
        'deltidsprosentPåGjennomføring', verdi -> 'deltidsprosentGjennomforing',
        'migrertFraTidligereFormat', jsonb_build_object(
                'migrering', 'V276',
                'beskrivelse', 'Konvertert fra formatet nå-tilstanden ble lagret i før 2026-10-06. ' ||
                               'Feltene i kanVæreUnøyaktige ble ikke lagret i det gamle formatet, og er utledet. ' ||
                               'De riktige verdiene finnes i kildesystemet.',
                'kanVæreUnøyaktige', jsonb_build_array(
                        'tiltakskodeFraKilden',
                        'kildestatus.kodeIKontrakten',
                        'kildestatus.årsak',
                        'kildestatus.opprettet'
                    ),
                'opprinneligVerdi', verdi
            )
    )
WHERE verdi -> 'typeKode' IS NOT NULL;
