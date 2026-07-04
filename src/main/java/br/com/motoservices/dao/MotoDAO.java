package br.com.motoservices.dao;

import br.com.motoservices.model.Moto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MotoDAO extends JpaRepository<Moto, Long> {

    Optional<Moto> findByMarcaAndModelo(String marca, String modelo);

    List<Moto> findByCilindradasBetween(int minimo, int maximo);
}
