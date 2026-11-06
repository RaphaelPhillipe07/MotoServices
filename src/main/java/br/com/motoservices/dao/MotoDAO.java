package br.com.motoservices.dao;

import br.com.motoservices.model.Moto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MotoDAO extends JpaRepository<Moto, Long> {

    Optional<Moto> findByMarcaIgnoreCaseAndModeloIgnoreCase(String marca, String modelo);

    Page<Moto> findByCilindradasBetween(int minimo, int maximo, Pageable pageable);
}
