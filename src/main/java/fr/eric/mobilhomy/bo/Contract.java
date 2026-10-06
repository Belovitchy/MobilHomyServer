package fr.eric.mobilhomy.bo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacationer_id")
    private Vacationer vacationer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mobilhome_id")
    private Mobilhome mobilhome;

    private boolean signed;

    private String path;
}
