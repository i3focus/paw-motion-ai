package com.pawmotionai.pet.application.usecase;

import com.pawmotionai.pet.domain.exception.InvalidPetException;
import com.pawmotionai.pet.domain.model.PetSize;
import com.pawmotionai.pet.domain.model.PetSpecies;

final class PetParser {

    private PetParser() {
    }

    static PetSpecies parseSpecies(String value) {
        try {
            return PetSpecies.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidPetException("Unsupported pet species: " + value);
        }
    }

    static PetSize parseSize(String value) {
        try {
            return PetSize.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidPetException("Unsupported pet size: " + value);
        }
    }

    static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
