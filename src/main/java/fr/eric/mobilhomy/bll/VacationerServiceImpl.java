package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Vacationer;
import fr.eric.mobilhomy.dal.VacationerRepository;
import fr.eric.mobilhomy.dto.VacationerAccountResDto;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class VacationerServiceImpl implements VacationerService{

    private final VacationerRepository vacationerRepository;

    @Override
    public VacationerAccountResDto getAccount(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vacationer not found with id: " + userId));
        return VacationerAccountResDto.fromEntity(vacationer);
    }
}
