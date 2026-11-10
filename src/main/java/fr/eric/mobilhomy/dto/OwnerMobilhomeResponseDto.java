package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import fr.eric.mobilhomy.bo.Image;
import fr.eric.mobilhomy.bo.Manager;
import fr.eric.mobilhomy.bo.Mobilhome;

import java.math.BigDecimal;
import java.util.List;

/** Vue propriétaire d'un mobil-home — noms JSON alignés sur OwnerMobilHome (Angular). */
public record OwnerMobilhomeResponseDto(
        Integer id,
        String title,
        String description,
        String location,
        String address,
        @JsonProperty("postal_code") String postalCode,
        String department,
        int capacity,
        BigDecimal price,
        @JsonProperty("ical_link") String icalLink,
        @JsonProperty("manager_id") Integer managerId,
        @JsonProperty("manager_name") String managerName,
        List<String> images
) {
    public static OwnerMobilhomeResponseDto fromEntity(Mobilhome m) {
        Manager manager = m.getManager();
        return new OwnerMobilhomeResponseDto(
                m.getId(),
                m.getName(),
                m.getDescription(),
                m.getLocation(),
                m.getAddress(),
                m.getPostalCode(),
                departmentOf(m.getPostalCode()),
                m.getCapacity(),
                m.getPrice(),
                m.getIcalLink(),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFirstname() + " " + manager.getName() : null,
                m.getImages() != null ? m.getImages().stream().map(Image::getPath).toList() : List.of()
        );
    }

    /**
     * Déduit le département du code postal :
     * 97xxx / 98xxx → 3 chiffres (outre-mer), 200xx-201xx → 2A, 202xx-206xx → 2B, sinon 2 chiffres.
     */
    public static String departmentOf(String postalCode) {
        if (postalCode == null || postalCode.length() < 2) {
            return null;
        }
        if (postalCode.startsWith("97") || postalCode.startsWith("98")) {
            return postalCode.length() >= 3 ? postalCode.substring(0, 3) : postalCode;
        }
        if (postalCode.startsWith("20") && postalCode.length() >= 3) {
            return postalCode.charAt(2) <= '1' ? "2A" : "2B";
        }
        return postalCode.substring(0, 2);
    }
}
