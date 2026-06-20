package com.fluxusbackend.beneficiary.infrastructure.clients.dto;

public record AddressDto(
        Long addressId,
        String street1,
        String street2,
        String city,
        String stateProvince,
        String postalCode,
        CountryDto country
) {
    public record CountryDto(Long countryId, String name) {
    }
}
