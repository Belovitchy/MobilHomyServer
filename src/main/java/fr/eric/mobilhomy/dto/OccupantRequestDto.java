package fr.eric.mobilhomy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public record OccupantRequestDto(
        @JsonProperty("firstname")
        @JsonAlias({"firstName", "firstname", "first_name"})
        String firstName,

        @JsonProperty("name")
        @JsonAlias({"lastName", "lastname", "last_name", "name"})
        String lastName,

        int age
) {
}
