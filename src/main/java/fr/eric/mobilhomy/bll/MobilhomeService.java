package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Mobilhome;

import java.util.List;

public interface MobilhomeService {

    List<Mobilhome> findAll();

    List<Mobilhome> search(String department, Integer capacity);

    Mobilhome getById(Integer id);
}
