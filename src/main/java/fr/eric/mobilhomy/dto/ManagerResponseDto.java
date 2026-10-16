package fr.eric.mobilhomy.dto;

import fr.eric.mobilhomy.bo.Manager;

public record ManagerResponseDto(
        Integer id,
        String firstname,
        String name,
        String phone,
        String email
) {
    public static ManagerResponseDto fromEntity(Manager manager) {
        if (manager == null) {
            return null;
        }

        String email = manager.getAuth() != null ? manager.getAuth().getLogin() : null;
        return new ManagerResponseDto(
                manager.getId(),
                manager.getFirstname(),
                manager.getName(),
                manager.getPhone(),
                email
        );
    }
}
