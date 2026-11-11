package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Mobilhome;
import fr.eric.mobilhomy.bo.Owner;

/** Mobil-home géré, en version courte (liste déroulante du formulaire de facture). */
public record ManagerMobilhomeResponseDto(
        Integer id,
        String title,
        String location,
        @JsonProperty("owner_name") String ownerName
) {
    public static ManagerMobilhomeResponseDto fromEntity(Mobilhome m) {
        Owner o = m.getOwner();
        return new ManagerMobilhomeResponseDto(
                m.getId(),
                m.getName(),
                m.getLocation(),
                o != null ? o.getFirstname() + " " + o.getName() : null
        );
    }
}
