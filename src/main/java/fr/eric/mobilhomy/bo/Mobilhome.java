package fr.eric.mobilhomy.bo;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Mobilhome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Manager manager;

    private String name;

    private String description;

    private String location;

    private int capacity;

    private String address;

    private String postalCode;

    private BigDecimal price;

    private String icalLink;

    @OneToMany(mappedBy = "mobilhome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    @ToString.Exclude
    private List<Image> images = new ArrayList<>();

    @OneToMany(mappedBy = "mobilhome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    @ToString.Exclude
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "mobilhome", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    @ToString.Exclude
    private List<Contract> contracts = new ArrayList<>();

}
