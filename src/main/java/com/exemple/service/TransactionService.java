package com.exemple.service;

import com.exemple.dto.request.TransactionRequest;
import com.exemple.dto.response.TransactionResponse;
import com.exemple.entity.Transaction;
import com.exemple.entity.User;
import com.exemple.repository.TransactionRepository;
import com.exemple.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final OfacService ofacService;

    /**
     * TRANSACTION BANCAIRE — règles ACID strictes
     *
     * ATOMIQUE     : débit + crédit ensemble ou rien du tout
     * COHÉRENTE    : solde ne peut pas être négatif
     * ISOLÉE       : SERIALIZABLE empêche deux débits simultanés sur le même compte
     * DURABLE      : commit BDD = définitif, pas d'annulation possible après
     *
     * Pas d'approbation admin — si toutes les conditions sont remplies
     * la transaction s'exécute immédiatement et est IRRÉVERSIBLE
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse initier(TransactionRequest req, User expediteur) {

        // Recharge l'expéditeur depuis la BDD avec lock pour éviter
        // les lectures obsolètes (dirty read)
        expediteur = userRepository.findById(expediteur.getId())
                .orElseThrow(() -> new RuntimeException("Expéditeur introuvable"));

        // 1. Vérification OFAC expéditeur
        ofacService.verifier(expediteur.getUsername());
        ofacService.verifier(expediteur.getEmail());

        // 2. Destinataire existe et est approuvé
        User destinataire = userRepository.findById(req.getDestinataireId())
                .orElseThrow(() -> new RuntimeException("Destinataire introuvable"));

        if (destinataire.getStatut() != User.Statut.APPROVED)
            throw new RuntimeException("Destinataire non autorisé");

        // 3. Vérification OFAC destinataire
        ofacService.verifier(destinataire.getUsername());
        ofacService.verifier(destinataire.getEmail());

        // 4. Pas d'auto-virement
        if (expediteur.getId().equals(destinataire.getId()))
            throw new RuntimeException("Auto-virement interdit");

        // 5. Montant valide
        if (req.getMontant() == null || req.getMontant().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Montant invalide");

        // 6. Solde suffisant — vérifié dans la même transaction DB
        if (expediteur.getSolde().compareTo(req.getMontant()) < 0)
            throw new RuntimeException("Solde insuffisant. Solde actuel : "
                    + expediteur.getSolde());

        // 7. Débit expéditeur — ATOMIQUE avec le crédit
        expediteur.setSolde(expediteur.getSolde().subtract(req.getMontant()));
        userRepository.save(expediteur);

        // 8. Crédit destinataire — même transaction DB
        destinataire.setSolde(destinataire.getSolde().add(req.getMontant()));
        userRepository.save(destinataire);

        // 9. Enregistrement de la transaction — statut COMPLETED immédiat
        Transaction transaction = Transaction.builder()
                .expediteur(expediteur)
                .destinataire(destinataire)
                .montant(req.getMontant())
                .devise(req.getDevise() != null ? req.getDevise() : "MAD")
                .description(req.getDescription())
                .statut(Transaction.Statut.COMPLETED)
                .build();

        transaction = transactionRepository.save(transaction);

        // Si une exception est levée ici ou n'importe où au-dessus,
        // @Transactional annule TOUT — ni le débit ni le crédit ne sont appliqués
        return TransactionResponse.from(transaction);
    }

    // Historique paginé — lecture seule, pas de lock
    @Transactional(readOnly = true)
    public Page<TransactionResponse> historique(User user, Pageable pageable) {
        return transactionRepository
                .findAllByUser(user, pageable)
                .map(TransactionResponse::from);
    }

    // Détail — vérifie que le user est expéditeur ou destinataire
    @Transactional(readOnly = true)
    public TransactionResponse detail(Long id, User user) {
        Transaction t = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction introuvable"));

        boolean concerne = t.getExpediteur().getId().equals(user.getId())
                || t.getDestinataire().getId().equals(user.getId());

        // Admin peut voir toutes les transactions
        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;

        if (!concerne && !isAdmin)
            throw new RuntimeException("Accès refusé");

        return TransactionResponse.from(t);
    }

    // ADMIN — créditer un compte (simule un versement bancaire)
    // Aussi @Transactional SERIALIZABLE — un crédit est aussi une opération financière
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void crediterSolde(Long userId, BigDecimal montant, User admin) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Montant invalide");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        user.setSolde(user.getSolde().add(montant));
        userRepository.save(user);

        // Enregistre aussi le crédit comme transaction pour la traçabilité
        Transaction versement = Transaction.builder()
                .expediteur(admin)   // l'admin est l'expéditeur (la banque)
                .destinataire(user)
                .montant(montant)
                .devise("MAD")
                .description("Versement administratif")
                .statut(Transaction.Statut.COMPLETED)
                .build();

        transactionRepository.save(versement);
    }

    // ADMIN — voir toutes les transactions
    @Transactional(readOnly = true)
    public Page<TransactionResponse> toutesLesTransactions(Pageable pageable) {
        return transactionRepository
                .findAllByOrderByCreatedAtDesc(pageable)
                .map(TransactionResponse::from);
    }
}
