package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.*;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import fr.eric.mobilhomy.dal.ReservationRepository;
import fr.eric.mobilhomy.dal.VacationerRepository;
import fr.eric.mobilhomy.dto.OccupantRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerRequestDto;
import fr.eric.mobilhomy.dto.ReservationVacationerResponseDto;
import fr.eric.mobilhomy.dto.ReservationVacationerUpdateRequestDto;
import fr.eric.mobilhomy.exception.BusinessRuleException;
import fr.eric.mobilhomy.exception.NotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Réservations côté vacancier. Le front contrôle déjà les saisies, mais toutes les règles
 * sont revérifiées ici : un appel direct à l'API ne doit pas pouvoir les contourner.
 */
@Service
@AllArgsConstructor
public class ReservationServiceImpl implements ReservationService{

    private final ReservationRepository reservationRepository;
    private final VacationerRepository vacationerRepository;
    private final MobilhomeRepository mobilhomeRepository;

    @Override
    @Transactional
    public ReservationVacationerResponseDto addReservation(Integer userId, ReservationVacationerRequestDto req) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (req == null || req.mobilhomeId() == null) {
            throw new IllegalArgumentException("Mobilhome ID must not be null");
        }
        if (req.startDate() == null || req.endDate() == null || !req.startDate().isBefore(req.endDate())) {
            throw new IllegalArgumentException("La date d'arrivée doit précéder la date de départ");
        }
        if (req.startDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La date d'arrivée ne peut pas être dans le passé");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Vacancier introuvable"));
        Mobilhome mobilhome = mobilhomeRepository.findById(req.mobilhomeId())
                .orElseThrow(() -> new NotFoundException("Mobil-home introuvable"));
        checkCapacity(mobilhome, req.occupants());
        // Seules les réservations validées bloquent le créneau : plusieurs demandes peuvent
        // être en concurrence, le propriétaire tranche (contrôle refait à la validation).
        if (reservationRepository.existsOverlap(mobilhome.getId(), -1, ReservationStatus.VALIDATED,
                req.startDate(), req.endDate())) {
            throw new BusinessRuleException("Ce mobil-home est déjà réservé sur ces dates");
        }

        Reservation res = Reservation.builder()
                .vacationer(vacationer)
                .mobilhome(mobilhome)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(ReservationStatus.PENDING)
                .build();
        applyDetails(res, req.immat(), req.occupants());
        Reservation savedReservation = reservationRepository.save(res);
        return ReservationVacationerResponseDto.fromEntity(savedReservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReservationVacationerResponseDto> getReservationsByUserId(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        return reservationRepository.findByVacationerId(userId)
                .stream()
                .map(ReservationVacationerResponseDto::fromEntity)
                .toList();
    }

    /** « Enregistrer » : met à jour le brouillon, qui reste PENDING. */
    @Override
    @Transactional
    public ReservationVacationerResponseDto updateReservation(Integer userId, Integer resaId, ReservationVacationerUpdateRequestDto req) {
        Reservation res = findOwnDraft(userId, resaId);
        checkCapacity(res.getMobilhome(), req.occupants());
        applyDetails(res, req.immat(), req.occupants());
        return ReservationVacationerResponseDto.fromEntity(reservationRepository.save(res));
    }

    /** « Envoyer au propriétaire » : dossier complet obligatoire, PENDING → COMPLETED (à traiter). */
    @Override
    @Transactional
    public ReservationVacationerResponseDto submitReservation(Integer userId, Integer resaId, ReservationVacationerUpdateRequestDto req) {
        Reservation res = findOwnDraft(userId, resaId);
        if (req.immat() == null || req.immat().isBlank()) {
            throw new IllegalArgumentException("La plaque d'immatriculation est obligatoire pour l'accès au camping");
        }
        if (req.occupants() == null || req.occupants().isEmpty()) {
            throw new IllegalArgumentException("Au moins un occupant est obligatoire");
        }
        checkCapacity(res.getMobilhome(), req.occupants());
        applyDetails(res, req.immat(), req.occupants());
        res.setStatus(ReservationStatus.COMPLETED);
        return ReservationVacationerResponseDto.fromEntity(reservationRepository.save(res));
    }

    /**
     * Réservation du vacancier connecté, encore modifiable. Celle d'un autre vacancier renvoie
     * 404 (on ne confirme pas son existence) ; une demande déjà envoyée, validée ou refusée
     * ne peut plus être modifiée (sinon elle repasserait en brouillon après validation).
     */
    private Reservation findOwnDraft(Integer userId, Integer resaId) {
        if (userId == null || resaId == null) {
            throw new IllegalArgumentException("Identifiant manquant");
        }
        Reservation res = reservationRepository.findById(resaId)
                .filter(r -> r.getVacationer() != null && userId.equals(r.getVacationer().getId()))
                .orElseThrow(() -> new NotFoundException("Réservation introuvable"));
        if (res.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessRuleException("Cette demande a déjà été envoyée au propriétaire : elle n'est plus modifiable");
        }
        return res;
    }

    private static void checkCapacity(Mobilhome mobilhome, List<OccupantRequestDto> occupants) {
        if (occupants != null && mobilhome.getCapacity() > 0 && occupants.size() > mobilhome.getCapacity()) {
            throw new BusinessRuleException("Trop d'occupants : ce mobil-home accueille "
                    + mobilhome.getCapacity() + " personnes maximum");
        }
    }

    private static void applyDetails(Reservation res, String immat, List<OccupantRequestDto> occupants) {
        res.setImmat(immat);
        if (occupants != null) {
            res.getOccupants().clear();
            occupants.forEach(o -> res.getOccupants().add(Occupant.builder()
                    .firstname(o.firstName())
                    .name(o.lastName())
                    .age(o.age())
                    .reservation(res)
                    .build()));
            res.setNumberOfPerson(occupants.size());
        }
    }
}
