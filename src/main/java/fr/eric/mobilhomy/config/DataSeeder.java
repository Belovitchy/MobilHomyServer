package fr.eric.mobilhomy.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Jeu de données de démonstration (base recréée à chaque démarrage : ddl-auto=create-drop).
 *
 * <p>Les dates des séjours sont RELATIVES à la date du jour : la démo montre toujours un séjour en cours,
 * une arrivée du jour, des demandes à traiter et un historique, quel que soit le jour de la soutenance.
 * La saison d'été (juillet-août) alimente le graphique d'occupation du tableau de bord propriétaire.</p>
 *
 * <pre>
 * Comptes (mot de passe entre parenthèses)
 *   admin@mail.fr   (admin)       Administrateur
 *   dupont@mail.fr  (owner)       Paul Dupont     — Océane, Azur, Les Pins, Bel Air (sans gérant)
 *   lefevre@mail.fr (owner)       Claire Lefèvre  — Marina, Dune
 *   marc@mail.fr    (manager)     Marc Durand     — gère Océane et Azur
 *   sarah@mail.fr   (manager)     Sarah Benali    — gère Les Pins (Dupont), Marina et Dune (Lefèvre)
 *   julie@mail.fr, thomas@mail.fr, camille@mail.fr, lucas@mail.fr, ines@mail.fr  (vacationer)
 *
 * Scénarios de démo (propriétaire Dupont, onglet « À traiter »)
 *   - Azur, Inès : demande validable sans conflit
 *   - Océane, Lucas : demande qui CHEVAUCHE le séjour validé de Thomas → la validation renvoie 409
 * </pre>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSeeder {

    private final AuthRepository authRepository;
    private final LinkRepository linkRepository;
    private final MobilhomeRepository mobilhomeRepository;
    private final ImageRepository imageRepository;
    private final InvoiceRepository invoiceRepository;
    private final OwnerManagerRepository ownerManagerRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder encoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            if (authRepository.count() > 0) {   // idempotent si devtools recharge le contexte
                log.info("Données déjà présentes — seed ignoré");
                return;
            }
            seed(LocalDate.now());
        };
    }

    private void seed(LocalDate today) {
        // Saison d'été déjà passée : celle de l'année en cours à partir d'octobre, sinon celle de l'an dernier.
        // (Évite tout chevauchement avec les séjours relatifs, qui commencent au plus tôt 25 jours avant aujourd'hui.)
        int season = today.getMonthValue() >= 10 ? today.getYear() : today.getYear() - 1;

        // ── 1. COMPTES (cascade ALL : l'Utilisateur est persisté avec son Auth) ─────────────────────
        account(Admin.builder().firstname("Éric").name("Admin").phone("0100000000").build(),
                "admin@mail.fr", "admin", RolesEnum.ADMIN);

        Owner dupont = (Owner) account(Owner.builder().firstname("Paul").name("Dupont").phone("0600000001").build(),
                "dupont@mail.fr", "owner", RolesEnum.OWNER);
        Owner lefevre = (Owner) account(Owner.builder().firstname("Claire").name("Lefèvre").phone("0600000004").build(),
                "lefevre@mail.fr", "owner", RolesEnum.OWNER);

        Manager marc = (Manager) account(Manager.builder().firstname("Marc").name("Durand").phone("0600000002").build(),
                "marc@mail.fr", "manager", RolesEnum.MANAGER);
        Manager sarah = (Manager) account(Manager.builder().firstname("Sarah").name("Benali").phone("0600000005").build(),
                "sarah@mail.fr", "manager", RolesEnum.MANAGER);

        Vacationer julie = vacationer("Julie", "Martin", "0600000003", "julie@mail.fr");
        Vacationer thomas = vacationer("Thomas", "Bernard", "0611111111", "thomas@mail.fr");
        Vacationer camille = vacationer("Camille", "Petit", "0622222222", "camille@mail.fr");
        Vacationer lucas = vacationer("Lucas", "Moreau", "0633333333", "lucas@mail.fr");
        Vacationer ines = vacationer("Inès", "Garcia", "0644444444", "ines@mail.fr");

        // ── 2. LIENS iCal des propriétaires (Booking, Airbnb, Abritel…) ───────────────────────────
        linkRepository.save(Link.builder().owner(dupont).name("Booking").url("https://ical.booking.com/v1/export?t=XXXX").build());
        linkRepository.save(Link.builder().owner(dupont).name("Airbnb").url("https://www.airbnb.fr/calendar/ical/XXXX.ics").build());
        linkRepository.save(Link.builder().owner(lefevre).name("Abritel").url("https://www.abritel.fr/icalendar/XXXX.ics").build());

        // ── 3. GÉRANTS rattachés aux propriétaires ─────────────────────────────────────────────────
        ownerManagerRepository.save(OwnerManager.builder().owner(dupont).manager(marc).build());
        ownerManagerRepository.save(OwnerManager.builder().owner(dupont).manager(sarah).build());
        ownerManagerRepository.save(OwnerManager.builder().owner(lefevre).manager(sarah).build());

        // ── 4. MOBIL-HOMES ─────────────────────────────────────────────────────────────────────────
        Mobilhome oceane = mobilhome(dupont, marc, "Océane",
                "Mobil-home 3 chambres, terrasse bois couverte, climatisation. À 200 m de l'espace aquatique.",
                "La Réserve", 6, "Camping Siblu La Réserve, 17 rue du Lac, Ronce-les-Bains", "17390", 80,
                "https://ical.booking.com/v1/export?t=XXXX", "/images/oceane-1.webp", "/images/oceane-2.webp");
        Mobilhome azur = mobilhome(dupont, marc, "Azur",
                "Mobil-home 2 chambres, proche plage, idéal couple avec enfants.",
                "Le Bois Soleil", 4, "Camping Siblu Le Bois Soleil, Saint-Georges-de-Didonne", "17110", 100,
                "https://www.airbnb.fr/calendar/ical/XXXX.ics", "/images/azur-1.webp");
        Mobilhome pins = mobilhome(dupont, sarah, "Les Pins",
                "Grand mobil-home 4 chambres, 2 salles d'eau, parfait pour les familles nombreuses.",
                "Les Charmettes", 8, "Camping Siblu Les Charmettes, Les Mathes", "17570", 120,
                null, "/images/oceane-2.webp");
        Mobilhome belAir = mobilhome(dupont, null, "Bel Air",
                "Mobil-home 2 chambres récemment rénové. Gérant à affecter.",
                "La Réserve", 4, "Camping Siblu La Réserve, Ronce-les-Bains", "17390", 70, null);
        Mobilhome marina = mobilhome(lefevre, sarah, "Marina",
                "Mobil-home 3 chambres vue sur l'étang, terrasse semi-couverte.",
                "Le Bois Dormant", 6, "Camping Siblu Le Bois Dormant, Saint-Jean-de-Monts", "85160", 95,
                null, "/images/azur-1.webp");
        Mobilhome dune = mobilhome(lefevre, sarah, "Dune",
                "Mobil-home 2 chambres à 5 minutes à pied de la plage.",
                "Les Dunes", 4, "Camping Siblu Les Dunes, Saint-Hilaire-de-Riez", "85270", 85, null);

        // ── 5. RÉSERVATIONS ─────────────────────────────────────────────────────────────────────────
        // a) Saison d'été passée : alimente le tableau de bord (taux d'occupation par semaine)
        resa(camille, oceane, LocalDate.of(season, 7, 5), 7, ReservationStatus.VALIDATED, "CA-512-PT",
                occ("Camille", "Petit", 34), occ("Hugo", "Petit", 36), occ("Zoé", "Petit", 6));
        resa(julie, oceane, LocalDate.of(season, 7, 12), 7, ReservationStatus.VALIDATED, "AA-123-BB",
                occ("Julie", "Martin", 38), occ("Antoine", "Martin", 40), occ("Léo", "Martin", 10), occ("Emma", "Martin", 7));
        resa(thomas, oceane, LocalDate.of(season, 7, 26), 7, ReservationStatus.VALIDATED, "TB-404-RN",
                occ("Thomas", "Bernard", 45), occ("Nadia", "Bernard", 43));
        resa(lucas, oceane, LocalDate.of(season, 8, 9), 7, ReservationStatus.VALIDATED, "LM-777-OR",
                occ("Lucas", "Moreau", 29), occ("Sarah", "Moreau", 28));
        resa(ines, azur, LocalDate.of(season, 7, 12), 7, ReservationStatus.VALIDATED, "IG-210-AZ",
                occ("Inès", "Garcia", 31), occ("Pablo", "Garcia", 4));
        resa(julie, azur, LocalDate.of(season, 8, 2), 7, ReservationStatus.VALIDATED, "AA-123-BB",
                occ("Julie", "Martin", 38), occ("Antoine", "Martin", 40));
        resa(camille, azur, LocalDate.of(season, 8, 16), 7, ReservationStatus.VALIDATED, "CA-512-PT",
                occ("Camille", "Petit", 34), occ("Zoé", "Petit", 6));
        resa(thomas, pins, LocalDate.of(season, 7, 19), 7, ReservationStatus.VALIDATED, "TB-404-RN",
                occ("Thomas", "Bernard", 45), occ("Nadia", "Bernard", 43), occ("Jules", "Bernard", 14),
                occ("Lina", "Bernard", 12), occ("Max", "Bernard", 9));
        resa(lucas, marina, LocalDate.of(season, 7, 5), 7, ReservationStatus.VALIDATED, "LM-777-OR",
                occ("Lucas", "Moreau", 29));
        resa(ines, marina, LocalDate.of(season, 8, 2), 14, ReservationStatus.VALIDATED, "IG-210-AZ",
                occ("Inès", "Garcia", 31), occ("Pablo", "Garcia", 4));
        resa(julie, dune, LocalDate.of(season, 7, 26), 14, ReservationStatus.VALIDATED, "AA-123-BB",
                occ("Julie", "Martin", 38), occ("Léo", "Martin", 10));

        // b) Autour d'aujourd'hui : séjour en cours, arrivée du jour, à venir, demandes, historique
        // Océane (Marc)
        resa(julie, oceane, today.minusDays(3), 7, ReservationStatus.VALIDATED, "AA-123-BB",       // en cours
                occ("Julie", "Martin", 38), occ("Antoine", "Martin", 40), occ("Léo", "Martin", 10), occ("Emma", "Martin", 7));
        resa(thomas, oceane, today.plusDays(20), 7, ReservationStatus.VALIDATED, "TB-404-RN",       // à venir
                occ("Thomas", "Bernard", 45), occ("Nadia", "Bernard", 43));
        resa(lucas, oceane, today.plusDays(22), 7, ReservationStatus.COMPLETED, "LM-777-OR",       // ⚠ chevauche Thomas
                occ("Lucas", "Moreau", 29), occ("Sarah", "Moreau", 28));
        resa(camille, oceane, today.minusDays(25), 7, ReservationStatus.VALIDATED, "CA-512-PT",    // passé récent
                occ("Camille", "Petit", 34), occ("Zoé", "Petit", 6));
        resa(julie, oceane, today.plusDays(60), 7, ReservationStatus.PENDING, null);                // brouillon

        // Azur (Marc)
        resa(thomas, azur, today, 7, ReservationStatus.VALIDATED, "TB-404-RN",                     // arrivée du jour
                occ("Thomas", "Bernard", 45), occ("Jules", "Bernard", 14));
        resa(ines, azur, today.plusDays(10), 7, ReservationStatus.COMPLETED, "IG-210-AZ",          // à traiter, sans conflit
                occ("Inès", "Garcia", 31), occ("Pablo", "Garcia", 4));
        resa(lucas, azur, today.minusDays(20), 5, ReservationStatus.REJECTED, "LM-777-OR",
                occ("Lucas", "Moreau", 29));
        resa(camille, azur, today.plusDays(40), 7, ReservationStatus.CANCELLED, "CA-512-PT",
                occ("Camille", "Petit", 34));

        // Les Pins (Sarah, chez Dupont)
        resa(julie, pins, today.plusDays(5), 7, ReservationStatus.COMPLETED, "AA-123-BB",          // à traiter
                occ("Julie", "Martin", 38), occ("Antoine", "Martin", 40), occ("Léo", "Martin", 10), occ("Emma", "Martin", 7));
        resa(ines, pins, today.plusDays(35), 7, ReservationStatus.VALIDATED, "IG-210-AZ",
                occ("Inès", "Garcia", 31), occ("Pablo", "Garcia", 4), occ("Carmen", "Garcia", 62));

        // Marina et Dune (Sarah, chez Lefèvre)
        resa(camille, marina, today.minusDays(10), 7, ReservationStatus.VALIDATED, "CA-512-PT",
                occ("Camille", "Petit", 34), occ("Hugo", "Petit", 36));
        resa(lucas, marina, today.plusDays(14), 7, ReservationStatus.VALIDATED, "LM-777-OR",
                occ("Lucas", "Moreau", 29), occ("Sarah", "Moreau", 28));
        resa(thomas, marina, today.plusDays(45), 7, ReservationStatus.COMPLETED, "TB-404-RN",
                occ("Thomas", "Bernard", 45));
        resa(julie, dune, today.plusDays(1), 4, ReservationStatus.VALIDATED, "AA-123-BB",
                occ("Julie", "Martin", 38));
        resa(ines, dune, today.plusDays(90), 7, ReservationStatus.PENDING, null);

        // ── 6. FACTURES DE FONCTIONNEMENT (saisies par les gérants, dues par les propriétaires) ─────
        invoice(marc, oceane, today.minusDays(18), InvoiceCategory.SERVICE, "Ménage fin de séjour — Océane", 65, InvoiceStatus.PAID);
        invoice(marc, azur, today.minusDays(15), InvoiceCategory.CONSUMABLE, "Consommables (gaz, vaisselle)", 28, InvoiceStatus.ISSUED);
        invoice(marc, oceane, today.minusDays(4), InvoiceCategory.SERVICE, "Remplacement du flexible de douche", 42.5, InvoiceStatus.ISSUED);
        invoice(marc, azur, today.minusDays(30), InvoiceCategory.CONSUMABLE, "Produits d'entretien (saisie en double)", 19.9, InvoiceStatus.CANCELLED);
        invoice(sarah, pins, today.minusDays(12), InvoiceCategory.SERVICE, "Nettoyage terrasse et mobilier", 80, InvoiceStatus.ISSUED);
        invoice(sarah, marina, today.minusDays(3), InvoiceCategory.SERVICE, "Ménage fin de séjour — Marina", 70, InvoiceStatus.PAID);
        invoice(sarah, dune, today.minusDays(1), InvoiceCategory.CONSUMABLE, "Bouteille de gaz 13 kg", 36, InvoiceStatus.ISSUED);

        log.info("Seed terminé : {} comptes, {} mobil-homes, {} réservations, {} factures",
                authRepository.count(), mobilhomeRepository.count(), reservationRepository.count(), invoiceRepository.count());
    }

    // =====================================================================
    // Fabriques
    // =====================================================================

    private Utilisateur account(Utilisateur user, String login, String password, RolesEnum role) {
        Auth auth = authRepository.save(Auth.builder()
                .utilisateur(user).login(login).password(encoder.encode(password)).authority(role).build());
        Utilisateur saved = auth.getUtilisateur();
        saved.setAuth(auth); // côté inverse de la relation, utile pour les DTO pendant le seed
        return saved;
    }

    private Vacationer vacationer(String firstname, String name, String phone, String login) {
        return (Vacationer) account(Vacationer.builder().firstname(firstname).name(name).phone(phone).build(),
                login, "vacationer", RolesEnum.VACATIONER);
    }

    private Mobilhome mobilhome(Owner owner, Manager manager, String name, String description, String location,
                                int capacity, String address, String postalCode, int price, String icalLink,
                                String... images) {
        Mobilhome m = mobilhomeRepository.save(Mobilhome.builder()
                .owner(owner).manager(manager).name(name).description(description).location(location)
                .capacity(capacity).address(address).postalCode(postalCode)
                .price(BigDecimal.valueOf(price)).icalLink(icalLink)
                .build());
        for (String path : images) {
            imageRepository.save(Image.builder().mobilhome(m).path(path).build());
        }
        return m;
    }

    /** Occupant non encore rattaché (prénom, nom, âge). */
    private static Occupant occ(String firstname, String name, int age) {
        return Occupant.builder().firstname(firstname).name(name).age(age).build();
    }

    /** Réservation de `nights` nuits ; les occupants sont enregistrés en cascade avec elle. */
    private Reservation resa(Vacationer vacationer, Mobilhome mobilhome, LocalDate start, int nights,
                             ReservationStatus status, String immat, Occupant... occupants) {
        Reservation r = Reservation.builder()
                .vacationer(vacationer).mobilhome(mobilhome)
                .startDate(start).endDate(start.plusDays(nights))
                .numberOfPerson(occupants.length)
                .immat(immat)
                .status(status)
                .build();
        for (Occupant o : List.of(occupants)) {
            o.setReservation(r);
            r.getOccupants().add(o);
        }
        return reservationRepository.save(r);
    }

    private void invoice(Manager manager, Mobilhome mobilhome, LocalDate date, InvoiceCategory category,
                         String description, double amount, InvoiceStatus status) {
        invoiceRepository.save(Invoice.builder()
                .manager(manager).mobilhome(mobilhome).owner(mobilhome.getOwner())
                .date(date).category(category).description(description)
                .amount(BigDecimal.valueOf(amount)).type(InvoiceType.OUTGOING).status(status)
                .build());
    }
}
