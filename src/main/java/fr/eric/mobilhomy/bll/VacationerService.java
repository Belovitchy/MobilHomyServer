package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.dto.VacationerAccountResDto;

public interface VacationerService {

    VacationerAccountResDto getAccount(Integer userId);
}
