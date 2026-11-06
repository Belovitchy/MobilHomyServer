package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Auth;
import fr.eric.mobilhomy.bo.Vacationer;
import fr.eric.mobilhomy.dal.AuthRepository;
import fr.eric.mobilhomy.dal.VacationerRepository;
import fr.eric.mobilhomy.dto.VacationerAccountReqDto;
import fr.eric.mobilhomy.dto.VacationerAccountResDto;
import fr.eric.mobilhomy.dto.VacationerMailReqDto;
import fr.eric.mobilhomy.dto.VacationerPassReqDto;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class VacationerServiceImpl implements VacationerService{

    private final VacationerRepository vacationerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthRepository authRepository;

    @Override
    public VacationerAccountResDto getAccount(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vacationer not found with id: " + userId));
        return VacationerAccountResDto.fromEntity(vacationer);
    }

    @Override
    public VacationerAccountResDto updateAccount(Integer userId, VacationerAccountReqDto req) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vacationer not found with id: " + userId));
        if(req.name() != null && req.firstname() != null && req.phone() != null){
            vacationer.setName(req.name());
            vacationer.setFirstname(req.firstname());
            vacationer.setPhone(req.phone());
        }
        Vacationer updated = vacationerRepository.save(vacationer);
        return VacationerAccountResDto.fromEntity(updated);
    }

    @Transactional
    @Override
    public VacationerAccountResDto updateEmail(Integer userId, VacationerMailReqDto req) {
        Vacationer vacationer = getVacationerWithVerifiedPassword(userId, req.currentPassword());
        Auth auth = vacationer.getAuth();
        auth.setLogin(req.newEmail());
        authRepository.save(auth);
        return VacationerAccountResDto.fromEntity(vacationer);
    }

    @Transactional
    @Override
    public VacationerAccountResDto updatePassword(Integer userId, VacationerPassReqDto req) {
        Vacationer vacationer = getVacationerWithVerifiedPassword(userId, req.currentPassword());
        Auth auth = vacationer.getAuth();
        auth.setPassword(passwordEncoder.encode(req.newPassword()));
        authRepository.save(auth);
        return VacationerAccountResDto.fromEntity(vacationer);
    }

    private Vacationer getVacationerWithVerifiedPassword(Integer userId, String currentPassword) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        Vacationer vacationer = vacationerRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Vacationer not found with id: " + userId));
        if (!passwordEncoder.matches(currentPassword, vacationer.getAuth().getPassword())) {
            throw new BadCredentialsException("Mot de passe incorrect");
        }
        return vacationer;
    }
}
