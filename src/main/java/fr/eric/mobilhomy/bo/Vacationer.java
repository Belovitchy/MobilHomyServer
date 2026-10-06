package fr.eric.mobilhomy.bo;

import jakarta.persistence.Entity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString(callSuper=true)
@Entity
public class Vacationer extends Utilisateur {

    private String phone;
}
