package br.com.motoservices.dao;

import br.com.motoservices.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ClienteDAO extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByCpfCnpj(String cpfCnpj);

    Page<Cliente> findByNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Cliente> findByProximaRevisaoBetween(LocalDate inicio, LocalDate fim, Pageable pageable);
}
