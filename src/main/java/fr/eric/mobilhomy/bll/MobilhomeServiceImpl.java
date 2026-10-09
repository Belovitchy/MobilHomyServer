package fr.eric.mobilhomy.bll;

import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.dal.MobilhomeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MobilhomeServiceImpl implements MobilhomeService {

    private final MobilhomeRepository mobilhomeRepository;

    @Override
    public List<Mobilhome> findAll() {
        return mobilhomeRepository.findAll();
    }

    @Override
    public List<Mobilhome> search(String department, Integer capacity) {
       if(department == null && capacity == null){
           return this.findAll();
       }
       if(department == null){
           return mobilhomeRepository.findAllByCapacity(capacity);
       }
       if(capacity == null){
           return mobilhomeRepository.findAllByDepartement(department);
       }
        return mobilhomeRepository.search(department, capacity);
    }

    @Override
    public Mobilhome getById(Integer id) {
        return mobilhomeRepository.findById(id)
                .orElseThrow(()-> new RuntimeException("Mobilhome introuvable avec l'id : " + id));
    }
}
