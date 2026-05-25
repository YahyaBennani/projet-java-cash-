package com.exemple.repository;

import com.exemple.entity.Transaction;
import com.exemple.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Toutes les transactions d'un user (envoyées OU reçues)
    @Query("SELECT t FROM Transaction t WHERE t.expediteur = :user OR t.destinataire = :user ORDER BY t.createdAt DESC")
    Page<Transaction> findAllByUser(@Param("user") User user, Pageable pageable);

    // Transactions envoyées par un user
    List<Transaction> findByExpediteurOrderByCreatedAtDesc(User expediteur);

    // Transactions par statut
    List<Transaction> findByStatutOrderByCreatedAtDesc(Transaction.Statut statut);

    // Toutes les transactions (pour admin)
    Page<Transaction> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
