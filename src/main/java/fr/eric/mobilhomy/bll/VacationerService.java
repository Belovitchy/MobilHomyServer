package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.dto.VacationerAccountReqDto;
import fr.eric.mobilhomy.dto.VacationerAccountResDto;
import fr.eric.mobilhomy.dto.VacationerMailReqDto;
import fr.eric.mobilhomy.dto.VacationerPassReqDto;
import jakarta.validation.Valid;

public interface VacationerService {

    VacationerAccountResDto getAccount(Integer userId);

    VacationerAccountResDto updateAccount(Integer userId, @Valid VacationerAccountReqDto req);

    VacationerAccountResDto updateEmail(Integer userId, @Valid VacationerMailReqDto req);

    VacationerAccountResDto updatePassword(Integer userId, @Valid VacationerPassReqDto req);
}
