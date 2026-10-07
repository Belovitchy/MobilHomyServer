package fr.eric.mobilhomy.config;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.BeanDefinitionDsl;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(AuthRepository authRepository,
                                   LinkRepository linkRepository,
                                   MobilhomeRepository mobilhomeRepository,
                                   ImageRepository imageRepository,
                                   InvoiceRepository invoiceRepository,
                                   OwnerManagerRepository ownerManagerRepository,
                                   ReservationRepository reservationRepository,
                                   OccupantRepository occupantRepository,
                                   PasswordEncoder encoder) {
        return args -> {

            if (authRepository.count() > 0) {   // idempotent si devtools recharge le contexte
                log.info("Données déjà présentes — seed ignoré");
                return;
            }

            // ── 1. USERS + AUTH (cascade ALL : le User est persisté avec l'Auth) ──
            // ⚠️ Adaptez les noms de setters/champs à vos entités réelles
            Auth authAdmin = authRepository.save(Auth.builder()
                    .utilisateur(Admin.builder().firstname("Éric").name("Admin").phone("0100000000").build())
                    .login("admin@mail.fr").password(encoder.encode("admin"))
                    .authority(RolesEnum.ADMIN).build());

            Auth authOwner = authRepository.save(Auth.builder()
                    .utilisateur(Owner.builder().firstname("Paul").name("Dupont").phone("0600000001").build())
                    .login("dupont@mail.fr").password(encoder.encode("owner"))
                    .authority(RolesEnum.OWNER).build());

            Auth authManager = authRepository.save(Auth.builder()
                    .utilisateur(Manager.builder().firstname("Marc").name("Durand").phone("0600000002").build())
                    .login("marc@mail.fr").password(encoder.encode("manager"))
                    .authority(RolesEnum.MANAGER).build());

            Auth authVacancier = authRepository.save(Auth.builder()
                    .utilisateur(Vacationer.builder().firstname("Julie").name("Martin").phone("0600000003").build())
                    .login("julie@mail.fr").password(encoder.encode("vacationer"))
                    .authority(RolesEnum.VACATIONER).build());

            Owner owner = (Owner) authOwner.getUtilisateur();
            Manager manager = (Manager) authManager.getUtilisateur();
            Vacationer vacationer = (Vacationer) authVacancier.getUtilisateur();

            // ── 2. LINKS iCal du propriétaire (Booking, Airbnb, Abritel…) ──
            linkRepository.save(Link.builder().owner(owner)
                    .name("Booking").url("https://ical.booking.com/v1/export?t=XXXX").build());
            linkRepository.save(Link.builder().owner(owner)
                    .name("Airbnb").url("https://www.airbnb.fr/calendar/ical/XXXX.ics").build());

            // ── 3. OWNERMANAGER : affectation du gérant aux mobil-homes du proprio ──
            ownerManagerRepository.save(OwnerManager.builder()
                    .owner(owner).manager(manager).build());

            // ── 4. MOBILHOME (owner + manager + données iCal) ──
            Mobilhome océane = mobilhomeRepository.save(Mobilhome.builder()
                    .owner(owner).manager(manager)
                    .name("Océane")
                    .description("Mobil-home 3 chambres, terrasse bois, climatisation")
                    .location("La Réserve").capacity(6)
                    .adress("Camping Siblu La Réserve, 17 Rue du Lac, Ronce-les-Bains")
                    .icalLink("https://ical.booking.com/v1/export?t=XXXX")
                    .build());

            Mobilhome azur = mobilhomeRepository.save(Mobilhome.builder()
                    .owner(owner).manager(manager)
                    .name("Azur")
                    .description("Mobil-home 2 chambres, proche plage")
                    .location("Le Bois Soleil").capacity(4)
                    .adress("Camping Siblu Le Bois Soleil, Saint-Georges-de-Didonne")
                    .icalLink("https://www.airbnb.fr/calendar/ical/XXXX.ics")
                    .build());

            // ── 5. IMAGES ──
            imageRepository.save(Image.builder().mobilhome(océane).path("/images/ocene-1.jpg").build());
            imageRepository.save(Image.builder().mobilhome(océane).path("/images/ocene-2.jpg").build());
            imageRepository.save(Image.builder().mobilhome(azur).path("/images/azur-1.jpg").build());

            // ── 6. INVOICE (factures de fonctionnement du gérant) ──
            invoiceRepository.save(Invoice.builder().owner(owner)
                    .description("Ménage fin de séjour — Océane")
                    .type(InvoiceType.OUTGOING).amount(BigDecimal.valueOf(65))
                    .status(InvoiceStatus.PAID).build());
            invoiceRepository.save(Invoice.builder().owner(owner)
                    .description("Consommables (gaz, vaisselle)")
                    .type(InvoiceType.OUTGOING).amount(BigDecimal.valueOf(28f))
                    .status(InvoiceStatus.ISSUED).build());


            // ── 7. RESERVATIONS + CONTRATS (persistés ensemble via le cascade) ──
            Contract contratSigne = Contract.builder()
                    .vacationer(vacationer).mobilhome(océane)
                    .signed(true)   // retourné signé → valide la réservation
                    .path("/contrats/contrat-SBLU-2026-0001.pdf")
                    .build();

            Contract contratEnvoye = Contract.builder()
                    .vacationer(vacationer).mobilhome(azur)
                    .signed(false)  // envoyé, en attente de signature
                    .path("/contrats/contrat-SBLU-2026-0002.pdf")
                    .build();

            // ── RESERVATIONS (vacancier + mobilhome + workflow) ──
            // Confirmée : contrat signé rattaché
            Reservation resa = reservationRepository.save(Reservation.builder()
                    .vacationer(vacationer).mobilhome(océane)
                    .startDate(LocalDate.of(2026, 7, 12))
                    .endDate(LocalDate.of(2026, 7, 19))
                    .numberOfPerson(4)
                    .status(ReservationStatus.VALIDATED)
                    .immat("AA-123-BB")
                    .sibluRef("SBLU-2026-0001")
                    .contract(contratSigne)
                    .build());

            // Contrat envoyé, pas encore signé
            Reservation resa2 = reservationRepository.save(Reservation.builder()
                    .vacationer(vacationer)
                    .mobilhome(azur)
                    .contract(contratEnvoye)             // signed=false ↔ CONTRACT_SENT
                    .startDate(LocalDate.of(2026, 8, 22))
                    .endDate(LocalDate.of(2026, 8, 29))
                    .numberOfPerson(3)
                    .status(ReservationStatus.COMPLETED)
                    .immat("AA-123-BB")
                    .sibluRef("SBLU-2026-0002")
                    .build());

            // En attente de validation proprio : pas encore de contrat
            Reservation resa3 = reservationRepository.save(Reservation.builder()
                    .vacationer(vacationer)
                    .mobilhome(océane)
                    // pas de .contract() → contract_id NULL
                    .startDate(LocalDate.of(2026, 10, 5))
                    .endDate(LocalDate.of(2026, 10, 12))
                    .numberOfPerson(2)
                    .status(ReservationStatus.PENDING)
                    .immat("AA-123-BB")
                    .build());

            // ── 8. OCCUPANTS (nom, prénom, âge → FunPass) ──
            occupantRepository.save(Occupant.builder()
                    .firstname("Léo").name("Martin").age(10).reservation(resa).build());
            occupantRepository.save(Occupant.builder()
                    .firstname("Emma").name("Martin").age(7).reservation(resa).build());

            log.info("Seed terminé : 4 comptes, 2 mobil-homes, 2 réservations");
        };
    }
}