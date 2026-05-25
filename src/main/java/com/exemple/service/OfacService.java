package com.exemple.service;

import com.exemple.repository.OfacRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OfacService {

    private final OfacRepository ofacRepository;

    public void verifier(String nom) {
        if (ofacRepository.existsByNomContainingIgnoreCaseAndActifTrue(nom)) {
            throw new RuntimeException(
                "Transaction bloquée : " + nom + " figure sur la liste OFAC");
        }
    }
}
